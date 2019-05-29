package unimelb.bitbox;

import java.io.IOException;

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

public class Server{

    // Declare the port number
    private static int port = 3000;

    // Identifies the user number connected
    private static int counter = 0;

    public static void main(String[] args) {
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket server = factory.createServerSocket(port)) {
            System.out.println("Waiting for client connection..");

            // Wait for connections.
            while (true) {
                Socket client = server.accept();
                counter++;
                System.out.println("Client " + counter + ": Applying for connection!");


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
//                    Integer result = parseCommand(command);
//                    JSONObject results = new JSONObject();
//                    results.put("result", result);
//                    output.writeUTF(results.toJSONString());
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}