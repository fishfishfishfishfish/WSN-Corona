package au.edu.usyd.corona.server.grammar;


import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.server.scheduler.RouteTask;
import au.edu.usyd.corona.scheduler.TaskID;

/**
 * This is a compiler for the ROUTE command in the grammar, which forces a
 * re-route of the network
 * 
 * @author Tim Dawborn
 */
class RouteCompiler extends QLPacketTypeCompiler<RouteTask> {
	private final TaskID taskId;
	
	public RouteCompiler(Tree root, int queryId) {
		super(root, queryId);
		this.taskId = new TaskID(queryId);
	}
	
	@Override
	public RouteTask compile() {
		return new RouteTask(taskId);
	}
}
