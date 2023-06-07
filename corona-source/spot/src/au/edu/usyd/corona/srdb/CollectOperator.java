package au.edu.usyd.corona.srdb;


import java.io.IOException;
import java.util.Vector;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.ChildResultStore;
import au.edu.usyd.corona.scheduler.Scheduler;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.scheduler.TaskNotFoundException;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.util.Logger;

import com.sun.spot.util.IEEEAddress;

/**
 * This class collects and returns a child table if it has been received in the
 * storage area
 * 
 * @author Raymes Khoury
 */
public class CollectOperator extends TableOperator {
	private static final int INTERVAL_CHECK = 100; //The time between checks of results
	private final ChildResultStore childResults;
	private final TaskID taskID;
	
	private static final int COLLECT_HEIGHT_DELTA = 4000;
	
	public CollectOperator(TaskID taskID, ChildResultStore childResults) {
		this.childResults = childResults;
		this.taskID = taskID;
	}
	
	public Table eval(int epoch) throws InvalidOperationException {
		if (Network.getInstance().getMode() == Network.MODE_UNITTEST)
			return new Table(taskID);
		
		// inits the time to wait before sending to be nothing
		long sendDelta = 0;
		
		// works out the height of the tree
		final byte heightOfTree = Network.getInstance().getHeight();
		
		sendDelta += COLLECT_HEIGHT_DELTA * heightOfTree;
		
		// works out the children of the node
		final int numChildrenExpected = Network.getInstance().getNumChildren();
		
		// while the time elapsed is less than the max to wait, and the number of 
		// results received is less than the expected number, poll for the results
		long timeElapsed = 0;
		while ((timeElapsed < sendDelta) && (childResults.numResults(epoch) < numChildrenExpected)) {
			timeElapsed += INTERVAL_CHECK;
			try {
				Thread.sleep(INTERVAL_CHECK);
			}
			catch (InterruptedException e) {
			}
		}
		
		// send the task to any nodes we didn't receive from in case they dont have it
		Vector received = childResults.getNodesReceivedFrom(epoch);
		Long[] children = Network.getInstance().getChildren();
		
		for (int i = 0; i < children.length; i++) {
			if (received == null || !received.contains(children[i])) {
				Logger.logError("Sending task to node that doesn't have it: " + IEEEAddress.toDottedHex(children[i].longValue()));
				try {
					Network.getInstance().sendToChild(Scheduler.getInstance().getTask(taskID).encode(), children[i].longValue());
				}
				catch (IOException e) {
					Logger.logError("Collect operator could not send: " + e);
				}
				catch (TaskNotFoundException e) {
					Logger.logError("Collect operator could not send: " + e);
				}
			}
		}
		
		// get the results that are available
		Vector results = childResults.getResults(epoch);
		
		// merge all the results into one table
		Table finalResult;
		if (results != null) {
			finalResult = (Table) results.firstElement();
			
			Table merger;
			for (int i = 1; i < results.size(); i++) {
				merger = (Table) results.elementAt(i);
				MergeOperator merge = new MergeOperator(taskID, new ReadOperator(finalResult), new ReadOperator(merger));
				finalResult = merge.eval(epoch);
			}
		}
		else {
			finalResult = new Table(taskID);
		}
		
		return finalResult;
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		return b.append(T_COLLECT).append(T_GROUP_OPEN).append(T_GROUP_CLOSE);
	}
}
