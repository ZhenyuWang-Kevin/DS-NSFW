package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
import java.net.*;
import java.util.logging.Logger;

/**
 * A TCP protocol class, handles all socket connections and port listening
 */

public class TCP_protocol implements Runnable{

    private static Logger log = Logger.getLogger(TCP_protocol.class.getName());

    private HashMap<String,Connection> IncommingConnections;
    private HashMap<String, Connection> OutgoingConnections;
    private ServerSocket listenSocket;

    public TCP_protocol(){

        IncommingConnections = new HashMap<String, Connection>();
        OutgoingConnections = new HashMap<String, Connection>();

        // set up listening for connections from others
        try {
            listenSocket = new ServerSocket(JsonUtils.getSelfHostPort().port);
            log.info("Starting the server, listening on port " + JsonUtils.getSelfHostPort().port);
        } catch (IOException e){
            log.warning(e.getMessage());
        }

        // start the thread to listen for connections
        Thread t = new Thread(this);
        t.start();

        // setup all outgoing connections
        String[] peersStr = Configuration.getConfigurationValue("peers").split(",");

        for(int i = 0; i < peersStr.length; i++){
            HostPort tmpP = new HostPort(peersStr[i]);
            if(!connectionExist(tmpP)){
                OutgoingConnections.put(tmpP.host, new Connection(tmpP));
                if(!OutgoingConnections.get(tmpP.host).flagActive){
                    OutgoingConnections.remove(tmpP.host);
                }
            }
        }
    }

    // A thread to listen for incoming connections
    public void run(){
        try{
            while(true){
                Socket peerSocket = listenSocket.accept();

                Connection c = new Connection(peerSocket, this);

                // if handshake success, establish the connection
                if(c.flagActive){
                    IncommingConnections.put(c.getPeerInfo().host, c);
                }
            }
        }catch(IOException e){
            log.warning(e.getMessage());
        }

    }

    /**
     *
     * @return whether the incoming connection has reached its maximum
     */
    public boolean checkNewConnectionAvailable(){

        if(IncommingConnections.size() < Integer.parseInt(
                Configuration.getConfigurationValue("maximumIncommingConnections")
        )){
            return true;
        }
        else{
            return false;
        }
    }

    /**
     *
     * @return returns an arraylist contains all existing connections
     */
    public ArrayList<String> getAllExistingConnections(){

        ArrayList<String> retArray = new ArrayList<String>();

        // Lambda expression to add all keys into ArrayList
        IncommingConnections.forEach((key, value) -> retArray.add(key));
        OutgoingConnections.forEach((key, value) -> retArray.add(key));

        return retArray;
    }

    /**
     * Check for whether a given address has been connected
     * @param p: A HostPort object
     * @return whether or not there is a connection
     */
    public boolean connectionExist(HostPort p){
        if(IncommingConnections.containsKey(p.host) || OutgoingConnections.containsKey(p.host)){
            return true;
        }
        else{
            return false;
        }
    }


    // for testing
    public void transmitSyncEvents(FileSystemEvent events) {

        String command = JsonUtils.FILE_CREATE_REQUEST(events.fileDescriptor, events.pathName); ;

        Connection c = IncommingConnections.get("localhost");
        c.sendCommand(command);
        c.setByteRequestAvailibility(true);
    }
}
