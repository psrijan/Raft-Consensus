package com.srijan.pandey.raft.module;

import akka.actor.*;
import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.*;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.NodeType;
import com.srijan.pandey.raft.utils.*;

import java.util.List;
import java.util.stream.Collectors;

public class FollowerModule extends AbstractModule {

    public FollowerModule(Node node ) {
        super(node, node.getState());
    }

    @Override
    public void preDestroy() {

    }

    @Override
    public void postBecome() {
        state.setNodeType(NodeType.Follower);

    }

    public AbstractActor.Receive getReceive() {
        return builder.match(StartMessage.class, this::processStartMessage)
                .match(AppendEntries.class, this::processAppendEntries)
                .match(Timeout.class, this::processTimeoutMessage)
                .match(ClientRequest.class, this::processClientRequest )
                .match(RequestVote.class, this::processRequestVote)
                .match(Heartbeat.class, this::processHeartbeat).build();
    }
    /**
     * If a client sends a request to this node which is a candidate then there are two possible scenarios
     * First one is when a client has requested a read of the tickets, in this scenario the client doesn't
     * send this response to the leader as this is an inconsistent read. The second scenario is when the
     * client requests a purchase request, this needs to be a consistent read and must be redirected to the
     * leader instead.
     * @todo check to see if we can redirect the message to leader or ask the client to
     * redirected to the leader, which will process this information.
     * @param clientRequest
     */
    public void processClientRequest(ClientRequest clientRequest) {
        System.out.println("FOLLOWER: Received Client REQUEST: " +  clientRequest.toString());
        // this is a Local Node operation as the client will not mind an inconsistent read operation
        if (clientRequest.getOperation().equals(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY)) {
            int ticketCount = LogUtil.getTicketAvailability(clientRequest, state);
            ClientResponse clientResponse = MessageUtil.createClientResponse(ticketCount);
            clientRequest.getClientRef().tell(clientResponse, node.getSelf());
        } else {
            // Client Message gets redirected to the Leader
            if (state.getLeaderRef() != null && clientRequest.getParams() == null) {
                clientRequest.setParams(state.getNodeName());
                System.out.println("FOLLOWER: " + state.getNodeName() + " Redirecting Client Request to Leader");
                state.getLeaderRef().tell(clientRequest, node.getSelf());
            } else {
                System.out.println("FOLLOWER: " + state.getNodeName() + " No Leader Found! Returning Response to client ");
                ClientResponse clientResponse = MessageUtil.createClientFailResponseForNoLeader();
                clientRequest.getClientRef().tell(clientResponse, node.getSelf());
            }
        }
    }

    /**
     * This function takes in a vote request and responds with
     * @param requestVote
     */
    private void processRequestVote(RequestVote requestVote) {
        // Term of the Candidate smaller than the current term of the
        // node. Deny Vote
//        System.out.println("FOLLOWER: " + state.getNodeName() + " Receiving Request Vote: " + requestVote.toString());
        boolean isUpdated = StateUtil.updateTermNumber(requestVote.getTermNumber(), state); // update the term number if new term
        if (isUpdated) { // if is updated then the follower has not voted for this term
            state.setVotedFor(null);
        }

        NodeUtil.cancelElectionTimeout(node); // cancel election timeout and start new one.
        NodeUtil.createElectionTimeout(node);

        if (requestVote.getTermNumber() < node.getState().getTermNumber() || state.getVotedFor() != null
                || state.getVotedTerm() >= requestVote.getTermNumber() || !StateUtil.isCandidateLogUpToDate(state, requestVote)) {
//            System.out.println("FOLLOWER: " + state.getNodeName() + " DENYING request Vote");
            ResponseVote responseVote = new ResponseVote();
            responseVote.setTermNumber(state.getTermNumber());
            responseVote.setVoteGranted(false);
            requestVote.getSentBy().tell(responseVote, node.getContext().getSelf());
        } else {
//            System.out.println("FOLLOWER: " + state.getNodeName() + " GRANTING request Vote");
            ResponseVote responseVote = new ResponseVote();
            responseVote.setTermNumber(state.getTermNumber());
            responseVote.setVoteGranted(true);
            requestVote.getSentBy().tell(responseVote, node.getContext().getSelf());
        }
    }
    /**
     * Adds all the raft node message to the central states of the user.
     * @param startMessage
     */
    public void processStartMessage(StartMessage startMessage) {
        System.out.println("FOLLOWER: " + startMessage.getNames() + "Receiving Start Message");
        state.setActors(startMessage.getNodes());
        state.setAllNodeNames(startMessage.getNames());
        NodeUtil.createElectionTimeout(node);
        node.setFileUtil(new FileUtil(startMessage.getBaseFilePath(), state.getNodeName())); // initializing file util
    }

