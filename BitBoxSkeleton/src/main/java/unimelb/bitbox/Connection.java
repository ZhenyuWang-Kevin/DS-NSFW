package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Connection implements Runnable {


    private static Logger log = Logger.getLogger(Connection.class.getName());

    private DataInputStream in;
    private DataOutputStream out;
    private Socket aSocket;
    private HostPort peerInfo;
    private TCPMain TCPmain;
    private ResponseHandler rh;
    private boolean readyForBytesRequest;
    private HashMap<String, ByteTransferTask> threadManager;
    private ExecutorService executor;

    public boolean flagActive;

    // Multi-threading tasks
    class ByteTransferTask implements Runnable{
        private ResponseHandler rh;
        public String fDesc;
        private Document doc;
        private long positionTracker = 0;
        private long remainingFileSize;
        public boolean finished = false;
        // task type, 0 for receiving file from peer, 1 for sending to peer
        private int taskType;
        private Connection c;

        public ByteTransferTask(String f, long fileSize, int type, ResponseHandler rh, Connection c){
            this.fDesc = f;
            this.doc = null;
            this.remainingFileSize = fileSize;
            taskType = type;
            this.rh = rh;
            this.c = c;
        }

        public void receive(Document d){
            this.doc = d;
        }

        public void run(){
            while(remainingFileSize > 0){
                if(doc != null) {
                    if(taskType == 0) {
                        if (doc.getLong("position") == positionTracker) {
                            synchronized (this){
                                rh.receivedFileBytesResponse(this.doc);
                            }
                        }
                        remainingFileSize -= doc.getLong("length");
                        positionTracker += doc.getLong("length");
                        if (remainingFileSize > 0) {
                            // TODO send next byte request
                            Document fD = (Document)doc.get("fileDescriptor");
                            FileSystemManager.FileDescriptor fDescriptor= ResponseHandler.fManager.new FileDescriptor(fD.getLong("lastModified"), fD.getString("md5"), fD.getLong("fileSize"));
                            synchronized (this){
                                c.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDescriptor,doc.getString("pathName"), positionTracker, Integer.parseInt(Configuration.getConfigurationValue("blockSize"))));
                            }
                        }
                        doc = null;
                    }
                    else if(taskType == 1){
                        synchronized (this)
                        {
                            rh.receivedFileBytesRequest(this.doc);
                        }
                        // Only update the remaining fileSize and position Tracker when two peers position are synchronized
                        if(doc.getLong("position") == positionTracker){
                            remainingFileSize -= doc.getLong("length");
                            positionTracker += doc.getLong("length");
                        }
                    }
                }
            }
            finished = true;
        }
    }


    // Main work goes here
    private void receiveCommand(Document json){
        Document fdesc;
        switch(json.getString("command")){
            case "INVALID_PROTOCOL":
                // TODO disconnect the connection
                break;

            case "HANDSHAKE_REQUEST":
                // TODO response with INVALID PROTOCOL, then disconnect the connection
                break;

            case "FILE_CREATE_REQUEST":
                // TODO check for whether file needs transfer
                //  if yes, send a response, then create a new thread
                fdesc = (Document)json.get("fileDescriptor");
                // if there is no thread for this key
                if(!threadManager.containsKey(fdesc.toString())){
                    threadManager.put(fdesc.toString(), new ByteTransferTask(fdesc.toString(), fdesc.getLong("fileSize"), 0, this.rh, this));
                    executor.execute(threadManager.get(fdesc.toString()));
                }
                else if(threadManager.get(fdesc.toString()).finished){
                    threadManager.remove(fdesc.toString());
                    threadManager.put(fdesc.toString(), new ByteTransferTask(fdesc.toString(), fdesc.getLong("fileSize"), 0, this.rh, this));
                    executor.execute(threadManager.get(fdesc.toString()));
                }
                rh.receivedFileCreateRequest(json);
                break;

            case "FILE_DELETE_REQUEST":
                rh.receivedFileDeleteRequest(json);
                break;

            case "FILE_MODIFY_REQUEST":
                rh.receivedFileModifyRequest(json);
                break;

            case "DIRECTORY_CREATE_REQUEST":
                rh.receivedDirectoryCreateRequest(json);
                break;

            case "DIRECTORY_DELETE_REQUEST":
                rh.receivedDirectoryDeleteRequest(json);
                break;

            case "FILE_BYTES_REQUEST":
                fdesc = (Document)json.get("fileDescriptor");
                if(threadManager.containsKey(fdesc.toString())){
                    synchronized (this) {
                        ByteTransferTask t = threadManager.get(fdesc.toString());
                        t.receive(json);
                    }
                }
                break;
            case "FILE_BYTES_RESPONSE":
                fdesc = (Document)json.get("fileDescriptor");
                if(threadManager.containsKey(fdesc.toString())){
                    synchronized (this) {
                        ByteTransferTask t = threadManager.get(fdesc.toString());
                        t.receive(json);
                    }
                }

            case "FILE_CREATE_RESPONSE":
                // when receive positive response, create thread to handle file transfer
                if(json.getBoolean("status")){
                    fdesc = (Document)json.get("fileDescriptor");
                    // if there is no thread for this key
                    if(!threadManager.containsKey(fdesc.toString())){
                        threadManager.put(fdesc.toString(), new ByteTransferTask(fdesc.toString(), fdesc.getLong("fileSize"), 1,this.rh, this));
                        executor.execute(threadManager.get(fdesc.toString()));
                    }
                    else if(threadManager.get(fdesc.toString()).finished){
                        threadManager.remove(fdesc.toString());
                        threadManager.put(fdesc.toString(), new ByteTransferTask(fdesc.toString(), fdesc.getLong("fileSize"),1,this.rh, this));
                        executor.execute(threadManager.get(fdesc.toString()));
                    }
                }
                rh.receivedFileCreateResponse(json);
                break;

            case "FILE_DELETE_RESPONSE":
                rh.receivedFileDeleteResponse(json);
                break;

            case "FILE_MODIFY_RESPONSE":
                rh.receivedFileModifyResponse(json);
                break;

            case "DIRECTORY_CREATE_RESPONSE":
                rh.receivedDirectoryCreateResponse(json);
                break;

            case "DIRECTORY_DELETE_RESPONSE":
                rh.receivedDirectoryDeleteResponse(json);
                break;

            default:
                break;
        }
    }

    private void connectionInit(){
        rh = new ResponseHandler(this);
        this.threadManager = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public void run() {
        log.info("Connection established with " + peerInfo.toString());
        // TODO handles the protocol
        while (true){
            // receive command
            try {
                aSocket.setSoTimeout(0);
                String data = in.readUTF();
                receiveCommand(JsonUtils.decodeBase64toDocument(data));
            } catch (IOException e) {
                log.warning(e.getMessage() + this.peerInfo);
            }
        }
    }

    // close the socket
    public void closeSocket(){
        try{
            in.close();
            out.close();
            aSocket.close();
        }catch(IOException e){
            log.warning(e.getMessage());
        }
    }

    public void sendCommand(String base64Str){
        try{
            out.writeUTF(base64Str);
        }catch(IOException e){
            log.warning(e.getMessage());
        }
    }

    // set the flag for byte transfer allowance
    public void setByteRequestAvailability(boolean val){
        this.readyForBytesRequest = val;
    }

    public HostPort getPeerInfo(){
        return peerInfo;
    }

    // when peers reached maximum connection, search for its adjacent peers and trying to connect
    // Huge chunk of private code, don't bother read through it.
    private boolean searchThroughPeers(ArrayList<Document> _peers){
        ArrayList<Document> peers = _peers;

        boolean connectionEstablished = false;

        while(!connectionEstablished){
            if(peers.size() == 0){
                return false;
            }
            HostPort peer = new HostPort(peers.remove(0));

            if(!TCPmain.connectionExist(peer)) {

                try {
                    aSocket = new Socket(peer.host, peer.port);
                    in = new DataInputStream(aSocket.getInputStream());
                    out = new DataOutputStream(aSocket.getOutputStream());

                    // send Handshake request to other peers
                    out.writeUTF(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));

                    String data = in.readUTF();
                    Document d = JsonUtils.decodeBase64toDocument(data);

                    if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {
                        peerInfo = new HostPort((Document) d.get("hostPort"));
                        flagActive = true;
                        connectionEstablished = true;

                        Thread t = new Thread(this);
                        t.start();
                    } else if (d.getString("command").equals("CONNECTION_REFUSED")) {
                        // TODO breath first search for other available peers
                        peers.addAll((ArrayList<Document>) d.get("peers"));
                        in.close();
                        out.close();
                        aSocket.close();
                    }
                } catch (IOException e) {
                    log.warning(e.getMessage() + " " + peer.toString());
                }
            }
        }
        return true;
    }

    public Connection(HostPort peer){


        connectionInit();

        try{
            aSocket = new Socket(peer.host, peer.port);
            in = new DataInputStream(aSocket.getInputStream());
            out = new DataOutputStream(aSocket.getOutputStream());

            // send Handshake request to other peers
            out.writeUTF(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));

            String data = in.readUTF();
            Document d = JsonUtils.decodeBase64toDocument(data);

            if(d.getString("command").equals("HANDSHAKE_RESPONSE")){
                peerInfo = new HostPort((Document) d.get("hostPort"));
                flagActive = true;

                Thread t = new Thread(this);
                t.start();
            }
            else if(d.getString("command").equals("CONNECTION_REFUSED")){
                in.close();
                out.close();
                aSocket.close();
                // TODO breath first search for other available peers
                ArrayList<Document> peers = (ArrayList<Document>)d.get("peers");
                if(!searchThroughPeers(peers)){
                    flagActive = false;
                }
            }
        }catch(UnknownHostException e){
            log.warning(e.getMessage());
        }catch(EOFException e){
            log.warning(e.getMessage());
        }catch(IOException e){
            log.warning(e.getMessage() + " " + peer.toString());
        }
    }

    // Incoming connection
    public Connection(Socket aSocket, TCPMain TCPmain){

        connectionInit();

        this.TCPmain = TCPmain;

        try{
            this.aSocket = aSocket;
            in = new DataInputStream(this.aSocket.getInputStream());
            out = new DataOutputStream(this.aSocket.getOutputStream());

            // TODO decode handshake message
            String data = in.readUTF();
            Document d = JsonUtils.decodeBase64toDocument(data);

            if(d.getString("command").equals("HANDSHAKE_REQUEST")){

                // TODO get peerInfo
                peerInfo = new HostPort((Document) d.get("hostPort"));

                if(!this.TCPmain.maximumConnectionReached()){
                    // available for connection, send HANDSHAKE RESPONSE
                    out.writeUTF(JsonUtils.HANDSHAKE_RESPONSE());

                    flagActive = true;

                    // start the thread for the socket
                    Thread t = new Thread(this);
                    t.start();
                }
                else{
                    // maximum connection reached
                    flagActive = false;
                    // not available, stop the connection
                    out.writeUTF(JsonUtils.CONNECTION_REFUSED(TCPmain, "connection limit reached"));
                    in.close();
                    out.close();
                    aSocket.close();
                }
            }
        } catch(IOException e){
            log.warning(e.getMessage());
        }
    }
}