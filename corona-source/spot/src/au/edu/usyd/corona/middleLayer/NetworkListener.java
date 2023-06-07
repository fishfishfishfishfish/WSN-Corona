package au.edu.usyd.corona.middleLayer;


/**
 * An interface for listeners that should respond to network events, such as
 * receiving a data packet.
 * 
 * @author Raymes Khoury
 */
public interface NetworkListener {
	public void receive(byte[] payload, long source);
	
}
