package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver, Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	private float timer;
	private TCPMain TCP;
	private UDPMain UDP;
	private int syncTIme = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
	private String mode;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		timer = 0;
		ResponseHandler.fManager = fileSystemManager;
		Connection.blockSize = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));

		mode = Configuration.getConfigurationValue("mode");
		if(mode.equals("tcp")) {
			TCP = new TCPMain();
		} else {
			UDP = new UDPMain();
		}


		Thread t = new Thread(this);
		t.start();
	}

	public void run(){
		// create a timer to count for sync events
		while(true){
			long start = System.currentTimeMillis();
			//log.info("time: " + timer/1000);
			if(timer >= syncTIme*1000){
				ArrayList<FileSystemEvent> events = fileSystemManager.generateSyncEvents();

				for(FileSystemEvent e:events){
					processFileSystemEvent(e);
				}
				timer = 0;
			}
			timer += (System.currentTimeMillis() - start);
		}
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events
		if(mode.equals("tcp"))
			TCP.addEvent(fileSystemEvent);
		else{
			UDP.addEvent(fileSystemEvent);
		}
	}

	/**
	 * Connect with the peer
	 * @param ip    ip address
	 * @param port  port number
	 * @return  boolean value indicate whether the connection is successful
	 *
	 * 	 * if the destination peer already connected, the return value is true
	 * 	 * if the peer connection successful, the return value is true
	 * 	 * if the peer connection failed, the return value is true
	 */
	public boolean connectTo(String ip, int port){
		if(mode.equals("tcp")){
			return TCP.peerConnectWith(ip, port);
		} else {
			return UDP.peerConnectWith(ip, port);
		}
	}

	/**
	 * Disconnect with a peer
	 * @param ip ip address
	 * @param port port number
	 * @return boolean value indicate whether the disconnection is successful
	 *
	 * 		* if there is no connection exist with given ip and port, return true
	 * 		* if successfully disconnected, return true
	 * 		* if there is ongoing file transfer activity, return false. The connection can be forced to shutdown by calling forceDisconnection() method
	 */
	public boolean disconnectTo(String ip, int port){
		if(mode.equals("tcp")){
			return TCP.peerDisconnectWith(ip, port);
		} else {
			return UDP.peerDisconnectWith(ip, port);
		}
	}

	/**
	 * Force disconnect with a peer
	 * @param ip
	 * @param port
	 * @return always returns true
	 */
	public boolean forceDisconnect(String ip, int port){
		if(mode.equals("tcp")){
			return TCP.forceDisconnection(ip, port);
		} else {
			return UDP.forceDisconnection(ip, port);
		}
	}
}
