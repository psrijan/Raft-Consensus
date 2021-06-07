package com.srijan.pandey.raft.utils;

import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.*;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.RaftState;

import java.util.ArrayList;
import java.util.List;

public class MessageUtil {
    public static AppendEntriesResponse creteAppendEntryResponse(RaftState state, Node node, boolean success) {
        AppendEntriesResponse appendEntriesResponse = new AppendEntriesResponse();
        appendEntriesResponse.setSuccess(success);
        appendEntriesResponse.setTermNumber(state.getTermNumber());
        appendEntriesResponse.setSentBy(node.getSelf());
        appendEntriesResponse.setNodeName(state.getNodeName());
        return appendEntriesResponse;
    }

    /**
     * Only difference between the heartbeat and appendentries is that HeartBeat doesn't
     * have any entries in it's values thus causing no additional action to be induced in the
     * receviver section.
     */
    public static Heartbeat getHeartbeat(RaftState raftState, String nodeName, Node leader) {
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setTermNumber(raftState.getTermNumber());
        heartbeat.setLeaderName(raftState.getNodeName());
        heartbeat.setLeaderRef(leader.getSelf());
        return heartbeat;
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
        appendEntries.setEntries(entrySuffix);

        if (appendEntries.getEntries() == null)  // a defensive check so that no append entry contains null value and causes follower to fail.
            appendEntries.setEntries(new ArrayList<>());
        System.out.println("LEADER: SENDING APPEND ENTRY:" + appendEntries.toString());
        return appendEntries;
    }

    public static ClientResponse createClientResponse(int inconsitentTicketcount) {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setTicketNo(inconsitentTicketcount);
        clientResponse.setMessage("<INCONSISTENT_READS>");
        clientResponse.setRequestSuccess(true);
        return clientResponse;
    }

    public static ClientResponse createClientFailResponseForNoLeader() {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setRequestSuccess(false);
        clientResponse.setMessage(" No elected Leader at the moment to server your request ! Please Retry Later!!");
        return clientResponse;
    }

    public static ClientResponse clientResponse(RaftState state) {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setTicketNo(state.getTicket());
        clientResponse.setMessage("<CONSISTENT_SUCCESS>");
        return clientResponse;
    }


    public static LogDetails createLogDetailsFromClientMessage(ClientRequest clientMessage, RaftState state) {
        LogDetails logDetails = new LogDetails();
        logDetails.setRefId(clientMessage.getRefId());
        logDetails.setCommand(clientMessage.getOperation());
        logDetails.setTerm(state.getTermNumber());
        return logDetails;
    }
}
