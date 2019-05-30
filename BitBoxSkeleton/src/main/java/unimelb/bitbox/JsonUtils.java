package unimelb.bitbox;

import unimelb.bitbox.util.*;

import java.util.Base64;


import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * A Json communication protocol static class.
 * All functions are static, that means it can be directly used as in JsonUtils.function()
 * Without the need of creating new object
 *
 * This class contains all the communication protocols required by the assignment
 * And all protocol is encoded in to base64 String automatically
 *
 * A usage example, sending a HANDSHAKE_REQUEST protocol:
 * ...
 *
 *  out.writeUTF(JsonUtils.HANDSHAKE_REQUEST());
 * ...
 *
 * The class also contains a base64 decoder that has been integrate with Aaron's JSON Document class.
 * A usage example, socket received a base64 encoded string -- data
 *
 * Document doc = JsonUtils.decodeBase64toDocument(data);
 *
 */

public class JsonUtils {

    // empty constructor for static class
    public JsonUtils(){}

    private static String base64encodedCommand(String command){
        return command;
    }

    /**
     * decode a base64 messages to Aaron provided document type
     * @param base64Str
     * @return a json Document
     */
    public static Document decodeBase64toDocument(String base64Str){
        return Document.parse(base64Str);
    }

    /* The code below are set of communication protocol,
    which are alright encoded into base64 strings*/


    /**
     * CONNECTION REFUSED
     * @param TCPmain: the main object of TCP_protocol, used for retrieving all connections
     * @param msg: message to send
     * @return base64 string
     */
    public static String CONNECTION_REFUSED(TCPMain TCPmain, String msg){
        Document d = new Document();

        d.append("comamnd", "CONNECTION_REFUSED");
        d.append("message", msg);
        d.append("peers", TCPmain.getAllConnections());

        return d.toJson();
    }

    /**
     * CONNECTION REFUSED
     * @param UDPmain
     * @param msg
     * @return
     */
    public static String CONNECTION_REFUSED(UDPMain UDPmain, String msg){
        Document d = new Document();

        d.append("comamnd", "CONNECTION_REFUSED");
        d.append("message", msg);
        d.append("peers", UDPmain.getAllConnections());

        return base64encodedCommand(d.toJson());
    }

    /**
     * HANDSHAKE REQUEST
     * @param p a HostPort object contains the host and port info of the peer
     * @return base64 encoded json string
     */
    public static String HANDSHAKE_REQUEST(HostPort p){
        Document d = new Document();

        d.append("command", "HANDSHAKE_REQUEST");
        d.append("hostPort", p.toDoc());

        return d.toJson();
    }

    // HANDSHAKE RESPONSE
    public static String HANDSHAKE_RESPONSE(){


        Document d = new Document();

        d.append("command","HANDSHAKE_RESPONSE");
        d.append("hostPort", getSelfHostPort().toDoc());

        return d.toJson();
    }

    /**
     * FILE CREATE REQUEST
     * @param fDesc a FIleDescriptor Object of the file
     * @param path the path of the file
     * @return base64 encoded json string
     */
    public static String FILE_CREATE_REQUEST(FileSystemManager.FileDescriptor fDesc, String path){
        Document d = new Document();
        d.append("command", "FILE_CREATE_REQUEST");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);

