package org.lotterm.asterisk.epursuit.agi;

import java.util.ArrayList;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiHangupException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.asteriskjava.live.AsteriskChannel;
import org.lotterm.asterisk.epursuit.EPursuit;

/**
 * @author thomas
 * Agi module which plays the recent recordList
 */
public class AgentAgi extends BaseAgiScript implements Agi {

	protected String extension = "agentAgi";

	// List of listeners
	private final ArrayList<AgiCallListener> listeners = new ArrayList<AgiCallListener>();

	private ArrayList<String> recordList;

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

	public void setRecordList(ArrayList<String> recordList) {
		System.out.println("Setting recordList: " + recordList.size());
		this.recordList = recordList;
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
			if (this.recordList != null && this.recordList.size() > 0) {
				// ...play the introduction...
				this.streamFile(EPursuit.properties.getProperty("agentsIntro"));
				// ...and play the recording
				for (String recording : this.recordList) {
					this.streamFile("beep");
					this.streamFile(EPursuit.properties.getProperty("recordPath") + recording);
				}
			} else {
				System.out.println("Error in Agent Agi Script. RecordList empty!");
				this.sayAlpha("ERROR!");
			}
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
		System.out.println("reached9");
	}
	
	@Override
	public String getExtension() {
		return "agentAgi";
	}

}
