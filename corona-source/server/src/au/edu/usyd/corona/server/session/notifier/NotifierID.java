package au.edu.usyd.corona.server.session.notifier;


import java.io.Serializable;

/**
 * This class represents the ID of a notifier, which consists of the notifier's
 * type and an identifier if applicable
 * 
 * @author Raymes Khoury
 */
@SuppressWarnings("serial")
public class NotifierID implements Serializable {
	/**
	 * The type of the notifier
	 * 
	 */
	public enum NotifierType implements Serializable {
		RESULT_TABLE_NOTIFIER, QUERIES_TABLE_NOTIFIER, USERS_TABLE_NOTIFIER
	}
	
	private final NotifierType type;
	private final int id;
	
	/**
	 * Construct a notifier with the given type (and no ID)
	 * 
	 * @param type The type of the notifier
	 */
	public NotifierID(NotifierType type) {
		this.type = type;
		this.id = -1;
	}
	
	/**
	 * Construct a notifier with the given type and ID
	 * 
	 * @param type The type of the notifier
	 * @param id The id of the notifier
	 */
	public NotifierID(NotifierType type, int id) {
		this.type = type;
		this.id = id;
	}
	
	/**
	 * Retrieve the type of the notifier
	 * 
	 * @return The type of the notifier
	 */
	public NotifierType getType() {
		return type;
	}
	
	/**
	 * Retrieve the id of the notifier
	 * 
	 * @return The id of the notifier
	 */
	public int getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof NotifierID) {
			NotifierID n = (NotifierID) o;
			if (n.type == type && n.id == id)
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return type.hashCode() ^ (new Integer(id)).hashCode();
	}
	
	@Override
	public String toString() {
		return "(" + type + "," + id + ")";
	}
}
