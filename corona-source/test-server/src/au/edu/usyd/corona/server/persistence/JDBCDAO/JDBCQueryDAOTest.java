package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.rmi.RemoteException;
import java.util.List;

import junit.framework.TestCase;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.KillTask;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.UserDAO;
import au.edu.usyd.corona.server.session.QueryRetrieveException;
import au.edu.usyd.corona.server.user.User;

/**
 * @author Edmund
 * 
 */
public class JDBCQueryDAOTest extends TestCase {
	private JDBCQueryDAO qd;
	private JDBCTaskDAO td;
	private JDBCDAOFactory factory;
	private User u;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Network.initialize(Network.MODE_UNITTEST);
		factory = new JDBCDAOFactory();
		factory.clean();
		factory.close();
		factory = new JDBCDAOFactory();
		
		qd = (JDBCQueryDAO) factory.getQueryDAO();
		td = (JDBCTaskDAO) factory.getTaskDAO();
		// A query inserted into the database need to reference a user. Create one.
		UserDAO ud = factory.getUserDAO();
		ud.addUser("user", "pass");
		u = ud.checkPassword("user", "pass");
	}
	
	@Override
	protected void tearDown() throws Exception {
		factory.clean();
		factory.close();
		super.tearDown();
	}
	
	public void addQuery(User u, int queryID) throws DAOException {
		TaskID tID = new TaskID(queryID);
		long time = System.currentTimeMillis();
		Query q = new Query(queryID, "KILL 5", time, time, time, 0, 1, 1, SchedulableTask.STATUS_SUBMITTED, u, tID.getNodeID(), tID.getLocalTaskID());
		SchedulableTask t = new KillTask(tID, 5);
		qd.insert(q);
		td.insert(t);
	}
	
	public void testInsertAndGetHighestQueryID() throws DAOException, QueryRetrieveException {
		// For the abnormal cases, expect to behave like typical findmax algorithm
		// When table contains no queries.
		assertEquals(-1, qd.getHighestQueryID());
		
		// Add some queries sequentially
		addQuery(u, 0);
		assertEquals(0, qd.getHighestQueryID());
		addQuery(u, 1);
		assertEquals(1, qd.getHighestQueryID());
		addQuery(u, 2);
		assertEquals(2, qd.getHighestQueryID());
		
		// Add some gaps to query ID's
		addQuery(u, 8);
		assertEquals(8, qd.getHighestQueryID());
		addQuery(u, 12);
		assertEquals(12, qd.getHighestQueryID());
		addQuery(u, 100);
		assertEquals(100, qd.getHighestQueryID());
		
		// Go backwards, jumping around
		addQuery(u, 54);
		assertEquals(100, qd.getHighestQueryID());
		addQuery(u, 7);
		assertEquals(100, qd.getHighestQueryID());
		
		// Negative query ID -- not expected under normal operation
		addQuery(u, -100);
		assertEquals(100, qd.getHighestQueryID());
		
		// Very large and very small numbers
		addQuery(u, Integer.MIN_VALUE);
		assertEquals(100, qd.getHighestQueryID());
		addQuery(u, Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, qd.getHighestQueryID());
	}
	
	public void testDelete() throws DAOException, RemoteException, QueryRetrieveException {
		// When database is empty
		assertEquals(-1, qd.getHighestQueryID());
		
		// Add some queries sequentially
		addQuery(u, 0);
		addQuery(u, 1);
		addQuery(u, 2);
		assertEquals(2, qd.getHighestQueryID());
		
		// Delete the last added queries
		qd.delete(2);
		assertEquals(1, qd.getHighestQueryID());
		qd.delete(1);
		assertEquals(0, qd.getHighestQueryID());
		qd.delete(0);
		assertEquals(-1, qd.getHighestQueryID());
		
		// Random slots
		addQuery(u, 100);
		addQuery(u, 101);
		
		RemoteQueryResultsInterface results = qd.retrieve("queryID = 100", null);
		assertEquals(1, results.getNumItems());
		qd.delete(100);
		assertEquals(0, results.getNumItems());
		
		// Deleting a nonexistent query shouldn't complain. Postconditions are kept.
		qd.delete(100);
		assertEquals(0, results.getNumItems());
		
		// Deleting an invalid query id shouldn't complain. Postconditions are kept.
		qd.delete(-100);
		
		// Ridicously large/small number...
		qd.delete(Integer.MAX_VALUE);
		qd.delete(Integer.MIN_VALUE);
	}
	
	public void testNumQueriesAndRetrieve() throws DAOException, RemoteException, QueryRetrieveException {
		
		List<Query> result;
		
		// When database is empty
		assertEquals(-1, qd.getHighestQueryID());
		RemoteQueryResultsInterface results = qd.retrieve(null, null);
		assertEquals(0, results.getNumItems()); // DAO to report expected number of results
		result = results.getItems(0, 0);
		assertEquals(0, result.size());
		
		// Getting the first query of one in total
		addQuery(u, 0);
		assertEquals(0, qd.getHighestQueryID());
		assertEquals(1, results.getNumItems()); // DAO to report expected number of results
		// Crop results s.t. no results come through
		result = results.getItems(0, 0);
		assertEquals(0, result.size());
		
		// Crop results to let through all one result
		result = results.getItems(0, 1);
		assertEquals(1, result.size());
		assertEquals(0, result.get(0).getQueryID());
		assertEquals("KILL 5", result.get(0).getQuery());
		// Over-set the range: 2 options here: either return a sub-optimal result, or
		// complain. Assume that we are going with returning a sub-optimal result. This is OK
		// Since we can find out the size of the results returned since it is a List.
		result = results.getItems(0, 10);
		assertEquals(1, result.size());
		
		//		 Put a few more queries into the database
		for (int i = 1; i <= 99; i++)
			addQuery(u, i);
		addQuery(u, 100);
		for (int i = 101; i <= 110; i++)
			addQuery(u, i);
		
		// Check total number of queries in database
		assertEquals(111, results.getNumItems()); // DAO to report expected number of results
		// Crop results s.t. no results come through
		result = results.getItems(100, 100);
		assertEquals(0, result.size());
		// Crop results to let through first 1 result
		result = results.getItems(0, 1);
		assertEquals(1, result.size());
		// Crop results to let through first 100 results
		result = results.getItems(0, 100);
		assertEquals(100, result.size());
		// Crop results to let through next 10 results
		result = results.getItems(100, 110);
		assertEquals(10, result.size());
		assertEquals(100, result.get(0).getQueryID());
		// Crop results to let through all 1101 results
		result = results.getItems(0, results.getNumItems());
		assertEquals(111, result.size());
		// When the start and end of ranges are reversed
		result = results.getItems(200, 100);
		assertEquals(0, result.size());
		
		// Retrieve a specific one
		results = qd.retrieve("queryID = 100", null);
		result = results.getItems(0, 1);
		assertEquals(1, result.size());
		assertEquals(100, result.get(0).getQueryID());
		assertEquals("KILL 5", result.get(0).getQuery());
		assertEquals(u.getId(), result.get(0).getUser().getId());
		
		// Retrieve based on some selection criteria
		results = qd.retrieve("queryID < 50", null);
		result = results.getItems(0, 50);
		assertEquals(50, result.size());
		
		// Ordering
		results = qd.retrieve(null, "queryID desc");
		result = results.getItems(0, results.getNumItems());
		assertEquals(110, result.get(0).getQueryID());
		
		// Ordering and filtering
		results = qd.retrieve("queryID < 50", "queryID desc");
		result = results.getItems(0, results.getNumItems());
		assertEquals(49, result.get(0).getQueryID());
		
		// Retrieve non-existent query
		results = qd.retrieve("queryID = 500000", null);
		result = results.getItems(0, results.getNumItems());
		assertEquals(0, result.size());
		
		// Error case: starting row number < 0
		results = qd.retrieve(null, null);
		result = results.getItems(-100, 0);
		assertEquals(0, result.size());
		
		// Error case: ending row number < 0
		result = results.getItems(0, -100);
		assertEquals(0, result.size());
		
		// Give bad where clause
		try {
			results = qd.retrieve("queryID is good", null);
			result = results.getItems(0, results.getNumItems());
			fail("Exception was not thrown for bad where clause.");
		}
		catch (DAOException e) {
			// Expected
		}
		
		// Give bad order by clause
		try {
			results = qd.retrieve(null, "queryID is bad");
			result = results.getItems(0, results.getNumItems());
			fail("Exception was not thrown for bad order by clause.");
		}
		catch (DAOException e) {
			// Expected
		}
	}
}
