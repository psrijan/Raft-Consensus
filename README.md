# RAFT-CONSENSUS: 
## Overview  
1. Raft is a distributed consensus algorithm that enables nodes in a cluster to self organize and work together regardless of indeterministic conditions in distributed computing: such as network partitions, node failures. 
1. Raft uses the concept of distributed log replication, to agree upon the work,  each raft node has committed in it's distributed logs. This is done via three types of raft cluster nodes (LEADER, FOLLOWER and CANDIDATE) nodes. 
1. Raft's node population, like humans, elect LEADERs which instruct other nodes on operations to perform in the nodes local state. Elections are successful when there is more than 50% node that vote for a particular CANDIDATE node, which wants to be a leader. This is called consensus in raft. 
1. In addition to this, Raft also needs to have distributed consensus when performing tasks. Each Raft node replicates the requests client sends to the leader. This is replication is one reason why RAFT is so tolerant on failures. 
1. RAFT also has a mechanism to commit logs. When RAFT leader identifies over 50% of the nodes have replicated the log, the leader commits the RAFT log and requests FOLLOWERS to do the same. This is yet another form of consensus seen in RAFT. 

## Implementation Details: 
This version of RAFT is implemented in AKKA. AKKA is a library for building concurrent and distributed systems in Java, Scala .Net. In this version of RAFT, I implement a 5 node distributed RAFT node, that maintains ticket information across different devices. In addition to this, the RAFT cluster also maintains notions of CONSISTENT and INCONSISTENT reads. CONSISTENT reads are accurate and exact reads in the RAFT system. However, obtaining consistent reads in distributed computing can sometimes be expensive in terms of time and compute resources. To handle these trade offs, the version also implements INCONSISTENT reads, which enables raft nodes to estimate the number of remaining TICKETS which is currently available in the system. Such mixed consistency modesl are important use cases for distributed computing. For instance, it is likely that systems such as AMAZONs shooping carts show tentative number of items avaiable in their warehouse rather than displaying their exact amount.

[Video Link - Raft Explaination and Implementation](https://drexel0-my.sharepoint.com/:v:/r/personal/sp3557_drexel_edu/Documents/raft/raft_final-2021-05-12_14.36.00.mkv?csf=1&web=1&e=33cObZ)