        return d.toJson();
    }

    /**
     * FILE CREATE RESPONSE
     * @param fDesc a FIleDescriptor Object of the file
     * @param path the path of the file
     * @param msg send messages
     * @param status a boolean value indicate whether the request is successful or not
     * @return base64 encoded json string
     */
    public static String FILE_CREATE_RESPONSE(FileSystemManager.FileDescriptor fDesc, String path, String msg, boolean status){
        Document d = new Document();
        d.append("command", "FILE_CREATE_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);
        d.append("meesage",msg);
        d.append("status",status);

        return d.toJson();
    }

    /**
     * FILE BYTES REQUEST
     * @param fDesc a FIleDescriptor Object of the file
     * @param path the path of the file
     * @param position the start position of the request bytes
     * @param length the length of the bytes that what to receive
     * @return base64 encoded json string
     */
    public static String FILE_BYTES_REQUEST(FileSystemManager.FileDescriptor fDesc, String path, long position, long length)
    {
        Document d = new Document();
        d.append("command", "FILE_BYTES_REQUEST");
        d.append("fileDescriptor",fDesc.toDoc());
        d.append("pathName",path);
        d.append("position", position);
        d.append("length",length);

        return d.toJson();
    }

    /**
     * FILE BYTES RESPONSE
     * @param fDesc  a FileDescriptor object of the file
     * @param path the path of the file
     * @param position the start position of the request bytes
     * @param length the length of the bytes
     * @param content base64 encoded bytes
     * @param msg the message send to the peer
     * @param status the status of the response
     * @return base64 encoded json string
     */
    public static String FILE_BYTES_RESPONSE(FileSystemManager.FileDescriptor fDesc,
                                             String path, long position, long length,
                                             String content, String msg,
                                             boolean status) {
        Document d = new Document();
        d.append("command", "FILE_BYTES_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName",path);
        d.append("position", position);
        d.append("length", length);
        d.append("content", content);
        d.append("message", msg);
        d.append("status", status);

        return d.toJson();
    }

    /**
     * FILE DELETE REQUEST
     * @param fDesc a FileDescriptor object of the file
     * @param path the path of the file
     * @return base64 encoded json string
     */
    public static String FILE_DELETE_REQUEST(FileSystemManager.FileDescriptor fDesc, String path)
    {
        Document d = new Document();
        d.append("command","FILE_DELETE_REQUEST");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);

        return d.toJson();
    }

    /**
     * FILE DELETE RESPONSE
     * @param fDesc  a FileDescriptor object of the file
     * @param path the path of the file
     * @param msg the message sent to the peer
     * @param status the status of the response
     * @return base64 encoded json string
     */
    public static String FILE_DELETE_RESPONSE(FileSystemManager.FileDescriptor fDesc,
                                              String path, String msg, boolean status){
        Document d = new Document();
        d.append("command", "FILE_DELETE_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);
        d.append("message",msg);
        d.append("status",status);

        return d.toJson();
    }

    /**
     * FILE MODIFY REQUEST
     * @param fDesc a FileDescriptor object of the file
     * @param path the path of the file
     * @return base64 encoded json string
     */
    public static String FILE_MODIFY_REQUEST(FileSystemManager.FileDescriptor fDesc, String path) {

        Document d = new Document();
        d.append("command", "FILE_MODIFY_REQUEST");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);

        return d.toJson();
    }

    /**
     * FILE MODIFY RESPONSE
     * @param fDesc a FileDescriptor object of the file
     * @param path  the path of the file
     * @param msg the message sent to the peer
     * @param status the status of the response
     * @return base64 encoded json string
     */
    public static String FILE_MODIFY_RESPONSE(FileSystemManager.FileDescriptor fDesc,
                                              String path, String msg, boolean status){
        Document d = new Document();
        d.append("command", "FILE_MODIFY_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);
        d.append("message", msg);
        d.append("status", status);

        return d.toJson();
    }

    /**
     * DIRECTORY CREATE REQUEST
     * @param dirPath the path of the directory
     * @return base64 encoded json string
     */
    public static String DIRECTORY_CREATE_REQUEST(String dirPath){
        Document d = new Document();
        d.append("command", "DIRECTORY_CREATE_REQUEST");
        d.append("pathName", dirPath);

        return d.toJson();
    }

    /**
     * DIRECTORY CREATE RESPONSE
     * @param dirPath the path of the directory
     * @param msg the message sent to the peer
     * @param status the status of the response
     * @return base64 encoded json string
     */
    public static String DIRECTORY_CREATE_RESPONSE(String dirPath, String msg, boolean status){
        Document d = new Document();
        d.append("command", "DIRECTORY_CREATE_RESPONSE");
        d.append("pathName", dirPath);
        d.append("message", msg);
        d.append("status", status);

        return d.toJson();
    }

    /**
     * DIRECTORY DELETE REQUEST
     * @param dirPath the path of the directory
     * @return base64 encoded json string
     */
    public static String DIRECTORY_DELETE_REQUEST(String dirPath){
        Document d = new Document();
        d.append("command", "DIRECTORY_DELETE_REQUEST");
        d.append("pathName", dirPath);

        return d.toJson();
    }

    /**
     * DIRECTORY DELETE RESPONSE
     * @param dirPath the path of the directory
     * @param msg the message sent to the peer
     * @param status the status of the response
     * @return base64 encoded json string
     */
    public static String DIRECTORY_DELETE_RESPONSE(String dirPath, String msg, boolean status) {
        Document d = new Document();
        d.append("command", "DIRECTORY_DELETE_RESPONSE");
        d.append("pathName", dirPath);
        d.append("message", msg);
        d.append("status", status);

        return d.toJson();
    }

    /**
     * INVALID_PROTOCOL
     * @param msg
     * @return base64 String
     */
    public static String INVALID_PROTOCOL(String msg){
        Document d = new Document();
        d.append("command", "INVALID_PROTOCOL");
        d.append("message", msg);

        return d.toJson();
    }



    /**
     * AUTH_REQUEST, CHALLENGE RESPOND FOR THE CLIENT, SEND BY CLIENT
     * @param msg
     * @return base64 encoded json string
     */
    public static String AUTH_REQUEST(String idt){
        Document d = new Document();
        d.append("command", "AUTH_REQUEST");
        d.append("identity", idt);

        return d.toJson();
    }

    /**
     * AUTH_RESPONSE, CHALLENGE RESPOND FOR THE CLIENT, SEND BY PEER
     * @param encrKey is the BASE64 ENCRYPED SECRET KEY
     * @param status
     * @return
     */
    public static String AUTH_RESPONSE_SUCCESS(String encrKey,String idt, boolean status){
        Document d = new Document();
        d.append("AES128", "AUTH_RESPONSE");
        d.append("identity", idt);
        d.append("Status",status);
        d.append("message", "public key found");

        return d.toJson();
    }

    /**
     * AUTH_RESPONSE_FALSE, CHALLENGE RESPOND FOR THE CLIENT, SEND BY PEER
     * @param encrKey is the BASE64 ENCRYPED SECRET KEY
     * @param status
     * @return
     */
    public static String AUTH_RESPONSE_FAIL(String encrKey,boolean status){
        Document d = new Document();
        d.append("AES128", "AUTH_RESPONSE");
        d.append("Status",status);
        d.append("message", "public key not found");

        return d.toJson();
    }

    /**
     * PAYLOAD,ONCE A SECRECT KEY ESTABLISH, ALL COMMENTS FORMAT
     * @param encrCmd is the ENCODED ENCRYPTED COMMAND/RESPONSE JSON STRING
     */
    public static String PAYLOAD(String encrCmd){
        Document d = new Document();
        d.append("payload", encrCmd);

        return d.toJson();
    }


    /**
     * LIST PEERS REQUEST, SEND BY CLIENT
     * @param encrCmd
     * @return
     */


    public static String LIST_PEERS_REQUEST(HashMap<String, Integer> List_Peers){
        Document d = new Document();

        Iterator iter = List_Peers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object host = entry.getKey();
            Object port = entry.getValue();
            d.append("host", host.toString());
            d.append("port",Integer.parseInt(port.toString()));

        }
        return d.toJson();
    }


    /**
     * CONNECT TO PEER REQUEST, SEND BY CLIENT
     * @param targetIP
     * @param targetPort
     * @return
     */
    public static String CONNECT_PEER_REQUEST(String targetIP,int targetPort){
        Document d = new Document();
        d.append("command", "CONNECT_PEER_REQUEST");
        d.append("host", targetIP);
        d.append("port",targetPort);

        return d.toJson();

    }

    /**
     * CONNECT TO PEER SUCCESS RESPONSE, SEND BY PEER
     * @param targetIP
     * @param targetPort
     * @param status
     * @return
     */
    public static String CONNECT_PEER_RESOPONSE_SUCCESS(String targetIP,int targetPort,boolean status){
        Document d = new Document();
        d.append("command", "CONNECT_PEER_RESPONSE");
        d.append("host", targetIP);
        d.append("port",targetPort);
        d.append("status",status);
        d.append("message","connected to peer");

        return d.toJson();
    }

    /**
     * CONNECT TO PEER FAIL RESPONSE, SEND BY PEER
     * @param targetIP
     * @param targetPort
     * @param status
     * @return
     */
    public static String CONNECT_PEER_RESOPONSE_FAIL(String targetIP,int targetPort,boolean status){
        Document d = new Document();
        d.append("command", "CONNECT_PEER_RESPONSE");
        d.append("host", targetIP);
        d.append("port",targetPort);
        d.append("status",status);
        d.append("message","connected failed");

        return d.toJson();
    }


    /**
     * DISCONNECT FROM PEER REQUEST, SEND BY CLIENT
     * @param targetIP
     * @param targetPort
     * @return
     */
    public static String DISCONNECT_PEER_REQUEST(String targetIP,int targetPort){
        Document d = new Document();
        d.append("command", "DISCONNECT_PEER_REQUEST");
        d.append("host", targetIP);
        d.append("port",targetPort);

        return d.toJson();
    }

    /**
     * DISCONNECT FROM PEER SUCCESS RESPONSE, SEND BY PEER
     * @param targetIP
     * @param targetPort
     * @param status
     * @return
     */
    public static String DISCONNECT_PEER_RESOPONSE_SUCCESS(String targetIP,int targetPort,boolean status){
        Document d = new Document();
        d.append("command", "DISCONNECT_PEER_RESPONSE");
        d.append("host", targetIP);
        d.append("port",targetPort);
        d.append("status",status);
        d.append("message","diconnected from peer");

        return d.toJson();
    }


    /**
     * DISCONNECT FROM PEER FAIR RESPONSE, SEND BY PEER
     * @param targetIP
     * @param targetPort
     * @param status
     * @return
     */
    public static String DISCONNECT_PEER_RESOPONSE_FAIL(String targetIP,int targetPort,boolean status){
        Document d = new Document();
        d.append("command", "DISCONNECT_PEER_RESPONSE");
        d.append("host", targetIP);
        d.append("port",targetPort);
        d.append("status",status);
        d.append("message","connection not active");

        return d.toJson();
    }



    //----------------------------------------
    // Other utils functions
    //----------------------------------------

    /**
     *
     * @return return the host port of it self
     */
    public static HostPort getSelfHostPort(){
        return new HostPort(
                Configuration.getConfigurationValue("advertisedName"),
                Integer.parseInt(Configuration.getConfigurationValue("port"))
        );
    }
}
