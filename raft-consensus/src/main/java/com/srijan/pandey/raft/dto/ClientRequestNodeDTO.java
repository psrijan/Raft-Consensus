package com.srijan.pandey.raft.dto;

import akka.actor.ActorRef;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientRequestNodeDTO {
    private ActorRef actorRef;
    private boolean isPrimary;
}
