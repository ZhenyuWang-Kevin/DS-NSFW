package unimelb.bitbox;


import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.Document;


import org.json.simple.JSONObject;


import unimelb.bitbox.util.Configuration;

public class Client
{
    private static String operation;

    private static String serverIP;
    private static int serverPort;

    private static String targetIP;
    private static int targetPort;

    private static String identityName;

    private static boolean authStatus;


    private static Logger log = Logger.getLogger(Client.class.getName());
    public static void main( String[] args ) {

        for (int i = 0; i < args.length; i++){

            //list_peers, connect_peer, disconnect_peer
            if(args[i].equals("-c")){
                operation = args[i+1];
            }

            // The host & port of the peer who is going to establish the connection
            //e.g. server.com:3000
            if(args[i].equals("-s")){
                serverIP = args[i+1].substring(0,args[i+1].indexOf(':'));
                serverPort = Integer.parseInt(args[i+1].substring(args[i+1].indexOf(':')+1));
            }

            // The target connection peer
            //e.g. bigdata.cis.unimelb.edu.au:8500
            if(args[i].equals("-p")){
                targetIP = args[i+1].substring(0,args[i+1].indexOf(':'));
                targetPort = Integer.parseInt(args[i+1].substring(args[i+1].indexOf(':')+1));
            }

            // The clients idnetity name
            //e.g. aaron@krusty
            if(args[i].equals("-i")){
                identityName = args[i+1];
            }

        }

        log.info("BitBox Client starting...");
        //Configuration.getConfiguration();

        try(Socket socket = new Socket(serverIP, serverPort);){

            // The JSON Parser
            JSONParser parser = new JSONParser();
            // Output and Input Stream
            DataInputStream input = new DataInputStream(socket.
                    getInputStream());
            DataOutputStream output = new DataOutputStream(socket.
                    getOutputStream());

            // Automatically send Auth request first
            output.writeUTF(JsonUtils.AUTH_REQUEST(identityName));
            output.flush();
            System.out.println("===============Send auth request=============");

            // Read Auth reply from server..
            String message = input.readUTF();

            JSONObject command = (JSONObject) parser.parse(message);
            authStatus = (boolean) command.get("Status");



            System.out.println(message);


            if (authStatus) {
                System.out.println("===============Receive auth request=============");
                System.out.println("Auth success, go to the operation");
                //list_peers, connect_peer, disconnect_peer
                switch (operation) {
                    case "list_peers":
                        System.out.println("===============Send list peers request===============");
                        output.writeUTF(JsonUtils.LIST_PEERS_REQUEST());
                        output.flush();
                        break;
                    case "connect_peer":
                        System.out.println("===============Send connection request===============");
                        output.writeUTF(JsonUtils.CONNECT_PEER_REQUEST(targetIP, targetPort));
                        output.flush();
                        break;
                    case "disconnect_peer":
                        System.out.println("===============Send disconnection request===============");
                        output.writeUTF(JsonUtils.DISCONNECT_PEER_REQUEST(targetIP, targetPort));
                        output.flush();
                        break;
                    default:
                        System.out.println("Unknown command");
                        break;
                }

            }

            // Receive connect respond from server..
            message = input.readUTF();
            command = (JSONObject) parser.parse(message);
            String list_respond = (String) command.get("message");

            System.out.println(message);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

    }


}



