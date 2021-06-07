package com.srijan.pandey.raft.utils;

import akka.actor.ActorRef;
import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.AppendEntries;
import com.srijan.pandey.raft.messages.AppendEntriesResponse;
import com.srijan.pandey.raft.messages.Heartbeat;
import com.srijan.pandey.raft.messages.Timeout;
import com.srijan.pandey.raft.misc.Constants;
import com.srijan.pandey.raft.module.AbstractModule;
import com.srijan.pandey.raft.state.NodeType;
import com.srijan.pandey.raft.state.RaftState;
import scala.concurrent.duration.Duration;
import akka.actor.Cancellable;

import java.util.concurrent.TimeUnit;

public class NodeUtil {

    public static AbstractModule getModule(Node actor, NodeType nodeType) {
        return actor.getModuleMap().get(nodeType);
    }


    public static void changeBehavior(Node node, NodeType nodeType) {
        node.getContext().become(NodeUtil.getModule(node, nodeType).getReceive()); // changing behavior
    }

    public static void cancelElectionTimeout(Node node) {
        node.getState().getElectionTimeout().cancel();
    }
    /**
     * @param node
     */
    public static void createElectionTimeout(Node node) {
        Timeout timeoutMessage = new Timeout(node.getState().getNodeName(), node.getState().getNodeType());
        Cancellable cancellable = node.getContext().system().scheduler().scheduleOnce(
                Duration.create(getRandomizedElectionTime(Constants.TimingIntervals.ELECTION_TIMEOUT_MIN, Constants.TimingIntervals.ELECTION_TIMEOUT_MAX), TimeUnit.MILLISECONDS)
                , node.getSelf(), timeoutMessage, node.getContext().getSystem().dispatcher(), node.getSelf());
        node.getState().setElectionTimeout(cancellable);
    }

    /**
     * These are very specific heartbeat messages that ensure that the Followers don't
     * timeout. Heartbeats don't have specific other purpose than this in this implementation
     * of raft
     */
    public static Cancellable createLeaderHeartbeatTimeout(Node leader , ActorRef sendTo, Heartbeat heartbeat) {
        return leader.getContext().getSystem().scheduler().scheduleAtFixedRate(Duration.create(Constants.TimingIntervals.SCHEDULER_DELAY, TimeUnit.MILLISECONDS),
                Duration.create(getRandomizedElectionTime(Constants.TimingIntervals.LEADER_HEARTBEAT_MIN, Constants.TimingIntervals.LEADER_HEARTBEAT_MAX), TimeUnit.MILLISECONDS), () -> {
                    sendTo.tell(heartbeat, leader.getSelf());
                }, leader.getContext().getSystem().dispatcher());
    }

    public static void checkAndApplyOperation(RaftState raftState) {
        int lastApplied = 0;
        while (raftState.getCommitIndex() > raftState.getLastApplied()) {
            lastApplied = raftState.getLastApplied();
            lastApplied++;
            StateUtil.applyClientOperation(raftState, raftState.getLog().get(lastApplied).getCommand());
            raftState.setLastApplied(lastApplied);
        }
    }

    public static void sendNegativeAppendEntryResponse(RaftState state, AppendEntries appendEntries, Node node) {
        System.out.println("FOLLOWER: Log index dont match ");
        AppendEntriesResponse appendEntriesResponse = MessageUtil.creteAppendEntryResponse(state, node, false);
        // Delayed Sends to ensure messages don't slow down the system
        node.getContext().system().scheduler().scheduleOnce(Duration.create(Constants.TimingIntervals.DELAYED_SENDS, TimeUnit.MILLISECONDS),
                appendEntries.getLeaderRef(),
                appendEntriesResponse, node.getContext().getDispatcher(), node.getSelf());
    }

    public static void sendPositiveAppendEntryResponse(RaftState state, AppendEntries appendEntries, Node node) {
        AppendEntriesResponse appendEntriesResponse = MessageUtil.creteAppendEntryResponse(state, node, true);
        // Delayed Sends
        node.getContext().system().scheduler().scheduleOnce(Duration.create(Constants.TimingIntervals.DELAYED_SENDS, TimeUnit.MILLISECONDS),
                appendEntries.getLeaderRef(),
                appendEntriesResponse, node.getContext().getDispatcher(), node.getSelf());
        //appendEntries.getLeaderRef().tell(appendEntriesResponse, null);

    }

    private static int getRandomizedElectionTime(int min, int max) {
        return (int)(Math.random() * (max - min) + min);
    }


    public static void clearTimeout(Node node) {
        node.getState().getElectionTimeout().cancel();
    }
}
