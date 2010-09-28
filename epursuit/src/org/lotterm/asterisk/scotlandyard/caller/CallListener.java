package org.lotterm.asterisk.scotlandyard.caller;

import org.lotterm.asterisk.scotlandyard.agi.Agi;

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
	public void callFinished(String destination, Agi extension, String... record);
}
