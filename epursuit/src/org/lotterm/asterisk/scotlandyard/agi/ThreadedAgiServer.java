package org.lotterm.asterisk.scotlandyard.agi;

import java.io.IOException;

import org.asteriskjava.fastagi.DefaultAgiServer;
import org.asteriskjava.fastagi.SimpleMappingStrategy;

/**
 * @author thomas
 * Starts up the Agi Server in a Thread
 */
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
