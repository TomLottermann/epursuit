package org.lotterm.asterisk.scotlandyard.agi;

import java.io.IOException;

import org.asteriskjava.fastagi.DefaultAgiServer;
import org.asteriskjava.fastagi.SimpleMappingStrategy;

public class ThreadedAgiServer extends Thread {
	private DefaultAgiServer server;

	public ThreadedAgiServer(SimpleMappingStrategy mapping, int port) {
		this.server = new DefaultAgiServer(mapping);
		this.server.setPort(port);
		this.start();
	}
	
	public void run() {
		try {
			this.server.startup();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
