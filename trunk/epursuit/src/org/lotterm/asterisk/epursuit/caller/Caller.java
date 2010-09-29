package org.lotterm.asterisk.epursuit.caller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.TimeoutException;
import org.lotterm.asterisk.epursuit.EPursuit;
import org.lotterm.asterisk.epursuit.agi.AgentAgi;
import org.lotterm.asterisk.epursuit.agi.Agi;
import org.lotterm.asterisk.epursuit.agi.MrXAgi;

/**
 * @author thomas Sends calls and organizes order
 */
public class Caller {

	private Logger log = Logger.getLogger(this.getClass().getCanonicalName());

	// List of the recorded calls
	private final ArrayList<String> recordList = new ArrayList<String>();

	// Manager Connection
	private ManagerConnection managerConnection;
	// Live connection to Asterisk
	private DefaultAsteriskServer asteriskServer;

	// Call list with the destinations
	private final ArrayList<String> mrxList = new ArrayList<String>();
	private final ArrayList<String> agentList = new ArrayList<String>();

	// The Agi modules where the calls are sent to
	private MrXAgi mrxAgi;
	private AgentAgi agentAgi;

	private Callcycle mrxCalls;
	private Callcycle agentCalls;

	/**
	 * Connect to Manager interface of local Asterisk, AsteriskServer and start
	 * calling MrX
	 * 
	 * @param agentAgi
	 * @param mrxAgi
	 */
	public Caller(AgentAgi agentAgi, MrXAgi mrxAgi) {

		// Read in all MrX
		this.readMrX();

		// Read in all Agents
		this.readAgents();

		this.mrxAgi = mrxAgi;
		this.agentAgi = agentAgi;

		// Start the ManagerFactory and connect up a manager
		ManagerConnectionFactory factory = new ManagerConnectionFactory(EPursuit.properties.getProperty("asteriskHost"), EPursuit.properties.getProperty("managerUser"),
				EPursuit.properties.getProperty("managerPassword"));

		this.managerConnection = factory.createManagerConnection();

		this.asteriskServer = new DefaultAsteriskServer(this.managerConnection);

		try {
			this.managerConnection.login();
		} catch (IllegalStateException e) {
			log.log(Level.WARNING, "Problem occurred while connecting to manager interface: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (AuthenticationFailedException e) {
			log.log(Level.SEVERE, "Authentication with manager failed.");
			e.printStackTrace();
		} catch (TimeoutException e) {
			log.log(Level.SEVERE, "Timeout during manager authentication.");
			e.printStackTrace();
		}

		// Start calling MrX
		this.callMrX();
	}

	/**
	 * Read in all MrX destinations
	 */
	public void readMrX() {
		this.mrxList.clear();
		try {
			BufferedReader mrxReader = new BufferedReader(new FileReader(EPursuit.properties.getProperty("mrxList")));
			String mrxLine;
			try {
				while ((mrxLine = mrxReader.readLine()) != null) {
					this.mrxList.add(mrxLine);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "mrxList could not be found at \"" + EPursuit.properties.getProperty("mrxList") + "\"");
			e.printStackTrace();
		}
	}

	/**
	 * Read in all Agent destinations
	 */
	public void readAgents() {
		try {
			BufferedReader agentReader = new BufferedReader(new FileReader(EPursuit.properties.getProperty("agentList")));
			String agentLine;
			try {
				while ((agentLine = agentReader.readLine()) != null) {
					this.agentList.add(agentLine);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			log.log(Level.SEVERE, "agentList could not be found at \"" + EPursuit.properties.getProperty("agentList") + "\"");
			e.printStackTrace();
		}
	}

	/**
	 * Are all MrX calls finished? => set records and call agents
	 */
	private void testForMrXFinished() {
		if (mrxCalls.isCallCycleFinished()) {
			log.log(Level.INFO, "Done with Mr X");
			agentAgi.setRecordList(recordList);
			log.log(Level.INFO, "Starting with agents");
			//TODO: ???????
			callAgents();
		} else {
			makeNextMrXCall();
		}
	}

	/**
	 * Are all Agent calls finished?
	 */
	private void testForAgentsFinished() {
		if (agentCalls.isCallCycleFinished()) {
			log.log(Level.INFO, "Done with Agents");

			// managerConnection.logoff();
		} else {
			makeNextAgentCall();
		}
	}

	/**
	 * start calling MrX
	 */
	public void callMrX() {
		mrxCalls=new Callcycle();
		for (String mrxDestination : mrxList) {
			log.log(Level.INFO, "Calling: " + mrxDestination);
			Call call = new Call(mrxDestination, this.mrxAgi, this.managerConnection, this.asteriskServer);
			call.addListener(new CallListener() {

				@Override
				public void callNotAnswered(String destination, Agi extension) {
					// give up calling that guy!
					log.log(Level.WARNING, "Giving up to call " + destination + ". The number is perhabs wrong.");

					testForMrXFinished();
				}

				@Override
				public void callFinished(String destination, Agi extension, String channel) {

					String record = mrxAgi.getRecordByChannel(channel);

					if (record != null) {
						recordList.add(record);
					} else {
						System.out.println("RECORD NULL: " + destination + " " + channel);
					}

					testForMrXFinished();
				}

			});
			mrxCalls.add(call);
			// TODO: folgendes ersetzen!
			//call.start();
		}
		for(int i=0; i<new Integer(EPursuit.properties.getProperty("maxCalls")); i++) {
			makeNextMrXCall();		
		}
	}

	private void makeNextMrXCall() {
		if(mrxCalls.countRunningCalls()<=new Integer(EPursuit.properties.getProperty("maxCalls"))) {
			Call unusedCall = mrxCalls.getUnusedCall();
			// TODO: catch null!!!!
			unusedCall.start();
		}
	}

	/**
	 * Call all agents
	 */
	public void callAgents() {
		agentCalls=new Callcycle();
		try {

			for (String agentDestination : agentList) {
				log.log(Level.INFO, "Calling: " + agentDestination);
				Call call = new Call(agentDestination, this.agentAgi, this.managerConnection, this.asteriskServer);
				call.addListener(new CallListener() {

					@Override
					public void callNotAnswered(String destination, Agi extension) {
						// give up calling that guy!
						log.log(Level.WARNING, "Giving up to call " + destination);

						testForAgentsFinished();
					}

					@Override
					public void callFinished(String destination, Agi extension, String channel) {
						// Call successful => one call less to bother about.

						testForAgentsFinished();
					}
				});
				agentCalls.add(call);
				// TODO: folgendes ersetzen!
				//call.start();
				
			}
			for(int i=0; i<new Integer(EPursuit.properties.getProperty("maxCalls")); i++)
				makeNextAgentCall();

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void makeNextAgentCall() {
		if(agentCalls.countRunningCalls()<=new Integer(EPursuit.properties.getProperty("maxCalls"))) {
			Call unusedCall = agentCalls.getUnusedCall();
			// TODO: catch null!!!!
			unusedCall.start();
		}
	}

}
