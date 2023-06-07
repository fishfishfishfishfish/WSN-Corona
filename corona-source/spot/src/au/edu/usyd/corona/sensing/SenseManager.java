package au.edu.usyd.corona.sensing;


import java.io.IOException;
import java.util.Vector;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.types.ValueType;
import au.edu.usyd.corona.util.SPOTTools;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.LEDColor;

/**
 * This class provides a public interface to the sensors defined in this
 * package. By invoking the {@link #sense(TaskID)} method on this class, all of
 * the registered sensors will be sensed from, and the values returned as a SRDB
 * {@link Table} object.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class SenseManager {
	private static final SenseManager instance = new SenseManager();
	private final Vector sensors;
	
	public static SenseManager getInstance() {
		return instance;
	}
	
	private SenseManager() {
		// hidden constructor
		
		// register sensors
		sensors = new Vector();
		sensors.addElement(new CountSensor());
		sensors.addElement(new NodeSensor());
		sensors.addElement(new TimeSensor());
		sensors.addElement(new AccelerationSensor(AccelerationSensor.TYPE_ACCX));
		sensors.addElement(new AccelerationSensor(AccelerationSensor.TYPE_ACCY));
		sensors.addElement(new AccelerationSensor(AccelerationSensor.TYPE_ACCZ));
		sensors.addElement(new SwitchSensor(SwitchSensor.TYPE_SW1));
		sensors.addElement(new SwitchSensor(SwitchSensor.TYPE_SW2));
		sensors.addElement(new LightSensor());
		sensors.addElement(new TemperatureSensor());
		sensors.addElement(new ParentSensor());
		sensors.addElement(new BatterySensor());
		sensors.addElement(new CPUSensor());
		sensors.addElement(new MemorySensor());
	}
	
	/**
	 * Performs a sense operation, sensing from all of the sensors and adding the
	 * resultant table to the passed in task's results
	 * 
	 * @param taskID the id of the task we are currently sensing for
	 * @return the corresponding Table object
	 */
	public synchronized Table sense(TaskID taskID) {
		// Flash a light
		if (Network.getInstance().getMode() == Network.MODE_SPOT) {
			EDemoBoard.getInstance().getLEDs()[3].setOn();
			EDemoBoard.getInstance().getLEDs()[3].setColor(LEDColor.WHITE);
		}
		
		Table res = new Table(taskID);
		ValueType[] row = new ValueType[sensors.size()];
		
		// get the sensor results
		try {
			for (int i = 0; i < sensors.size(); i++)
				row[i] = ((Sensor) sensors.elementAt(i)).sense();
		}
		catch (IOException e) {
			SPOTTools.reportError(e);
		}
		
		// add the result row to the result table
		res.addRow(row);
		
		if (Network.getInstance().getMode() == Network.MODE_SPOT) {
			EDemoBoard.getInstance().getLEDs()[3].setOff();
		}
		
		return res;
	}
	
	/**
	 * @return an array of the (ordered) names of all of the registered sensors
	 * that will be sensed from upon a call to {@link #sense(TaskID)}
	 */
	public synchronized String[] getColumnNames() {
		String[] names = new String[sensors.size()];
		for (int i = 0; i < names.length; i++)
			names[i] = ((Sensor) sensors.elementAt(i)).getSensorName();
		return names;
	}
}
