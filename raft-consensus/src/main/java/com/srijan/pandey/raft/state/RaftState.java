package com.srijan.pandey.raft.state;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import com.srijan.pandey.raft.messages.ClientRequest;
import com.srijan.pandey.raft.messages.ResponseVote;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RaftState {
    private NodeType nodeType = NodeType.Follower;
    private long termNumber; // persistent
    private String votedFor; // persistent
    private long votedTerm; // additional
    private List<LogDetails> log = new ArrayList<>(); // persistent
    private String nodeName;
    private List<ActorRef> actors = new ArrayList<>(); // ActorRef for individual nodes
    private List<String> allNodeNames; // serially ordered list of nodes
    private Cancellable electionTimeout;
    private List<Cancellable> leaderTimeouts;
    private int commitIndex = -1; // Volatile
    private int lastApplied = -1; // Volatile
    private Map<String, ClientRequest> clientRequestMap = new HashMap<>();


    //----- CLIENT APPLIED STATE ------------------------------------

    private Integer ticket = 100000; // initially there are 100000 tickets in the machine

    //---------------------------------------------------------------

    // ----------------- LEADER SPECIFIC -------------------------------
    private Map<String, Integer> nodeNextIndexes = new HashMap<>();
    private Map<String, Integer> nodeMatchIndexes = new HashMap<>();
    //------------------------------------------------------------------
    Map<Boolean, List<ResponseVote>> voteResponseMap; // Key is Granted Field.

    private ActorRef leaderRef;


    public void addToLog(LogDetails logDetails) {
        log.add(logDetails);
    }


    public RaftState() {
    }
}

