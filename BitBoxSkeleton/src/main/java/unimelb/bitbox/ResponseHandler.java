package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

public class ResponseHandler {
    private static Logger log = Logger.getLogger(ResponseHandler.class.getName());
    private static FileSystemManager fManager;
    private Connection connection;
    private static long maximumBlockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
    private int maximumRequestResend;

    public static void setFileSystemManager(FileSystemManager f){
        fManager = f;
    }

    // static class constructor
    public ResponseHandler(Connection c){
        this.connection = c;
        this.maximumRequestResend = 3;
    };

    public void receivedFileCreateRequest(Document d){
        //
    }

    public void receivedFileCreateResponse(Document d){

    }

    public void receivedFileDeleteRequest(Document d){
    }

    public void receivedFileDeleteResponse(Document d){
    }

    public void receivedFileModifyRequest(Document d){
    }

    public void receivedFileModifyResponse(Document d){
    }

    public void receivedDirectoryCreateRequest(Document d){}

    public void receivedDirectoryCreateResponse(Document d){}

    public void receivedDirectoryDeleteRequest(Document d){}

    public void receivedDirectoryDeleteResponse(Document d){}

    public void receivedFileBytesRequest(Document d){
    }

    private String base64encodedString(ByteBuffer buf){
        return Base64.getEncoder().encodeToString(buf.array());
    }

}
