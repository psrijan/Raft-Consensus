package com.srijan.pandey.raft;


import akka.actor.Props;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.srijan.pandey.raft.messages.ClientStartMessage;
import com.srijan.pandey.raft.messages.OperationType;
import com.srijan.pandey.raft.messages.StartMessage;
import com.srijan.pandey.raft.stub.ClientOperationStubUtil;

import java.io.File;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

public class Main {

    // Please create a folder named raftdata to store your data in a particular path WITH A / IN THE END
    private static final String BASE_FILE_PATH = "/home/srijan/raftdata/";

    public static void main(String[] args) {
        int NO_OF_ACTOR = 5;
        boolean redirectLogToFile = false;

        System.out.println("Creating Actor System");
        setLogPath(BASE_FILE_PATH, redirectLogToFile);

        final ActorSystem system = ActorSystem.create("MySystem");
        List<ActorRef> clusterNodeList = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (int i = 0; i < NO_OF_ACTOR; i++) {
            final ActorRef myActor = system.actorOf(Props.create(Node.class), "node-" + i);
            names.add("node-" + i);
            clusterNodeList.add(myActor);
        }

        final StartMessage startMessage = new StartMessage(clusterNodeList, names, BASE_FILE_PATH);
        clusterNodeList.forEach(actorRef -> {
            System.out.println("Main Sending Start Message ");
            actorRef.tell(startMessage, null);
        });

        List<ActorRef> clientNodes = new ArrayList<>();
        IntStream.range(0, names.size()).forEach(i -> {
            ActorRef primaryNode = clusterNodeList.get(i);
            String primaryNodeName = names.get(i);
            List<OperationType> clientCommand = ClientOperationStubUtil.getClientBasedOperation(i);
            ClientStartMessage clientStartMessage = new ClientStartMessage(clientCommand, clusterNodeList, names);
            clientStartMessage.setActorSystem(system); // used for querying cluster nodes.
            clientStartMessage.setPrimaryNode(primaryNode);
            clientStartMessage.setPrimaryNodeName(primaryNodeName);
            clientStartMessage.setClientName("client-"+i);
            ActorRef clientRef = system.actorOf(Props.create(ClientNode.class), "client-"+ i);
            clientRef.tell(clientStartMessage, null);
            clientNodes.add(clientRef);
        });
    }

    /**
     * The version of Raft utilizes souts to print it's log information
     * @param filePath
     */
    public static void setLogPath(String filePath, boolean addToFile) {
        if (!addToFile)
            return;

        DateFormat dateFormat = new SimpleDateFormat("_dd-MM-yyyy_hh-mm");
        String curDate = dateFormat.format(new Date());
        filePath = filePath + "raft" + curDate + ".log";
        try {
            System.out.println("Logs Redirected to " + filePath);
            System.out.println("Set redirectLogToFile = false to see console logs...");
            File file = new File(filePath);
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception ex ) {
            ex.printStackTrace();
            System.out.println("ERROR: Unable to redirect stream to file");
        }
    }

}
