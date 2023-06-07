package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.ByteType;
import au.edu.usyd.corona.types.ValueType;

/**
 * A sensor which returns the CPU usage as a whole number percentage (range 0 to
 * 100) from the {@link CPUUsageMonitor}.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class CPUSensor implements Sensor {
	
	public String getSensorName() {
		return "cpu";
	}
	
	public ValueType sense() {
		if (Network.getInstance().getMode() == Network.MODE_SPOT)
			return new ByteType(CPUUsageMonitor.getInstance().getUsage());
		else
			return new ByteType(0);
	}
	
}
