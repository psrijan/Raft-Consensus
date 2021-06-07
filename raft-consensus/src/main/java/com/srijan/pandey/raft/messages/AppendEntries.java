package com.srijan.pandey.raft.messages;

import akka.actor.ActorRef;
import com.srijan.pandey.raft.state.LogDetails;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
public class AppendEntries extends BaseMessage implements Serializable {
    private String leaderName;
    private ActorRef leaderRef;
    private int prevLogIndex;
    private long prevLogTerm;
    private List<LogDetails> entries = new ArrayList<>();
    private int leaderCommit;
}
