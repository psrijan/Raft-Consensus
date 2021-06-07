package com.srijan.pandey.raft.module;

import akka.actor.AbstractActor;
import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.*;
import com.srijan.pandey.raft.misc.Constants;
import com.srijan.pandey.raft.state.NodeType;
import com.srijan.pandey.raft.utils.*;

import java.util.List;

public class CandidateModule extends AbstractModule {

    public CandidateModule(Node actor) {
        super(actor, actor.getState());
    }

    /**
     * This preDestroy Indicates the Node Going to FOLLLOWER NODE
     */
    @Override
    public void preDestroy() {
        NodeUtil.changeBehavior(node, NodeType.Follower);
        NodeUtil.getModule(node, NodeType.Follower).postBecome();
        StateUtil.manageStateCandidateOutOfTerm(state);
    }

    @Override
    public void postBecome() {

    }

    public AbstractActor.Receive getReceive() {
        return builder.match(RequestVote.class, this::processRequestVote)
                .match(Timeout.class, this::processTimeout)
                .match(AppendEntries.class, this::processAppendEntries)
                .match(ResponseVote.class, this::processResponseVote)
                .match(ClientRequest.class, this::processClientRequest)
                .match(Heartbeat.class, this::processHeartbeat).build();
    }


    /**
     * If a client sends a request to this node which is a candidate then there are two possible scenarios
     * First one is when a client has requested a read of the tickets, in this scenario the client doesn't
     * send this response to the leader as this is an inconsistent read. The second scenario is when the
     * client requests a purchase request, this needs to be a consistent read and must be redirected to the
     * leader instead.
     * redirected to the leader, which will process this information.
     * @param clientRequest
     */
    public void processClientRequest(ClientRequest clientRequest ) {
        System.out.println("CANDIDATE: Received Client REQUEST: " +  clientRequest.toString());

        // this is a Local Node operation as the client will not mind an inconsistent read operation
        if (clientRequest.getOperation().equals(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY)) {
            int ticketCount = LogUtil.getTicketAvailability(clientRequest, state);
            ClientResponse clientResponse = MessageUtil.createClientResponse(ticketCount);
            clientRequest.getClientRef().tell(clientResponse, node.getSelf());
        } else {
            // Client Message gets redirected to the Leader
            // ideally candidate should not be find a leader, but there might be some scenarios where the leaderRef might
            // not have been unset.
            if (state.getLeaderRef() != null) {
                System.out.println("CANDIDATE: " + state.getNodeName() + " Redirecting Client Request to Leader");
                state.getLeaderRef().tell(clientRequest, node.getSelf());
            } else {
                System.out.println("CANDIDATE: " + state.getNodeName() + " No Leader Found! Returning Response to client ");
                ClientResponse clientResponse = MessageUtil.createClientFailResponseForNoLeader();
                clientRequest.getClientRef().tell(clientResponse, node.getSelf());
            }
        }
    }

    /**
     * An append entry to a Candidate indicates that there is already a leader which is serving the current cluster. Now this
     * candidate can turn back into a follower and start replicating the commands sent by clients.
     * @param appendEntries
     */
    public void processAppendEntries(AppendEntries appendEntries) {
        System.out.println("CANDIDATE: " + state.getNodeName() + "Received Append Entries from Leader - Becoming Follower ");
        StateUtil.updateTermNumber(appendEntries.getTermNumber(), state);
        state.getElectionTimeout().cancel(); // cancelling current election timeout
        NodeUtil.createElectionTimeout(node);
        state.setLeaderRef(appendEntries.getLeaderRef());
        NodeUtil.changeBehavior(node, NodeType.Follower);
        // I think there is no need to send back a RPC response, just moving to a follower state is sufficient here
//        AppendEntriesResponse appendEntriesResponse = new AppendEntriesResponse();
//        appendEntriesResponse.setSuccess(true);
//        appendEntriesResponse.setTermNumber(state.getTermNumber());
//        appendEntriesResponse.setSentBy(node.getSelf());
//        appendEntries.getLeaderRef().tell(appendEntriesResponse, node.getSelf());
    }

    public void processHeartbeat(Heartbeat heartbeat) {
        System.out.println("FOLLOWER: " + state.getNodeName() + " Heartbeat " );
        boolean isUpdated = StateUtil.updateTermNumber(heartbeat.getTermNumber(), state);
        // ---- Create new Election Timeout ----
        StateUtil.manageStateCandidateOutOfTerm(state);
        NodeUtil.createElectionTimeout(node);
        NodeUtil.changeBehavior(node, NodeType.Follower);
        state.setLeaderRef(heartbeat.getLeaderRef());
    }

    /**
     * This indicates that the timeout for the current node has expired and
     * will start a new election term
     * @param timeout
     */
    public void processTimeout(Timeout timeout) {
        StateUtil.manageStateCandidateTermStart(state); // increments term | clears vote map | votes for self
        NodeUtil.createElectionTimeout(node); // create a new timer with randomized seed value
        ModuleUtil.sendRequestVote(state, node);
    }

    public void processResponseVote(ResponseVote responseVote) {
        log.debug(state.getNodeName() + "Receving Vote Response" + responseVote.toString());
        System.out.println(state.getNodeName() + "Receiving Vote Response" + responseVote.toString());
        boolean isUpdated = StateUtil.updateTermNumber(responseVote.getTerm(), state);

        // return to follower mode higher term RPC
        if (isUpdated) {
            preDestroy();
        }

        boolean isVoteGranted = responseVote.isVoteGranted();
        List<ResponseVote> curRespList = state.getVoteResponseMap().get(isVoteGranted);
        curRespList.add(responseVote);
        System.out.println("CANDIDATE: " + state.getNodeName() + " HAS: " + state.getVoteResponseMap().get(true).size() + " VOTES");
        // the candidate that received majority of the votes switch to a Leader
        if (state.getVoteResponseMap().get(Constants.Misc.VOTE_GRANTED).size() > state.getActors().size() / 2) {
            System.out.println("CANDIDATE: " + state.getNodeName() + "exceeded VOTE threshold" + " <<LEADER>>");
            state.getElectionTimeout().cancel();
            NodeUtil.changeBehavior(node, NodeType.Leader);
            LeaderModule leaderModule = (LeaderModule) NodeUtil.getModule(node, NodeType.Leader);
            leaderModule.postBecome();
        }
    }

    public void postBecomingCandidate() {
        System.out.printf("CANDIDATE: Node %s has become a candidate \n", state.getNodeName());
        StateUtil.manageStateCandidateTermStart(state);
        ModuleUtil.sendRequestVote(state, node);
        NodeUtil.createElectionTimeout(node); // create a election timeout
    }

    /**
     * If a Candidate receives a request vote message than the candidate denies the voteGrant
     * @param requestVote
     */
    public void processRequestVote(RequestVote requestVote) {
        boolean isTermUpdated = StateUtil.updateTermNumber(requestVote.getTermNumber(), state);
        ResponseVote responseVote = new ResponseVote();
        responseVote.setVoteGranted(false);
        responseVote.setTermNumber(state.getTermNumber());
        requestVote.getSentBy().tell(responseVote, null);

        if (isTermUpdated) {
           preDestroy();
        }
    }
}
