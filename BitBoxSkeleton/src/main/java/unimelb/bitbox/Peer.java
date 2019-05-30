package unimelb.bitbox;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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



public class Peer 
{

    private ResponseHandler rh;
    private static String advertisedName;
    private static int PeerPort;


    // Identifies the user number con
    private static int counter = 0;

	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {

        //Information of this Peer

        advertisedName = Configuration.getConfigurationValue("advertisedName");
        PeerPort = Integer.parseInt(Configuration.getConfigurationValue("port"));


        /*
        public peer(String PeerIP, int PeerHost){
            this.PeerIP = PeerIP;
            this.PeerHost = PeerHost;
        }

         */

        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try(ServerSocket server = factory.createServerSocket(PeerPort)){
            System.out.println("Waiting for client connection..");

            // Wait for connections.
            while(true){
                Socket client = server.accept();
                counter++;
                System.out.println("Client "+counter+": Applying for connection!");

                // Start a new thread for a connection
                Thread t = new Thread(() -> serveClient(client));
                t.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }



        /*
         reference from connection
        // Main work goes here
    public void receiveCommand(Document json){
        Document fdesc;
        log.info("received command: " + json.getString("command"));
        switch(json.getString("command")){
            case "HANDSHAKE_RESPONSE":

        */

        //java -cp bitbox.jar unimelb.bitbox.Peer
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();


        //new ServerMain();

        
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
            System.out.println("CLIENT: "+input.readUTF());
            output.writeUTF("Server: Hi Client "+counter+" !!!");

            // Receive more data..
            while(true){
                if(input.available() > 0){
                    // Attempt to convert read data to JSON
                    String message = input.readUTF();


                    //message = decryptMessage(message);

                    JSONObject command = (JSONObject) parser.parse(message);
                    System.out.println("COMMAND RECEIVED: "+command.toJSONString());
                    Integer result = parseCommand(command);
                    JSONObject results = new JSONObject();
                    results.put("result", result);
                    output.writeUTF(results.toJSONString());
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static String decryptMessage(String message){
        // Decrypt result
        try {
            String key = "5v8y/B?D(G+KbPeS";
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            message = new String(cipher.doFinal(Base64.getDecoder().decode(message.getBytes())));
            System.err.println("Decrypted message: "+message);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;
    }

    private static void sendEncrypted(String message, DataOutputStream output){
        // Encrypt first
        String key = "5v8y/B?D(G+KbPeS";
        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES");
            // Perform encryption
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
            System.err.println("Encrypted text: "+new String(encrypted));
            output.writeUTF(Base64.getEncoder().encodeToString(encrypted));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static Integer parseCommand(JSONObject command) {

        int result = 0;

        if(command.containsKey("command_name")){
            System.out.println("IT HAS A COMMAND NAME");
        }

        if(command.get("command_name").equals("Math")){
            //Math math = new Math();
            Integer firstInt = Integer.parseInt(command.get("first_integer").toString());
            Integer secondInt = Integer.parseInt(command.get("second_integer").toString());

            switch((String) command.get("method_name")){
                case "add":
                    result = firstInt + secondInt;
                    break;
                case "multiply":
                    result = firstInt * secondInt;
                    break;
                case "subtract":
                    result = firstInt - secondInt;
                    break;
                default:
                    // Really bad design!!
                    try {
                        throw new Exception();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }
        }
        // TODO Auto-generated method stub
        return result;
    }





}
