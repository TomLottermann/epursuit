package org.lotterm.asterisk.epursuit.agi;

/**
 * @author thomas
 * Agi interface (necessary for the Call to be more generic)
 */
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
