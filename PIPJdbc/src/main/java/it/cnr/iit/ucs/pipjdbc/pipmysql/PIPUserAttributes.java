package it.cnr.iit.ucs.pipjdbc.pipmysql;


import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;

import iit.cnr.it.ucsinterface.obligationmanager.ObligationInterface;
import iit.cnr.it.ucsinterface.pip.PIPBase;
import iit.cnr.it.ucsinterface.pip.exception.PIPException;
import iit.cnr.it.usagecontrolframework.configuration.xmlclasses.XMLAttribute;
import iit.cnr.it.usagecontrolframework.configuration.xmlclasses.XMLPip;
import iit.cnr.it.xacmlutilities.Attribute;
import iit.cnr.it.xacmlutilities.Category;
import iit.cnr.it.xacmlutilities.DataType;
import iit.cnr.it.xacmlutilities.policy.utility.JAXBUtility;
import it.cnr.iit.pip.pipmysql.tables.UserAttributes;
import oasis.names.tc.xacml.core.schema.wd_17.RequestType;

public class PIPUserAttributes extends PIPBase {

  /**
   * Whenever a PIP has to retrieve some informations related to an attribute
   * that is stored inside the request, it has to know in advance all the
   * informations to retrieve that atrtribute. E.g. if this PIP has to retrieve
   * the informations about the subject, it has to know in advance which is the
   * attribute id qualifying the subject, its category and the datatype used,
   * otherwise it is not able to retrieve the value of that attribute, hence it
   * would not be able to communicate with the AM properly
   */
  private Category expectedCategory;

  private boolean initialized = false;
  
  String filter;
  UserAttributes userAttributes;
  Map<String, String> attributeMap;

  private String databaseURL;
  private ConnectionSource connection;

  // list that stores the attributes on which a subscribe has been performed
  protected final BlockingQueue<Attribute> subscriptions = new LinkedBlockingQueue<>();

  private final static Logger LOGGER = Logger
      .getLogger(PIPUserAttributes.class.getName());

  private Dao<UserAttributes, String> daoUser;
  private Timer timer = new Timer();

  private PSQLSubscriberTimer subscriberTimer;

  public PIPUserAttributes(String xmlPip) {
    super(xmlPip);
    if (!super.isInitialized()) {

    }
    if (initialize(xmlPip)) {
      subscriberTimer = new PSQLSubscriberTimer(contextHandlerInterface,
          subscriptions, null, this);
      timer.scheduleAtFixedRate(subscriberTimer, 0, 10 * 1000);
    }
  }

