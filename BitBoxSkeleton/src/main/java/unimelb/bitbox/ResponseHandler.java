package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

public class ResponseHandler {
    private static Logger log = Logger.getLogger(ResponseHandler.class.getName());
    public static FileSystemManager fManager;
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
    }

    public boolean receivedFileCreateRequest(Document d){
    	boolean accept = false;
    	synchronized (this) {
			String pathName = d.getString("pathName");
			Document desc = (Document) d.get("fileDescriptor");
			FileSystemManager.FileDescriptor fDesc =
					fManager.new FileDescriptor(desc.getLong("lastModified"), desc.getString("md5"), desc.getLong("fileSize"));

			if (fManager.isSafePathName(pathName)) {
				try {
					if (!fManager.fileNameExists(pathName)) {
						//ensure no file in that path and try to create one
						fManager.createFileLoader(pathName, desc.getString("md5"), desc.getLong("fileSize"), desc.getLong("lastModified"));
						if (!fManager.checkShortcut(pathName)) {
							accept = true;
							connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName, "file loader ready", true));
							connection.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDesc, d.getString("pathName"), 0, maximumBlockSize < fDesc.fileSize ? maximumBlockSize : fDesc.fileSize));
							// github
						} else {
							connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName, "file exist already, create a copy from local", false));
						}

					} else if (!fManager.fileNameExists(pathName, fDesc.md5)){
						//pathname already exists
						// check for lastModified value
						if(fManager.modifyFileLoader(pathName, fDesc.md5, fDesc.lastModified)) {
							log.info("file ready for modified");
							accept = true;
							connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName, "file loader ready", true));
							connection.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDesc, d.getString("pathName"), 0, maximumBlockSize < fDesc.fileSize ? maximumBlockSize : fDesc.fileSize));
						}
						else {

							connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName, "pathname already exists", false));
						}
					}
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					log.warning(e.getMessage());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.warning(e.getMessage());
				}
			} else {
				//unsafe pathname given
				connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName, "unsafe pathname given", false));
			}
		}
		return accept;
    }

    public boolean receivedFileCreateResponse(Document d){
    	return d.getBoolean("status");
    }

    public void receivedFileDeleteRequest(Document d){
    	synchronized (this) {
			String pathName = d.getString("pathName");
			Document desc = (Document) d.get("fileDescriptor");
			FileSystemManager.FileDescriptor fDesc =
					fManager.new FileDescriptor(desc.getLong("lastModified"), desc.getString("md5"), desc.getLong("fileSize"));
			if (fManager.isSafePathName(pathName)) {
				try {
					if (fManager.fileNameExists(pathName)) {
						//file has been found and try to delete
						if (fManager.deleteFile(pathName, desc.getLong("lastModified"), desc.getString("md5"))) {
							connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "file deleted", true));
						} else {
							connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "there was a problem deleting the file", false));
						}
					} else {
						//pathname does not exist
						connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "pathname does not exist", false));
					}
				} catch(Exception e){
					log.warning(e.getMessage());
				}
			} else {
				//unsafe pathname given
				connection.sendCommand(JsonUtils.FILE_DELETE_RESPONSE(fDesc, pathName, "unsafe pathname given", false));

			}
		}
    	
    }

    public void receivedFileDeleteResponse(Document d){}

    public boolean receivedFileModifyRequest(Document d){
    	boolean accept = false;
    	synchronized (this) {
			String pathName = d.getString("pathName");
			Document desc = (Document) d.get("fileDescriptor");
			FileSystemManager.FileDescriptor fDesc =
					fManager.new FileDescriptor(desc.getLong("lastModified"), desc.getString("md5"), desc.getLong("fileSize"));

			if (fManager.isSafePathName(pathName)) {

				//check if file exist
				if (fManager.fileNameExists(pathName)) {
					//check if file already exists with matching content
					if (!fManager.fileNameExists(pathName, desc.getString("md5"))) {
						try {
							if (fManager.modifyFileLoader(pathName, desc.getString("md5"), desc.getLong("lastModified"))) {
								accept = true;
								connection.sendCommand(JsonUtils.FILE_MODIFY_RESPONSE(fDesc, pathName, "file loader ready", true));
								connection.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDesc, d.getString("pathName"), 0, maximumBlockSize < fDesc.fileSize ? maximumBlockSize : fDesc.fileSize));
							} else {
								connection.sendCommand(JsonUtils.FILE_MODIFY_RESPONSE(fDesc, pathName, "there was a problem modifying the file", false));
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							log.warning(e.getMessage());
						}
					} else {
						connection.sendCommand(JsonUtils.FILE_MODIFY_RESPONSE(fDesc, pathName, "file already exists with matching content", false));
					}
				} else {
					try {
						if (fManager.createFileLoader(pathName, desc.getString("md5"), desc.getLong("fileSize"),desc.getLong("lastModified"))) {
							accept = true;
							connection.sendCommand(JsonUtils.FILE_MODIFY_RESPONSE(fDesc, pathName, "file loader ready", true));
							connection.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDesc, d.getString("pathName"), 0, maximumBlockSize < fDesc.fileSize ? maximumBlockSize : fDesc.fileSize));
						} else {
							connection.sendCommand(JsonUtils.FILE_MODIFY_RESPONSE(fDesc, pathName, "there was a problem modifying the file", false));
						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						log.warning(e.getMessage());
					} catch (NoSuchAlgorithmException e){
						log.warning(e.getMessage());
					}
				}

			} else {
				//unsafe pathname given
				connection.sendCommand(JsonUtils.FILE_MODIFY_RESPONSE(fDesc, pathName, "unsafe pathname given", false));

			}
		}

		return accept;
    }

    public boolean receivedFileModifyResponse(Document d){
		return d.getBoolean("status");
    }

    public void receivedDirectoryCreateRequest(Document d){
    	synchronized (this) {
			String pathName = d.getString("pathName");

			if (fManager.isSafePathName(pathName)) {
				//if directory is exist
				if (!fManager.dirNameExists(pathName)) {
					//create directory
					if (fManager.makeDirectory(pathName)) {
						connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE(pathName, "directory created", true));
					} else {
						connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE(pathName, "there was a problem creating the directory", false));

					}
				}

			} else {
				//unsafe pathname given
				connection.sendCommand(JsonUtils.DIRECTORY_CREATE_RESPONSE(pathName, "unsafe pathname given", false));

			}
		}
    	
    }

    public void receivedDirectoryCreateResponse(Document d){}

    public void receivedDirectoryDeleteRequest(Document d){
    	synchronized (this) {
			String pathName = d.getString("pathName");

			if (fManager.isSafePathName(pathName)) {
				try {
					if (fManager.dirNameExists(pathName)) {
						//directory has been found and then then try to delete
						if (fManager.deleteDirectory(pathName)) {
							connection.sendCommand(JsonUtils.DIRECTORY_DELETE_RESPONSE(pathName, "directory deleted", true));
						} else {
							connection.sendCommand(JsonUtils.DIRECTORY_DELETE_RESPONSE(pathName, "there was a problem deleting the directory", false));

						}

					} else {
						//pathname does not exist
						connection.sendCommand(JsonUtils.DIRECTORY_DELETE_RESPONSE(pathName, "pathname does not exist", false));
					}
				} catch (Exception e){
					log.warning(e.getMessage());
				}
			} else {
				//unsafe pathname given
				connection.sendCommand(JsonUtils.DIRECTORY_DELETE_RESPONSE(pathName, "unsafe pathname given", false));

			}
		}
    }

    public void receivedDirectoryDeleteResponse(Document d){}

    public void receivedFileBytesRequest(Document d){
		// TODO send file byte response according to the request document
			synchronized (this) {
				Document desc = (Document) d.get("fileDescriptor");
				FileSystemManager.FileDescriptor f = fManager.new FileDescriptor(desc.getLong("lastModified"),
						desc.getString("md5"),
						desc.getLong("fileSize"));

				String pathName = d.getString("pathName");

				try {
					ByteBuffer buf = fManager.readFile(f.md5, d.getLong("position"), d.getLong("length"));
					if(buf == null){
						log.warning("null buffer");
					}
					String content = base64encodedString(buf);

					//check if file exist
					if (fManager.fileNameExists(pathName)) {

						connection.sendCommand(JsonUtils.FILE_BYTES_RESPONSE(f, pathName, d.getLong("position"), d.getLong("length"), content, "successful read", true));

					} else {
						//if failed cancel file loader and send failure response
						fManager.cancelFileLoader(pathName);
						connection.sendCommand(JsonUtils.FILE_BYTES_RESPONSE(f, pathName, d.getLong("position"), d.getLong("length"), content, "unsuccessful read", false));
					}

				} catch (IOException e) {
					log.warning(e.getMessage());
				} catch (NoSuchAlgorithmException e) {
					log.warning(e.getMessage());
				}
			}
    }

    public void receivedFileBytesResponse(Document d){
		//TODO handle received byte response from peer
		synchronized (this) {
			Document desc = (Document) d.get("fileDescriptor");
			FileSystemManager.FileDescriptor f = fManager.new FileDescriptor(desc.getLong("lastModified"),
					desc.getString("md5"),
					desc.getLong("fileSize"));

			String pathName = d.getString("pathName");
			String content = d.getString("content");

			try {

				ByteBuffer buf = convertStringToByte(content);
				//write to the file
				if (!fManager.writeFile(pathName, buf, d.getLong("position"))) {
					//check if file has been written completely
					//fManager.checkWriteComplete(pathName);
					//if failed cancel file loader and send failure response
					fManager.cancelFileLoader(pathName);
				}
			} catch (IOException e) {
				log.warning(e.getMessage());
			}
		}
	}

	//ByteBuffer to String
    private String base64encodedString(ByteBuffer buf){
        return Base64.getEncoder().encodeToString(buf.array());
    }


	//String to Bytebuffer
    private ByteBuffer convertStringToByte(String content) throws UnsupportedEncodingException {
		return ByteBuffer.wrap(Base64.getDecoder().decode(content));
	}


}
