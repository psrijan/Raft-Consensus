package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class RequestVote extends BaseMessage {
    private String candidateId;
    private long lastLogIndex;
    private Long lastLogTerm;
    private ActorRef sentBy;

    public RequestVote() {

    }

    public RequestVote(String candidateId, ActorRef sentBy, long termNumber) {
        this.candidateId = candidateId;
        this.sentBy = sentBy;
        super.setTermNumber(termNumber);
    }

}
