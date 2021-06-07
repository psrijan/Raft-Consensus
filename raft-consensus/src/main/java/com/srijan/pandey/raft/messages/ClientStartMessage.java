package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
@Getter
@Setter
public class ClientStartMessage extends StartMessage{
    private List<OperationType> commands;
    private ActorRef primaryNode;
    private String primaryNodeName;
    private String clientName;
    private ActorSystem actorSystem;

    public ClientStartMessage(List<OperationType> commands , List<ActorRef> nodes, List<String> nodeName) {
        super(nodes, nodeName, null);
        this.commands = commands;
    }

}
