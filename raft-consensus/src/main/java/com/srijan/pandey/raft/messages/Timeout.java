package com.srijan.pandey.raft.messages;

import com.srijan.pandey.raft.state.NodeType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Timeout extends BaseMessage {
    private String nodeName;
    private NodeType nodeType;

    public Timeout(String nodeName, NodeType nodeType) {
        this.nodeName = nodeName;
        this.nodeType = nodeType;
    }
}
