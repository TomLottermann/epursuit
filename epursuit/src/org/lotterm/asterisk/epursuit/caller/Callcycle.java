package org.lotterm.asterisk.epursuit.caller;

import java.util.ArrayList;

/**
 * @author thomas
 */
public class Callcycle {
	
	private ArrayList<Call> list=new ArrayList<Call>();
	
	/**
	 * Add a Call to the cycle
	 * @param call
	 */
	public void add(Call call) {
		this.list.add(call);
	}
	
	/**
	 * Clear the cycle
	 */
	public void clear() {
		this.list.clear();
	}
	
	/**
	 * Returns the Call associated to a destination
	 * 
	 * @param destination
	 * @return
	 */
	public Call getCallByDestination(String destination) {
		for (Call call : this.list) {
			if(call.getDestination().equals(destination)) {
				return call;
			}
		}
		return null;
	}
	
	/**
	 * Returns the Call which has not yet been started
	 * 
	 * @return
	 */
	public Call getUnusedCall() {
		for (Call call : this.list) {
			if(call.getCallState()==CallState.NOSTART) {
				return call;
			}
		}
		return null;
	}
	
	/**
	 * Count Calls which have been started and not ended
	 * 
	 * @return
	 */
	public int countRunningCalls() {
		int count=0;
		for (Call call : this.list) {
			if(call.getCallState()!=CallState.SUCCESSFUL&&call.getCallState()!=CallState.NOSTART&&call.getCallState()!=CallState.TIMEOUT) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Test if the CallCycle is finished or not
	 * 
	 * @return
	 */
	public boolean isCallCycleFinished() {
		int count=0;
		for (Call call : this.list) {
			if(call.getCallState()!=CallState.SUCCESSFUL&&call.getCallState()!=CallState.TIMEOUT) {
				count++;
			}
		}
		return (count==0);
	}
	
}
