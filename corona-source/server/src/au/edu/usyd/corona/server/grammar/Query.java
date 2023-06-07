package au.edu.usyd.corona.server.grammar;


import java.io.Serializable;

import au.edu.usyd.corona.server.user.User;

/**
 * This class stores all information associated with a Query that has been
 * executed in the system. It is only used to notify the Client (GUI) of details
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class Query implements Serializable {
	// Query status 
	public static final int STATUS_SUBMITTED = 0;
	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_KILLED = 2;
	public static final int STATUS_COMPLETE = 3;
	
	private long submittedTime; // Time the job was submitted to the system
	private long firstExecutionTime; // Time the job will be first executed
	private User user; // The user who executed the query
	private String query; // The actual query string that was executed
	private long executionTime; // The next time that the query will be executed (or the last time if the query is complete)
	private int queryID; // The id of the query
	private long reschedulePeriod; // The time between executions of the query
	private int runCountLeft; // The number of executions left of the query
	private int runCountTotal; // The total number of times the query will be executed
	private int status; // The status of the query (SUBMITTED, RUNNING, KILLED, COMPLETE)
	private long rootTaskNodeID; // The nodeID of the Task associated with this query
	private int rootTaskLocalID; // The localID of the Task associated with this query
	
	public Query() {
	}
	
	public Query(int queryID, String query, long submittedTime, long executionTime, long firstExecutionTime, long reschedulePeriod, int runCountLeft, int runCountTotal, int status, User user, long rootTaskNodeID, int rootTaskLocalID) {
		this.queryID = queryID;
		this.query = query;
		this.submittedTime = submittedTime;
		this.executionTime = executionTime;
		this.firstExecutionTime = firstExecutionTime;
		this.reschedulePeriod = reschedulePeriod;
		this.runCountLeft = runCountLeft;
		this.runCountTotal = runCountTotal;
		this.status = status;
		this.user = user;
		this.rootTaskNodeID = rootTaskNodeID;
		this.rootTaskLocalID = rootTaskLocalID;
	}
	
	public long getRootTaskNodeID() {
		return rootTaskNodeID;
	}
	
	public void setRootTaskNodeID(long rootTaskNodeID) {
		this.rootTaskNodeID = rootTaskNodeID;
	}
	
	public int getRootTaskLocalID() {
		return rootTaskLocalID;
	}
	
	public void setRootTaskLocalID(int rootTaskLocalID) {
		this.rootTaskLocalID = rootTaskLocalID;
	}
	
	public int getQueryID() {
		return queryID;
	}
	
	public void setQueryID(int queryID) {
		this.queryID = queryID;
	}
	
	public long getSubmittedTime() {
		return submittedTime;
	}
	
	public void setSubmittedTime(long submittedTime) {
		this.submittedTime = submittedTime;
	}
	
	public long getFirstExecutionTime() {
		return firstExecutionTime;
	}
	
	public void setFirstExecutionTime(long firstExecutionTime) {
		this.firstExecutionTime = firstExecutionTime;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public long getExecutionTime() {
		return executionTime;
	}
	
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}
	
	public long getReschedulePeriod() {
		return reschedulePeriod;
	}
	
	public void setReschedulePeriod(long reschedulePeriod) {
		this.reschedulePeriod = reschedulePeriod;
	}
	
	public int getRunCountLeft() {
		return runCountLeft;
	}
	
	public void setRunCountLeft(int runCountLeft) {
		this.runCountLeft = runCountLeft;
	}
	
	public int getRunCountTotal() {
		return runCountTotal;
	}
	
	public void setRunCountTotal(int runCountTotal) {
		this.runCountTotal = runCountTotal;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
}
