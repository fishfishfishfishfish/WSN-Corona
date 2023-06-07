package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.util.Logger;

import com.sun.spot.util.IEEEAddress;

/**
 * This class represents a Task which forwards an exception message to the
 * Desktop if an exception has occurred on a node.
 * 
 * @author Tim Dawborn
 */
public class PropgateExceptionTask extends SchedulableTask {
	public static final String NOTHING = "<unknown>";
	private long address;
	private String message;
	private String toString;
	private String clazz;
	
	public PropgateExceptionTask() {
	}
	
	public PropgateExceptionTask(Throwable e) {
		super(new TaskID(-1));
		address = Network.getInstance().getMyAddress();
		message = e.getMessage() == null ? NOTHING : e.getMessage();
		toString = e.toString();
		clazz = e.getClass().getName();
	}
	
	public PropgateExceptionTask(String message) {
		super(new TaskID(-1));
		address = Network.getInstance().getMyAddress();
		this.message = message;
		toString = NOTHING;
		clazz = NOTHING;
	}
	
	public PropgateExceptionTask(String message, Throwable e) {
		super(new TaskID(-1));
		address = Network.getInstance().getMyAddress();
		this.message = message + "\n" + (e.getMessage() == null ? NOTHING : e.getMessage());
		toString = e.toString();
		clazz = e.getClass().getName();
	}
	
	protected void _decode(DataInput data) throws IOException {
		address = data.readLong();
		message = data.readUTF();
		toString = data.readUTF();
		clazz = data.readUTF();
	}
	
	protected void _deconstruct() {
	}
	
	protected void _encode(DataOutput data) throws IOException {
		data.writeLong(address);
		data.writeUTF(message);
		data.writeUTF(toString);
		data.writeUTF(clazz);
	}
	
	protected void _execute() {
		if (Network.getInstance().getMode() == Network.MODE_BASESTATION) {
			StringBuffer b = new StringBuffer();
			b.append("-----------------------------\n");
			b.append("Address : ").append(IEEEAddress.toDottedHex(address)).append('\n');
			b.append("Type    : ").append(clazz).append('\n');
			b.append("Message : ").append(message).append('\n');
			b.append("toString: ").append(toString).append('\n');
			b.append("-----------------------------\n");
			Logger.logError(b.toString());
		}
	}
	
	protected void _reschedule() {
	}
	
	public void baseInit() throws IOException {
	}
	
	public void nodeInit() {
	}
	
}
