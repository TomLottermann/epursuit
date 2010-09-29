package org.lotterm.asterisk.epursuit;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.asteriskjava.fastagi.AgiScript;
import org.asteriskjava.fastagi.SimpleMappingStrategy;
import org.lotterm.asterisk.epursuit.agi.AgentAgi;
import org.lotterm.asterisk.epursuit.agi.MrXAgi;
import org.lotterm.asterisk.epursuit.agi.ThreadedAgiServer;
import org.lotterm.asterisk.epursuit.caller.Caller;

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

			
			// start AGI-server
			this.startServer(agentAgi, mrxAgi);
			
			new Caller(agentAgi, mrxAgi);

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
	private void startServer(AgentAgi agentAgi, MrXAgi mrxAgi) throws IllegalStateException, IOException {
		final SimpleMappingStrategy mapping = new SimpleMappingStrategy();

		final Map<String, AgiScript> mappingMap = new HashMap<String, AgiScript>();

		mappingMap.put(agentAgi.getExtension() + ".agi", agentAgi);
		mappingMap.put(mrxAgi.getExtension() + ".agi", mrxAgi);

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
