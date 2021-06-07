package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class AppendEntriesResponse extends BaseMessage{
    private boolean success;
    private ActorRef sentBy;
    private String nodeName;
}
