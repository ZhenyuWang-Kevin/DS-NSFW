package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * A connection class, responsible for managing a single socket and its connection
 */

public class Connection implements Runnable {

    public boolean flagActive;
    DataInputStream in;
    DataOutputStream out;
    Socket aSocket;
    HostPort peerInfo;
    TCP_protocol TCPmain;

    // create connection to peer or any available peers of peer

    /**
     * The connection constructot that used for starting connection with other peer
     *
     * @param peer a hostport object contains the information about other peer
     */
    public Connection(HostPort peer) {
        try {
            aSocket = new Socket(peer.host, peer.port);
            in = new DataInputStream(aSocket.getInputStream());
            out = new DataOutputStream(aSocket.getOutputStream());

            // send Handshake request to other peers
            out.writeUTF(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));

            // read the peer response
            String data = in.readUTF();
            Document d = JsonUtils.decodeBase64toDocument(data);

            // handling response
            if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {

                // TODO: print out information about the connection

                peerInfo = new HostPort((Document)d.get("hostPort"));
                flagActive = true;
                // connection successful, start this connection thread
                Thread t = new Thread(this);
                t.start();
            } else if (d.getString("command").equals("CONNECTION_REFUSED")) {
                // connection denied due to maximum number of connection reached by the peer
                // start searching for its adjacent peers
                in.close();
                out.close();
                aSocket.close();
                // breath first search for other available peers
                ArrayList<Document> peers = (ArrayList<Document>) d.get("peers");
                if (!searchThroughPeers(peers)) {
                    flagActive = false;
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown: " + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                try {
                    aSocket.close();
                } catch (IOException e) {
                    System.out.println("IO: " + e.getMessage());
                }
        }
    }

    // Incoming connection

    /**
     * The constructor used for receiving a handshake request
     *
     * @param aSocket the connection socket received
     * @param TCPmain the TCP protocol object, used for retrieving information
     */
    public Connection(Socket aSocket, TCP_protocol TCPmain) {

        this.TCPmain = TCPmain;

        try {
            this.aSocket = aSocket;
            in = new DataInputStream(this.aSocket.getInputStream());
            out = new DataOutputStream(this.aSocket.getOutputStream());

            // decode handshake message
            String data = in.readUTF();
            Document d = JsonUtils.decodeBase64toDocument(data);

            if (d.getString("command").equals("HANDSHAKE_REQUEST")) {

                // get peerInfo
                peerInfo = new HostPort((Document) d.get("hostPort"));

                if (this.TCPmain.checkNewConnectionAvailable()) {
                    // available for connection, send HANDSHAKE RESPONSE
                    out.writeUTF(JsonUtils.HANDSHAKE_RESPONSE());

                    flagActive = true;

                    // start the thread for the socket
                    Thread t = new Thread(this);
                    t.start();
                } else {
                    // maximum connection reached
                    flagActive = false;
                    // not available, stop the connection
                    out.writeUTF(JsonUtils.CONNECTION_REFUSED(TCPmain, "connection limit reached"));
                    in.close();
                    out.close();
                    aSocket.close();
                }
            }

        } catch (IOException e) {
            System.out.println("Connection: " + e.getMessage());
        }
    }

    /**
     * get the host port info of the peer
     *
     * @return HostPort object
     */
    public HostPort getPeerInfo() {
        return peerInfo;
    }

    public void run() {
        // TODO handles the protocol
    }

    // when peers reached maximum connection, search for its adjacent peers and trying to connect
    // Huge chunk of private code, don't bother read through it.
    private boolean searchThroughPeers(ArrayList<Document> _peers) {
        ArrayList<Document> peers = _peers;

        boolean connectionEstablisthed = false;

        while (!connectionEstablisthed) {
            if (peers.size() == 0) {
                return false;
            }
            HostPort peer = new HostPort(peers.remove(0));

            if (!TCPmain.connectionExist(peer)) {

                try {
                    aSocket = new Socket(peer.host, peer.port);
                    in = new DataInputStream(aSocket.getInputStream());
                    out = new DataOutputStream(aSocket.getOutputStream());

                    // send Handshake request to other peers
                    out.writeUTF(JsonUtils.HANDSHAKE_REQUEST(JsonUtils.getSelfHostPort()));

                    String data = in.readUTF();
                    Document d = JsonUtils.decodeBase64toDocument(data);

                    if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {
                        peerInfo = new HostPort((Document)d.get("hostPort"));
                        flagActive = true;
                        connectionEstablisthed = true;
                        Thread t = new Thread(this);
                        t.start();
                    } else if (d.getString("command").equals("CONNECTION_REFUSED")) {
                        // breath-first search for other available peers
                        peers.addAll((ArrayList<Document>) d.get("peers"));
                        in.close();
                        out.close();
                        aSocket.close();
                    }
                } catch (UnknownHostException e) {
                    System.out.println("Unknown: " + e.getMessage());
                } catch (EOFException e) {
                    System.out.println("EOF: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO: " + e.getMessage());
                } finally {
                    if (aSocket != null)
                        try {
                            aSocket.close();
                        } catch (IOException e) {
                            System.out.println("IO: " + e.getMessage());
                        }
                }
            }
        }
        return true;
    }
}
