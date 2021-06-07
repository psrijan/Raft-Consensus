package com.srijan.pandey.raft.utils;

import com.srijan.pandey.raft.messages.OperationType;
import com.srijan.pandey.raft.state.LogDetails;
import com.srijan.pandey.raft.state.RaftState;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtil {

    private String baseFilePath;
    private String nodeName;

    public String getFilePath() {
        return baseFilePath + nodeName;
    }

    public static void main(String[] args) {
        FileUtil fileUtil = new FileUtil("/home/srijan/raftdata/", "testnode");
        RaftState raftState = new RaftState();
        List<LogDetails>ld = new ArrayList<>();
        LogDetails lg1 = new LogDetails();
        lg1.setTerm(1);
        lg1.setCommand(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        lg1.setRefId("REF1");
        ld.add(lg1);
        raftState.setLog(ld);
        fileUtil.writeToFile(raftState, 0 , 1, 1);
        fileUtil.writeToFile(raftState, 0, 1, 1);
    }

    public FileUtil(String baseFile, String nodeName) {
       this.baseFilePath = baseFile;
       this.nodeName = nodeName;

       try {
            File file = new File(getFilePath());
            if (file.exists())
                file.delete();
            boolean created = file.createNewFile();
            System.out.println("Created New File - STATUS:" + created);
       } catch (IOException ex ) {
           ex.printStackTrace();
       }
    }

    /**
     * Read all the logs from the file for restart
     * @param nodeName
     * @return
     */
    public List<LogDetails> readFile(String nodeName) {
        List<LogDetails> logDetailsList = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(baseFilePath+ nodeName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                String[] vals = line.split("|");
                LogDetails logDetails = new LogDetails();
                logDetails.setCommand(OperationType.valueOf(vals[0]));
                logDetails.setTerm(Integer.parseInt(vals[1]));
                logDetailsList.add(logDetails);
            }
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logDetailsList;
    }

    /**
     * Writes the log values from start to end index to the file
     */
    public void writeToFile(RaftState raftState, int startIndex, int endIndex, int res) {
        System.out.println("NODE: " + nodeName + "Writing to File");
        List<LogDetails> logDetails = raftState.getLog();
        try {
            FileWriter fileWriter = new FileWriter(baseFilePath + nodeName, true);
            for (int i = startIndex; i < endIndex; i++) {
                LogDetails ld = logDetails.get(i);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                String command = ld.getCommand().toString();
                String term = String.valueOf(ld.getTerm());
                String refId = ld.getRefId();
                String toFile = command+"|"+term+"|"+refId+ "|" + res +"\n";
                System.out.println("NODE DATA TO FILE: " + toFile);
                bufferedWriter.write(toFile);
                bufferedWriter.flush();
            }
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    public static List<String> readOperations(String fileName) {
        return Arrays.asList("ADD", "ADD", "SUBTRACT");
    }

//    public List<String> readOperations1(String fileName) {
//        List<String> operations = new ArrayList<>();
//        URL is = getClass().getResource(fileName);
//        URI uri = new URI();
//        File file  = new File(is);
//        try {
//            FileReader fileReader = new FileReader(is);
//            BufferedReader bufferedReader = new BufferedReader(fileReader);
//            String line = null;
//
//            while ((line = bufferedReader.readLine()) != null) {
//                operations.add(line);
//            }
//        } catch (FileNotFoundException fileNotFoundException) {
//            fileNotFoundException.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return operations;
//    }
}
