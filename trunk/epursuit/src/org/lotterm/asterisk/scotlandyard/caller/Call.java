package org.lotterm.asterisk.scotlandyard.caller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.action.OriginateAction;
import org.lotterm.asterisk.scotlandyard.ScotlandYard;
import org.lotterm.asterisk.scotlandyard.agi.Agi;
import org.lotterm.asterisk.scotlandyard.agi.AgiCallListener;
import org.lotterm.asterisk.scotlandyard.caller.originate.OriginateCallbackAdapter;

/**
 * @author thomas
 * Call class. Makes and observes call.
 */
public class Call extends Thread {

	private Logger log = Logger.getLogger(this.getClass().getCanonicalName());

	private ManagerConnection managerConnection;
	private DefaultAsteriskServer asteriskServer;

	private String destination;
	private Agi agi;

	private String currentChannel;
	private OriginateAction originateAction;

	private boolean success = false;
	private int tries = 0;

	private Timer timeoutTimer = new Timer();

	private ArrayList<CallListener> listeners = new ArrayList<CallListener>();

	/**
	 * Constructs the object and starts the thread
	 * 
	 * @param destination			Call destination
	 * @param agi					Agi module associated with the call
	 * @param managerConnection		Necessary for control
	 * @param asteriskServer		Necessary for control
	 */
	public Call(final String destination, final Agi agi, ManagerConnection managerConnection, DefaultAsteriskServer asteriskServer) {
		this.destination = destination;
		this.agi = agi;
		this.managerConnection = managerConnection;
		this.asteriskServer = asteriskServer;

		this.agi.addListener(new AgiCallListener() {

			@Override
			public void callStarted(String channel) {
				// TODO: Delete or no delete?
			}

			@Override
			public void callNotSuccessful(String channel) {
				if (channel.equals(currentChannel)) {
					success = false;
					noAnswer();
				}
			}

			@Override
			public void callFinished(String channel, String... record) {
				if (channel.equals(currentChannel)) {
					for (CallListener listener : listeners) {
						listener.callFinished(destination, agi, record);
					}
				}
			}
		});

		this.start();
	}

	/**
	 * Registers a listener
	 * 
	 * @param listener
	 */
	public void addListener(CallListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Increases the tries counter by one and test for maxTries.
	 */
	private void noAnswer() {

		try {
			this.timeoutTimer.cancel();
		} catch (IllegalStateException e) {
			System.out.println("Unable to cancel timer");
		}

		this.timeoutTimer = new Timer();

		// Plus one failed try
		tries++;

		// too many tries => tell listeners
		if (tries >= new Integer(ScotlandYard.properties.getProperty("maxTries"))) {
			for (CallListener listener : listeners) {
				listener.callNotAnswered(destination, agi);
			}
		} else {
			// schedule retry			
			this.timeoutTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					if (!success) {
						makeCall();
					}
				}
			}, new Long(ScotlandYard.properties.getProperty("retryTime")));
		}

	}

	/**
	 * All the magic happens here. Call and register listeners
	 */
	private void makeCall() {
		// send the originate action (Call)
		this.asteriskServer.originateAsync(originateAction, new OriginateCallbackAdapter() {
			@Override
			public void onDialing(final AsteriskChannel asteriskChannel) {
				log.log(Level.INFO, "Dialing: " + asteriskChannel.getName());
				currentChannel = asteriskChannel.getName();
				// Listen for cool stuff like "ringing"
				asteriskChannel.addPropertyChangeListener(new PropertyChangeListener() {

					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						// is the phone ringing?
						if (evt.getPropertyName().equals("state") && evt.getNewValue().toString().equals("RINGING")) {
							// Schedule hangup timeout
							timeoutTimer.schedule(new TimerTask() {

								@Override
								public void run() {
									if (!success) {
										asteriskServer.getChannelByName(currentChannel).hangup();
										// no "noAnswer()" called here because a onNoAnswer event will come in anyway
									}
								}
							}, new Long(ScotlandYard.properties.getProperty("callTime")));
						}
					}
				});
			}

			@Override
			public void onSuccess(AsteriskChannel asteriskChannel) {
				success = true;
				log.log(Level.INFO, "Connection successful: " + asteriskChannel.getName());

			}

			@Override
			public void onNoAnswer(AsteriskChannel asteriskChannel) {
				log.log(Level.INFO, "Channel not answered: " + currentChannel);
				noAnswer();
			}

			@Override
			public void onBusy(AsteriskChannel asteriskChannel) {
				noAnswer();

				log.log(Level.INFO, "Busy: " + asteriskChannel.getName());
			}

			@Override
			public void onFailure(LiveException cause) {
				noAnswer();

				if (cause.getClass().getCanonicalName().equals("org.asteriskjava.live.NoSuchChannelException")) {
					// Called when the channel is busy... dunno why
					log.log(Level.INFO, "Channel perhabs busy. Retrying...");
				} else {
					log.log(Level.WARNING, "Received unknown error. Retrying... \n" + cause);
				}

			}
		});

	}

	public void run() {
		
		// set up the Call
		this.originateAction = new OriginateAction();
		this.originateAction.setChannel(destination); // SIP/1083484/01703846797
		this.originateAction.setContext(ScotlandYard.properties.getProperty("context"));
		this.originateAction.setExten(agi.getExtension());
		this.originateAction.setPriority(new Integer(1));
		// TODO: Which timeout to use? Occurs sometimes when the phone is down or never receives an event (for some reason)
		this.originateAction.setTimeout(new Long(new Integer(ScotlandYard.properties.getProperty("callTime")) + 20000));
		this.originateAction.setAsync(true);
		
		// make initial call
		this.makeCall();
	}
}
