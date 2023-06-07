package au.edu.usyd.corona.server.grammar;


import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.server.scheduler.TimeSyncTask;
import au.edu.usyd.corona.scheduler.TaskID;

/**
 * This is a compiler for the SYNC command in the grammar, which forces resync
 * of the time synchronization within the network.
 * 
 * @author Tim Dawborn
 */
class SyncCompiler extends QLPacketTypeCompiler<TimeSyncTask> {
	public SyncCompiler(Tree root, int queryId) {
		super(root, queryId);
	}
	
	@Override
	public TimeSyncTask compile() {
		return new TimeSyncTask(new TaskID(queryId));
	}
}
