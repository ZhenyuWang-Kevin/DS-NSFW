package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HostPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
import java.net.*;

/**
 * A TCP protocol class, handles all socket connections and port listening
 */

public class TCP_protocol implements Runnable {

    private HashMap<HostPort, Connection> IncommingConnections;
    private HashMap<HostPort, Connection> OutgoingConnections;
    private ServerSocket listenSocket;

    public TCP_protocol() {

        IncommingConnections = new HashMap<>();
        OutgoingConnections = new HashMap<>();

        // set up listening for connections from others
        try {
            listenSocket = new ServerSocket(JsonUtils.getSelfHostPort().port);
        } catch (IOException e) {
            System.out.println("Listen socket: " + e.getMessage());
        }

        // start the thread to listen for connections
        Thread t = new Thread(this);
        t.start();

        // setup all outgoing connections
        String[] peersStr = Configuration.getConfigurationValue("peers").split(",");

        for (int i = 0; i < peersStr.length; i++) {
            HostPort tmpP = new HostPort(peersStr[i]);
            if (!connectionExist(tmpP)) {
                Connection c = new Connection(tmpP);
                if (c.flagActive) {
                    OutgoingConnections.put(c.getPeerInfo(), c);
                }
            }
        }
    }

    // A thread to listen for incoming connections
    public void run() {
        try {
            while (true) {
                Socket peerSocket = listenSocket.accept();

                Connection c = new Connection(peerSocket, this);

                // if handshake success, establish the connection
                if (c.flagActive) {
                    IncommingConnections.put(c.getPeerInfo(), c);
                }
            }
        } catch (IOException e) {
            System.out.println("Listen socket:" + e.getMessage());
        }

    }

    /**
     * @return whether the incoming connection has reached its maximum
     */
    public boolean checkNewConnectionAvailable() {

        if (IncommingConnections.size() < Integer.parseInt(
                Configuration.getConfigurationValue("maximumIncommingConnections")
        )) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return returns an arraylist contains all existing connections
     */
    public ArrayList<HostPort> getAllExistingConnections() {

        ArrayList<HostPort> retArray = new ArrayList<HostPort>();

        // Lambda expression to add all keys into ArrayList
        IncommingConnections.forEach((key, value) -> retArray.add(key));
        OutgoingConnections.forEach((key, value) -> retArray.add(key));

        return retArray;
    }

    /**
     * Check for whether a given address has been connected
     *
     * @param p: A HostPort object
     * @return whether or not there is a connection
     */
    public boolean connectionExist(HostPort p) {
        if (IncommingConnections.containsKey(p) || OutgoingConnections.containsKey(p)) {
            return true;
        } else {
            return false;
        }
    }

}
