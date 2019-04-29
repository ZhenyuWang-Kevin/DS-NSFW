package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.util.Base64;

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
        return Base64.getEncoder().encodeToString(command.getBytes());
    }

    /**
     * decode a base64 messages to Aaron provided document type
     * @param base64Str
     * @return a json Document
     */
    public static Document decodeBase64toDocument(String base64Str){

        String data = new String(Base64.getDecoder().decode(base64Str.getBytes()));
        return Document.parse(data);
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

        return base64encodedCommand(d.toJson());
    }

    // HANDSHAKE RESPONSE
    public static String HANDSHAKE_RESPONSE(){


        Document d = new Document();

        d.append("command","HANDSHAKE_RESPONSE");
        d.append("hostPort", getSelfHostPort().toDoc());

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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
        d.append("command", " FILE_BTYES_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName",path);
        d.append("position", position);
        d.append("length", length);
        d.append("content", content);
        d.append("message", msg);
        d.append("status", status);

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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
        d.append("pathName", dirPath);
        d.append("message", msg);
        d.append("status", status);

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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

        return base64encodedCommand(d.toJson());
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
