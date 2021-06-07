package com.srijan.pandey.raft;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.srijan.pandey.raft.module.AbstractModule;
import com.srijan.pandey.raft.module.CandidateModule;
import com.srijan.pandey.raft.module.FollowerModule;
import com.srijan.pandey.raft.module.LeaderModule;
import com.srijan.pandey.raft.state.NodeType;
import com.srijan.pandey.raft.state.RaftState;
import com.srijan.pandey.raft.utils.FileUtil;
import com.srijan.pandey.raft.utils.NodeUtil;
import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Node extends AbstractActor {

    private RaftState state;
    private Cancellable timeoutScheduler = null;
    private Map<NodeType, AbstractModule> moduleMap;
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private FileUtil fileUtil;
    public Node() {
        state = new RaftState();
        state.setNodeName(self().path().name());
        AbstractModule leaderModule = new LeaderModule(this);
        AbstractModule followerModule = new FollowerModule(this);
        AbstractModule candiateModule = new CandidateModule(this);

        // Basically sets Leader/Candidate/Follower functions in their respective modules
        // which can be queried using the NodeType avaialble in a Nodes State.
        moduleMap = new HashMap<>();
        moduleMap.put(NodeType.Candidate, candiateModule);
        moduleMap.put(NodeType.Leader, leaderModule);
        moduleMap.put(NodeType.Follower, followerModule);

        // scheduling timeouts for nodes
        NodeUtil.createElectionTimeout(this);
    }

    @Override
    public Receive createReceive() {
        return moduleMap.get(state.getNodeType()).getReceive(); // Default state is set as Follower in RaftState class.
    }

}
