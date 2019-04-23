package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.HostPort;

import java.util.Base64;

public class JsonUtils {

    // empty constructor for static class
    public JsonUtils(){}

    private static String base64encodedCommand(String command){
        return Base64.getEncoder().encodeToString(command.getBytes());
    }

    public static JSONObject decodeBase64toJSONobj(String base64Str){
        String JSONstr = new String(Base64.getDecoder().decode(base64Str.getBytes()));

        JSONParser parser = new JSONParser();

        JSONObject retVal = new JSONObject();

        try{
            JSONArray array = (JSONArray)(parser.parse(JSONstr));

            retVal = (JSONObject)array.get(1);

        } catch (ParseException e) {
            retVal.put("command","INVALID_PROTOCOL");
            retVal.put("message", "message parsing error");
        }

        return retVal;
    }

    /* The code below are set of communication protocol,
    which are alright encoded into base64 strings*/

    // HANDSHAKE REQUEST
    // @params:
    //  HostPort p : a HostPort object contains the host and port info of the peer

    public static String HANDSHAKE_REQUEST(HostPort p){
        Document d = new Document();

        d.append("command", "HANDSHAKE_REQUEST");
        d.append("hostPort", p.toDoc());

        return base64encodedCommand(d.toString());
    }

    // HANDSHAKE RESPONSE
    public static String HANDSHAKE_RESPONSE(){
        HostPort thisPort = new HostPort(
                Configuration.getConfigurationValue("advertisedName"),
                Integer.parseInt(Configuration.getConfigurationValue("port"))
        );

        Document d = new Document();

        d.append("command","HANDSHAKE_RESPONSE");
        d.append("hostPort", thisPort.toDoc());

        return base64encodedCommand(d.toString());
    }

    // FILE CREATE REQUEST
    // @params:
    //  FileDescriptor fDesc: a FIleDescriptor Object of the file
    //  String path: the path of the file
    public static String FILE_CREATE_REQUEST(FileSystemManager.FileDescriptor fDesc, String path){
        Document d = new Document();
        d.append("command", "FILE_CREATE_REQUEST");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);

        return base64encodedCommand(d.toString());
    }

    // FILE CREATE RESPONSE
    // @params:
    // FileDescriptor fDesc: a FIleDescriptor Object of the file
    //  String path: the path of the file
    //  String msg: send messages
    //  boolean status: a boolean value indicate whether the request is successful or not
    public static String FILE_CREATE_RESPONSE(FileSystemManager.FileDescriptor fDesc, String path, String msg, boolean status){
        Document d = new Document();
        d.append("command", "FILE_CREATE_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);
        d.append("meesage",msg);
        d.append("status",status);

        return base64encodedCommand(d.toString());
    }

    // FILE BYTES REQUEST
    // @params:
    // FileDescriptor fDesc: a FIleDescriptor Object of the file
    // String path: the path of the file
    // int Position: the start position of the request bytes
    // int length: the length of the bytes that what to receive
    public static String FILE_BYTES_REQUEST(FileSystemManager.FileDescriptor fDesc, String path, int position, int length)
    {
        Document d = new Document();
        d.append("command", "FILE_BYTES_REQUEST");
        d.append("fileDescriptor",fDesc.toDoc());
        d.append("pathName",path);
        d.append("position", position);
        d.append("length",length);

        return base64encodedCommand(d.toString());
    }

    // FILE BYTES RESPONSE
    // @params:
    // FileDescriptor fDesc: a FileDescriptor object of the file
    // String path: the path of the file
    // int Position: the start position of the request bytes
    // int length: the length of the bytes
    // String content: base64 encoded bytes
    // String message: the message send to the peer
    // boolean status: the status of the response
    public static String FILE_BYTES_RESPONSE(FileSystemManager.FileDescriptor fDesc,
                                             String path, int position, int length,
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

        return base64encodedCommand(d.toString());
    }

    // FILE DELETE REQUEST
    // @params:
    // FileDescriptor fDesc: a FileDescriptor object of the file
    // String path: the path of the file
    public static String FILE_DELETE_REQUEST(FileSystemManager.FileDescriptor fDesc, String path)
    {
        Document d = new Document();
        d.append("command","FILE_DELETE_REQUEST");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);

        return base64encodedCommand(d.toString());
    }

    // FILE_DELETE_RESPONSE
    // @params:
    //  FileDescriptor fDesc: a FileDescriptor object of the file
    //  String path: the path of the file
    //  String msg: the message sent to the peer
    //  boolean status: the status of the response
    public static String FILE_DELETE_RESPONSE(FileSystemManager.FileDescriptor fDesc,
                                              String path, String msg, boolean status){
        Document d = new Document();
        d.append("command", "FILE_DELETE_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);
        d.append("message",msg);
        d.append("status",status);

        return base64encodedCommand(d.toString());
    }

    // FILE MODIFY REQUEST
    // @params:
    //  FileDescriptor fDesc: a FileDescriptor object of the file
    //  String path: the path of the file
    public static String FILE_MODIFY_REQUEST(FileSystemManager.FileDescriptor fDesc, String path) {

        Document d = new Document();
        d.append("command", "FILE_MODIFY_REQUEST");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);

        return base64encodedCommand(d.toString());
    }

    // FILE MODIFY RESPONSE
    // @params:
    //  FileDescriptor fDesc: a FileDescriptor object of the file
    //  String path: the path of the file
    //  String msg: the message sent to the peer
    //  boolean status: the status of the response
    public static String FILE_MODIFY_RESPONSE(FileSystemManager.FileDescriptor fDesc,
                                              String path, String msg, boolean status){
        Document d = new Document();
        d.append("command", "FILE_MODIFY_RESPONSE");
        d.append("fileDescriptor", fDesc.toDoc());
        d.append("pathName", path);
        d.append("message", msg);
        d.append("status", status);

        return base64encodedCommand(d.toString());
    }

    // DIRECTORY CREATE REQUEST
    // @params:
    // String dirPath: the path of the directory
    public static String DIRECTORY_CREATE_REQUEST(String dirPath){
        Document d = new Document();
        d.append("command", "DIRECTORY_CREATE_REQUEST");
        d.append("pathName", dirPath);

        return base64encodedCommand(d.toString());
    }

    // DIRECTORY CREATE RESPONSE
    // @params:
    //  String dirPath: the path of the directory
    //  String msg: the message sent to the peer
    //  boolean status: the status of the response
    public static String DIRECTORY_CREATE_RESPONSE(String dirPath, String msg, boolean status){
        Document d = new Document();
        d.append("pathName", dirPath);
        d.append("message", msg);
        d.append("status", status);

        return base64encodedCommand(d.toString());
    }

    // DIRECTORY DELETE REQUEST
    // @params:
    // String dirPath: the path of the directory
    public static String DIRECTORY_DELETE_REQUEST(String dirPath){
        Document d = new Document();
        d.append("command", "DIRECTORY_DELETE_REQUEST");
        d.append("pathName", dirPath);

        return base64encodedCommand(d.toString());
    }

    // DIRECTORY DELETE RESPONSE
    // @params:
    //  String dirPath: the path of the directory
    //  String msg: the message sent to the peer
    //  boolean status: the status of the response
    public static String DIRECTORY_DELETE_RESPONSE(String dirPath, String msg, boolean status){
        Document d = new Document();
        d.append("command", "DIRECTORY_DELETE_RESPONSE");
        d.append("pathName", dirPath);
        d.append("message", msg);
        d.append("status", status);

        return base64encodedCommand(d.toString());
    }
}
