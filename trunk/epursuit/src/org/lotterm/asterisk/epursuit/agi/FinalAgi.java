package org.lotterm.asterisk.epursuit.agi;

import java.util.ArrayList;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiHangupException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.lotterm.asterisk.epursuit.EPursuit;

/**
 * @author thomas Agi module which plays the recent recordList
 */
public class FinalAgi extends BaseAgiScript implements Agi {

	protected String extension = "finalAgi";

	// List of listeners
	private final ArrayList<AgiCallListener> listeners = new ArrayList<AgiCallListener>();

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
			// Tell everyone that call started
			for (AgiCallListener listener : this.listeners) {
				listener.callStarted(channel.getName());
			}
			// ...play the introduction...
			this.streamFile(EPursuit.properties.getProperty("finalSound"));
			this.hangup();
		} catch (AgiHangupException e) {
			// Hangup not fired because we don't care if someone hangs up!
		}
		fireListenersFinished(channel);
	}

	private synchronized void fireListenersFinished(AgiChannel channel) {
		// Tell everyone that call finished
		for (AgiCallListener listener : this.listeners) {
			listener.callFinished(channel.getName());
		}
	}

	@Override
	public String getExtension() {
		return "finalAgi";
	}

}
