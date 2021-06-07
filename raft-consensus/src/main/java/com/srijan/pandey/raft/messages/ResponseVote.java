package com.srijan.pandey.raft.messages;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ResponseVote extends BaseMessage{
    private long term;
    private boolean voteGranted;
}
