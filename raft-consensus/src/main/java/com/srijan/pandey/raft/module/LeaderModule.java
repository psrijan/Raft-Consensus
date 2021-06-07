package com.srijan.pandey.raft.module;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.*;
import com.srijan.pandey.raft.misc.Constants;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.NodeType;
import com.srijan.pandey.raft.state.RaftState;
import com.srijan.pandey.raft.utils.*;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LeaderModule extends AbstractModule {

    public LeaderModule(Node actor) {
        super(actor, actor.getState());
    }

    @Override
    public void preDestroy() {
        StateUtil.manageStateLeaderTermEnd(state);
        NodeUtil.changeBehavior(node, NodeType.Follower);
        FollowerModule followerModule = (FollowerModule) NodeUtil.getModule(node, NodeType.Follower);
        followerModule.postBecome();
    }

    public AbstractActor.Receive getReceive() {
        return builder.match(AppendEntriesResponse.class, this::processAppendEntriesResponse)
                .match(AppendEntries.class, this::processAppendEntries)
                .match(Heartbeat.class, this::processHeartbeat)
                .match(ClientRequest.class, this::processClientRequest).build();
    }

    /**
     * The leader accepts the client message and then adds it to it's own log
     * Commit means when majority of the server has replicated the log.
     * Ideally you cannot send a reply back without committing a log
     * One option would be to store ClientRequest and send it for replication
     * Once majority of the node replicate reply back to the client.
     *
     * There is a small difference in this version of Raft Implementation. Inconsistent reads
     * are not appended to the RAFT client log and immediately returned with a response.
     * @param clientMessage
     */
    public void processClientRequest(ClientRequest clientMessage) {
        System.out.println("LEADER: Received Client Message " + clientMessage.toString());
        //Inconsistent reads need not be persisted in raft logs
        if (clientMessage.getOperation().equals(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY)) {
            int ticket = LogUtil.getTicketAvailability(clientMessage, state);
            ClientResponse clientResponse = MessageUtil.createClientResponse(ticket);
            clientMessage.getClientRef().tell(clientResponse, node.getSelf());
        } else {
            state.getClientRequestMap().put(clientMessage.getRefId(), clientMessage);
            LogDetails logDetails = MessageUtil.createLogDetailsFromClientMessage(clientMessage, state);
            state.addToLog(logDetails); // append entries to the local log
            // While not necessary - this version of raft uses this match node value for validating commits later on
            Map<String, Integer> matchIndexMap = state.getNodeMatchIndexes();
            matchIndexMap.put(state.getNodeName(), state.getLog().size() - 1);  // leader will match all the values so it is fair to say it's size - 1
            sendAppendEntriesRpc();
        }
    }

    /**
     * Once a client message is received and is added to the log it is sent out to all other servers
     * for replication. Once this replication process is complete, identified by majority of AppendEntriesResponse
     * succeeded, then commit the message to the state machine.
     */
    public void sendAppendEntriesRpc() {
        for (int i = 0; i < state.getAllNodeNames().size(); i++) {
            if (state.getAllNodeNames().get(i).equalsIgnoreCase(state.getNodeName())) // don't send append entries to self
                continue;
            String nodeName = state.getAllNodeNames().get(i);
            ActorRef actorRef = state.getActors().get(i);
            AppendEntries appendEntries = createNewAppendEntry(state, node, nodeName);
            actorRef.tell(appendEntries, node.getSelf());
        }
    }

    /**
     * Only to be called by leader
     * @param state
     * @return
     */
    public static AppendEntries createNewAppendEntry(RaftState state, Node node, String nodeName) {
        int nextLogIndex = state.getNodeNextIndexes().getOrDefault(nodeName, 0);
        nextLogIndex = Math.max(nextLogIndex, 0); // sometimes there is a weird issue where the value goes less than -1
        int prevLogIndex = nextLogIndex - 1;
        AppendEntries appendEntries = new AppendEntries();
        appendEntries.setEntries(new ArrayList<>());
        appendEntries.setTermNumber(state.getTermNumber());
        appendEntries.setLeaderRef(node.getSelf());
        appendEntries.setLeaderName(state.getNodeName());
        appendEntries.setPrevLogIndex(prevLogIndex);
        appendEntries.setLeaderCommit(state.getCommitIndex());

        if (prevLogIndex > 0 && prevLogIndex < state.getLog().size()) {// check for indexes
            appendEntries.setPrevLogTerm(state.getLog().get(prevLogIndex).getTerm());
        }

        int adjustedPrevLogIndex = Math.max(prevLogIndex, 0); // in the case where there is no previous log index
        List<LogDetails> entrySuffix = state.getLog().subList(adjustedPrevLogIndex, state.getLog().size());
        List<LogDetails> appendEntryLog = appendEntries.getEntries();
        for (LogDetails log: entrySuffix) {
            appendEntryLog.add(log);
        }

        if (appendEntries.getEntries() == null)  // a defensive check so that no append entry contains null value and causes follower to fail.
            appendEntries.setEntries(new ArrayList<>());
        System.out.println("LEADER: SENDING APPEND ENTRY:" + appendEntries.toString());
        return appendEntries;
    }

    public void processAppendEntriesResponse(AppendEntriesResponse appendEntriesResponse) {
        System.out.println("LEADER: " + state.getNodeName() + " Receiving Append Entries Response: " + appendEntriesResponse.toString());
        boolean isStateUpdated = StateUtil.updateTermNumber(appendEntriesResponse.getTermNumber(), state);
        if (isStateUpdated) { // higher term append entries response than the server itself
            preDestroy();
            return;
        }

        if (appendEntriesResponse.isSuccess()) {
            LogUtil.incrementNextLogEntryIndex(appendEntriesResponse.getNodeName(), state);
        } else { // In case append entries fails
            /* don't send append entries response to self this is a defensive check as I got responses from the same node during
             some time during my implmentation **/
            if (appendEntriesResponse.getNodeName().equalsIgnoreCase(state.getNodeName()))
                return;
            LogUtil.decrementNextLogEntryIndex(appendEntriesResponse.getNodeName(), state);
            AppendEntries appendEntries = MessageUtil.createNewAppendEntry(state, node, appendEntriesResponse.getNodeName());
            // Leader retries with a decremented append index
            System.out.println("LEADER " + state.getNodeName() + " Retring: " + appendEntriesResponse.getNodeName() + " Entries: " + appendEntries.toString());
            ActorRef retryRef = appendEntriesResponse.getSentBy();
            // Delayed Sends to ensure that AppendEntriesResponse.failure don't generate massive message loads in the system due consistent pinging FOLLOWER <-> LEADER
            node.getContext().system().scheduler().scheduleOnce(Duration.create(Constants.TimingIntervals.DELAYED_SENDS, TimeUnit.MILLISECONDS),
                    () -> {
                        retryRef.tell(appendEntries, node.getSelf());
                    }, node.getContext().getDispatcher());
        }
        validateAndCommit();
    }

    /**
     * Validates the states to see if there are any logs that can be committed and written to disk.
     */
    public void validateAndCommit() {
        // check for N
        int maxMatchIndex = state.getLog().size();

        /*
         * This piece of code tries to check the number of nodes which has a given matchIndex greater than the current committed
         * value and tries to commit that value.
         */
        for (int N = state.getCommitIndex() + 1; N < maxMatchIndex; N++) {
            Map<String, Integer> matchIndexMap = state.getNodeMatchIndexes();
            // validate and count nodes that have the match index is greater than N
            final int NN = N; // final value required for stream
            int countGreaterThanN = (int) matchIndexMap.values().stream().filter(matchIndex -> matchIndex >= NN).count();

            // Greater than majority and log(N) is in the current term
            if (countGreaterThanN > state.getAllNodeNames().size() / 2  && state.getLog().get(N).getTerm() == state.getTermNumber()) {
                System.out.println("LEADER: Majority of Nodes Replicated State - COMMITTING - APPLYING " );
                state.setCommitIndex(N);
                StateUtil.applyClientOperation(state, state.getLog().get(N).getCommand()); // apply operation to the state machine
                node.getFileUtil().writeToFile(state,  N, N +1, state.getTicket()); // write from N to N + 1 (Current commit index)
                state.setLastApplied(N);
                sendClientResponse(N);
            }

            NodeUtil.checkAndApplyOperation(state); // All Server Requirement to apply committed values
        }
    }

    public void sendClientResponse(int lastCommittedOperationIndex) {
        ClientResponse  response = MessageUtil.clientResponse(state);
        ClientRequest request = state.getClientRequestMap().get(state.getLog().get(lastCommittedOperationIndex).getRefId());
        request.getClientRef().tell(response, node.self()); // sending notification to client by picking from client map
        //state.getClientRequestMap().remove(request.getRefId()); // no more required and can be removed now as txn processed
    }

    public void processAppendEntries(AppendEntries appendEntries) {
        boolean termUpdated = StateUtil.updateTermNumber(appendEntries.getTermNumber(), state);
        if (termUpdated) { // higher term server sending heartbeats move back to follower state
            preDestroy();
            AppendEntriesResponse appendEntriesResponse = MessageUtil.creteAppendEntryResponse(state, node, true);
            appendEntries.getLeaderRef().tell(appendEntriesResponse, null); // send the message to the leader
        } else {
            if (appendEntries.getLeaderName().equalsIgnoreCase(state.getNodeName())) // if in case message from self then don't redirect
                return;
            AppendEntriesResponse appendEntriesResponse = MessageUtil.creteAppendEntryResponse(state, node, false); // will not be vacating leader position
            appendEntries.getLeaderRef().tell(appendEntriesResponse, null); // send the message to the leader
            // when this leader sends response to the new leader, it should effectively remove the new leader.
        }
    }

    public void processHeartbeat(Heartbeat heartbeat) {
        boolean termUpdated = StateUtil.updateTermNumber(heartbeat.getTermNumber(), state);
        if (termUpdated) {
            NodeUtil.changeBehavior(node, NodeType.Follower);
            FollowerModule followerModule = (FollowerModule) NodeUtil.getModule(node, NodeType.Follower);
            StateUtil.manageStateLeaderTermEnd(state);
            followerModule.postBecome();
        }
    }

    /**
     *
     */
    public void postBecome() {
        System.out.println("LEADER: " + state.getNodeName() + "IS A LEADER NOW!!!");
        // Send append entries to all other entries stating that I am the leader
        StateUtil.manageStateLeaderTermStart(state);
        List<Cancellable> leaderTimeouts = new ArrayList<>();
        /*
         * Sends Heartbeat append entries to all the servers
         * Starts a scheduler to send out messages
         */
        for (int i = 0; i < state.getAllNodeNames().size(); i++) {
            if (state.getAllNodeNames().get(i).equals(state.getNodeName())) // Do not send append entries to self
                continue;
            Heartbeat heartbeat= MessageUtil.getHeartbeat(state , state.getAllNodeNames().get(i), node);
            ActorRef toActorRef = state.getActors().get(i);
            state.getActors().get(i).tell(heartbeat, node.getSelf()); // initial heartbeats
            Cancellable cancellable = NodeUtil.createLeaderHeartbeatTimeout(node, toActorRef, heartbeat); // followup heartbeats
            leaderTimeouts.add(cancellable);
        }
        state.setLeaderTimeouts(leaderTimeouts);
    }

}
