package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.scheduler.Scheduler;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.scheduler.TaskNotFoundException;
import au.edu.usyd.corona.util.Logger;

/**
 * Performs the required sense operation and returns the result
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public class SenseOperator extends TableOperator {
	private final TaskID taskID;
	
	public SenseOperator(TaskID taskID) {
		this.taskID = taskID;
	}
	
	public Table eval(int epoch) {
		Table table = new Table(taskID);
		if (Network.getInstance().getMode() == Network.MODE_SPOT) {
			// sense from the sensors
			try {
				table = ((QueryTask) Scheduler.getInstance().getTask(taskID)).getSensorResult(epoch);
			}
			catch (TaskNotFoundException e) {
				Logger.logError("Could not store sensed result: " + e);
			}
		}
		return table;
	}
	
	public StringBuffer toTokens() {
		return new StringBuffer().append(T_SENSE).append(T_GROUP_OPEN).append(T_GROUP_CLOSE);
	}
}
