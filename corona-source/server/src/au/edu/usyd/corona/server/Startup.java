package au.edu.usyd.corona.server;


import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.sensing.CPUUsageMonitor;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.QueryDAO;
import au.edu.usyd.corona.server.scheduler.BaseScheduler;
import au.edu.usyd.corona.server.session.RemoteSessionManager;
import au.edu.usyd.corona.server.session.notifier.NotifierManager;
import au.edu.usyd.corona.server.util.IDGenerator;
import au.edu.usyd.corona.util.SPOTTools;

import com.sun.spot.util.IEEEAddress;

/**
 * Host application for Basestation.
 * 
 * @author Edmund Tse
 * @author Raymes Khoury
 */
public class Startup {
	
	/**
	 * Initialise all the subsystems and start the server program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Init java Logger
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("config/logging.properties"));
		}
		catch (Exception e) {
			SPOTTools.terminate(e);
		}
		Logger logger = Logger.getLogger(Startup.class.getCanonicalName());
		logger.info("Starting up ...");
		
		// Init corona logger
		au.edu.usyd.corona.util.Logger.setOut(System.out);
		au.edu.usyd.corona.util.Logger.setLogLevel(au.edu.usyd.corona.util.Logger.DEBUG);
		
		// Init corona
		Network.initialize(Network.MODE_BASESTATION);
		BaseScheduler.initialize();
		CPUUsageMonitor.initialize();
		TimeSync.initialize();
		logger.info("My address is " + IEEEAddress.toDottedHex(Network.getInstance().getMyAddress()));
		
		// Init DB
		IDGenerator queryIdGenerator = new IDGenerator();
		try {
			QueryDAO qd = DAOFactory.getInstance().getQueryDAO();
			int highestQueryId = qd.getHighestQueryID();
			logger.fine("Found highest query ID to be " + highestQueryId + ", setting generator to the next number");
			queryIdGenerator.set(highestQueryId + 1);
		}
		catch (Exception e) {
			e.printStackTrace();
			SPOTTools.terminate(e);
		}
		
		// Init NotifierManager
		NotifierManager.getInstance().start();
		
		// Init RMI
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		try {
			Naming.rebind("SessionManager", new RemoteSessionManager(queryIdGenerator));
			logger.fine("SessionManager initialised");
		}
		catch (Exception e) {
			e.printStackTrace();
			SPOTTools.terminate(e);
		}
	}
}
