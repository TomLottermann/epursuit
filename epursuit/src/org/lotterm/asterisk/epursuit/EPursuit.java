package org.lotterm.asterisk.epursuit;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.fastagi.AgiScript;
import org.asteriskjava.fastagi.SimpleMappingStrategy;
import org.lotterm.asterisk.epursuit.agi.AgentAgi;
import org.lotterm.asterisk.epursuit.agi.FinalAgi;
import org.lotterm.asterisk.epursuit.agi.MrXAgi;
import org.lotterm.asterisk.epursuit.agi.ThreadedAgiServer;
import org.lotterm.asterisk.epursuit.caller.Caller;
import org.lotterm.asterisk.epursuit.ui.Shell;

/**
 * @author thomas
 */
public class EPursuit {

	public static Properties properties = new Properties();
	
	/**
	 * Loads properties, starts AGI, sets Listeners...
	 * 
	 * @param location
	 */
	public EPursuit(String location) {
		try {
			// load properties
			this.loadProperties(location);

			// create the AGI-modules
			final AgentAgi agentAgi = new AgentAgi();
			final MrXAgi mrxAgi = new MrXAgi();
			final FinalAgi finalAgi = new FinalAgi();

			
			// start AGI-server
			this.startServer(agentAgi, mrxAgi, finalAgi);
			
			Caller caller = new Caller(agentAgi, mrxAgi, finalAgi);
			
			Logger.getLogger("org.asteriskjava.live.internal.ChannelManager").setUseParentHandlers(false);
			Logger.getLogger("org.asteriskjava.live.internal.AsteriskServerImpl").setUseParentHandlers(false);
			Logger.getLogger("org.asteriskjava.fastagi.internal.AgiConnectionHandler").setUseParentHandlers(false);
			Logger.getLogger("org.asteriskjava.fastagi.internal.AgiConnectionHandler").setLevel(Level.OFF);
			
			new Shell(caller);
			
		} catch (IOException e) {
			System.out.println("Unable to read properties file. DIED");
			e.printStackTrace();
		}
	}

	/**
	 * Starts the AGI-server with the two modules
	 * 
	 * @param agentAgi	Agents module
	 * @param mrxAgi		MrX module
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private void startServer(AgentAgi agentAgi, MrXAgi mrxAgi, FinalAgi finalAgi) throws IllegalStateException, IOException {
		final SimpleMappingStrategy mapping = new SimpleMappingStrategy();

		final Map<String, AgiScript> mappingMap = new HashMap<String, AgiScript>();

		mappingMap.put(agentAgi.getExtension() + ".agi", agentAgi);
		mappingMap.put(mrxAgi.getExtension() + ".agi", mrxAgi);
		mappingMap.put(finalAgi.getExtension() + ".agi", finalAgi);

		mapping.setMappings(mappingMap);

		new ThreadedAgiServer(mapping, new Integer(EPursuit.properties.getProperty("agiPort")));
	}

	/**
	 * Loads properties-file
	 * 
	 * @param location
	 * @throws IOException
	 */
	private void loadProperties(String location) throws IOException {
		// put properties file into a stream
		BufferedInputStream stream = new BufferedInputStream(new FileInputStream(location));

		// load it into properties
		EPursuit.properties.load(stream);
		stream.close();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws IllegalStateException
	 */
	public static void main(String[] args) throws IllegalStateException, IOException {
		if(args.length==1) {
			new EPursuit(args[0]);			
		} else {
			System.out.println("Wrong syntax!");
			System.out.println("Please add one parameter which leads to the config file!");
		}
	}
	
}
