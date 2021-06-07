package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StartMessage extends BaseMessage {
    private List<ActorRef> nodes;
    private List<String> names;
    private String baseFilePath;
    public StartMessage(List<ActorRef> nodes, List<String> names ,String baseFilePath)  {
        this.nodes = nodes;
        this.names = names;
        this.baseFilePath = baseFilePath;
    }

    public StartMessage() {

    }
}
