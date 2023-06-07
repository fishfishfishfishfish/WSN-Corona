package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import au.edu.usyd.corona.collections.Comparable;
import au.edu.usyd.corona.io.ByteArrayDataInputStream;
import au.edu.usyd.corona.io.ByteArrayDataOutputStream;
import au.edu.usyd.corona.util.ClassIdentifiers;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * An abstract class representing an item which can be scheduled to run in the
 * Scheduler
 * 
 * @author Raymes Khoury
 * @see Scheduler
 */
public abstract class SchedulableTask extends TaskDetails implements Comparable, Transmittable {
	public static final int RUNCOUNT_FOREVER = Integer.MIN_VALUE;
	
	public SchedulableTask() {
		super();
	}
	
	public SchedulableTask(TaskID taskID) {
		super(taskID);
	}
	
	public SchedulableTask(TaskID taskID, long firstExecutionTime, long reschedulePeriod, int runCountTotal) {
		super(taskID, firstExecutionTime, reschedulePeriod, runCountTotal, runCountTotal);
	}
	
	public SchedulableTask(TaskID taskID, long firstExecutionTime, long reschedulePeriod, int runCountTotal, int runCountLeft) {
		super(taskID, firstExecutionTime, reschedulePeriod, runCountTotal, runCountLeft);
	}
	
	/**
	 * Returns whether the task needs to be rescheduled
	 * 
	 * @return true if the task needs to be rescheduled else false
	 */
	public boolean needsRescheduling() {
		return (runCountLeft > 0 || runCountTotal == RUNCOUNT_FOREVER) && (status != STATUS_KILLED);
	}
	
	/**
	 * Does any initialisation required by the task when added to the Scheduler
	 * on the basestation. Mainly used to transmit the task, which only needs to
	 * be done on the basestation.
	 * 
	 * @throws IOException If there is a problem transmitting the task
	 */
	public abstract void baseInit() throws IOException;
	
	/**
	 * Does any initialisation required by the task when added to the Scheduler
	 * on the node.
	 * 
	 */
	public abstract void nodeInit();
	
	/**
	 * Reschedules the task
	 */
	public void reschedule() {
		executionTime += reschedulePeriod;
		_reschedule();
	}
	
	/**
	 * Reschedules the task
	 */
	protected abstract void _reschedule();
	
	/**
	 * Executes the task
	 */
	public void execute() {
		_execute();
		if (runCountTotal != RUNCOUNT_FOREVER)
			runCountLeft--;
	}
	
	/**
	 * Does the work of the task
	 */
	protected abstract void _execute();
	
	public void deconstruct() {
		_deconstruct();
	}
	
	/**
	 * Executed when the task is about to be destroyed - ie. it does not need to
	 * be rescheduled or run any more
	 */
	protected abstract void _deconstruct();
	
	public int compareTo(Object o) {
		return (int) (getExecutionTime() - ((SchedulableTask) o).getExecutionTime());
	}
	
	public void encode(DataOutput data) throws IOException {
		taskID.encode(data);
		_encode(data);
	}
	
	/**
	 * Encodes the task onto the end of the given DataOutput
	 * 
	 * @param data The DataOutput object to encode to
	 * @throws IOException If there is a problem encoding
	 */
	protected abstract void _encode(DataOutput data) throws IOException;
	
	public void decode(DataInput data) throws IOException {
		taskID = new TaskID(data);
		_decode(data);
	}
	
	/**
	 * Decode the task from the end of the given DataInput
	 * 
	 * @param data The DataInput to decode from
	 * @throws IOException If there is a problem decoding
	 */
	protected abstract void _decode(DataInput data) throws IOException;
	
	public byte[] encode() throws IOException {
		ByteArrayDataOutputStream data = new ByteArrayDataOutputStream();
		data.writeByte(ClassIdentifiers.getID(this.getClass()));
		encode(data);
		return data.getBytes();
	}
	
	public static SchedulableTask decode(byte[] bytes) throws IOException {
		ByteArrayDataInputStream data;
		data = new ByteArrayDataInputStream(bytes);
		try {
			byte classID = data.readByte();
			SchedulableTask t = (SchedulableTask) ClassIdentifiers.getClass(classID).newInstance();
			t.decode(data);
			return t;
		}
		catch (Exception e) {
			SPOTTools.reportError("Could not instantiate Task object from recieved byte[]", e);
			e.printStackTrace();
			return null;
		}
	}
}
