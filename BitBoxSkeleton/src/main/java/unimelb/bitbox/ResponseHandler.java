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
    	String pathName = d.getString("pathName");
    	Document desc = (Document)d.get("fileDescriptor");
    	FileSystemManager.FileDescriptor fDesc =
                fManager.new FileDescriptor(desc.getLong("lastModified"),desc.getString("md5"),desc.getLong("fileSize"));
    	
    	if(fManager.isSafePathName(pathName)) {
    		try {
				if(fManager.checkShortcut(pathName)){
					//file has been found and try to delete
					if(fManager.deleteFile(pathName,desc.getLong("lastModified"),desc.getString("md5"))) {
						connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "file deleted", true));
					}else {
						connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "there was a problem deleting the file", false));
					}
					
					
				}else {
					//there was a problem deleting the file
					connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "pathname does not exist", false));
				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				log.warning(e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.warning(e.getMessage());
			}
    	}else {
    		//unsafe pathname given
    		connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "unsafe pathname given", false));
    		
    	}
    	
    }

    public void receivedFileDeleteResponse(Document d){}

    public void receivedFileModifyRequest(Document d){
    }

    public void receivedFileModifyResponse(Document d){
    }

    public void receivedDirectoryCreateRequest(Document d){
    	String pathName = d.getString("pathName");
   	
    	if(fManager.isSafePathName(pathName)) {
    		try {
				if(!fManager.checkShortcut(pathName)){
					//create directory
					if(fManager.makeDirectory(pathName)) {
						connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE( pathName, "directory created", true));
					}else {
						connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE( pathName, "there was a problem creating the directory", false));
					
					}				
				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				log.warning(e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.warning(e.getMessage());
			}
    	}else {
    		//unsafe pathname given
    		connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE(pathName, "unsafe pathname given", false));
    		
    	}
    	
    }

    public void receivedDirectoryCreateResponse(Document d){}

    public void receivedDirectoryDeleteRequest(Document d){
    	String pathName = d.getString("pathName");
       	
    	if(fManager.isSafePathName(pathName)) {
    		try {
				if(fManager.checkShortcut(pathName)){
					//directory has been found and then then try to delete
					if(fManager.deleteDirectory(pathName)) {
						connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE( pathName, "directory deleted", true));
					}else {
						connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE( pathName, "there was a problem creating the directory", false));
					
					}
					
				}else {
					//pathname does not exist
					connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE( pathName, "pathname does not exist", false));
				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				log.warning(e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.warning(e.getMessage());
			}
    	}else {
    		//unsafe pathname given
    		connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE(pathName, "unsafe pathname given", false));
    		
    	}
    }

    public void receivedDirectoryDeleteResponse(Document d){}

    public void receivedFileBytesRequest(Document d){
    }

    private String base64encodedString(ByteBuffer buf){
        return Base64.getEncoder().encodeToString(buf.array());
    }

}
