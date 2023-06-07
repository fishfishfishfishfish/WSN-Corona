package au.edu.usyd.corona.srdb;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.scheduler.Scheduler;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.scheduler.TaskNotFoundException;
import au.edu.usyd.corona.types.ValueType;
import au.edu.usyd.corona.util.ClassIdentifiers;
import au.edu.usyd.corona.util.Logger;
import au.edu.usyd.corona.util.SPOTTools;

/**
 * This class represents a Table in the SunSPOT Relational DataBase
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class Table {
	private final Vector rows; // the rows of the table, stored as a vector of ValueType[]
	private TaskID taskID; // the task ID number which the table is associated with
	
	public Table() {
		this(null);
	}
	
	/**
	 * Constructor for the table, were the attributes needed are defined
	 * 
	 * @param taskID the task ID that the table stores results from; used as the
	 * equivalent entity as a table name
	 */
	public Table(TaskID taskID) {
		this(taskID, new Vector());
	}
	
	private Table(TaskID taskID, Vector rows) {
		this.taskID = taskID;
		this.rows = rows;
	}
	
	/**
	 * @param row the row to add
	 */
	public void addRow(ValueType[] row) {
		rows.addElement(row);
	}
	
	/**
	 * Drops all the data stored in the table
	 */
	public void clear() {
		rows.removeAllElements();
	}
	
	/**
	 * Returns an {@link Enumeration}(of ValueType[]'s) which allows sequential
	 * access to all the rows in the table
	 * 
	 * @return an Enumeration<ValueType[]> iterator to all the rows in the table
	 */
	public Enumeration elements() {
		return rows.elements();
	}
	
	/**
	 * Return a single row in the Table
	 * 
	 * @param row The index of the row to return
	 * @return The row in the table
	 */
	public ValueType[] getRow(int row) {
		return (ValueType[]) rows.elementAt(row);
	}
	
	/**
	 * Counts the number of columns in the table. If there are no rows in the
	 * table at the time of calling, this method returns 0
	 * 
	 * @return the number of columns in the table
	 */
	public int getNumCols() {
		return (rows.isEmpty()) ? 0 : ((ValueType[]) rows.firstElement()).length;
	}
	
	/**
	 * Counts the number of rows in the table
	 * 
	 * @return the number of rows in the table
	 */
	public int getNumRows() {
		return rows.size();
	}
	
	/**
	 * @return the task ID that the table is storing data for
	 */
	public TaskID getTaskID() {
		return taskID;
	}
	
	/**
	 * Checks to see if the table is currently empty or not
	 * 
	 * @return whether there is data in the table or not
	 */
	public boolean isEmpty() {
		return rows.size() == 0;
	}
	
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append("Table TaskID: " + taskID);
		out.append('\n');
		for (int i = 0; i != rows.size(); i++) {
			ValueType[] row = (ValueType[]) rows.elementAt(i);
			
			for (int j = 0; j != row.length; j++) {
				if (j != 0)
					out.append("\t");
				out.append(row[j]);
			}
			out.append('\n');
		}
		return out.toString();
	}
	
	/**
	 * Encodes a table to a ByteArrayStream
	 * 
	 * @param b The ByteArrayStream to encode to
	 * @throws IOException
	 */
	public void encode(DataOutput b) throws IOException {
		taskID.encode(b);
		b.writeInt(rows.size());
		for (Enumeration rowEnum = rows.elements(); rowEnum.hasMoreElements();) {
			ValueType[] row = (ValueType[]) rowEnum.nextElement();
			for (int i = 0; i < row.length; ++i)
				row[i].encode(b);
		}
	}
	
	/**
	 * Decodes a ByteArrayStream to a table
	 * 
	 * @param b The ByteArrayStream to encode from
	 * @throws IOException
	 */
	public void decode(DataInput b) throws IOException {
		taskID = new TaskID(b);
		QueryTask task = null;
		try {
			task = (QueryTask) Scheduler.getInstance().getTask(taskID);
		}
		catch (TaskNotFoundException e) {
			Logger.logError("Could not decode the table: " + e);
			return;
		}
		byte[] schema = task.getNetworkSchema();
		
		rows.removeAllElements();
		
		int length = b.readInt();
		for (int i = 0; i < length; ++i) {
			ValueType[] row = new ValueType[schema.length];
			for (int j = 0; j < schema.length; ++j) {
				try {
					row[j] = (ValueType) (ClassIdentifiers.getClass(schema[j])).newInstance();
					row[j].decode(b);
				}
				catch (InstantiationException e) {
					SPOTTools.reportError(e);
				}
				catch (IllegalAccessException e) {
					SPOTTools.reportError(e);
				}
			}
			rows.addElement(row);
		}
	}
}
