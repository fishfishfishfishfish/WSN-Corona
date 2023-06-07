package au.edu.usyd.corona.server.persistence.DAOinterface;


/**
 * Represents an Exception resulting from a Data Access Object. This should
 * abstract from the actual implementation details of the DAO.
 * 
 * This class can be used directly or subclassed to provide appropriate error
 * reporting.
 * 
 * @author Raymes Khoury
 */
@SuppressWarnings("serial")
public class DAOException extends Exception {
	
	public DAOException(String msg, Exception e) {
		super(msg, e);
	}
	
	public DAOException(String msg) {
		super(msg);
	}
	
	@Override
	public String getMessage() {
		if (getCause() != null)
			return super.getMessage() + ": " + getCause().getMessage();
		return super.getMessage();
	}
	
	@Override
	public void printStackTrace() {
		if (getCause() != null)
			getCause().printStackTrace();
		super.printStackTrace();
	}
}
