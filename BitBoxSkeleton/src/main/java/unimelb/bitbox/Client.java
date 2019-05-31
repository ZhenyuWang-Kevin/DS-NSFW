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

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;


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

            // Received the encrypted Secret key from the peer
            String AES128 = (String) command.get("AES128");

            // Read local private key file and get key strings
            String priKey = ClientKeys.getKeyContent("bitboxclient_rsa");
            // convert private key into compatible key format
            PrivateKey privateKey = RSAConverter.convertPriKey(priKey);

            // get the secret key after decrypting
            String sKey = RSAEncryption.PriDecrypt(AES128, privateKey);

            if (authStatus) {
                System.out.println("===============SKEY success return go to the operation=============");
                //list_peers, connect_peer, disconnect_peer
                String encrypted_message = "";
                switch (operation) {
                    case "list_peers":
                        System.out.println("===============Send list peers request===============");

                        output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                (JsonUtils.LIST_PEERS_REQUEST(),sKey)));
                        output.flush();

                        break;

                    case "connect_peer":
                        System.out.println("===============Send connection request===============");

                        output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                (JsonUtils.CONNECT_PEER_REQUEST(targetIP, targetPort),sKey)));
                        output.flush();

                        break;

                    case "disconnect_peer":
                        System.out.println("===============Send disconnection request===============");

                        output.writeUTF(JsonUtils.PAYLOAD(encrypteMessage
                                (JsonUtils.DISCONNECT_PEER_REQUEST(targetIP, targetPort),sKey)));
                        output.flush();

                        break;
                    default:
                        System.out.println("Unknown command");
                        break;
                }
            }else{

                System.out.println("===============Oh no Auth fail=============");
                System.out.println(message);
            }



            System.out.println("===============Receive operation respond from client===============");
            // Receive connect respond from server..
            message = input.readUTF();
            command = (JSONObject) parser.parse(message);
            System.out.println(decrypteMessage((String) command.get("payload"),sKey));

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

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



