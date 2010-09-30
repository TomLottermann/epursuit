package org.lotterm.asterisk.epursuit.caller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.action.OriginateAction;
import org.lotterm.asterisk.epursuit.EPursuit;
import org.lotterm.asterisk.epursuit.agi.Agi;
import org.lotterm.asterisk.epursuit.agi.AgiCallListener;
import org.lotterm.asterisk.epursuit.caller.originate.OriginateCallbackAdapter;

/**
 * @author thomas Call class. Makes and observes call.
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

	private CallState state=CallState.NOSTART;
	
	private boolean hangup=false;

	/**
	 * Constructs the object and starts the thread
	 * 
	 * @param destination
	 *            Call destination
	 * @param agi
	 *            Agi module associated with the call
	 * @param managerConnection
	 *            Necessary for control
	 * @param asteriskServer
	 *            Necessary for control
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
				if (channel.equals(Call.this.currentChannel)) {
					Call.this.success = false;
					Call.this.noAnswer();

				}
			}

			@Override
			public void callFinished(String channel) {
				if (channel.equals(Call.this.currentChannel)) {
					try {
						//TODO: FIX THIS!!!
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Call.this.state=CallState.SUCCESSFUL;
					for (CallListener listener : Call.this.listeners) {
						listener.callFinished(destination, agi, Call.this.currentChannel);
					}
				}
			}
		});
	}
	
	@Override
	public void start() {
		this.hangup=false;
		this.state=CallState.BUSY;
		super.start();
	}
	
	public CallState getCallState() {
		return this.state;
	}
	
	public String getDestination() {
		return this.destination;
	}
	
	public void hangup() {
		try {
			this.asteriskServer.getChannelByName(this.currentChannel).hangup();
			this.hangup=true;
		} catch(Exception e) {}
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
		this.tries++;

		// too many tries => tell listeners
		if (this.tries >= new Integer(EPursuit.properties.getProperty("maxTries"))) {
			this.state=CallState.TIMEOUT;
			for (CallListener listener : this.listeners) {
				listener.callNotAnswered(this.destination, this.agi);
			}
		} else {
			this.state=CallState.RETRY;
			// schedule retry
			this.timeoutTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					if (!Call.this.success) {
						Call.this.makeCall();
					}
				}
			}, new Long(EPursuit.properties.getProperty("retryTime")));
		}

	}

	private void callRejected() {
		System.out.println("CALL REJECTED!!!!!!!");
		try {
			this.timeoutTimer.cancel();
		} catch (IllegalStateException e) {
			System.out.println("Unable to cancel timer");
		}

		this.timeoutTimer = new Timer();

		// schedule retry
		this.timeoutTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (!Call.this.success) {
					Call.this.makeCall();
				}
			}
		}, new Long(EPursuit.properties.getProperty("callTime")) * 2);

	}

	/**
	 * All the magic happens here. Call and register listeners
	 */
	private void makeCall() {
		// send the originate action (Call)
		this.asteriskServer.originateAsync(this.originateAction, new OriginateCallbackAdapter() {
			@Override
			public void onDialing(final AsteriskChannel asteriskChannel) {
				Call.this.log.log(Level.INFO, "Dialing: " + asteriskChannel.getName() + " " + Call.this.destination);
				Call.this.state=CallState.DIALING;
				Call.this.currentChannel = asteriskChannel.getName();
				// Make sure when there was a hangup event that it is realy hung up.
				if(Call.this.hangup)
					Call.this.hangup();
				// Listen for cool stuff like "ringing"
				asteriskChannel.addPropertyChangeListener(new PropertyChangeListener() {

					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						// is the phone ringing?
						if (evt.getPropertyName().equals("state") && evt.getNewValue().toString().equals("RINGING")) {
							log.log(Level.INFO, "Ringing: " + asteriskChannel.getName() + " " + Call.this.destination);
							// Schedule hangup timeout
							Call.this.timeoutTimer.schedule(new TimerTask() {

								@Override
								public void run() {
									if (!Call.this.success) {
										Call.this.asteriskServer.getChannelByName(Call.this.currentChannel).hangup();
										// no "noAnswer()" called here because a
										// onNoAnswer event will come in anyway
									}
								}
							}, new Long(EPursuit.properties.getProperty("callTime")));
						}
					}
				});
			}

			@Override
			public void onSuccess(AsteriskChannel asteriskChannel) {
				Call.this.state=CallState.RUNNING;
				Call.this.success = true;
				Call.this.log.log(Level.INFO, "Connection successful: " + asteriskChannel.getName() + " " + Call.this.destination);

			}

			@Override
			public void onNoAnswer(AsteriskChannel asteriskChannel) {
				Call.this.log.log(Level.INFO, "Channel not answered: " + Call.this.currentChannel + " " + Call.this.destination);
				if (asteriskChannel.getHangupCause().toString().equals("CALL_REJECTED")) {
					Call.this.noAnswer();
				} else {
					Call.this.noAnswer();
				}
			}

			@Override
			public void onBusy(AsteriskChannel asteriskChannel) {
				Call.this.noAnswer();

				Call.this.log.log(Level.INFO, "Busy: " + asteriskChannel.getName() + " " + Call.this.destination);
			}

			@Override
			public void onFailure(LiveException cause) {
				Call.this.noAnswer();

				if (cause.getClass().getCanonicalName().equals("org.asteriskjava.live.NoSuchChannelException")) {
					// Called when the channel is busy... dunno why
					Call.this.log.log(Level.INFO, "Channel perhabs busy. " + Call.this.destination);
				} else {
					Call.this.log.log(Level.WARNING, "Received unknown error.\n" + cause + " " + Call.this.destination);
				}

			}
		});

	}

	@Override
	public void run() {

		// set up the Call
		this.originateAction = new OriginateAction();
		this.originateAction.setChannel(this.destination); // SIP/1083484/01703846797
		this.originateAction.setContext(EPursuit.properties.getProperty("context"));
		this.originateAction.setExten(this.agi.getExtension());
		this.originateAction.setPriority(new Integer(1));
		// TODO: Which timeout to use? Occurs sometimes when the phone is down
		// or never receives an event (for some reason)
		this.originateAction.setTimeout(new Long(60000));
		this.originateAction.setAsync(true);

		// make initial call
		this.makeCall();
	}
}
