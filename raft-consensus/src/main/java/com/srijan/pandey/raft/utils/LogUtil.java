package com.srijan.pandey.raft.utils;

import com.srijan.pandey.raft.messages.AppendEntriesResponse;
import com.srijan.pandey.raft.messages.ClientRequest;
import com.srijan.pandey.raft.messages.OperationType;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.RaftState;

import java.util.List;
import java.util.Map;

/**
 * This is a class for Raft log specific updates that a node makes
 */
public class LogUtil {


    /**
     * This function gives quick but inconsistent reads of ticket information from a nodes client applied state
     * @param clientRequest
     * @param state
     * @return
     */
    public static int getTicketAvailability(ClientRequest clientRequest, RaftState state) {
        // Apply all operations from the commit index to the size of the log and return the output
        List<LogDetails> logDetailList = state.getLog();
        int commitIndex = state.getCommitIndex();
        int ticketCount = state.getTicket();

        for (int i = commitIndex + 1; i < logDetailList.size(); i++) {
            LogDetails logDetails = logDetailList.get(i);
            OperationType operationType = logDetails.getCommand();
            // you dont have to worry about OperationType.CHECK_TICKET_AVAILABILITY as this doesn't change the value of
            // the ticket information
            if(operationType.equals(OperationType.PURCHASE)) {
                ticketCount--;
            }
        }
        return ticketCount;
    }

    public static void incrementNextLogEntryIndex(String nodeName, RaftState raftState) {
        Map<String, Integer> logNextIndexMap = raftState.getNodeNextIndexes();
        int size = raftState.getLog().size();
        logNextIndexMap.put(nodeName, size); // Because we take all entries from the last dissimilar one and apply it we take the whole size here
        Map<String, Integer> matchIndexMap = raftState.getNodeMatchIndexes();
        matchIndexMap.put(nodeName, size); // matchIndex Identifies the
    }

    /**
     * This happens incase we get a NAK from the follower or other nodes while log replication
     */
    public static void decrementNextLogEntryIndex(String nodeName, RaftState raftState) {
        Map<String, Integer> logNextIndexMap = raftState.getNodeNextIndexes();
        Integer index = logNextIndexMap.getOrDefault(nodeName, 0);
        logNextIndexMap.put(nodeName, --index);
    }
}