    public void processTimeoutMessage(Timeout timeoutMessage) {
        log.debug("FOLLOWER: {} Timer Elapsed...", state.getNodeName());
        System.out.println("FOLLOWER: " + state.getNodeName() + " has timed out " );
        // Timeout message from self change to a candidate
        StateUtil.incrementTerm(state);
        state.setLeaderRef(null);
        node.getContext().become(NodeUtil.getModule(node, NodeType.Candidate).getReceive()); // changing behavior to Candidate
        NodeUtil.changeBehavior(node, NodeType.Candidate);
        state.getElectionTimeout().cancel();
        CandidateModule candidateModule = (CandidateModule) NodeUtil.getModule(node, NodeType.Candidate);
        candidateModule.postBecomingCandidate();
    }

    public void processHeartbeat(Heartbeat heartbeat) {
//        System.out.println("FOLLOWER: " + state.getNodeName() + " Heartbeat " );

        // ---- Create new Election Timeout ----
        NodeUtil.cancelElectionTimeout(node);
        NodeUtil.createElectionTimeout(node);
        state.setLeaderRef(heartbeat.getLeaderRef());
        boolean isUpdated = StateUtil.updateTermNumber(heartbeat.getTermNumber(), state);
    }

    /**
     * Term < currentTerm reply false
     * @param appendEntries
     */
    public void processAppendEntries(AppendEntries appendEntries) {
        System.out.println("FOLLOWER: " + state.getNodeName() + " Entries: " + appendEntries.toString() + " from: " + appendEntries.getLeaderName());
        System.out.println("FOLLOWER: " + state.getNodeName() + " State Log: "  +
                state.getLog().stream().map(logDetails ->  logDetails.toString()).collect(Collectors.joining("-", "{", "}")));
        // -----  Update Term Number --------
        boolean isUpdated = StateUtil.updateTermNumber(appendEntries.getTermNumber(), state);

        if (StateUtil.entryTermLessThanStateTerm(appendEntries, state)) {
            System.out.println("FOLLOWER: Term Less than LEADER");
            NodeUtil.sendNegativeAppendEntryResponse(state, appendEntries, node);
            return;
        }

        if (StateUtil.entriesConflict(state, appendEntries)) {
            System.out.println("FOLLOWER: Term conflict in state and appendentries");
            NodeUtil.sendNegativeAppendEntryResponse(state, appendEntries, node);
            return;
        }

        List<LogDetails> stateLogEntries = state.getLog();
        List<LogDetails> appendEntriesLog = appendEntries.getEntries();
        int prevLogIndex = appendEntries.getPrevLogIndex();

        /**
         * Adding Logs to Follower LogEntries
         */
        int adjustedPrevLog = Math.max(prevLogIndex, 0); // if less than 0 then adjust it to 0 for copying append entries.
        System.out.println("ADJUSTED LOG INDEX: " + state.getNodeName() +  " " + adjustedPrevLog);
        System.out.println("ADJUSTED + APPENDENTRIESLOG" + (adjustedPrevLog + appendEntriesLog.size()));
        System.out.println("APPEND ENTRIES: " + appendEntries.toString());
        for (int i = adjustedPrevLog; i <  adjustedPrevLog + appendEntriesLog.size(); i++) {
            // if pointer less than stateLogEntries size replace the values in stateLog, these are conflicting entries
            if (i <  stateLogEntries.size()) {
                stateLogEntries.add(i, appendEntriesLog.get(i - adjustedPrevLog));
            } else {
                stateLogEntries.add(appendEntriesLog.get(i - adjustedPrevLog));
            }
        }

        validateAndApplyCommits(appendEntries);
        // Commits are durable
        NodeUtil.checkAndApplyOperation(state); // the paper mentions a need for this check in all nodes.
        NodeUtil.sendPositiveAppendEntryResponse(state,appendEntries, node);
    }

    private void validateAndApplyCommits(AppendEntries appendEntries) {
        if (appendEntries.getLeaderCommit() > state.getCommitIndex()) {
            // This condition makes sense, regardless of how much info a Follower replicates, it can only commit upto how much the leader has committed
            int curCommitIndex = Math.min(appendEntries.getLeaderCommit(), state.getLog().size() - 1); // second is index of last new entry
            int prevCommitIndex = state.getCommitIndex();
            // First paragraph of page 311 of Raft tells how clients should commit
            for (int i = prevCommitIndex + 1; i <= curCommitIndex; i++) {
                StateUtil.applyClientOperation(state, state.getLog().get(i).getCommand()); // Apply Client Operation to State Machine See: Rule For Server -> All Server-> Point 1
                node.getFileUtil().writeToFile(state, i, i + 1, state.getTicket()); // write one at a time
            }
            state.setCommitIndex(curCommitIndex);
            state.setLastApplied(curCommitIndex);
        }
    }
}
