package it.cnr.iit.ucs.pipjdbc.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import it.cnr.iit.common.reject.Reject;

public final class DBInfoStorage {

	private static String dbUrl;
	private static final String dbFallbackUrl = "jdbc:sqlite::memory:";

	private static volatile boolean initialized = false;
	private static ConnectionSource connection;
	private static Map<Class<?>, Dao<?, String>> daoMap = new ConcurrentHashMap<Class<?>, Dao<?, String>>();

	private static Logger LOGGER = Logger.getLogger(DBInfoStorage.class.getName());

	private DBInfoStorage() {
	}

	public static boolean start(String databaseUrl) {
		dbUrl = databaseUrl;
		System.out.println("called start in CommonDatabase");
		try {
			connection = new JdbcPooledConnectionSource(dbUrl);
			return initialized = true;
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		try {
			connection = new JdbcPooledConnectionSource(dbFallbackUrl);
			return initialized = true;
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return initialized = false;
	}

	public static Boolean stop() {
		if (initialized == false) {
			LOGGER.log(Level.SEVERE, "portal DB was not correctly initialized");
			return false;
		}

		try {
			connection.close();

		} catch (SQLException ex) {
			ex.printStackTrace();
			initialized = false;
			return false;
		}
		return true;
	}

	private static void refresh() {
		if (!connection.isOpen()) {
			LOGGER.log(Level.INFO, "Refreshing db connection...");
			start(dbUrl);
		}
	}

	@SuppressWarnings("unchecked")
	public static Dao<?, String> getDao(Class<?> clazz) {
		try {
			connection = new JdbcPooledConnectionSource(dbUrl);

			if (!daoMap.containsKey(clazz)) {
				Dao<?, String> dao = (Dao<?, String>) DaoManager.createDao(connection, clazz);
				LOGGER.log(Level.INFO, "Creating table " + dao.getDataClass().getName());
				TableUtils.createTableIfNotExists(connection, dao.getDataClass());
				daoMap.put(dao.getDataClass(), dao);
			}
			return daoMap.get(clazz);
		} catch (SQLException ex) {
			ex.printStackTrace();
			initialized = false;
			return null;
		}
	}

	public static <T> boolean createOrUpdateEntry(T entry) {
		if (initialized == false) {
			LOGGER.log(Level.SEVERE, "DB was not correctly initialized");
			return false;
		}
		refresh();

		try {
			@SuppressWarnings("unchecked")
			Dao<T, String> dao = (Dao<T, String>) getDao(entry.getClass());
			dao.createOrUpdate(entry);
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}

		LOGGER.log(Level.INFO, "Entry created");
		return true;
	}

	public static <T> boolean deleteEntry(int id, Class<T> clazz) {
		if (initialized == false) {
			LOGGER.log(Level.SEVERE, "DB was not correctly initialized");
			return false;
		}
		try {
			refresh();
			@SuppressWarnings("unchecked")
			Dao<T, String> dao = (Dao<T, String>) getDao(clazz);
			dao.deleteById(String.valueOf(id));
		} catch (SQLException ex) {
			return false;
		}

		LOGGER.log(Level.INFO, "Entry removed");
		return true;
	}

	public static List<?> getWholeTable(Class<?> clazz) {
		System.out.println("called getWholeTable");
		refresh();
		try {
			return getDao(clazz).queryForAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> Object getElementById(int id, Class<?> clazz) {
		refresh();
		try {
			Dao<T, String> dao = (Dao<T, String>) getDao(clazz);
			return dao.queryForId(String.valueOf(id));

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<?> getRowsByOneOrMoreParams(Map<String, Object> params, Class<?> clazz) {
		refresh();
		try {
			Dao<T, String> dao = (Dao<T, String>) getDao(clazz);
			return dao.queryForFieldValuesArgs(params);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<?> getColumn(String columnName, Class<?> clazz) {
		refresh();
		try {
			Dao<T, String> dao = (Dao<T, String>) getDao(clazz);
			return dao.queryBuilder().selectColumns(columnName).query();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static <T> T getField(String column, String value, Class<T> clazz) {
		refresh();
		List<T> objs = getFields(column, value, clazz);
		if (objs == null || objs.size() > 1) {
			throw new IllegalStateException("Same field used multiple times");
		}
		return objs.stream().findFirst().orElse(null);
	}

	public static <T> List<T> getFields(String column, String value, Class<T> clazz) {
		refresh();
		Reject.ifBlank(column);
		Reject.ifBlank(value);
		try {
			Dao<T, String> dao = (Dao<T, String>) getDao(clazz);
			return dao.queryBuilder().where().eq(column, value).query();
		} catch (Exception e) {
			LOGGER.severe(() -> e.getClass().getSimpleName() + " : " + column + " :" + value + ", " + e.getMessage());
		}
		return new ArrayList<>();
	}

}
