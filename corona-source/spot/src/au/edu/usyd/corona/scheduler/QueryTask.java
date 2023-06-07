package au.edu.usyd.corona.scheduler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Hashtable;

import au.edu.usyd.corona.grammar.TokenParseException;
import au.edu.usyd.corona.grammar.TokenParser;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.sensing.SenseManager;
import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.srdb.TableOperator;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.util.ClassIdentifiers;
import au.edu.usyd.corona.util.Logger;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * This class is responsible for executing a query. It handles all aspects of
 * query execution including execution of the expression tree.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public class QueryTask extends SchedulableTask {
	// Mutex to force only 1 relational algebra execution at any time. Prevents stack overflow
	private static Object mutex = new Object();
	
	// network variables
	protected TableOperator networkTree; //the composite tree executed on the nodes
	protected byte[] networkSchema; // The schema of the resultant table
	protected String tokenStream; //the relational token string
	
	// basestation variables
	protected TableOperator baseTree; //the composite tree executed on the base
	protected String[] baseAttributes;
	protected byte[] baseSchema; // The schema of the final table
	
	// common variables
	protected Hashtable senseResults; // Hashtable of sensor tables. Maps epoch to sensor table for that epoch
	protected ChildResultStore childResults; // Mapping of epoch to results from children of that epoch
	
	public QueryTask(TableOperator networkTree, TableOperator baseTree, long timeExecution, int runCount, long period, TaskID tID, byte[] networkSchema, byte[] baseSchema, String[] baseAttributes, ChildResultStore results) throws TokenParseException, IOException {
		super(tID, timeExecution, period, runCount);
		this.networkSchema = networkSchema;
		this.baseSchema = baseSchema;
		this.baseAttributes = baseAttributes;
		this.baseTree = baseTree;
		this.networkTree = networkTree;
		this.tokenStream = networkTree.toTokens().toString();
		childResults = results;
		senseResults = new Hashtable();
	}
	
	public QueryTask() {
		super();
		senseResults = new Hashtable();
		childResults = new ChildResultStore();
	}
	
	private TableOperator parseTokens() throws TokenParseException {
		// Start the parser and generate the expression tree, the corresponding taskset, and then add the generated taskset to the scheduler
		TokenParser parser = new TokenParser();
		TableOperator res = parser.parse(this);
		return res;
	}
	
	protected void _reschedule() {
	}
	
	protected void _deconstruct() {
	}
	
	/**
	 * Returns the time in milliseconds until this task needs to be executed
	 * 
	 * @param currentTime the current synchronized system time in milliseconds
	 * @return the time difference
	 */
	public long getTimeDelta(long currentTime) {
		return executionTime - currentTime;
	}
	
	/**
	 * This method is called when the system time is the time that this task set
	 * wants to be executed.
	 */
	protected void _execute() {
		int epoch = runCountTotal - runCountLeft;
		try {
			switch (Network.getInstance().getMode()) {
			case Network.MODE_SPOT:
				synchronized (mutex) {
					networkTree.eval(epoch);
				}
				removeSensorResult(epoch);
				break;
			case Network.MODE_BASESTATION:
				baseTree.eval(epoch);
				removeSensorResult(epoch);
				break;
			}
		}
		catch (InvalidOperationException e) {
			SPOTTools.reportError(e);
		}
	}
	
	/**
	 * @return The schema of the resultant table
	 */
	public byte[] getNetworkSchema() {
		return networkSchema;
	}
	
	/**
	 * @return The schema of the resultant table
	 */
	public Class[] getBaseClassSchema() {
		Class[] classes = new Class[baseSchema.length];
		for (int i = 0; i != classes.length; i++)
			classes[i] = ClassIdentifiers.getClass(baseSchema[i]);
		return classes;
	}
	
	public TableOperator getBaseTree() {
		return baseTree;
	}
	
	/**
	 * Return the token stream of the query
	 * 
	 * @return Token stream
	 */
	public String getTokenStream() {
		return tokenStream;
	}
	
	public String[] getAttributes() {
		return baseAttributes;
	}
	
	protected void _decode(DataInput data) throws IOException {
		this.executionTime = data.readLong();
		this.reschedulePeriod = data.readLong();
		this.runCountLeft = data.readInt();
		this.runCountTotal = data.readInt();
		this.tokenStream = data.readUTF();
		byte schemaLen = data.readByte();
		this.networkSchema = new byte[schemaLen];
		for (int i = 0; i < schemaLen; ++i)
			networkSchema[i] = data.readByte();
		
		senseResults = new Hashtable();
		childResults = new ChildResultStore();
		
		try {
			networkTree = parseTokens();
		}
		catch (TokenParseException e) {
			SPOTTools.reportError(e);
		}
	}
	
	protected void _encode(DataOutput data) throws IOException {
		data.writeLong(executionTime);
		data.writeLong(reschedulePeriod);
		data.writeInt(runCountLeft);
		data.writeInt(runCountTotal);
		data.writeUTF(tokenStream);
		data.writeByte(networkSchema.length);
		data.write(networkSchema);
	}
	
	/**
	 * Get the sensed Table for a given epoch
	 * 
	 * @param epoch the epoch to get the sensed Table for
	 * @return The sensed Table for the given epoch
	 */
	public synchronized Table getSensorResult(int epoch) {
		Integer key = new Integer(epoch);
		if (senseResults.containsKey(key)) {
			return (Table) senseResults.get(key);
		}
		Table res;
		if (Network.getInstance().getMode() == Network.MODE_SPOT) {
			res = SenseManager.getInstance().sense(taskID);
			senseResults.put(key, res);
		}
		else {
			res = new Table(taskID);
		}
		
		return res;
	}
	
	/**
	 * 
	 * @param epoch
	 */
	public synchronized void removeSensorResult(int epoch) {
		Integer key = new Integer(epoch);
		if (senseResults.containsKey(key))
			senseResults.remove(key);
	}
	
	public ChildResultStore getChildResults() {
		return childResults;
	}
	
	public void nodeInit() {
		Logger.logDebug("Scheduled task to execute at: " + executionTime + ". Current time: " + TimeSync.getInstance().getTime());
		Scheduler.getInstance().addTask(new SensorTask(new TaskID(taskID.getQueryID()), executionTime, reschedulePeriod, runCountTotal, runCountLeft, this));
	}
	
	public void baseInit() throws IOException {
		Network.getInstance().sendToDescendants(encode());
	}
}