  /**
   * Initializes the various fields of this PIP
   * 
   * @param xmlPip
   *          the xml configuration
   * @return true if everything goes fine, false otherwise
   */
  private boolean initialize(String xmlPip) {
    try {
      XMLPip xml = JAXBUtility.unmarshalToObject(XMLPip.class, xmlPip);
      List<XMLAttribute> attributes = xml.getAttributes();
      DatabaseConfiguration configurationInterface = DatabaseConfiguration
          .createDBConfiguration(xml);
      databaseURL = configurationInterface.getURL();
      connection = new JdbcPooledConnectionSource(databaseURL);
      for (XMLAttribute xmlAttribute : attributes) {
        attributeMap = xmlAttribute.getArgs();
        Attribute attribute = new Attribute();
        if (!attribute.createAttributeId(attributeMap.get(ATTRIBUTE_ID))) {
          LOGGER.log(Level.SEVERE, "[PIPUserAttributes] wrong set Attribute");
          return false;
        }
        if (!attribute
            .setCategory(Category.toCATEGORY(attributeMap.get(CATEGORY)))) {
          LOGGER.log(Level.SEVERE,
              "[PIPUserAttributes] wrong set category " + attributeMap.get(CATEGORY));
          return false;
        }
        if (!attribute.setAttributeDataType(
            DataType.toDATATYPE(attributeMap.get(DATA_TYPE)))) {
          LOGGER.log(Level.SEVERE, "[PIPUserAttributes] wrong set datatype");
          return false;
        }
        if (attribute.getCategory() != Category.ENVIRONMENT) {
          if (!setExpectedCategory(attributeMap.get(EXPECTED_CATEGORY))) {
            return false;
          }
        }
        addAttribute(attribute);
      }
      daoUser = DaoManager.createDao(connection, UserAttributes.class);
      initialized = true;
      return true;
    } catch (

    Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  @Override
  public void subscribe(RequestType accessRequest) throws PIPException {
     LOGGER.log(Level.SEVERE, "[PIPMYSQL] SUBSCRIBEEEE");

    if (accessRequest == null || !initialized || !isInitialized()) {
      LOGGER.log(Level.SEVERE, "[PIPUserAttributes] wrong initialization" + initialized
          + "\t" + isInitialized());
      return;
    }

    subscriberTimer.setContextHandlerInterface(contextHandlerInterface);

    if (subscriberTimer.getContextHandler() == null || contextHandlerInterface == null) {
      LOGGER.log(Level.SEVERE, "Context handler not set");
      return;
    }

    retrieve(accessRequest);

    for (Attribute attribute : getAttributes()) {
	    // add the attribute to the subscription list
	    if (!subscriptions.contains(attribute)) {
	      subscriptions.add(attribute);
	    }
    }
  }
  

//  private String getPilotType () {
//	  for(Entry e : attributeMap)
//  }

  @Override
  public void retrieve(RequestType accessRequest) throws PIPException {
    if (accessRequest == null || !initialized || !isInitialized()) {
      LOGGER.log(Level.SEVERE, "[PIPUserAttributes] wrong initialization" + initialized
          + "\t" + isInitialized());
      return;
    }

    filter = accessRequest.extractValue(Category.SUBJECT);

    LOGGER.log(Level.SEVERE, "[PIPUserAttributes] wrong initialization" + initialized
            + "\t" + isInitialized());
    userAttributes = read(filter);

    for (Attribute attribute : getAttributes()) {
      if (attribute.getAttributeId().contains("organisation")) {

    	try{
	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
	            .getAttributeId() + " for " + filter + " Value:" + userAttributes
	                .getOrganizationName());
	
	        accessRequest.addAttribute(attribute.getCategory().toString(), attribute
	            .getAttributeDataType().toString()
	            .toString(), attribute.getAttributeId(), userAttributes
	                .getOrganizationName());
    	}catch (Exception e) {
    		accessRequest.addAttribute(attribute.getCategory().toString(), attribute
    	            .getAttributeDataType().toString()
    	            .toString(), attribute.getAttributeId(), "");
		}
      }
      if (attribute.getAttributeId().contains("role")) {
      	try{

	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
	            .getAttributeId() + " for " + filter + " Value:" + userAttributes
	                .getRole());
	
	        accessRequest.addAttribute(attribute.getCategory().toString(), attribute
	            .getAttributeDataType().toString()
	            .toString(), attribute.getAttributeId(), userAttributes
	                .getRole());
      	}catch (Exception e) {
      		accessRequest.addAttribute(attribute.getCategory().toString(), attribute
    	            .getAttributeDataType().toString()
    	            .toString(), attribute.getAttributeId(), "");
		}
      }
      if (attribute.getAttributeId().contains("ismemberof")) {
        	try{

  	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
  	            .getAttributeId() + " for " + filter + " Value:" + userAttributes
  	                .getGroup());
  	
  	        accessRequest.addAttribute(attribute.getCategory().toString(), attribute
  	            .getAttributeDataType().toString()
  	            .toString(), attribute.getAttributeId(), userAttributes
  	                .getGroup());
        	}catch (Exception e) {
        		accessRequest.addAttribute(attribute.getCategory().toString(), attribute
      	            .getAttributeDataType().toString()
      	            .toString(), attribute.getAttributeId(), "");
        	}
        }
      if (attribute.getAttributeId().contains("country")) {
        	try{

  	        LOGGER.log(Level.INFO, "[PIPUserAttributes] AttributeId: " + attribute
  	            .getAttributeId() + " for " + filter + " Value:" + userAttributes
  	                .getCountry());
  	
  	        accessRequest.addAttribute(attribute.getCategory().toString(), attribute
  	            .getAttributeDataType().toString()
  	            .toString(), attribute.getAttributeId(), userAttributes
  	                .getCountry());
        	}catch (Exception e) {
        		accessRequest.addAttribute(attribute.getCategory().toString(), attribute
      	            .getAttributeDataType().toString()
      	            .toString(), attribute.getAttributeId(), "");
  		}
        }
    }
    // accessRequest.addAttribute(Category.SUBJECT.toString(),
    // attribute.getAttributeDataType().toString(), attribute.getAttributeId(),
    // value);
  }

  @Override
  public void unsubscribe(List<Attribute> attributes) throws PIPException {
    // TODO Auto-generated method stub

  }

  @Override
  public String retrieve(Attribute attributeRetrievals) throws PIPException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String subscribe(Attribute attributeRetrieval) throws PIPException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void retrieve(RequestType request,
      List<Attribute> attributeRetrievals) {
    // TODO Auto-generated method stub

  }

  @Override
  public void subscribe(RequestType request,
      List<Attribute> attributeRetrieval) {
    // TODO Auto-generated method stub

  }

  @Override
  public void updateAttribute(String json) throws PIPException {
    // TODO Auto-generated method stub

  }

  @Override
  public void performObligation(ObligationInterface obligation) {
    // TODO Auto-generated method stub

  }

  final private boolean setExpectedCategory(String category) {
    // BEGIN parameter checking
    if (!isInitialized() || category == null || category.isEmpty()) {
      initialized = false;
      return false;
    }
    // END parameter checking
    Category categoryObj = Category.toCATEGORY(category);
    if (categoryObj == null) {
      initialized = false;
      return false;
    }
    this.expectedCategory = categoryObj;
    return true;
  }

  private UserAttributes read(String string) {
	 UserAttributes user = null;
    try {
      QueryBuilder<UserAttributes, String> qbAttributes = daoUser
          .queryBuilder();
      List<UserAttributes> attributes = qbAttributes.where()
          .eq("username", string).query();
      if(attributes.size()>0)
    	  user = attributes.get(0);      
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return user;
    
  }
  

}
