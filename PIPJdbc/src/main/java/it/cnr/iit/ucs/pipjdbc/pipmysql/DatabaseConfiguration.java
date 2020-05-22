package it.cnr.iit.ucs.pipjdbc.pipmysql;

import java.util.ArrayList;
import java.util.Map;

import iit.cnr.it.usagecontrolframework.configuration.xmlclasses.XMLPip;
import it.cnr.iit.sqlmiddleware.HIBERNATE_CONFIGURATIONS;
import it.cnr.iit.sqlmiddlewareinterface.SQLMiddlewarePIPConfigurationInterface;

/**
 * This class represents the configuration for the SQLMiddleware.
 * <p>
 * Via this class we configure the SQLMiddleware to deal with the underlying
 * database. This class will be filled up via the configurations provided in the
 * conf.xml.
 * </p>
 * 
 * @author antonio
 *
 */
public class DatabaseConfiguration
    implements SQLMiddlewarePIPConfigurationInterface {

  private boolean initialized = false;

  private String driver;
  private String password;
  private String username;
  private String connections;
  private String url;
  private ArrayList<Class<?>> classes = new ArrayList<>();

  public static final DatabaseConfiguration createDBConfiguration(XMLPip xml) {
    // BEGIN parameter checking
    if (xml == null) {
      return null;
    }
    // END parameter checking
    DatabaseConfiguration configuration = new DatabaseConfiguration();
    Map<String, String> args = xml.getAttributes().get(0).getArgs();
    if ((configuration.driver = args
        .get(HIBERNATE_CONFIGURATIONS.DRIVER_CLASS.getString())) == null) {
      return null;
    }
    if ((configuration.password = args
        .get(HIBERNATE_CONFIGURATIONS.PASSWORD.getString())) == null) {
      return null;
    }
    if ((configuration.username = args
        .get(HIBERNATE_CONFIGURATIONS.USERNAME.getString())) == null) {
      return null;
    }
    if ((configuration.url = args
        .get(HIBERNATE_CONFIGURATIONS.URL.getString())) == null) {
      return null;
    }
    configuration.initialized = true;
    return configuration;
  }

  @Override
  public String getDriver() {
    // BEGIN parameter checking
    if (!initialized) {
      return null;
    }
    // END parameter checking
    return driver;
  }

  @Override
  public String getUsername() {
    // BEGIN parameter checking
    if (!initialized) {
      return null;
    }
    // END parameter checking
    return username;
  }

  @Override
  public String getPassword() {
    // BEGIN parameter checking
    if (!initialized) {
      return null;
    }
    // END parameter checking
    return password;
  }

  @Override
  public String getConnections() {
    // BEGIN parameter checking
    if (!initialized) {
      return null;
    }
    // END parameter checking
    return connections;
  }

  @Override
  public String getURL() {
    // BEGIN parameter checking
    if (!initialized) {
      return null;
    }
    // END parameter checking
    return url;
  }

  @Override
  public ArrayList<Class<?>> getClassesForTable() {
    // BEGIN parameter checking
    if (!initialized) {
      return null;
    }
    // END parameter checking
    return classes;
  }

  public void addClass(Class<?> clazz) {
    // BEGIN paramter checking
    if (!initialized) {
      return;
    }
    // END parameter checking
    classes.add(clazz);
  }

}
