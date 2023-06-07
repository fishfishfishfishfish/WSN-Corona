package au.edu.usyd.corona.sensing;


import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.ValueType;

import com.sun.spot.sensorboard.EDemoBoard;

/**
 * A sensor which reads the temperature from the thermometer on the EDemoBoard
 * in degrees Celsius, and returns the closest whole number
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public class TemperatureSensor implements Sensor {
	
	public String getSensorName() {
		return "temp";
	}
	
	public ValueType sense() throws IOException {
		if (Network.getInstance().getMode() == Network.MODE_SPOT)
			return new IntType((int) EDemoBoard.getInstance().getADCTemperature().getCelsius());
		else
			return new IntType(0);
	}
}
