package org.lotterm.asterisk.scotlandyard.caller;

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
import org.lotterm.asterisk.scotlandyard.ScotlandYard;
import org.lotterm.asterisk.scotlandyard.agi.AgentAgi;
import org.lotterm.asterisk.scotlandyard.agi.Agi;
import org.lotterm.asterisk.scotlandyard.agi.MrXAgi;

public class Caller {

	private Logger log = Logger.getLogger(this.getClass().getCanonicalName());

	// List of the recorded calls
	private final ArrayList<String> recordList = new ArrayList<String>();

	// Manager Connection
	private ManagerConnection managerConnection;
	// Live connection to Asterisk
	private DefaultAsteriskServer asteriskServer;

	private final ArrayList<String> mrxList = new ArrayList<String>();
	private final ArrayList<String> agentList = new ArrayList<String>();
	
	private int unfinishedMrXCalls = 0;
	private int unfinishedAgentCalls = 0;

	private MrXAgi mrxAgi;
	private AgentAgi agentAgi;

	/**
	 * Setup listener, connect to Manager interface of local Asterisk
	 * 
	 * @param mrx
	 */
	public Caller(AgentAgi agentAgi, MrXAgi mrxAgi) {

		// Read in all MrX
		this.readMrX();

		// Read in all Agents
		this.readAgents();
		
		
		this.mrxAgi=mrxAgi;
		this.agentAgi = agentAgi;

		// TODO: DEV: change 192.168.2.33 to localhost
		ManagerConnectionFactory factory = new ManagerConnectionFactory("192.168.2.33", ScotlandYard.properties.getProperty("managerUser"), ScotlandYard.properties.getProperty("managerPassword"));

		this.managerConnection = factory.createManagerConnection();

		this.asteriskServer = new DefaultAsteriskServer(this.managerConnection);

		try {
			this.managerConnection.login();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticationFailedException e) {
			System.err.println("Authentication with manager failed.");
			e.printStackTrace();
		} catch (TimeoutException e) {
			System.err.println("Timeout during manager authentication.");
			e.printStackTrace();
		}

		this.callMrX();
	}

	private void readMrX() {
		this.mrxList.clear();
		try {
			BufferedReader mrxReader = new BufferedReader(new FileReader(ScotlandYard.properties.getProperty("mrxList")));
			String mrxLine;
			try {
				while ((mrxLine = mrxReader.readLine()) != null) {
					this.mrxList.add(mrxLine);
				}
				this.unfinishedMrXCalls = this.mrxList.size();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			System.err.println("mrxList could not be found!");
			e.printStackTrace();
		}
	}

	private void readAgents() {
		try {
			BufferedReader agentReader = new BufferedReader(new FileReader(ScotlandYard.properties.getProperty("agentList")));
			String agentLine;
			try {
				while ((agentLine = agentReader.readLine()) != null) {
					this.agentList.add(agentLine);
				}
				this.unfinishedAgentCalls = this.agentList.size();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			System.err.println("agentList could not be found!");
			e.printStackTrace();
		}
	}

	private void testForMrXFinished() {
		if (unfinishedMrXCalls == 0) {
			log.log(Level.INFO, "Done with Mr X");
			agentAgi.setRecordList(recordList);
			log.log(Level.INFO, "Starting with agents");
			callAgents();
		}
	}

	private void testForAgentsFinished() {
		if (unfinishedAgentCalls == 0) {
			log.log(Level.INFO, "DONE with Agents");

			// managerConnection.logoff();
		}
	}

	private void callMrX() {
		try {

			for (String mrxDestination : mrxList) {
				log.log(Level.INFO, "Calling: " + mrxDestination);
				Call call = new Call(mrxDestination, this.mrxAgi, this.managerConnection, this.asteriskServer);
				call.addListener(new CallListener() {

					@Override
					public void callNotAnswered(String destination, Agi extension) {
						// give up calling that guy!
						log.log(Level.WARNING, "Giving up to call " + destination);
						unfinishedMrXCalls--;

						testForMrXFinished();
					}

					@Override
					public void callFinished(String destination, Agi extension, String... record) {
						unfinishedMrXCalls--;
						
						recordList.add(record[0]);

						testForMrXFinished();
					}
				});
			}

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void callAgents() {
		try {

			for (String agentDestination : agentList) {
				log.log(Level.INFO, "Calling: " + agentDestination);
				Call call = new Call(agentDestination, this.agentAgi, this.managerConnection, this.asteriskServer);
				call.addListener(new CallListener() {

					@Override
					public void callNotAnswered(String destination, Agi extension) {
						// give up calling that guy!
						log.log(Level.WARNING, "Giving up to call " + destination);
						unfinishedAgentCalls--;

						testForAgentsFinished();
					}

					@Override
					public void callFinished(String destination, Agi extension, String... record) {
						unfinishedAgentCalls--;

						testForAgentsFinished();
					}
				});
			}

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
