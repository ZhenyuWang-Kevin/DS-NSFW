package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;

public class Client
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //This is peers' command
        //java -cp bitbox.jar unimelb.bitbox.Peer

        /*
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

         */


        //list the courrently connected
        //java -cp bitbox.jar unimelb.bitbox.Client -c list_peers -s server.com:3000
        



        //connect
        //java -cp bitbox.jar unimelb.bitbox.Client -c connect_peer -s server.com:3000\
        //    -p bigdata.cis.unimelb.edu.au:8500



        //disconnect
        //java -cp bitbox.jar unimelb.bitbox.Client -c disconnect_peer -s server.com:3000\
        //    -p bigdata.cis.unimelb.edu.au:8500







        new Peer();
        
    }
}
