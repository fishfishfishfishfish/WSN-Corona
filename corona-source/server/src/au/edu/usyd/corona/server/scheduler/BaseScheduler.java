package au.edu.usyd.corona.server.scheduler;


import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.KillTask;
import au.edu.usyd.corona.scheduler.PropgateExceptionTask;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.scheduler.Scheduler;
import au.edu.usyd.corona.scheduler.TaskDetails;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.TaskDAO;
import au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCDAOFactory;
import au.edu.usyd.corona.server.session.notifier.NotifierID;
import au.edu.usyd.corona.server.session.notifier.NotifierManager;
import au.edu.usyd.corona.server.session.notifier.NotifierID.NotifierType;
import au.edu.usyd.corona.util.Logger;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * This class handles execution of Task's on the Basestation. It also provides
 * persistence of Task objects.
 * 
 * 
 * @author Raymes Khoury
 */
public class BaseScheduler extends Scheduler {
	private final TaskDAO taskDAO;
	
	public static void initialize() {
		if (instance == null) {
			try {
				instance = new BaseScheduler();
			}
			catch (DAOException e) {
				SPOTTools.terminate(e);
			}
			(thread = new Thread(instance, "Scheduler")).start();
			Logger.logDebug("Base Scheduler initialized");
		}
	}
	
	private BaseScheduler() throws DAOException {
		super();
		taskDAO = DAOFactory.getInstance().getTaskDAO();
		
		// Kill existing incomplete queries.  An alternative may be to recover however this is currently infeasible
		List<TaskDetails> incomplete = taskDAO.retrieve("status = " + TaskDetails.STATUS_RUNNING + " OR status = " + TaskDetails.STATUS_SUBMITTED, "", 0, TaskDAO.MAX_TASKS_RETRIEVED);
		
		TaskID.setLocalTaskID((taskDAO.getHighestLocalTaskID(Network.getInstance().getMyAddress()) + 1));
		
		HashSet<Integer> killedQueries = new HashSet<Integer>();
		for (TaskDetails t : incomplete) {
			int qID = t.getTaskId().getQueryID();
			Logger.logDebug("Killing: " + qID);
			if (!killedQueries.contains(qID)) {
				addTask(new KillTask(new TaskID(qID), qID));
			}
			t.setStatus(TaskDetails.STATUS_KILLED);
			taskDAO.update(t);
		}
	}
	
	@Override
	public synchronized void addTask(SchedulableTask task) {
		// checks for duplicates
		if (tasksTable.containsKey(task.getTaskId())) {
			Logger.logDebug("[scheduler] ignoring duplicate");
			return;
		}
		
		// do the base init on the task
		try {
			task.baseInit();
		}
		catch (IOException e) {
			Logger.logError("Could not init the task");
		}
		
		// if its not an exception task, persist it
		if (!(task instanceof PropgateExceptionTask)) {
			try {
				Logger.logDebug("INSERTING TASK INTO DAO: " + task.getTaskId() + " " + task.getClass());
				taskDAO.insert(task);
			}
			catch (DAOException e) {
				Logger.logError("Could not insert task into DAO");
			}
		}
		
		// do the add task
		_addTask(task);
		
		// notify appropriately
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierType.RESULT_TABLE_NOTIFIER, task.getTaskId().getQueryID()));
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierType.QUERIES_TABLE_NOTIFIER));
	}
	
	@Override
	public synchronized void reschedule(SchedulableTask task) {
		super.reschedule(task);
		try {
			Logger.logDebug("Connections used: " + ((JDBCDAOFactory) DAOFactory.getInstance()).getNumConnectionsUsed());
			taskDAO.update(task);
		}
		catch (DAOException e) {
			e.printStackTrace();
			Logger.logError("Could not update task in DAO: " + e);
		}
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierType.RESULT_TABLE_NOTIFIER, task.getTaskId().getQueryID()));
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierType.QUERIES_TABLE_NOTIFIER));
	}
	
	@Override
	protected synchronized void removeTask(SchedulableTask task) {
		super.removeTask(task);
		try {
			taskDAO.update(task);
		}
		catch (DAOException e) {
			e.printStackTrace();
			Logger.logError("Could not update task in DAO: " + e);
		}
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierType.RESULT_TABLE_NOTIFIER, task.getTaskId().getQueryID()));
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierType.QUERIES_TABLE_NOTIFIER));
	}
}
