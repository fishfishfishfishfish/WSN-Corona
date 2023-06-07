package au.edu.usyd.corona.sensing;


import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.ValueType;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;

/**
 * A sensor which reads the state of one of the switches on the SunSPOT node,
 * and returns a {@link BooleanType} according to whether or not the switch is
 * closed.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class SwitchSensor implements Sensor {
	public static final byte TYPE_SW1 = 0;
	public static final byte TYPE_SW2 = 1;
	
	private final byte button;
	
	public SwitchSensor(byte button) {
		this.button = button;
	}
	
	public String getSensorName() {
		if (button == TYPE_SW1)
			return "sw1";
		else if (button == TYPE_SW2)
			return "sw2";
		return null;
	}
	
	public ValueType sense() throws IOException {
		if (Network.getInstance().getMode() != Network.MODE_SPOT)
			return new BooleanType(0);
		
		ISwitch sw = null;
		if (button == TYPE_SW1)
			sw = EDemoBoard.getInstance().getSwitches()[0];
		else if (button == TYPE_SW2)
			sw = EDemoBoard.getInstance().getSwitches()[1];
		return new BooleanType(sw.isClosed());
	}
	
}
