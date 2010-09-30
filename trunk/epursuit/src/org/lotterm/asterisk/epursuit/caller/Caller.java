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
	
	private ArrayList<CallerListener> listeners=new ArrayList<CallerListener>();

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
			this.log.log(Level.WARNING, "Problem occurred while connecting to manager interface: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (AuthenticationFailedException e) {
			this.log.log(Level.SEVERE, "Authentication with manager failed.");
			e.printStackTrace();
		} catch (TimeoutException e) {
			this.log.log(Level.SEVERE, "Timeout during manager authentication.");
			e.printStackTrace();
		}

		// Start calling MrX
		this.callMrX();
	}
	
	public void addCallerListener(CallerListener listener) {
		this.listeners.add(listener);
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
			this.log.log(Level.SEVERE, "mrxList could not be found at \"" + EPursuit.properties.getProperty("mrxList") + "\"");
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
			this.log.log(Level.SEVERE, "agentList could not be found at \"" + EPursuit.properties.getProperty("agentList") + "\"");
			e.printStackTrace();
		}
	}

	/**
	 * Are all MrX calls finished? => set records and call agents
	 */
	private void testForMrXFinished() {
		if (this.mrxCalls.isCallCycleFinished()) {
			this.log.log(Level.INFO, "Done with Mr X");
			
			this.agentAgi.setRecordList(this.recordList);
			
			for (CallerListener listener : this.listeners) {
				listener.mrxCallsFinished();
			}
		} else {
			this.makeNextMrXCall();
		}
	}

	/**
	 * Are all Agent calls finished?
	 */
	private void testForAgentsFinished() {
		if (this.agentCalls.isCallCycleFinished()) {
			this.log.log(Level.INFO, "Done with Agents");
			for (CallerListener listener : this.listeners) {
				listener.agentCallsFinished();
			}
			
			// managerConnection.logoff();
		} else {
			this.makeNextAgentCall();
		}
	}

	/**
	 * start calling MrX
	 */
	public void callMrX() {
		this.mrxCalls = new Callcycle();
		for (String mrxDestination : this.mrxList) {
			this.log.log(Level.INFO, "Calling: " + mrxDestination);
			Call call = new Call(mrxDestination, this.mrxAgi, this.managerConnection, this.asteriskServer);
			call.addListener(new CallListener() {

				@Override
				public void callNotAnswered(String destination, Agi extension) {
					// give up calling that guy!
					Caller.this.log.log(Level.WARNING, "Giving up to call " + destination + ". The number is perhabs wrong.");

					Caller.this.testForMrXFinished();
				}

				@Override
				public void callFinished(String destination, Agi extension, String channel) {

					String record = Caller.this.mrxAgi.getRecordByChannel(channel);

					if (record != null) {
						Caller.this.recordList.add(record);
					} else {
						System.out.println("RECORD NULL: " + destination + " " + channel);
					}

					Caller.this.testForMrXFinished();
				}

			});
			this.mrxCalls.add(call);
			// TODO: folgendes ersetzen!
			// call.start();
		}
		for (int i = 0; i < new Integer(EPursuit.properties.getProperty("maxCalls")); i++) {
			this.makeNextMrXCall();
		}
	}

	private void makeNextMrXCall() {
		if (this.mrxCalls.countRunningCalls() <= new Integer(EPursuit.properties.getProperty("maxCalls"))) {
			Call unusedCall = this.mrxCalls.getUnusedCall();
			// TODO: catch null!!!!
			unusedCall.start();
		}
	}

	/**
	 * Call all agents
	 */
	public void callAgents() {
		this.agentCalls = new Callcycle();
		try {

			for (String agentDestination : this.agentList) {
				this.log.log(Level.INFO, "Calling: " + agentDestination);
				Call call = new Call(agentDestination, this.agentAgi, this.managerConnection, this.asteriskServer);
				call.addListener(new CallListener() {

					@Override
					public void callNotAnswered(String destination, Agi extension) {
						// give up calling that guy!
						Caller.this.log.log(Level.WARNING, "Giving up to call " + destination);

						Caller.this.testForAgentsFinished();
					}

					@Override
					public void callFinished(String destination, Agi extension, String channel) {
						// Call successful => one call less to bother about.

						Caller.this.testForAgentsFinished();
					}
				});
				this.agentCalls.add(call);
				// TODO: folgendes ersetzen!
				// call.start();

			}
			for (int i = 0; i < new Integer(EPursuit.properties.getProperty("maxCalls")); i++)
				this.makeNextAgentCall();

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void makeNextAgentCall() {
		if (this.agentCalls.countRunningCalls() <= new Integer(EPursuit.properties.getProperty("maxCalls"))) {
			Call unusedCall = this.agentCalls.getUnusedCall();
			// TODO: catch null!!!!
			unusedCall.start();
		}
	}

	/**
	 * make test call
	 */
	public void testCall() {
		this.log.log(Level.INFO, "Test Call: " + EPursuit.properties.getProperty("testCall"));
		Call call = new Call(EPursuit.properties.getProperty("testCall"), this.agentAgi, this.managerConnection, this.asteriskServer);
		call.addListener(new CallListener() {

			@Override
			public void callNotAnswered(String destination, Agi extension) {
				// give up calling that guy!
				Caller.this.log.log(Level.WARNING, "Giving up to call " + destination + ". The number is perhabs wrong.");
				for (CallerListener listener : Caller.this.listeners) {
					listener.testCallFinished();
				}
			}

			@Override
			public void callFinished(String destination, Agi extension, String channel) {
				Caller.this.log.log(Level.INFO, "Testcall finished");
				for (CallerListener listener : Caller.this.listeners) {
					listener.testCallFinished();
				}
			}

		});
		call.start();
	}

}
