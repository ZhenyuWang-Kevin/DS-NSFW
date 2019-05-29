package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;

public class Client
{
    private static String operation;
    private static String serverPort;
    private static String clientPort;
    private static String identityName;

    private static Logger log = Logger.getLogger(Client.class.getName());
    public static void main( String[] args ) {


        for (int i = 0; i < args.length; i++){

            //list_peers, connect_peer, disconnect_peer
            if(args[i].equals("-c")){
                operation = args[i+1];
            }

            //e.g. server.com:3000
            if(args[i].equals("-s")){
                serverPort = args[i+1].substring(args[i+1].indexOf(':'),args[i+1].length());
                System.out.println("ServerPort: " + serverPort);
            }

            //e.g.  bigdata.cis.unimelb.edu.au:8500
            if(args[i].equals("-p")){
                clientPort = args[i+1].substring(args[i+1].indexOf(':'),args[i+1].length());
                System.out.println("clientPort: " + clientPort);
            }


            //e.g. aaron@krusty
            if(args[i].equals("-i")){
                identityName = args[i+1];
                System.out.println("identify Name: " + identityName);
            }

        }


    /*

        //This is peers' command
        //java -cp bitbox.jar unimelb.bitbox.Peer
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");


*/


        log.info("BitBox Client starting...");
        Configuration.getConfiguration();

        new Peer();
        //new ServerMain();

    }
}
