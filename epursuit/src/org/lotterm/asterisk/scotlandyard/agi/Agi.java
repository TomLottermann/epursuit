package org.lotterm.asterisk.scotlandyard.agi;

public interface Agi {

	/**
	 * Extension used by Asterisk to adress this Agi. 
	 * 
	 * @return
	 */
	public String getExtension();
	
	/**
	 * Adds a CallListener to the list of listeners
	 * 
	 * @param listener
	 */
	public void addListener(AgiCallListener listener);
}
