package au.edu.usyd.corona.srdb;


import java.io.IOException;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.ChildResultStore;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.scheduler.TransmitResultsTask;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * This class forwards the result of table operations to its parent node
 * 
 * @author Raymes Khoury
 */
public class ForwardOperator extends TableOperator {
	private final ChildResultStore childResults;
	
	public ForwardOperator(TableOperator t, ChildResultStore childResults) {
		children = new TableOperator[]{t};
		this.childResults = childResults;
	}
	
	/**
	 * Forwards the result of table operations to the parent node, or writes the
	 * contents to a flash file. Returns the resultant table.
	 */
	public Table eval(int epoch) throws InvalidOperationException {
		Table table = children[0].eval(epoch);
		
		if (Network.getInstance().getMode() == Network.MODE_UNITTEST)
			return table;
		
		TransmitResultsTask t = new TransmitResultsTask(new TaskID(table.getTaskID().getQueryID()), table, epoch);
		try {
			Network.getInstance().sendToParent(t.encode());
		}
		catch (IOException e) {
			SPOTTools.reportError(e);
		}
		
		// Remove the old results for that epoch
		childResults.removeResults(epoch);
		
		return table;
	}
	
	public StringBuffer toTokens() {
		return new StringBuffer().append(T_FORWARD).append(T_GROUP_OPEN).append(children[0].toTokens()).append(T_GROUP_CLOSE);
	}
}
