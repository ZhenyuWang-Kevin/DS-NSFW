package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;

//import org.kohsuke.args4j.CmdLineParser;


import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;



public class Client
{

	private static Logger log = Logger.getLogger(Client.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
/*

        //Object that will store the parsed command line arguments
        CmdLineArgs argsBean = new CmdLineArgs();


        //Parser provided by args4j
        CmdLineParser parser = new CmdLineParser(argsBean);
        try {


            //Parse the arguments
            parser.parseArgument(args);


            //After parsing, the fields in argsBean have been updated with the given
            //command line arguments
            System.out.println("Operation: " + argsBean.getOperation());
            System.out.println("ServerPort: " + argsBean.getServerPort());


        } catch (CmdLineException e) {

            System.err.println(e.getMessage());

            //Print the usage to help the user understand the arguments expected
            //by the program
            parser.printUsage(System.err);
        }

        /*

        //This is peers' command
        //java -cp bitbox.jar unimelb.bitbox.Peer
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Client starting...");
        Configuration.getConfiguration();

        */


        new Peer();
        //new ServerMain();
        
    }
}
