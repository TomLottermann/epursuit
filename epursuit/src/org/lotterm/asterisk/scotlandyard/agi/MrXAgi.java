package org.lotterm.asterisk.scotlandyard.agi;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiHangupException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.lotterm.asterisk.scotlandyard.ScotlandYard;

/**
 * @author thomas AGI component which receives all the calls from the manager
 *         (Yes it only receives!)
 */
public class MrXAgi extends BaseAgiScript implements Agi {

	// Filename randomizer
	private final Random random = new Random();

	// List of listeners
	private final ArrayList<AgiCallListener> listeners = new ArrayList<AgiCallListener>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.lotterm.asterisk.scotlandyard.agi.Agi#addListener(org.lotterm.asterisk
	 * .scotlandyard.agi.AgiCallListener)
	 */
	@Override
	public void addListener(AgiCallListener listener) {
		if (listener != null)
			this.listeners.add(listener);
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
				fileName = ScotlandYard.properties.getProperty("recordPath") + name;
			} while (new File(fileName).exists());

			// Call all listeners
			for (AgiCallListener listener : listeners) {
				listener.callStarted(String.valueOf(name));
			}

			// ...play the introduction...
			this.streamFile(ScotlandYard.properties.getProperty("mrxIntro"));

			// ..record the file.
			this.recordFile(fileName, "gsm", "brauchkeinschwein", new Integer(ScotlandYard.properties.getProperty("maxTalkTime")), 0, new Boolean(ScotlandYard.properties.getProperty("beep")),
					new Integer(ScotlandYard.properties.getProperty("maxWaitTime")));
			// Call all listeners
			for (AgiCallListener listener : listeners) {
				listener.callFinished(channel.getName(), String.valueOf(name));
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
