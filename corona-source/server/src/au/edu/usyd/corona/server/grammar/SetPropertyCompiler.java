package au.edu.usyd.corona.server.grammar;


import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.scheduler.SetPropertyTask;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * <p>This is a compiler for the SET command in the grammar, allows the setting
 * of variables used in the system, such as the resync period for time
 * synchronization.</p>
 * 
 * @author Tim Dawborn
 */
class SetPropertyCompiler extends QLPacketTypeCompiler<SetPropertyTask> {
	private final TaskID taskId;
	private final User user;
	
	public SetPropertyCompiler(Tree root, int queryId, User user) {
		super(root, queryId);
		this.taskId = new TaskID(queryId);
		this.user = user;
	}
	
	@Override
	public SetPropertyTask compile() throws QLCompileException {
		// only admin users can use this compiler
		if (user.getAccessLevel() != AccessLevel.ADMIN)
			throw new QLCompileException("Only users with administrative access can use the SET syntax");
		
		// ensure the number fits at least in a long
		long value = 0;
		byte type;
		try {
			value = Long.parseLong(root.getChild(1).getText());
		}
		catch (NumberFormatException e) {
			throw new QLCompileException("Number is too large or too small");
		}
		
		// check for valid properties
		final String property = root.getChild(0).getText().toUpperCase();
		if (property.equals("SYNC_EPOCH")) {
			checkBounds(value, 1000, Integer.MAX_VALUE, property, "milliseconds");
			type = SetPropertyTask.SET_SYNC_EPOCH;
		}
		else if (property.equals("MONITORING_EPOCH")) {
			checkBounds(value, 1000, Integer.MAX_VALUE, property, "milliseconds");
			type = SetPropertyTask.SET_MONITORING_EPOCH;
		}
		else if (property.equals("REROUTE_EPOCH")) {
			checkBounds(value, 1000, Integer.MAX_VALUE, property, "milliseconds");
			type = SetPropertyTask.SET_REROUTE_EPOCH;
		}
		else if (property.equals("INTERCLUSTER_POWER")) {
			checkBounds(value, -32, 31, property, "decibels");
			type = SetPropertyTask.SET_INTERCLUSTER_POWER;
		}
		else if (property.equals("INTRACLUSTER_POWER")) {
			checkBounds(value, -32, 31, property, "decibels");
			type = SetPropertyTask.SET_INTRACLUSTER_POWER;
		}
		else
			throw new QLCompileException("Unknown property to set '" + property + "'");
		
		return new SetPropertyTask(taskId, type, (int) value);
	}
	
	private static void checkBounds(long value, int min, int max, String property, String units) throws QLCompileException {
		if (value < min || value > max)
			throw new QLCompileException("Invalid value; " + property + " value must be in the range [" + min + "," + max + "] (" + units + ")");
	}
}
