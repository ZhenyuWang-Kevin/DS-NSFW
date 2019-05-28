package unimelb.bitbox.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import unimelb.bitbox.Connection;
import unimelb.bitbox.JsonUtils;
import unimelb.bitbox.TCPMain;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class UDPMain {
	
	private static Logger log = Logger.getLogger(UDPMain.class.getName());
    private HashMap<String, Connection> Incomming;
    private HashMap<String, Connection> Outgoing;
    private ServerSocket listenSocket;
    private Queue<FileSystemEvent> eventBuffer;
    private boolean serverActive, communicationActive;
    
    // listening for incoming connections
    private Thread server = new Thread(){
        @Override
        public void run(){
            while(serverActive){
                try{
                	
                	DatagramSocket ds=new DatagramSocket(10000);
                    byte [] buf=new byte[1024];
                    DatagramPacket dp=new DatagramPacket(buf,buf.length);
                    ds.receive(dp);
                    String ip=dp.getAddress().getHostAddress();
                    int port=dp.getPort();
                    String data =new String(dp.getData(),0,dp.getLength());
                    InetAddress aHost = InetAddress
                    		.getByName(ip);

                    Socket incommingConnection = listenSocket.accept();
                    Connection c = new Connection(aHost,port,,this);
                    if(c.flagActive){
                        Incomming.put(c.getPeerInfo().toString(), c);
                    }
                }catch(IOException e){
                    log.warning(e.getMessage());
                }
            }
        }
    };

    public UDPMain(){
        // initalize the event buffer
        eventBuffer = new LinkedList<>();
        Incomming = new HashMap<>();
        Outgoing = new HashMap<>();

        // initialize server socket
        try{
            listenSocket = new ServerSocket(JsonUtils.getSelfHostPort().port);
        }catch(IOException e){
            log.warning(e.getMessage());
        }

        // Start Connect with peers
        String[] peersStr = Configuration.getConfigurationValue("peers").split(",");
        for (String peer : peersStr){
            HostPort tmp = new HostPort(peer);
            // if connection does not exist
            if(!connectionExist(tmp)){
                // try connect with the peer
                Connection c = new Connection(tmp);
                c.TCPmainPatch(this);
                // if connect successful, add to Outgoing hashmap
                if(c.flagActive){
                    Outgoing.put(tmp.toString(), c);
                }
            }
        }

        // start listening for incoming connection
        serverActive = true;
        server.start();

        // start the thread for managing all connections
        communicationActive = true;
        communication.start();
    }
}
