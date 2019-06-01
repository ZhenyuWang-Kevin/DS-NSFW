package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Connection implements Runnable {


    private static Logger log = Logger.getLogger(Connection.class.getName());

    private BufferedReader in;
    private BufferedWriter out;
    private Socket TCPSocket;
    private HostPort peerInfo;
    private int peerPort;
    public static TCPMain TCPmain;
    public static UDPMain UDPmain;
    private ResponseHandler rh;
    private HashMap<String, ByteTransferTask> threadManager;
    private ExecutorService executor;
    private DatagramSocket UDPSocket;
    private String mode;
    private InetAddress address;
    public static int blockSize;

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
        private long timeOutThreshold;
        private long timer;

        public ByteTransferTask(String key, long fileSize, int type, ResponseHandler rh, Connection c, String pathName){
            this.key = key;
            this.doc = null;
            this.remainingFileSize = fileSize;
            this.pathName = pathName;
            taskType = type;
            this.rh = rh;
            this.c = c;
            this.timeOutThreshold = 3000*1000;
            this.timer = 0;
        }

        public void receive(Document d){
            synchronized (this) {
                this.doc = d;
            }
        }

        public void run(){
            // finish until file transfer complete or reach time out
            while(remainingFileSize > 0){
                long start = System.currentTimeMillis();
                synchronized (this) {
                    if (doc != null) {
                        timer = 0;
                        log.info("file size remaining: " + remainingFileSize);
                        if (taskType == 0) {
                            if (doc.getLong("position") == positionTracker) {
                                rh.receivedFileBytesResponse(this.doc);
                            }
                            remainingFileSize -= doc.getLong("length");
                            positionTracker += doc.getLong("length");
                            if (remainingFileSize > 0) {
                                // TODO send next byte request
                                int size = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
                                Document fD = (Document) doc.get("fileDescriptor");
                                FileSystemManager.FileDescriptor fDescriptor = ResponseHandler.fManager.new FileDescriptor(fD.getLong("lastModified"), fD.getString("md5"), fD.getLong("fileSize"));
                                c.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDescriptor, doc.getString("pathName"), positionTracker, remainingFileSize >= size ? size : remainingFileSize ));
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
                this.timer += System.currentTimeMillis() - start;
                if(this.timer > this.timeOutThreshold){
                    log.warning("File transfer timeout: " + pathName);
                    try {
                        ResponseHandler.fManager.cancelFileLoader(pathName);
                    }catch(IOException e){
                        log.warning(e.getMessage());
                    }
                    break;
                }
            }

            // after transfer complete, check for completion
            try{
                boolean complete = ResponseHandler.fManager.checkWriteComplete(pathName);
                log.info(pathName + " write completion: " + complete);
                if(!complete && taskType == 0){
                    ResponseHandler.fManager.cancelFileLoader(pathName);
                }
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
        threadManager.remove(key);
    }

    public void TCPmainPatch(TCPMain m){
        TCPmain = m;
    }
    public void UDPmainPatch(UDPMain m){
        UDPmain = m;
    }

    // Main work goes here
    public void receiveCommand(Document json){
        Document fdesc;
        log.info("received command: " + json.getString("command"));
        switch(json.getString("command")){
            case "INVALID_PROTOCOL":
                // TODO disconnect the connection
                disconnect();
                break;

            case "HANDSHAKE_REQUEST":
                // TODO response with INVALID PROTOCOL, then disconnect the connection
                sendCommand(JsonUtils.INVALID_PROTOCOL("Invalid command!!"));
                disconnect();
                break;

            case "FILE_CREATE_REQUEST":
                // TODO check for whether file needs transfer
                //  if yes, send a response, then create a new thread
                synchronized (this) {
                    if(rh.receivedFileCreateRequest(json)) {
                        fdesc = (Document) json.get("fileDescriptor");
                        // if there is no thread for this key
                        if (!threadManager.containsKey(fdesc.toJson() + json.getString("pathName"))) {
                            threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 0, this.rh, this, json.getString("pathName")));
                            executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                        } else if (threadManager.get(fdesc.toJson()+ json.getString("pathName")).finished) {
                            threadManager.remove(fdesc.toJson() + json.getString("pathName"));
                            threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 0, this.rh, this, json.getString("pathName")));
                            executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                        }
                    }
                }
                break;

            case "FILE_DELETE_REQUEST":
                rh.receivedFileDeleteRequest(json);
                break;

            case "FILE_MODIFY_REQUEST":
                synchronized (this) {
                    if(rh.receivedFileModifyRequest(json)) {
                        fdesc = (Document) json.get("fileDescriptor");
                        // if there is no thread for this key
                        if (!threadManager.containsKey(fdesc.toJson() + json.getString("pathName"))) {
                            threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 0, this.rh, this, json.getString("pathName")));
                            executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                        } else if (threadManager.get(fdesc.toJson()+ json.getString("pathName")).finished) {
                            threadManager.remove(fdesc.toJson() + json.getString("pathName"));
                            threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 0, this.rh, this, json.getString("pathName")));
                            executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                        }
                    }
                }
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
                    if (threadManager.containsKey(fdesc.toJson() + json.getString("pathName"))) {
                        ByteTransferTask t = threadManager.get(fdesc.toJson() + json.getString("pathName"));
                        t.receive(json);
                    }
                }
                break;
            case "FILE_BYTES_RESPONSE":
                synchronized(this) {
                    fdesc = (Document) json.get("fileDescriptor");
                    if (threadManager.containsKey(fdesc.toJson() + json.getString("pathName"))) {
                        ByteTransferTask t = threadManager.get(fdesc.toJson() + json.getString("pathName"));
                        t.receive(json);
                    }
                }
                break;
            case "FILE_CREATE_RESPONSE":
                // when receive positive response, create thread to handle file transfer
                if(rh.receivedFileCreateResponse(json)) {
                    synchronized (this) {
                        if (json.getBoolean("status")) {
                            fdesc = (Document) json.get("fileDescriptor");
                            // if there is no thread for this key
                            if (!threadManager.containsKey(fdesc.toJson() + json.getString("pathName"))) {
                                threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 1, this.rh, this, json.getString("pathName")));
                                executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                            } else if (threadManager.get(fdesc.toJson() + json.getString("pathName")).finished) {
                                threadManager.remove(fdesc.toJson() + json.getString("pathName"));
                                threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 1, this.rh, this, json.getString("pathName")));
                                executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                            }
                        }

                    }
                }
                break;

            case "FILE_DELETE_RESPONSE":
                rh.receivedFileDeleteResponse(json);
                break;

            case "FILE_MODIFY_RESPONSE":
                if(rh.receivedFileModifyResponse(json)) {
                    synchronized (this) {
                        if (json.getBoolean("status")) {
                            fdesc = (Document) json.get("fileDescriptor");
                            // if there is no thread for this key
                            if (!threadManager.containsKey(fdesc.toJson() + json.getString("pathName"))) {
                                threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 1, this.rh, this, json.getString("pathName")));
                                executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                            } else if (threadManager.get(fdesc.toJson() + json.getString("pathName")).finished) {
                                threadManager.remove(fdesc.toJson() + json.getString("pathName"));
                                threadManager.put(fdesc.toJson() + json.getString("pathName"), new ByteTransferTask(fdesc.toJson() + json.getString("pathName"), fdesc.getLong("fileSize"), 1, this.rh, this, json.getString("pathName")));
                                executor.execute(threadManager.get(fdesc.toJson() + json.getString("pathName")));
                            }
                        }


                    }
                }

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
        peerPort = -1;
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
        while (flagActive){
            synchronized (this) {
                // receive command
                if (mode.equals("tcp")) {
                    try {

                        String data = in.readLine();
                        if (data == null) {
                            log.info("disconnect with " + peerInfo.toString());
                            closeSocket();
                            flagActive = false;
                        } else {
                            log.info("receiving data: " + data);
                            receiveCommand(Document.parse(data));
                        }

                    } catch (IOException e) {
                        log.warning(e.getMessage() + this.peerInfo);
                        closeSocket();

                    }
                } else {
                    try{
                        byte[] buffer = new byte[blockSize];
                        DatagramPacket buf = new DatagramPacket(buffer, buffer.length);
                        UDPSocket.receive(buf);
                        InetAddress incomeAddr = InetAddress.getByName(buf.getAddress().getHostAddress());
                        if(!incomeAddr.equals(InetAddress.getByName(peerInfo.host))){
                            log.warning("un-match ip address detected, possible port hijacking. Ignore incoming message");
                        } else {
                            String data = new String(buf.getData(), 0, buf.getLength());
                            if (data.equals("")) {
                                log.info("disconnect with " + peerInfo.toString());
                                flagActive = false;
                            } else {
                                log.info("receiving data: " + data);
                                receiveCommand(Document.parse(data));
                            }
                        }
                    } catch(IOException e){
                        log.warning(e.getMessage());
                    }
                }
            }
        }
    }

    // close the socket
    public void closeSocket(){
        try{
            flagActive = false;
            log.info("Disconnect with " + peerInfo.toString());
            sendCommand(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));

            if(in != null)
                in.close();
            if(out != null)

                out.close();
            if(TCPSocket != null)
                TCPSocket.close();
            if(UDPSocket != null)
                UDPSocket.close();
        }catch(Exception e){
            log.info("Socket closed");
        }
    }

    public boolean disconnect(){
        if(threadManager.size() > 0){
            log.warning("File transfer in action, try disconnect later");
            return false;
        } else {
            if(mode.equals("tcp"))
                TCPmain.removeConnection(peerInfo.toString());
            else
                UDPmain.removeConnection(peerInfo.toString());
            closeSocket();
            return true;
        }
    }

    public void sendCommand(String base64Str) {
            try {
               // log.info("sending command " + base64Str);
                if(mode.equals("tcp")) {
                    out.write(base64Str);
                    out.newLine();
                    out.flush();
                } else {
                    if(peerPort == -1) {
                        DatagramPacket command = new DatagramPacket(base64Str.getBytes(), base64Str.length(), address, peerInfo.port);
                        UDPSocket.send(command);
                    } else{
                        DatagramPacket command = new DatagramPacket(base64Str.getBytes(), base64Str.length(), address, peerPort);
                        UDPSocket.send(command);
                    }

                }
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
    }


    public HostPort getPeerInfo(){
        return peerInfo;
    }

    // when peers reached maximum connection, search for its adjacent peers and trying to connect
    // Huge chunk of private code, don't bother read through it.
    private boolean searchThroughPeers(ArrayList<Document> _peers) {
        ArrayList<Document> peers = _peers;

        boolean connectionEstablished = false;

        while (!connectionEstablished) {
            if (peers.size() == 0) {
                return false;
            }
            HostPort peer = new HostPort(peers.remove(0));

            if (mode.equals("tcp")) {
                if (!TCPmain.connectionExist(peer)) {

                    try {
                        TCPSocket = new Socket(peer.host, peer.port);
                        in = new BufferedReader(new InputStreamReader(TCPSocket.getInputStream(), StandardCharsets.UTF_8));
                        out = new BufferedWriter(new OutputStreamWriter(TCPSocket.getOutputStream(), StandardCharsets.UTF_8));

                        // send Handshake request to other peers
                        out.write(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));
                        out.newLine();
                        out.flush();
                        TCPSocket.setSoTimeout(3 * 1000);
                        String data = in.readLine();
                        Document d = Document.parse(data);

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
                    }
                }
            } else {
                if (!UDPmain.connectionExist(peer)) {

                    try {
                        UDPSocket = new DatagramSocket();
                        sendCommand(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));
                        // send Handshake request to other peers
                        UDPSocket.setSoTimeout(3 * 1000);
                        byte[] buffer = new byte[blockSize];
                        DatagramPacket buf = new DatagramPacket(buffer, buffer.length);
                        UDPSocket.receive(buf);
                        String data = new String(buf.getData());
                        Document d = Document.parse(data);

                        if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {
                            peerInfo = new HostPort((Document) d.get("hostPort"));
                            peerPort = buf.getPort();
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
                    }
                }
            }
        }
        return true;
    }

    // establish connection with other peers
    public Connection(HostPort peer, String mode){

        this.mode = mode;
        connectionInit();
        this.peerInfo = peer;
        try{
            if(mode.equals("tcp")) {
                TCPSocket = new Socket();
                TCPSocket.connect(new InetSocketAddress(peer.host, peer.port), 5000);
                in = new BufferedReader(new InputStreamReader(TCPSocket.getInputStream(), StandardCharsets.UTF_8));
                out = new BufferedWriter(new OutputStreamWriter(TCPSocket.getOutputStream(), StandardCharsets.UTF_8));

                // send Handshake request to other peers
                out.write(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));
                out.newLine();
                out.flush();
                TCPSocket.setSoTimeout(3 * 1000);
                String data = in.readLine();
                Document d = Document.parse(data);

                if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {
                    peerInfo = new HostPort((Document) d.get("hostPort"));
                    flagActive = true;
                    TCPSocket.setSoTimeout(0);
                    Thread t = new Thread(this);
                    t.start();
                } else if (d.getString("command").equals("CONNECTION_REFUSED")) {
                    in.close();
                    out.close();
                    TCPSocket.close();
                    // TODO breath first search for other available peers
                    ArrayList<Document> peers = (ArrayList<Document>) d.get("peers");
                    if (!searchThroughPeers(peers)) {
                        flagActive = false;
                    }
                }
            } else {
                UDPSocket = new DatagramSocket();
                address = InetAddress.getByName(peerInfo.host);
                sendCommand(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));
                UDPSocket.setSoTimeout(3 * 1000);
                byte[] buffer = new byte[blockSize];
                DatagramPacket command = new DatagramPacket(buffer, buffer.length);
                UDPSocket.receive(command);
                Document d = Document.parse(new String(command.getData(),0,command.getLength()));

                if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {
                    peerInfo = new HostPort((Document) d.get("hostPort"));
                    flagActive = true;
                    UDPSocket.setSoTimeout(0);
                    peerPort = command.getPort();
                    Thread t = new Thread(this);
                    t.start();
                } else if (d.getString("command").equals("CONNECTION_REFUSED")) {
                    UDPSocket.close();
                    // TODO breath first search for other available peers
                    ArrayList<Document> peers = (ArrayList<Document>) d.get("peers");
                    if (!searchThroughPeers(peers)) {
                        flagActive = false;
                    }
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

    public void updatePort(int port){
        this.peerPort = port;
        sendCommand(JsonUtils.HANDSHAKE_RESPONSE());
    }

    public Connection(InetAddress addr, int port, Document d){
        connectionInit();
        mode = "udp";

        try{
            UDPSocket = new DatagramSocket();
            this.address = addr;
            peerInfo = new HostPort((Document) d.get("hostPort"));

            if(d.getString("command").equals("HANDSHAKE_REQUEST")){

                peerPort = port;

                if(!UDPmain.maximumConnectionReached()){
                    // available for connection, send HANDSHAKE RESPONSE
                    sendCommand(JsonUtils.HANDSHAKE_RESPONSE());
                    flagActive = true;

                    // start the thread for the socket
                    Thread t = new Thread(this);
                    t.start();
                }
                else{

                    // not available, stop the connection
                    sendCommand(JsonUtils.CONNECTION_REFUSED(UDPmain, "Limitation reached"));

                    // maximum connection reached
                    flagActive = false;
                }
            }

        }catch(Exception e){
            log.warning(e.getMessage());
        }
    }

    // Incoming connection
    public Connection(Socket aSocket){

        mode = "tcp";
        connectionInit();

        try{

            this.TCPSocket = aSocket;
            in = new BufferedReader(new InputStreamReader(aSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(aSocket.getOutputStream(), StandardCharsets.UTF_8));

            // TODO decode handshake message
            String data = in.readLine();
            Document d = Document.parse(data);

            if(d.getString("command").equals("HANDSHAKE_REQUEST")){

                // TODO get peerInfo
                peerInfo = new HostPort((Document) d.get("hostPort"));

                if(!TCPmain.maximumConnectionReached()){
                    // available for connection, send HANDSHAKE RESPONSE
                    out.write(JsonUtils.HANDSHAKE_RESPONSE());
                    out.newLine();
                    flagActive = true;

                    // start the thread for the socket
                    Thread t = new Thread(this);
                    t.start();
                }
                else{
                    // maximum connection reached
                    flagActive = false;
                    // not available, stop the connection
                    out.write(JsonUtils.CONNECTION_REFUSED(TCPmain, "connection limit reached"));
                    out.newLine();
                    out.flush();
                    closeSocket();
                }
            }
        } catch(IOException e){
            log.warning(e.getMessage());
            closeSocket();
        }
    }
}
