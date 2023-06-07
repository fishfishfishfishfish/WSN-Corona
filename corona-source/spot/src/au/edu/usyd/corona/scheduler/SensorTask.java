package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * This class is responsible for performing a sense operation.
 * 
 * @author Raymes Khoury
 */
public class SensorTask extends SchedulableTask {
	private final QueryTask query;
	
	public SensorTask(TaskID taskID, long firstExecutionTime, long reschedulePeriod, int runCountTotal, int runCountLeft, QueryTask query) {
		super(taskID, firstExecutionTime, reschedulePeriod, runCountTotal, runCountLeft);
		this.query = query;
	}
	
	protected void _decode(DataInput data) throws IOException {
		
	}
	
	protected void _deconstruct() {
		
	}
	
	protected void _encode(DataOutput data) throws IOException {
		
	}
	
	protected void _execute() {
		int epoch = runCountTotal - runCountLeft;
		query.getSensorResult(epoch);
	}
	
	protected void _reschedule() {
	}
	
	public void baseInit() throws IOException {
	}
	
	public void nodeInit() {
	}
}
