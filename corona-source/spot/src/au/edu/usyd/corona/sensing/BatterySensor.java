package au.edu.usyd.corona.sensing;


import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.ByteType;
import au.edu.usyd.corona.types.ValueType;

import com.sun.spot.peripheral.Spot;

/**
 * A sensor which reads from the battery level from the SunSPOT, and returns a
 * whole number representing the battery level as a percentage (range 0 to 100).
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class BatterySensor implements Sensor {
	
	public String getSensorName() {
		return "battery";
	}
	
	public ValueType sense() {
		if (Network.getInstance().getMode() == Network.MODE_SPOT)
			return new ByteType(Spot.getInstance().getPowerController().getBattery().getBatteryLevel());
		else
			return new ByteType(0);
	}
}
