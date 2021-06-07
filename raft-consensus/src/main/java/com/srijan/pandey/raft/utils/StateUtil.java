package com.srijan.pandey.raft.utils;

import com.srijan.pandey.raft.messages.AppendEntries;
import com.srijan.pandey.raft.messages.OperationType;
import com.srijan.pandey.raft.messages.RequestVote;
import com.srijan.pandey.raft.messages.ResponseVote;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.NodeType;
import com.srijan.pandey.raft.state.RaftState;
import akka.actor.Cancellable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateUtil {

    /**
     * Updates The term number of the Server
     * @param termNumber new term number
     * @param raftState raft state to get the term number of the server
     * @return Returns true if there is an update in the term number
     */
    public static boolean updateTermNumber(long termNumber, RaftState raftState) {
        if (raftState.getTermNumber() < termNumber) {
            raftState.setTermNumber(termNumber);
            return true;
        }
        return false;
    }

    public static void incrementTerm(RaftState raftState) {
        long termNumber = raftState.getTermNumber();
        raftState.setTermNumber(++termNumber);
    }

    public static void voteForSelf(RaftState raftState) {
        raftState.setVotedFor(raftState.getNodeName());
        raftState.setVotedTerm(raftState.getTermNumber());
    }

    public static void manageStateCandidateOutOfTerm(RaftState state) {
        state.setNodeType(NodeType.Follower);
        state.setVotedFor(null);
        state.getVoteResponseMap().put(true, new ArrayList<>());
        state.getVoteResponseMap().put(false, new ArrayList<>());
        state.getElectionTimeout().cancel();
    }

    public static void manageStateCandidateTermStart(RaftState raftState) {
        raftState.setNodeType(NodeType.Candidate);
        incrementTerm(raftState);
        voteForSelf(raftState);

        Map<Boolean, List<ResponseVote>> voteResponseMap = new HashMap<>();
        List<ResponseVote> granted = new ArrayList<>();
        List<ResponseVote> notGranted = new ArrayList<>();
        voteResponseMap.put(true, granted);
        voteResponseMap.put(false, notGranted);
        granted.add(new ResponseVote()); // response vote from self later used as a check for identifying leader
        raftState.setVoteResponseMap(voteResponseMap); // clear the hashmap for new canddiate term
    }

    /**
     * Handles most state related changes when transitioning to a leader state
     *  - Clears the vote
     *  - Sets initial commit index
     *  - Sets initial last applied index
     *  - term number hack
     *  - nextIndex and matchIndex
     * @param raftState
     */
    public static void manageStateLeaderTermStart(RaftState raftState) {
        Map<Boolean, List<ResponseVote>> voteResponseMap = new HashMap<>();
        List<ResponseVote> granted = new ArrayList<>(); // clears out the vote response map
        List<ResponseVote> notGranted = new ArrayList<>();
        voteResponseMap.put(true, granted);
        voteResponseMap.put(false, notGranted);
        raftState.setVoteResponseMap(voteResponseMap); // clear the hashmap for new canddiate term
        raftState.setNodeType(NodeType.Leader);
        raftState.setTermNumber(raftState.getTermNumber() + 5);
        raftState.setCommitIndex(-1); // when leader initialized leader commit index set to 0 and monotonically increases
        raftState.setLastApplied(-1);
        Map<String, Integer> nextIndices = raftState.getNodeNextIndexes();
        for (String key : raftState.getAllNodeNames()) {
            nextIndices.put(key, raftState.getLog().size()); // Initially all the nodes have the indices equal to leaders last log index = size().
        }

        for (String key : raftState.getAllNodeNames()) {
            raftState.getNodeMatchIndexes().put(key, 0); // initialized to zero and monotonically increases in value
        }
    }

    public static void manageStateLeaderTermEnd(RaftState raftState) {
        List<Cancellable> cancellables = raftState.getLeaderTimeouts();
        if (cancellables != null) {
            cancellables.forEach(Cancellable::cancel);
        }
        cancellables = new ArrayList<>();
        raftState.setLeaderTimeouts(cancellables);
        raftState.setCommitIndex(-1);
        raftState.setNodeType(NodeType.Follower);
    }

    // Candidates log is atleast as uptodate as receivers log
    public static boolean isCandidateLogUpToDate(RaftState raftState, RequestVote requestVote) {
        return requestVote.getLastLogIndex() >= raftState.getLog().size() - 1;
    }

//    public static boolean containsPreviousLogIndex(RaftState raftState, AppendEntries appendEntries) {
//        List<LogDetails> logDetails = appendEntries.getEntries();
//
////        if (raftState.getLog() == null || raftState.getLog().size() == 0)
////            return true; // returns true as there is no log and there is no need for the comparision
//
//        if (appendEntries.getPrevLogIndex() < 0 || appendEntries.getPrevLogIndex() >= appendEntries.getEntries().size()) // index out of bounds
//            return false;
//
//        if (logDetails.get(appendEntries.getPrevLogIndex()).getTerm() != appendEntries.getPrevLogTerm()) // term doesn't match
//            return false;
//
//        return true;
//    }

    public static boolean entryTermLessThanStateTerm(AppendEntries appendEntries, RaftState raftState) {
        return appendEntries.getTermNumber() < raftState.getTermNumber();
    }

    /**
     * I think entries conflict if we have previousLogIndex value greater than 0. Which means that there is
     * already something written in the log we need to look out for.
     * @param raftState
     * @param appendEntries
     * @return
     */
    public static boolean entriesConflict(RaftState raftState, AppendEntries appendEntries) {
        List<LogDetails> stateLogDetails = raftState.getLog();
        int prevLogIndex = appendEntries.getPrevLogIndex();

        if (prevLogIndex < 0) // there is no log written in the follower before so no conflict
            return false;

        if (stateLogDetails.isEmpty())
            return false;

        LogDetails statePrevLog= stateLogDetails.get(prevLogIndex);
        if (statePrevLog.getTerm() != appendEntries.getPrevLogTerm()) // Entries conflict
            return true;
        return false;
    }


    /**
     * Encoding of state machine
     * @param state
     * @param operationType
     */
    public static void applyClientOperation(RaftState state , OperationType operationType) {
        int counter = state.getTicket();

        switch (operationType) {
            case INCONSISTENT_CHECK_TICKET_AVAILABILITY:
                // no action done in this case for the client state ideally just return a value back to the client
                break;
            case PURCHASE:
                state.setTicket(--counter);
                break;
            default:
                //do nothing
        }
    }
}
