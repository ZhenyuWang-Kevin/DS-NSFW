package unimelb.bitbox;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.net.ServerSocketFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;



import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;


import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;




public class Peer
{

    private ResponseHandler rh;
    private static String advertisedName;
    private static int PeerPort;

    //private static ServerMain s;
    private static ServerMain s;

    // Identifies the user number con
    private static String key_identity;
    private static byte[] public_key;


    private static HashMap<String, String> List_Peers;

    private static Map<String, String> clientInfo = new HashMap<String, String>();


    private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {

        // shutdown event handler
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run(){
                log.info("Shutdown bitbox, disconnect all peers.");
                s.clearAllConnection();
            }
        }, "Shutdown-thread"));

        //Information of this Peer
        advertisedName = Configuration.getConfigurationValue("advertisedName");
        PeerPort = Integer.parseInt(Configuration.getConfigurationValue("port"));




        //java -cp bitbox.jar unimelb.bitbox.Peer
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();


        try{
            s = new ServerMain();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        ServerSocketFactory factory = ServerSocketFactory.getDefault();

        try(ServerSocket server = factory.createServerSocket(PeerPort)){

            // Wait for connections.
            while(true){


                Socket client = server.accept();

                // Start a new thread for a connection
                Thread t = new Thread(() -> serveClient(client));
                t.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }



    }


    private static void serveClient(Socket client){
        try(Socket clientSocket = client){

            // The JSON Parser
            JSONParser parser = new JSONParser();
            // Input stream
            DataInputStream input = new DataInputStream(clientSocket.
                    getInputStream());
            // Output Stream
            DataOutputStream output = new DataOutputStream(clientSocket.
                    getOutputStream());


            //read Auth requet from client
            String message = input.readUTF();
            JSONObject command = (JSONObject) parser.parse(message);
            String identifyName = (String) command.get("identity");


            String authorized_keys = Configuration.getConfigurationValue("authorized_keys");



            //check if the client is successfully authorized by the server peer
            boolean status = false;
            String sKey = "";
            if (getClientPubKey(authorized_keys, identifyName)){


                status = true;


                // convert public key string into compatible key format and get the identity
                String pubKey = clientInfo.get(identifyName);

                // convert public key string into compatible key format and get the identity
                PublicKey publicKey = RSAConverter.decodePublicKey(pubKey);


                sKey = AES.generateSecreteKey(128);

                //[BASE64 ENCODED, ENCRYPTED SECRET KEY]
                // encrypt secrete key with client’s public key
                String encrKey = RSAEncryption.PubEncrypt(sKey,publicKey);

                // Automatically send Auth request first
                output.writeUTF(JsonUtils.AUTH_RESPONSE_SUCCESS(encrKey,status));
                output.flush();
                System.out.println("==================Auth public key success==================");


            }else{
                // Automatically send Auth request first
                status = false;
                output.writeUTF(JsonUtils.AUTH_RESPONSE_FAIL(status));
                output.flush();
                System.out.println("==================Auth public key fail==================");

            }


            if(status){


                // Attempt to convert read data to JSON
                message = input.readUTF();
                System.out.println(message);


                command = (JSONObject) parser.parse(message);

                // Receive connection request from the client
                String decryptedMsg = decrypteMessage((String) command.get("payload"),sKey);

                System.out.println("=================Receive request from client===============");
                System.out.println(decryptedMsg);

                command = (JSONObject) parser.parse(decryptedMsg);
                String request = (String) command.get("command");


                boolean connectStatus = false;
                boolean disconnectStatus = false;


                switch (request) {
                    case "CONNECT_PEER_REQUEST":

                        System.out.println("=================Connect peer request===============");
                        String targetIP1 = (String) command.get("host");
                        Integer targetPort1 = Integer.parseInt(command.get("port").toString());
                        s.connectTo(targetIP1, targetPort1);
                        connectStatus = true;

                        if(connectStatus){

                            output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                    (JsonUtils.CONNECT_PEER_RESOPONSE_SUCCESS(targetIP1,targetPort1,connectStatus),sKey)));
                            output.flush();

                            String hostAndPeer1 = targetIP1+targetPort1;

                            List_Peers.put(hostAndPeer1,targetIP1);

                        }else{

                            output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                    (JsonUtils.CONNECT_PEER_RESOPONSE_FAIL(targetIP1,targetPort1,connectStatus),sKey)));
                            output.flush();

                        }
                        break;
                    case "DISCONNECT_PEER_REQUEST":

                        System.out.println("=================Disconnect peer request===============");
                        String targetIP2 = (String) command.get("host");
                        Integer targetPort2 = Integer.parseInt(command.get("port").toString());

                        s.disconnectTo(targetIP2, targetPort2);
                        disconnectStatus = true;

                        if(disconnectStatus){

                            output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                    (JsonUtils.DISCONNECT_PEER_RESOPONSE_SUCCESS(targetIP2, targetPort2, disconnectStatus),sKey)));
                            output.flush();

                            String hostAndPeer2 = targetIP2+targetPort2;

                            List_Peers.remove(hostAndPeer2,targetIP2);


                        }else{

                            output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                    (JsonUtils.DISCONNECT_PEER_RESOPONSE_FAIL(targetIP2, targetPort2, disconnectStatus),sKey)));
                            output.flush();
                        }


                        break;
                    case "LIST_PEERS_REQUEST":

                        Iterator iter = List_Peers.entrySet().iterator();
                        if (iter.hasNext()){
                            System.out.println("=================List peers request================");
                            output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                    (JsonUtils.LIST_PEERS_RESPOND(List_Peers),sKey)));
                            output.flush();
                        }
                        break;
                    default:
                        System.out.println("Unknown command from client");
                        break;
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*
     * read key files from local file
     * @param key file name
     * @return pub/pri key
     */
    private static String getKeyContent(String filename) throws IOException {
        InputStream is = new FileInputStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        String lines = "";
        while ((line = reader.readLine()) != null) {
            lines = lines + line;

        }

        return lines;
    }

    /*
     * get identity name
     */
    private static String getIdentityName(String pubKey){
        // look for the Base64 encoded part of the line to decode
        // both ssh-rsa and ssh-dss begin with "AAAA" due to the length bytes
        for (String part : pubKey.split(" ")) {
            if (part.contains("@")){
                Peer.key_identity = part;
            }
            if (part.startsWith("AAAA")) {
                Peer.public_key = Base64.getDecoder().decode(part);
            }
        }

        return Peer.key_identity;
    }



    /*
     * from the authorized public keys list to find this client's public key
     * if not exist, then refuse connection request
     * @param authorized public key strings
     * @param current client's identity
     */
    private static Boolean getClientPubKey(String authorized_keys, String ClientIdentity){
        String tmp_key=null;
        String tmp_identity=null;

        // there are more than one key stores
        if (authorized_keys.contains(",")){
            // split the string by comma
            for (String key : authorized_keys.split(",")){

                // retrieve identity and public key
                for (String part : key.split(" ")) {
                    if (part.contains("@")){

                        if (ClientIdentity.equals(part)){
                            tmp_identity = part;
                            clientInfo.put(tmp_identity, tmp_key);
                            return true;
                        }

                    }
                    if (part.startsWith("AAAA")) {
                        tmp_key = part;
                    }
                }
            }
        }else{
            for (String part : authorized_keys.split(" ")) {
                if (part.contains("@")){

                    if (ClientIdentity.equals(part)){
                        tmp_identity = part;
                        clientInfo.put(tmp_identity, tmp_key);
                        return true;
                    }
                }
                if (part.startsWith("AAAA")) {
                    tmp_key = part;
                }
            }
        }

        return false;

    }


    /**
     * Encrypte message
     * @param message input message
     * @param sKey secret key used to encrypte message
     * @return
     */
    private static String encrypteMessage(String message, String sKey){

        byte[] tmp = AES.Encrypt(message, sKey);

        return AES.parseByte2HexStr(tmp);

    }


    /**
     * Decrypte message
     * @param decrypteMessage input message
     * @param sKey secret key used to encrypte message
     * @return
     */
    private static String decrypteMessage(String encrypteMessage, String sKey){

        byte[] tmp = AES.parseHexStr2Byte(encrypteMessage);

        return AES.Decrypt(tmp, sKey);

    }




}
