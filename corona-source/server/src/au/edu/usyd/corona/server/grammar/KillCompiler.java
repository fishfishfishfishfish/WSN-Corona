package au.edu.usyd.corona.server.grammar;


import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.scheduler.KillTask;
import au.edu.usyd.corona.scheduler.TaskID;

/**
 * This is a compiler for the KILL command in the grammar, which is used to kill
 * an executing query within the system
 * 
 * @author Tim Dawborn
 */
class KillCompiler extends QLPacketTypeCompiler<KillTask> {
	public KillCompiler(Tree root, int queryId) {
		super(root, queryId);
	}
	
	@Override
	public KillTask compile() throws QLCompileException {
		if (root.getChildCount() != 1)
			throw new QLCompileException("Expected id of task to kill; found nothing");
		
		int killId;
		try {
			killId = Integer.parseInt(root.getChild(0).getText());
		}
		catch (NumberFormatException e) {
			throw new QLCompileException("Invalid value for kill id: " + root.getChild(0).getText());
		}
		
		return new KillTask(new TaskID(queryId), killId);
	}
}
