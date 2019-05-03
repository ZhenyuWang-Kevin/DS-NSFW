package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Connection implements Runnable {


    private static Logger log = Logger.getLogger(Connection.class.getName());

    private DataInputStream in;
    private DataOutputStream out;
    private Socket aSocket;
    private HostPort peerInfo;
    public static TCPMain TCPmain;
    private ResponseHandler rh;
    private HashMap<String, ByteTransferTask> threadManager;
    private ExecutorService executor;

    public boolean flagActive;

    // Multi-threading tasks
    class ByteTransferTask implements Runnable{
        private ResponseHandler rh;
        public String key;
        private Document doc;
        private long positionTracker = 0;
        private long remainingFileSize;
        public boolean finished = false;
        // task type, 0 for receiving file from peer, 1 for sending to peer
        private int taskType;
        private Connection c;
        private String pathName;

        public ByteTransferTask(String key, long fileSize, int type, ResponseHandler rh, Connection c){
            this.key = key;
            this.doc = null;
            this.remainingFileSize = fileSize;
            taskType = type;
            this.rh = rh;
            this.c = c;
        }

        public void receive(Document d){
            synchronized (this) {
                this.doc = d;
                pathName = this.doc.getString("pathName");
            }
        }

        public void run(){
            while(remainingFileSize > 0){
                synchronized (this) {
                    if (doc != null) {
                        log.info("file size remaining: " + remainingFileSize);
                        if (taskType == 0) {
                            if (doc.getLong("position") == positionTracker) {
                                rh.receivedFileBytesResponse(this.doc);
                            }
                            remainingFileSize -= doc.getLong("length");
                            positionTracker += doc.getLong("length");
                            if (remainingFileSize > 0) {
                                // TODO send next byte request
                                Document fD = (Document) doc.get("fileDescriptor");
                                FileSystemManager.FileDescriptor fDescriptor = ResponseHandler.fManager.new FileDescriptor(fD.getLong("lastModified"), fD.getString("md5"), fD.getLong("fileSize"));
                                c.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDescriptor, doc.getString("pathName"), positionTracker, Integer.parseInt(Configuration.getConfigurationValue("blockSize"))));
                            }
                            doc = null;
                        } else if (taskType == 1) {
                            rh.receivedFileBytesRequest(this.doc);
                            // Only update the remaining fileSize and position Tracker when two peers position are synchronized
                            if (doc.getLong("position") == positionTracker) {
                                remainingFileSize -= doc.getLong("length");
                                positionTracker += doc.getLong("length");
                            }
                            doc = null;
                        }
                    }
                }
            }
            try{
                boolean complete = ResponseHandler.fManager.checkWriteComplete(pathName);
                log.info("(This is debug info only)file write complete " + complete);
            } catch (NoSuchAlgorithmException e) {
                log.warning(e.getMessage());
            } catch (IOException e) {
                log.warning(e.getMessage());
            }
            finished = true;
            c.removeTransferTask(key);
        }
    }

    public void removeTransferTask(String key){
        synchronized (this){
            threadManager.remove(key);
        }
    }


    public void TCPmainPatch(TCPMain m){
        this.TCPmain = m;
    }

    // Main work goes here
    private void receiveCommand(Document json){
        Document fdesc;
        log.info("received command: " + json.getString("command"));
        switch(json.getString("command")){
            case "INVALID_PROTOCOL":
                // TODO disconnect the connection
                closeSocket();
                break;

            case "HANDSHAKE_REQUEST":
                // TODO response with INVALID PROTOCOL, then disconnect the connection
                sendCommand(JsonUtils.INVALID_PROTOCOL("Invalid command!!"));
                closeSocket();
                break;

            case "FILE_CREATE_REQUEST":
                // TODO check for whether file needs transfer
                //  if yes, send a response, then create a new thread
                synchronized (this) {
                    if(rh.receivedFileCreateRequest(json)) {
                        fdesc = (Document) json.get("fileDescriptor");
                        // if there is no thread for this key
                        if (!threadManager.containsKey(fdesc.toJson())) {
                            threadManager.put(fdesc.toJson() + json.getString("namePath"), new ByteTransferTask(fdesc.toJson() + json.getString("namePath"), fdesc.getLong("fileSize"), 0, this.rh, this));
                            executor.execute(threadManager.get(fdesc.toJson()));
                        } else if (threadManager.get(fdesc.toJson()).finished) {
                            threadManager.remove(fdesc.toJson());
                            threadManager.put(fdesc.toJson() + json.getString("namePath"), new ByteTransferTask(fdesc.toJson() + json.getString("namePath"), fdesc.getLong("fileSize"), 0, this.rh, this));
                            executor.execute(threadManager.get(fdesc.toJson()));
                        }
                    }
                }
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
                synchronized (this) {
                    fdesc = (Document) json.get("fileDescriptor");
                    if (threadManager.containsKey(fdesc.toJson())) {
                        ByteTransferTask t = threadManager.get(fdesc.toJson());
                        t.receive(json);
                    }
                }
                break;
            case "FILE_BYTES_RESPONSE":
                synchronized(this) {
                    fdesc = (Document) json.get("fileDescriptor");
                    if (threadManager.containsKey(fdesc.toJson())) {
                        ByteTransferTask t = threadManager.get(fdesc.toJson());
                        t.receive(json);
                    }
                }
                break;
            case "FILE_CREATE_RESPONSE":
                // when receive positive response, create thread to handle file transfer
                synchronized (this) {
                    if (json.getBoolean("status")) {
                        fdesc = (Document) json.get("fileDescriptor");
                        // if there is no thread for this key
                        if (!threadManager.containsKey(fdesc.toJson())) {
                            threadManager.put(fdesc.toJson() + json.getString("namePath"), new ByteTransferTask(fdesc.toJson() + json.getString("namePath"), fdesc.getLong("fileSize"), 1, this.rh, this));
                            executor.execute(threadManager.get(fdesc.toJson()));
                        } else if (threadManager.get(fdesc.toJson()).finished) {
                            threadManager.remove(fdesc.toJson());
                            threadManager.put(fdesc.toJson() + json.getString("namePath"), new ByteTransferTask(fdesc.toJson() + json.getString("namePath"), fdesc.getLong("fileSize"), 1, this.rh, this));
                            executor.execute(threadManager.get(fdesc.toJson()));
                        }
                    }
                    rh.receivedFileCreateResponse(json);
                }
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
                sendCommand(JsonUtils.INVALID_PROTOCOL("Invalid command!!"));
                closeSocket();
                break;
        }
    }

    private void connectionInit(){
        rh = new ResponseHandler(this);
        this.threadManager = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public void run() {
        synchronized (this) {
            log.info("Connection established with " + peerInfo.toString());
            // TODO handles synchronized events
            ArrayList<FileSystemManager.FileSystemEvent> events = ResponseHandler.fManager.generateSyncEvents();

            for (FileSystemManager.FileSystemEvent e : events) {
                switch (e.event) {
                    case FILE_CREATE:
                        sendCommand(JsonUtils.FILE_CREATE_REQUEST(e.fileDescriptor, e.pathName));
                        break;
                    case FILE_DELETE:
                        sendCommand(JsonUtils.FILE_DELETE_REQUEST(e.fileDescriptor, e.pathName));
                        break;
                    case FILE_MODIFY:
                        sendCommand(JsonUtils.FILE_MODIFY_REQUEST(e.fileDescriptor, e.pathName));
                        break;
                    case DIRECTORY_CREATE:
                        sendCommand(JsonUtils.DIRECTORY_CREATE_REQUEST(e.pathName));
                        break;
                    case DIRECTORY_DELETE:
                        sendCommand(JsonUtils.DIRECTORY_DELETE_REQUEST(e.pathName));
                        break;
                }
            }
        }


        // TODO handles the protocol
        while (true){
            synchronized (this) {
                // receive command
                try {

                    String data = in.readUTF();
                    log.info("receiving data: " + data);
                    receiveCommand(JsonUtils.decodeBase64toDocument(data));

                } catch (IOException e) {
                    log.warning(e.getMessage() + this.peerInfo);
                    closeSocket();
                }
            }
        }
    }

    // close the socket
    public void closeSocket(){
        try{
            log.info("Disconnect with " + peerInfo.toString());
            TCPmain.removeConnection(peerInfo.toString());
            if(in != null)
                in.close();
            if(out != null)
                out.close();
            if(aSocket != null)
                aSocket.close();
        }catch(IOException e){
            log.warning(e.getMessage());
        }
    }

    public void sendCommand(String base64Str) {
            try {
                log.info("sending command " + base64Str);
                out.writeUTF(base64Str);
            } catch (IOException e) {
                log.warning(e.getMessage());
            }
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
                        closeSocket();
                    }
                } catch (IOException e) {
                    log.warning(e.getMessage() + " " + peer.toString());
                    closeSocket();
                }
            }
        }
        return true;
    }

    public Connection(HostPort peer){


        connectionInit();
        this.peerInfo = peer;
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
            closeSocket();
        }catch(EOFException e){
            closeSocket();
            log.warning(e.getMessage());
        }catch(IOException e){
            log.warning(e.getMessage() + " " + peer.toString());
            closeSocket();
        }
    }

    // Incoming connection
    public Connection(Socket aSocket){

        connectionInit();

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

                if(!TCPmain.maximumConnectionReached()){
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
            closeSocket();
        }
    }
}