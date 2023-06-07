package au.edu.usyd.corona;


import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.scheduler.Scheduler;
import au.edu.usyd.corona.sensing.CPUUsageMonitor;
import au.edu.usyd.corona.util.ClassIdentifiers;
import au.edu.usyd.corona.util.Logger;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.util.IEEEAddress;

/**
 * The main method of execution on the nodes in the system. This method
 * initialises all of the subsystems.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 * 
 */
public class Startup extends MIDlet {
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
	}
	
	protected void pauseApp() {
	}
	
	protected void startApp() throws MIDletStateChangeException {
		// disable deep sleep mode as we want to always receive radio
		Spot.getInstance().getSleepManager().disableDeepSleep();
		
		// Initialise the logger
		Logger.setOut(System.out);
		Logger.setLogLevel(Logger.DEBUG);
		
		// Turn the LEDs on
		EDemoBoard.getInstance().getLEDs()[0].setOn();
		EDemoBoard.getInstance().getLEDs()[1].setOn();
		EDemoBoard.getInstance().getLEDs()[2].setOn();
		EDemoBoard.getInstance().getLEDs()[3].setOn();
		EDemoBoard.getInstance().getLEDs()[4].setOn();
		
		// Start the system up
		Logger.logGeneral("--- Starting program ---");
		Network.initialize(Network.MODE_SPOT);
		Scheduler.initialize();
		CPUUsageMonitor.initialize();
		TimeSync.initialize();
		ClassIdentifiers.getClass((byte) 0);
		
		// Print my address
		Logger.logDebug("My address is " + IEEEAddress.toDottedHex(Network.getInstance().getMyAddress()));
	}
}
