package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

/**
 * A sensor which returns a {@link LongType} of the current network-synchronized
 * time that this sensor was invoked at.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class TimeSensor implements Sensor {
	
	public String getSensorName() {
		return "time";
	}
	
	public ValueType sense() {
		return new LongType(TimeSync.getInstance().getTime());
	}
}
