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
        Document desc = (Document)d.get("fileDescriptor");
        String pathName = d.getString("pathName");
        FileSystemManager.FileDescriptor fDesc =
                fManager.new FileDescriptor(desc.getLong("lastModified"),desc.getString("md5"),desc.getLong("fileSize"));

        synchronized (this){
            if(fManager.isSafePathName(pathName)){
                try{
                    if(!fManager.checkShortcut(pathName)){
                        long remainingFileBytes = fDesc.fileSize;
                        int positionTracker = 0;
                        connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName, "ready", true));
                        Document data;
                        ByteBuffer buf = ByteBuffer.allocate((int)fDesc.fileSize);

                        while(remainingFileBytes > 0) {
                            int attemps = 0;
                            while(attemps < maximumRequestResend){
                                connection.sendCommand(JsonUtils.FILE_BYTES_REQUEST(fDesc, pathName, positionTracker,
                                        remainingFileBytes < maximumBlockSize ? remainingFileBytes : maximumBlockSize));
                                data = connection.listenForPackage();

                                if(data == null){
                                    attemps++;
                                }
                                else{
                                    boolean blockRepeat = true;
                                    boolean jumpOutAttemps = false;
                                    while(blockRepeat){
                                        if(data == null)
                                        {
                                            break;
                                        }
                                        else{
                                            if(data.getLong("position") == positionTracker){
                                                blockRepeat = false;
                                                positionTracker += data.getLong("length");
                                                remainingFileBytes -= data.getLong("length");
                                                buf.put(Base64.getDecoder().decode(data.getString("content")));
                                                jumpOutAttemps = true;
                                            }
                                            else{
                                                data = connection.listenForPackage();
                                            }
                                        }
                                    }
                                    if(jumpOutAttemps){
                                        break;
                                    }
                                }
                            }
                            if(attemps == maximumRequestResend){
                                log.warning("file transfer timeout");
                                // TODO close connection with the peer, the peer is dead
                                connection.flagActive = false;
                                connection.closeSocket();
                                break;
                            }
                        }

                        if(remainingFileBytes == 0) {
                            fManager.createFileLoader(pathName, fDesc.md5, fDesc.fileSize, fDesc.lastModified);
                            fManager.writeFile(pathName, buf, 0);
                        }

                    } else {
                        //TODO response with FILE CREATION RESPONSE false
                        connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc, pathName,"file already exist",false));
                    }
                }catch(NoSuchAlgorithmException e){
                    log.warning(e.getMessage());
                }catch(IOException e){
                    log.warning(e.getMessage());
                }
            } else {
                // TODO response with FILE CREATION RESPONSE false
                connection.sendCommand(JsonUtils.FILE_CREATE_RESPONSE(fDesc,pathName,"Unsafe path detected", false));
            }
        }
    }

    public void receivedFileCreateResponse(Document d){

        if(d.getBoolean("status")) {
            Document descriptor = (Document) d.get("fileDescriptor");
            FileSystemManager.FileDescriptor f = fManager.new FileDescriptor(descriptor.getLong("lastModified"),
                    descriptor.getString("md5"),
                    descriptor.getLong("fileSize"));

            String pathName = d.getString("pathName");

            if (true) {

                // TODO listen for request
                connection.setByteRequestAvailibility(true);
            }
        }
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

        synchronized (this){
            Document desc = (Document) d.get("fileDescriptor");
            FileSystemManager.FileDescriptor f = fManager.new FileDescriptor(desc.getLong("lastModified"),
                    desc.getString("md5"),
                    desc.getLong("fileSize"));

            String pathName = d.getString("pathName");

            if (true) {
                try {
                    ByteBuffer buf = fManager.readFile(f.md5, d.getLong("position"), d.getLong("length"));
                    String content = base64encodedString(buf);
                    connection.sendCommand(JsonUtils.FILE_BYTES_RESPONSE(f, pathName, d.getLong("position"), d.getLong("length"), content, "block sent", true));
                }catch(IOException e){
                    log.warning(e.getMessage());
                }catch(NoSuchAlgorithmException e){
                    log.warning(e.getMessage());
                }
            }
        }
    }

    private String base64encodedString(ByteBuffer buf){
        return Base64.getEncoder().encodeToString(buf.array());
    }

}
