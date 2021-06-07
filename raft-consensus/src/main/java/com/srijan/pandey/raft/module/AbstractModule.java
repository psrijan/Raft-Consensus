package com.srijan.pandey.raft.module;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.ClientRequest;
import com.srijan.pandey.raft.messages.OperationType;
import com.srijan.pandey.raft.state.RaftState;

public abstract class AbstractModule {
    protected Node node; // Actor the receive is tied to to make changes to it's states
    protected RaftState state;
    protected ReceiveBuilder builder;
    protected LoggingAdapter log;

    public AbstractModule(Node node, RaftState raftState) {
        this.node = node;
        this.state = raftState;
        builder = node.receiveBuilder();
        log = Logging.getLogger(node.getContext().getSystem(), this);
    }


    public AbstractModule() {
        builder = ReceiveBuilder.create();
    }

    // If Follower(Si) becomes Candidate(Sj) then candidate will call up preMorph to manage all of
    // the necessary states of the Si lifecycle.
    public abstract void preDestroy();

    // If Follower becomes a Candidate then follower will call this function at the end of its lifecycle to
    // manage state and perform any other management function
    public abstract void postBecome();

    public abstract AbstractActor.Receive getReceive();
}
