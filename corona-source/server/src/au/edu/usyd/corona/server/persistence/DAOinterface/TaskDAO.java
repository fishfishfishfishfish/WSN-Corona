package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.util.List;

import au.edu.usyd.corona.scheduler.TaskDetails;
import au.edu.usyd.corona.scheduler.TaskID;

/**
 * A DAO for accessing Task objects.
 * 
 * @author Raymes Khoury
 */
public interface TaskDAO {
	
	public static final int MAX_TASKS_RETRIEVED = 1000;
	
	/**
	 * Store a Task
	 * 
	 * @param task The Task to store
	 * @throws DAOException If there is a problem storing the Task
	 */
	public void insert(TaskDetails task) throws DAOException;
	
	/**
	 * Update an existing task
	 * 
	 * @param task The Task to update
	 * @throws DAOException If there is a problem updating the Task
	 */
	public void update(TaskDetails task) throws DAOException;
	
	/**
	 * Delete a Task
	 * 
	 * @param taskId The id of the Task to delete
	 * @throws DAOException If there is a problem deleting the Task
	 */
	public void delete(TaskID taskId) throws DAOException;
	
	/**
	 * Retrieve Task objects matching the given criteria. Criteria are specified
	 * in the style of an SQL WHERE clause, i.e. as conjunctions or disjunctions
	 * of [attribute][operator][value] sequences. The resultant order of the
	 * Tasks is specified in the style of an SQL ORDER BY clause, i.e. a list of
	 * comma-separated attributes to sort by. Order of sorting can be specified
	 * with ASC or DESC. Also, only a subset of the Task objects can be selected
	 * (i.e. a certain page of Tasks). This is achieved by specifying a start and
	 * end Task to retrieve, of the entire results.
	 * 
	 * Will return the Task's [startEntry, endEntry). If startEntry == endEntry,
	 * no Task's will be returned.
	 * 
	 * @param whereClause The criteria of Task objects to return
	 * @param orderByClause The order in which to return Task objects
	 * @param startEntry The first entry of the Results to return
	 * @param endEntry The last entry of the Results to return
	 * @return The results matching the criteria given, in the order specified
	 * @throws DAOException If there is a problem retrieving the Tasks
	 */
	public List<TaskDetails> retrieve(String whereClause, String orderByClause, int startEntry, int endEntry) throws DAOException;
	
	/**
	 * Return the highest local task ID of the given node ID
	 * 
	 * @param node The node whose highest local ID to retrieve
	 * @return The highest local ID for that node
	 * @throws DAOException If there is a problem retrieving the ID
	 */
	public int getHighestLocalTaskID(long node) throws DAOException;
}
