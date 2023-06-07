package au.edu.usyd.corona.sensing;


import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.ValueType;

import com.sun.spot.sensorboard.EDemoBoard;

/**
 * A sensor which reads the light value from the light sensor on the EDemoBoard
 * on the SunSPOT
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class LightSensor implements Sensor {
	
	public String getSensorName() {
		return "light";
	}
	
	public ValueType sense() throws IOException {
		if (Network.getInstance().getMode() == Network.MODE_SPOT)
			return new IntType(EDemoBoard.getInstance().getLightSensor().getValue());
		else
			return new IntType(0);
	}
}
