package org.lotterm.asterisk.epursuit.ui;

import java.io.IOException;
import java.util.ArrayList;

import jline.ConsoleReader;

import org.lotterm.asterisk.epursuit.caller.Caller;
import org.lotterm.asterisk.epursuit.caller.CallerListener;

public class Shell {

	private ArrayList<ShellListener> listeners = new ArrayList<ShellListener>();

	private Caller caller;
	private ConsoleReader console;

	private ShellState state = ShellState.IDLE;

	private Thread mainThread = new Thread() {
		public void run() {
			Shell.this.commandPrompt();
		}
	};
	
	public Shell(Caller caller) {
		this.caller = caller;
		this.caller.addCallerListener(new CallerListener() {

			@Override
			public void testCallFinished() {

				Shell.this.mainThread=new Thread() {
					public void run() {
						if (Shell.this.getShellState() == ShellState.TESTCALL) {
							System.out.println("Test call finished.");
							Shell.this.state = ShellState.IDLE;
							Shell.this.commandPrompt();
						}
						if (Shell.this.getShellState() == ShellState.CALLCYCLE) {
							System.out.println("Test Call finished");
							try {
								boolean success = false;
								while (!success) {
									String line = Shell.this.console.readLine("Was the testcall okay? y: call Agents n: recall Mr X, c: Cancel cycle [Y/n/c] ");
									if (line.toLowerCase().equals("y") || line.equals("")) {
										success = true;

									} else if (line.toLowerCase().equals("n")) {
										success = true;
										System.out.println("Calling MrX...");
										Shell.this.caller.callMrX();
										return;
									} else if (line.toLowerCase().equals("c")) {
										success = true;
										System.out.println("Aborting...");
										Shell.this.state = ShellState.IDLE;
										Shell.this.commandPrompt();
										return;
									}
								}

							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println("Calling agents...");
							Shell.this.caller.callAgents();
						}
					}
				};
				Shell.this.mainThread.start();
			}

			@Override
			public void mrxCallsFinished() {
				Shell.this.mainThread=new Thread() {
					public void run() {
						if (Shell.this.getShellState() == ShellState.MRXCALL) {
							System.out.println("MrX calls finished.");
							Shell.this.state = ShellState.IDLE;
							Shell.this.commandPrompt();
						}
						if (Shell.this.getShellState() == ShellState.CALLCYCLE) {
							System.out.println("MrX calls finished. Test calling.");
							Shell.this.caller.testCall();
						}
					}
				};
				Shell.this.mainThread.start();
			}

			@Override
			public void agentCallsFinished() {
				Shell.this.mainThread=new Thread() {
					public void run() {
						if (Shell.this.getShellState() == ShellState.AGENTCALL) {
							System.out.println("Agent calls finished.");
							Shell.this.state = ShellState.IDLE;
							Shell.this.commandPrompt();
						}
						if (Shell.this.getShellState() == ShellState.CALLCYCLE) {
							System.out.println("Agent calls finished.");
							System.out.println("Callcycle finished.");
							Shell.this.state = ShellState.IDLE;
							Shell.this.commandPrompt();
						}
					}
				};
				Shell.this.mainThread.start();

			}

			@Override
			public void finalCallsFinished() {
				Shell.this.mainThread=new Thread() {
					public void run() {
						if (Shell.this.getShellState() == ShellState.FINALCALL) {
							System.out.println("Final calls finished.");
							Shell.this.state = ShellState.IDLE;
							Shell.this.commandPrompt();
						}
					}
				};
				Shell.this.mainThread.start();
			}
		});

		try {
			this.console = new ConsoleReader();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.mainThread.start();
	}

	public void addShellListener(ShellListener listener) {
		this.listeners.add(listener);
	}

	public ShellState getShellState() {
		return this.state;
	}

	private void commandPrompt() {
		if (this.getShellState() == ShellState.IDLE) {
			try {
				String line = this.console.readLine("ePursuit # ");
				this.newCommand(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("CODEFEHLER!");
		}
	}

	private void newCommand(String line) {
		if (line.equals("call mrx")) {
			System.out.println("Calling MrX...");
			this.state = ShellState.MRXCALL;
			this.caller.callMrX();
		} else if (line.equals("call test")) {
			System.out.println("Testcalling...");
			this.state = ShellState.TESTCALL;
			this.caller.testCall();
		} else if (line.equals("call agents")) {
			System.out.println("Calling agents...");
			this.state = ShellState.AGENTCALL;
			this.caller.callAgents();
		} else if (line.equals("call cycle")) {
			System.out.println("Starting callcycle...");
			System.out.println("Calling MrX...");
			this.state = ShellState.CALLCYCLE;
			this.caller.callMrX();
		} else if (line.equals("call final")) {
			System.out.println("Starting final call...");
			System.out.println("Calling agents...");
			this.state = ShellState.FINALCALL;
			this.caller.finalCall();
		} else if (line.equals("quit")) {
			// TODO: Better!!!!! Close manager hangup...
			System.out.println("Quitting...");
			System.exit(0);
		} else if (line.equals("")) {
			this.commandPrompt();
		} else {
			System.out.println("Unknown command.");
			this.commandPrompt();
		}
	}
}
