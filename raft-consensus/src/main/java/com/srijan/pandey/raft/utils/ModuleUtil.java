package com.srijan.pandey.raft.utils;

import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.ClientRequest;
import com.srijan.pandey.raft.messages.OperationType;
import com.srijan.pandey.raft.messages.RequestVote;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.RaftState;

import java.util.List;
import java.util.stream.IntStream;

public class ModuleUtil {
    /**
     * Once a Follower becomes a candidate it request votes to all the available
     * Nodes in the cluster
     */
    public static void sendRequestVote(RaftState state, Node node) {
        System.out.println("CANDIDATE: " + state.getNodeName() + " Sending Request Vote: ");
        RequestVote requestVote = new RequestVote(state.getNodeName(), node.getSelf(), state.getTermNumber());

        try {
            IntStream.range(0, state.getActors().size()).forEach(i -> {
                if (!state.getLog().isEmpty()) {
                    String nodeName = state.getNodeName();
                    int nextLogIndex = state.getNodeNextIndexes().getOrDefault(nodeName, 0);
                    int lastLogIndex = nextLogIndex - 1;
                    if (lastLogIndex >= 0) { // keeping an additional check for index
                        LogDetails logDetails = state.getLog().get(lastLogIndex);
                        requestVote.setLastLogTerm(logDetails.getTerm());
                        requestVote.setLastLogIndex(lastLogIndex);
                    }
                }
                System.out.println("NODE: " + state.getNodeName() + " Sending Request Vote to " + state.getAllNodeNames().get(i));
                state.getActors().get(i).tell(requestVote, node.getSelf());
            });
        } catch (Exception ex) {
            System.out.println("<ERROR> NO ACTOR FOUND");
            ex.printStackTrace();
        }
    }

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
}
