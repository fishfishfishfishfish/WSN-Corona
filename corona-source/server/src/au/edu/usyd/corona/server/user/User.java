package au.edu.usyd.corona.server.user;


import java.io.Serializable;

/**
 * This class represents a User and contains all information associated with a
 * User of the system. As of present this includes only an id, a username, a
 * password and an access level.
 * 
 * @author Edmund Tse
 */
@SuppressWarnings("serial")
public class User implements Serializable {
	/**
	 * This enum represents the available access levels of a user. A class would
	 * be required here if user access levels became more complicated.
	 * 
	 * The ADMIN level has full access to the system. The USER level has access
	 * only to view queries that they have executed.
	 * 
	 * @author Edmund Tse
	 * 
	 */
	public static enum AccessLevel implements Serializable {
		ADMIN, USER;
	}
	
	public static final AccessLevel DEFAULT_ACCESS_LEVEL = AccessLevel.USER;
	
	private int id;
	private String username;
	private String password;
	private AccessLevel accessLevel;
	
	// Constructors
	public User(String username) {
		this(username, null);
	}
	
	public User(String username, String password) {
		this(-1, username, password, DEFAULT_ACCESS_LEVEL);
	}
	
	public User(String username, String password, AccessLevel accessLevel) {
		this(-1, username, password, accessLevel);
	}
	
	public User(int id, String username, String password, AccessLevel accessLevel) {
		this.id = id;
		this.username = username.trim().toLowerCase();
		this.password = password;
		this.accessLevel = accessLevel;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setAccessLevel(AccessLevel accessLevel) {
		this.accessLevel = accessLevel;
	}
	
	public AccessLevel getAccessLevel() {
		return accessLevel;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof User))
			return false;
		return id == ((User) obj).id;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public String toString() {
		return "User (" + username + ")";
	}
}
