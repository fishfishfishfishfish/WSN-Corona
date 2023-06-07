package au.edu.usyd.corona.scheduler;


import au.edu.usyd.corona.middleLayer.TimeSync;

/**
 * This class contains all the common details that a Task in the system require.
 * These include the Task ID, the next execution time, the reschedule period,
 * the number of executions in total, number of executions left and the status
 * of the task.
 * 
 * @author Raymes Khoury
 */
public class TaskDetails {
	public static final int STATUS_SUBMITTED = 0;
	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_KILLED = 2;
	public static final int STATUS_COMPLETE = 3;
	
	protected long executionTime;
	protected TaskID taskID;
	protected long reschedulePeriod;
	protected int runCountLeft;
	protected int runCountTotal;
	protected int status;
	
	public TaskDetails(TaskID taskID, long firstExecutionTime, long reschedulePeriod, int runCountTotal, int runCountLeft) {
		this.taskID = taskID;
		this.executionTime = firstExecutionTime;
		this.reschedulePeriod = reschedulePeriod;
		this.runCountTotal = runCountTotal;
		this.runCountLeft = runCountLeft;
		status = STATUS_SUBMITTED;
	}
	
	public TaskDetails(TaskID taskID) {
		this(taskID, TimeSync.getInstance().getTime(), 0, 1, 1);
	}
	
	public TaskDetails() {
		this(null);
	}
	
	public long getExecutionTime() {
		return executionTime;
	}
	
	public TaskID getTaskId() {
		return taskID;
	}
	
	public long getReschedulePeriod() {
		return reschedulePeriod;
	}
	
	public int getRunCountLeft() {
		return runCountLeft;
	}
	
	public int getRunCountTotal() {
		return runCountTotal;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}
	
	public void setTaskID(TaskID taskID) {
		this.taskID = taskID;
	}
	
	public void setReschedulePeriod(long reschedulePeriod) {
		this.reschedulePeriod = reschedulePeriod;
	}
	
	public void setRunCountLeft(int runCountLeft) {
		this.runCountLeft = runCountLeft;
	}
	
	public void setRunCountTotal(int runCountTotal) {
		this.runCountTotal = runCountTotal;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
}
