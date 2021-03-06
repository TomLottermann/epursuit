package org.lotterm.asterisk.epursuit.caller;

import org.lotterm.asterisk.epursuit.agi.Agi;

/**
 * @author thomas
 * connects Caller with Call
 */
public interface CallListener {
	/**
	 * Triggered if the call is not answered as many times as the Config says (maxTries)
	 * @param destination
	 * @param extension
	 */
	public void callNotAnswered(String destination, Agi extension);
	
	/**
	 * Triggered if the call was successful
	 * @param destination
	 * @param extension
	 */
	public void callFinished(String destination, Agi extension, String channel);
}
