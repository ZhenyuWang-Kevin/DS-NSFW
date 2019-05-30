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

		mode = Configuration.getConfigurationValue("mode");
		if(mode.equals("TCP")) {
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
		if(mode.equals("TCP"))
			TCP.addEvent(fileSystemEvent);
		else{
			UDP.addEvent(fileSystemEvent);
		}
	}
}