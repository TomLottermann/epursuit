package org.lotterm.asterisk.scotlandyard.agi;

/**
 * @author thomas
 * Tells useful call events
 */
public interface AgiCallListener {
	
	/**
	 * Called when call starts
	 * 
	 * @param recordId
	 */
	public void callStarted(String channel);

	/**
	 * Called when call ends and recording finished
	 * 
	 * @param recordId
	 */
	public void callFinished(String channel, String... record);
	
	/**
	 * Called when user hangs up before recording
	 * 
	 * @param channel
	 */
	public void callNotSuccessful(String channel);
	
}
