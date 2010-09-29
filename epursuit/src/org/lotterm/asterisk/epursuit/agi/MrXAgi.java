package org.lotterm.asterisk.epursuit.agi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiHangupException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.lotterm.asterisk.epursuit.EPursuit;

/**
 * @author thomas 
 * Agi module which records the calls to MrX
 */
public class MrXAgi extends BaseAgiScript implements Agi {

	// Filename randomizer
	private final Random random = new Random();

	// List of listeners
	private final ArrayList<AgiCallListener> listeners = new ArrayList<AgiCallListener>();
	
	// Channel to recordname relation
	private HashMap<String, String> recordMap=new HashMap<String, String>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.lotterm.asterisk.epursuit.agi.Agi#addListener(org.lotterm.asterisk
	 * .scotlandyard.agi.AgiCallListener)
	 */
	@Override
	public void addListener(AgiCallListener listener) {
		if (listener != null)
			this.listeners.add(listener);
	}
	
	/**
	 * Returns the name of the record by channel name
	 * @param channel
	 * @return
	 */
	public String getRecordByChannel(String channel) {
		return this.recordMap.get(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.asteriskjava.fastagi.AgiScript#service(org.asteriskjava.fastagi.
	 * AgiRequest, org.asteriskjava.fastagi.AgiChannel)
	 */
	@Override
	public void service(AgiRequest request, AgiChannel channel) throws AgiException {
		try {
			// Answer the channel...
			this.answer();

			// retrieve a non-existent filename
			String fileName;
			int name;
			do {
				name = Math.abs(this.random.nextInt());
				fileName = EPursuit.properties.getProperty("recordPath") + name;
			} while (new File(fileName).exists());

			// Call all listeners
			for (AgiCallListener listener : listeners) {
				listener.callStarted(String.valueOf(name));
			}

			// ...play the introduction...
			this.streamFile(EPursuit.properties.getProperty("mrxIntro"));

			// ..record the file.
			this.recordFile(fileName, "gsm", "brauchkeinschwein", new Integer(EPursuit.properties.getProperty("maxTalkTime")), 0, new Boolean(EPursuit.properties.getProperty("beep")),
					new Integer(EPursuit.properties.getProperty("maxWaitTime")));
			
			this.recordMap.put(channel.getName(), String.valueOf(name));
			// Call all listeners
			for (AgiCallListener listener : listeners) {
				listener.callFinished(channel.getName());
			}
		} catch (AgiHangupException e) {
			for (AgiCallListener listener : listeners) {
				listener.callNotSuccessful(channel.getName());
			}
		}

	}

	@Override
	public String getExtension() {
		return "mrxAgi";
	}

}
