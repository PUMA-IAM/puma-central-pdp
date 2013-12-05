package puma.central.pdp.attr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityDatabase {

	/**************************
	 * CONSTRUCTOR
	 */

	private static final Logger logger = Logger.getLogger(EntityDatabase.class
			.getName());

	/**
	 * Initializes this new EntityDatabase. Does not open a connection yet.
	 */
	public EntityDatabase() {
		
	}

	/**************************
	 * DATABASE OPERATIONS
	 */
	
	private Connection conn;
	
	/**
	 * Sets up the connection to the database in read/write mode. 
	 * Autocommit is disabled for this connection, so know you have to commit yourself!
	 */
	public void open() {
		open(false);
	}
	
	/**
	 * Sets up the connection to the database in given mode. 
	 * Autocommit is disabled for this connection, so know you have to commit yourself!
	 */
	public void open(boolean readOnly) {
		try {
			conn = null; // TODO get db connection
			conn.setReadOnly(readOnly);
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Cannot open connection.", e);
		}
	}
	
	/**
	 * Commits all operations.
	 */
	public void commit() {
		try {
			conn.commit();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Cannot commit.", e);
		}
	}
	
	/**
	 * Closes the connection to the database.
	 */
	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error when closing connection to the database.", e);
		}
	}
	
	/**
	 * Opens a connection, creates the tables, commits and closes the connection.
	 */
	public static void createTables() {
		// TODO update this query
		try {
			Connection conn = null; // TODO get connection to the database  
			PreparedStatement createTablesPS = conn.prepareStatement("CREATE TABLE `attributes` (\n" + 
					"  `id` int(11) NOT NULL AUTO_INCREMENT,\n" + 
					"  `entity_id` varchar(45) NOT NULL,\n" + 
					"  `attribute_key` varchar(45) NOT NULL,\n" + 
					"  `attribute_value` varchar(100) NOT NULL,\n" + 
					"  PRIMARY KEY (`id`),\n" + 
					"  KEY `index` (`entity_id`,`attribute_key`)\n" + 
					");");
			createTablesPS.execute();
			conn.close();
			logger.info("Successfully created tables.");
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Cannot create tables.", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Fetches all supported XACML attribute ids from the database.
	 */
	public Set<String> getSupportedXACMLAttributeIds() {
		return null; // TODO fetch from the database
	}
	
	/**
	 * Fetches the data type of the attribute family with given id from the database.
	 */
	public DataType getDataType(String attributeId) {
		return null; // TODO fetch from the database
	}

	/**
	 * Fetches a string attribute from the database using the connection of this database.
	 * Does NOT commit or close.
	 */
	public Set<String> getStringAttribute(String entityId, String key) {
		// TODO update
		try {
			PreparedStatement getAttributePS = conn.prepareStatement("SELECT * FROM attributes WHERE entity_id=? && attribute_key=?;");
			getAttributePS.setString(1, entityId);
			getAttributePS.setString(2, key);
			ResultSet result = getAttributePS.executeQuery();
			// process the result
			Set<String> r = new HashSet<String>();
			while (result.next()) {
				r.add(result.getString("attribute_value"));
			}
			return r;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Cannot execute query.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches an integer attribute from the database using the connection of this database.
	 * Does NOT commit or close.
	 */
	public Set<Integer> getIntegerAttribute(String entityId, String key) {
		// TODO update
		Set<String> strings = getStringAttribute(entityId, key);
		Set<Integer> result = new HashSet<Integer>();
		for(String s: strings) {
			result.add(Integer.valueOf(s));
		}
		return result;
	}

	/**
	 * Fetches a boolean attribute from the database using the connection of this database.
	 * Does NOT commit or close.
	 */
	public Set<Boolean> getBooleanAttribute(String entityId, String key) {
		// TODO update
		Set<String> strings = getStringAttribute(entityId, key);
		Set<Boolean> result = new HashSet<Boolean>();
		for(String s: strings) {
			if(s.equals("true")) {
				result.add(true);
			} else {
				result.add(false);
			}
		}
		return result;
	}

	/**
	 * Fetches a boolean attribute from the database using the connection of this database.
	 * Does NOT commit or close.
	 */
	public Set<Date> getDateAttribute(String entityId, String key) {
		// TODO update
		Set<String> strings = getStringAttribute(entityId, key);
		Set<Date> result = new HashSet<Date>();
		DateFormat df = DateFormat.getInstance();
		for(String s: strings) {
			try {
				Date d = df.parse(s);
				result.add(d);
			} catch (ParseException e) {
				logger.log(Level.WARNING, "error when parsin date", e);
			}
		}
		return result;
	}

//	/**
//	 * Stores an integer attribute in the database using the connection of this database.
//	 * Does NOT commit or close.
//	 */
//	public void storeAttribute(String entityId, String key, int value) {
//		// TODO update
//		this.storeAttribute(entityId, key, "" + value);
//	}
//
//	/**
//	 * Stores a boolean attribute in the database using the connection of this database.
//	 * Does NOT commit or close.
//	 */
//	public void storeAttribute(String entityId, String key, boolean value) {
//		// TODO update
//		String v;
//		if(value) {
//			v = "true";
//		} else {
//			v = "false";
//		}
//		this.storeAttribute(entityId, key, v);
//	}
//	
//	/**
//	 * Stores a string attribute in the database using the connection of this database.
//	 * Does NOT commit or close.
//	 */
//	public void storeAttribute(String entityId, String key, Date value) {
//		// TODO update
//		String v;
//		if(value != null) {
//			DateFormat df = DateFormat.getInstance();
//			v = df.format(value);
//		} else {
//			v = "";
//		}
//		this.storeAttribute(entityId, key, v);
//	}
//	
//	/**
//	 * Stores a set of string attributes in the database using the connection of this database.
//	 * Does NOT commit or close.
//	 */
//	public void storeAttribute(String entityId, String key, Collection<String> value) {
//		// TODO update
//		for(String s: value) {
//			this.storeAttribute(entityId, key, s);
//		}
//	}
//	
//	/**
//	 * Stores a string attribute in the database using the connection of this database.
//	 * Does NOT commit or close.
//	 */
//	public void storeAttribute(String entityId, String key, String value) {
//		// TODO update
//		try {
//			PreparedStatement storeAttributePS = conn.prepareStatement("INSERT INTO attributes VALUES (default, ?, ?, ?);");
//			storeAttributePS.setString(1, entityId);
//			storeAttributePS.setString(2, key);
//			storeAttributePS.setString(3, value);
//			storeAttributePS.executeUpdate();
//		} catch (SQLException e) {
//			logger.log(Level.SEVERE, "Cannot execute query.", e);
//			throw new RuntimeException(e);
//		}
//	}

}
