package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ClientRequest {
    private String refId; // uniquely identifies each client operation in the raft network
    private OperationType operation;
    private ActorRef clientRef;
    private String params;


    public ClientRequest(String refId, OperationType operationType, ActorRef clientRef) {
        this.refId = refId;
        this.operation = operationType;
        this.clientRef= clientRef;
    }
}
