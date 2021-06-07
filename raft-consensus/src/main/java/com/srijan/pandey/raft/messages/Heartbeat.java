package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Heartbeat extends BaseMessage {
    private ActorRef leaderRef;
    private String leaderName;
}
