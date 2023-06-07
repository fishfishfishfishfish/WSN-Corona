package au.edu.usyd.corona.util;


import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.PropgateExceptionTask;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.LEDColor;

/**
 * This class contains some methods to assist in error handling on the spots
 * 
 * @author Raymes Khoury
 * @author Quincy Tse
 */
public class SPOTTools {
	private SPOTTools() {
		// hide the constructor
	}
	
	public static void reportError(Throwable e) {
		e.printStackTrace();
		Logger.logError(e.toString());
		reportError(new PropgateExceptionTask(e));
	}
	
	public static void reportError(String msg) {
		Logger.logError(msg);
		reportError(new PropgateExceptionTask(msg));
	}
	
	public static void reportError(String msg, Throwable e) {
		Logger.logError(msg);
		reportError(new PropgateExceptionTask(msg, e));
	}
	
	private static void reportError(PropgateExceptionTask task) {
		// propogate exception
		try {
			if (Network.getInstance().getMode() == Network.MODE_SPOT)
				Network.getInstance().sentToRoot(task.encode());
		}
		catch (IOException err) {
		}
		
		// error LED's if possible
		if (Network.getInstance().getMode() == Network.MODE_SPOT)
			EDemoBoard.getInstance().getLEDs()[4].setColor(LEDColor.RED);
	}
	
	/**
	 * Causes the application to terminate
	 * 
	 * @param message The message to display before termination
	 * @param exitCode The exit code to terminate with
	 */
	public static void terminate(String message, int exitCode) {
		if (exitCode != 0)
			Logger.logError(message);
		else
			Logger.logDebug(message);
		System.exit(exitCode);
	}
	
	/**
	 * Causes the application to terminate
	 * 
	 * @param e An exception that resulted in termination
	 */
	public static void terminate(Exception e) {
		e.printStackTrace();
		terminate("Exception thrown", 1);
	}
}
