package it.cnr.iit.ucs.pipreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * Creates an object used to manage the interaction with the LDAP server:
 * authenticated access, retrieving and updating attribute values for users
 *
 * @author Fabio Bindi and Filippo Lauria
 */
public class LDAPConnector {

	private final static Logger LOGGER = Logger.getLogger(LDAPConnector.class.getName());

	private final String ldaphost;
	private final String binddn;

	private final Hashtable<String, String> environment;
	private LdapContext ldapContext;

	/**
	 * Constructor: it creates parameters to connect to the LDAP server
	 * 
	 * @param ldaphost_   host on which the ldap server is running (i.e
	 *                    ldap://ldap.forumsys.com)
	 * @param binddn_     the Distinguished Name binddn to bind to the LDAP
	 *                    directory. (i.e. cn=admin,dc=cnr,dc=it)
	 * @param searchbase_ the starting point for the search instead of the default.
	 *                    It represents the user which we want to read/update the
	 *                    attributes (i.e uid=fabio.bindi,ou=iit,dc=cnr,dc=it)
	 */
	public LDAPConnector(String ldaphost_, String binddn_) {
		ldaphost = ldaphost_;
		binddn = binddn_;

		environment = new Hashtable<>();
		ldapContext = null;
	}

	/**
	 * Initializes the environment and authenticates to the LDAP server
	 * 
	 * @param password_
	 * @throws NamingException
	 */
	public void authenticate(String password_) throws NamingException {

		// initialize the environment
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, ldaphost);
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, binddn);
		environment.put(Context.SECURITY_CREDENTIALS, password_);

		// This is the actual Authentication piece. Will throw
		// javax.naming.AuthenticationException
		// if the users password is not correct. Other exceptions may include IO
		// (server not found) etc.
		ldapContext = new InitialLdapContext(environment, null);
	}

	/**
	 * Searchs into the LDAP servers the attributes values of a certain user
	 * 
	 * @param searchAttributesList List of attributes names of which we want to read
	 *                             its values
	 * @return a map containing each attribute name and its set of values (could be
	 *         just one)
	 */
	public Map<String, Set<String>> search(String searchbase, List<String> searchAttributesList, String filter) {
		String searchFilter = "(uid=" + filter + ")";

		String[] searchAttributes_ = new String[searchAttributesList.size()];
		searchAttributes_ = searchAttributesList.toArray(searchAttributes_);

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(searchAttributes_);

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		Map<String, Set<String>> resultMap = new HashMap<>();

		try {
			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes ldapAttributes = result.getAttributes();
				NamingEnumeration<?> ldapEnumeration = ldapAttributes.getAll();

				while (ldapEnumeration.hasMoreElements()) {
					Set<String> ldapValueSet = new HashSet<>();
					javax.naming.directory.Attribute ldapAttribute = (javax.naming.directory.Attribute) ldapEnumeration
							.next();

					NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll();
					while (ldapValueEnumeration.hasMoreElements()) {
						ldapValueSet.add((String) ldapValueEnumeration.next());
					}

					resultMap.put(ldapAttribute.getID(), ldapValueSet);
					ldapValueEnumeration.close();
				}

				ldapEnumeration.close();
			}

			answer.close();
			return resultMap;

		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return resultMap;
		}
	}

	public ArrayList<String> getPilots() {
		String searchbase = "ou=Pilots,dc=c3isp,dc=eu";
		String searchFilter = "(&(objectclass=*))";

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(new String[] { "ou" });

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.ONELEVEL_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		ArrayList<String> pilots = new ArrayList<String>();

		try {
			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				pilots.add(result.getName());
			}
			answer.close();
			return pilots;

		} catch (NamingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Retrieves the schema related to each user, thus it retrieves in which LDAP
	 * directories User information are stored.
	 * <p>
	 * In case of a regular user this function will return cn=user,ou=Users, while
	 * in BT use case this function will return
	 * cn=paulabarnes,ou=Users,ou=Enterprise,ou=Pilots,dc=c3isp,dc=eu
	 * </p>
	 * 
	 * @param identifier the user identifier
	 * @return the schema of that user
	 */
	public String getSchemaForUser(String pilotName, String identifier) {
		String searchFilter = "(uid=" + identifier + ")";
		String searchbase = "dc=c3isp,dc=eu";
		searchbase = "ou=" + pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(new String[] { "dn" });

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		String returnedValue = null;

		try {
			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				returnedValue = result.getName();
			}
			answer.close();
			return returnedValue;

		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return "";
		}
	}

	public String getPilotName(ArrayList<String> pilotNameList, String identifier) {
		try {
			for (String pilotName : pilotNameList) {
				String searchFilter = "(uid=" + identifier + ")";
				String searchbase = "dc=c3isp,dc=eu";
				searchbase = pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

				// Create the search controls
				SearchControls searchCtls = new SearchControls();
				searchCtls.setReturningAttributes(new String[] { "cn" });

				// Specify the search scope
				searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

				// Now try a simple search and get some attributes
				// as defined in searchAttributes_
				NamingEnumeration<SearchResult> answer;

				String returnedValue = null;

				answer = ldapContext.search(searchbase, searchFilter, searchCtls);
				while (answer.hasMoreElements()) {
					SearchResult result = answer.next();
					returnedValue = result.getName();
				}
				answer.close();

				if (returnedValue != null) {
					return pilotName.split("ou=")[1];
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return null;
		}
		return null;
	}

	public String getTopPilotName(ArrayList<String> pilotNameList, String identifier) {
		try {
			for (String pilotName : pilotNameList) {
				String searchFilter = "(uid=" + identifier + ")";
				String searchbase = "dc=c3isp,dc=eu";
				searchbase = pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

				// Create the search controls
				SearchControls searchCtls = new SearchControls();
				searchCtls.setReturningAttributes(new String[] { "dn" });

				// Specify the search scope
				searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

				// Now try a simple search and get some attributes
				// as defined in searchAttributes_
				NamingEnumeration<SearchResult> answer;

				String returnedValue = null;

				answer = ldapContext.search(searchbase, searchFilter, searchCtls);
				while (answer.hasMoreElements()) {
					SearchResult result = answer.next();
					returnedValue = result.getName();

				}
				answer.close();

				if (returnedValue != null) {
					return returnedValue.split("ou=")[1];
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return null;
		}
		return null;
	}

	/**
	 * Searchs into the LDAP servers the attributes values of a certain user
	 * 
	 * @param searchAttributesList List of attributes names of which we want to read
	 *                             its values
	 * @return a map containing each attribute name and its set of values (could be
	 *         just one)
	 * @deprecated
	 */
	@Deprecated
	public ArrayList<String> btIsMemberOf_old(String filter) {
		String btSearchbase = "dc=c3isp,dc=eu";
		String searchFilter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=cn=" + filter
				+ ",ou=Users,ou=Enterprise,ou=Pilots,dc=c3isp,dc=eu))";

		String[] searchAttributes_ = new String[] { "dn", "o" };

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(searchAttributes_);

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		ArrayList<String> resultMap = new ArrayList<>();

		try {
			answer = ldapContext.search(btSearchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				resultMap.add(result.getName().split(",")[0].split("cn=")[1]);
			}
			answer.close();
			LOGGER.info(resultMap.toString());
			return resultMap;

		} catch (

		NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return resultMap;
		}
	}

	public Map<String, Set<String>> roleInStandard(String filter, String pilotName) {
		String btSearchbase = "dc=c3isp,dc=eu";
		String searchFilter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=cn=" + filter + ",ou=Users,ou="
				+ pilotName + ",ou=Pilots,dc=c3isp,dc=eu))";
		// String searchFilter = "(uid=user)";
		String[] searchAttributes_ = new String[] { "cn" };

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(searchAttributes_);

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		Map<String, Set<String>> resultMap = new HashMap<>();

		try {
			answer = ldapContext.search(btSearchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes ldapAttributes = result.getAttributes();
				NamingEnumeration<?> ldapEnumeration = ldapAttributes.getAll();
				if (!resultMap.containsKey("role")) {
					resultMap.put("role", new HashSet<String>());
				}
				resultMap.get("role").add(result.getName().split(",")[0].split("cn=")[1]);

				while (ldapEnumeration.hasMoreElements()) {
					Set<String> ldapValueSet = new HashSet<>();
					javax.naming.directory.Attribute ldapAttribute = (javax.naming.directory.Attribute) ldapEnumeration
							.next();

					NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll();
					while (ldapValueEnumeration.hasMoreElements()) {
						ldapValueSet.add((String) ldapValueEnumeration.next());
					}

					resultMap.put(ldapAttribute.getID(), ldapValueSet);
					ldapValueEnumeration.close();
				}

				ldapEnumeration.close();
			}

			answer.close();
			return resultMap;

		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return null;
		}
	}

	public Set<String> companyInStandard(String filter, String pilotName) {
		String searchFilter = "(uid=" + filter + ")";
		String searchbase = "ou=" + pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(new String[] { "o" });

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		Set<String> organization = new HashSet<>();
		try {
			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes ldapAttributes = result.getAttributes();
				NamingEnumeration<?> ldapEnumeration = ldapAttributes.getAll();

				while (ldapEnumeration.hasMoreElements()) {
					Set<String> ldapValueSet = new HashSet<>();
					javax.naming.directory.Attribute ldapAttribute = (javax.naming.directory.Attribute) ldapEnumeration
							.next();

					NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll();
					while (ldapValueEnumeration.hasMoreElements()) {
						ldapValueSet.add((String) ldapValueEnumeration.next());
					}

					organization.addAll(ldapValueSet);
				}
			}
			answer.close();

			return organization;

		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return organization;
		}
	}

	public String getCnInStandard(String filter, String pilotName) {
		try {
			String searchFilter = "(uid=" + filter + ")";
			String searchbase = "ou=" + pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

			// Create the search controls
			SearchControls searchCtls = new SearchControls();
			searchCtls.setReturningAttributes(new String[] { "cn" });

			// Specify the search scope
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// Now try a simple search and get some attributes
			// as defined in searchAttributes_
			NamingEnumeration<SearchResult> answer;

			String cnName = null;

			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				cnName = result.getName();
			}
			answer.close();
			if (cnName != null) {
				return cnName.split("cn=")[1];
			}

			return cnName;

		} catch (NamingException e) {
			e.printStackTrace();
			System.out.println("Exception in LDAP CN name");
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return null;
		}
	}

	public String getCnInSubpilot(String filter, String pilotName, String topPilotName) {
		try {
			String searchFilter = "(uid=" + filter + ")";
			String searchbase = "ou=" + topPilotName + ", ou=" + pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

			// Create the search controls
			SearchControls searchCtls = new SearchControls();
			searchCtls.setReturningAttributes(new String[] { "cn" });

			// Specify the search scope
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// Now try a simple search and get some attributes
			// as defined in searchAttributes_
			// as defined in searchAttributes_
			NamingEnumeration<SearchResult> answer;

			String cnName = null;

			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				cnName = result.getName();
			}
			answer.close();
			if (cnName != null) {
				return cnName.split("cn=")[1];
			}

			return cnName;

		} catch (NamingException e) {
			e.printStackTrace();
			System.out.println("Exception in LDAP CN name");
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return null;
		}
	}

	public Set<String> getCountry(String identifier) {
		String country = null;
		Set<String> setCountry = new HashSet<>();

		try {

			String searchFilter = "(cn=" + identifier + ")";
			String searchbase = "ou=Users,dc=c3isp,dc=eu";
			// Create the search controls
			SearchControls searchCtls = new SearchControls();
			searchCtls.setReturningAttributes(new String[] { "postalAddress" });

			// Specify the search scope
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// Now try a simple search and get some attributes
			// as defined in searchAttributes_
			NamingEnumeration<SearchResult> answer;

			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {

				SearchResult result = answer.next();
				Attributes attributes = result.getAttributes();
				country = attributes.get("postalAddress").toString().split("postalAddress:")[1].replaceAll("\\s+", "");
				System.out.println("COUNTRYYY: " + country);
				setCountry.add(country);
				return setCountry;

			}
			answer.close();

		} catch (NamingException e) {
			e.printStackTrace();
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			setCountry.add("Empty");

			return setCountry;
		}
		setCountry.add("Empty");

		return setCountry;
	}

	/**
	 * Searchs into the LDAP servers the attributes values of a certain user
	 * 
	 * @param searchAttributesList List of attributes names of which we want to read
	 *                             its values
	 * @return a map containing each attribute name and its set of values (could be
	 *         just one)
	 */
	/*
	 * public Map<String, Set<String>> roleInKent(String filter) { String
	 * btSearchbase = "dc=c3isp,dc=eu"; String searchFilter =
	 * "(&(objectclass=groupOfUniqueNames)(uniqueMember=cn=" + filter +
	 * ",ou=SME Pilot,ou=SME,ou=Pilots,dc=c3isp,dc=eu))"; // String searchFilter =
	 * "(uid=user)";
	 * 
	 * String[] searchAttributes_ = new String[] { "cn" };
	 * 
	 * System.out.println("hello");
	 * 
	 * // Create the search controls SearchControls searchCtls = new
	 * SearchControls(); searchCtls.setReturningAttributes(searchAttributes_);
	 * 
	 * // Specify the search scope
	 * searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	 * 
	 * // Now try a simple search and get some attributes // as defined in
	 * searchAttributes_ NamingEnumeration<SearchResult> answer;
	 * 
	 * Map<String, Set<String>> resultMap = new HashMap<>();
	 * 
	 * try { answer = ldapContext.search(btSearchbase, searchFilter, searchCtls);
	 * while (answer.hasMoreElements()) { SearchResult result = answer.next();
	 * Attributes ldapAttributes = result.getAttributes(); NamingEnumeration<?>
	 * ldapEnumeration = ldapAttributes.getAll(); if
	 * (!resultMap.containsKey("role")) { resultMap.put("role", new
	 * HashSet<String>()); }
	 * resultMap.get("role").add(result.getName().split(",")[0] .split("cn=")[1]);
	 * 
	 * while (ldapEnumeration.hasMoreElements()) { Set<String> ldapValueSet = new
	 * HashSet<>(); javax.naming.directory.Attribute ldapAttribute =
	 * (javax.naming.directory.Attribute) ldapEnumeration .next();
	 * 
	 * NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll(); while
	 * (ldapValueEnumeration.hasMoreElements()) { ldapValueSet.add((String)
	 * ldapValueEnumeration.next()); }
	 * 
	 * resultMap.put(ldapAttribute.getID(), ldapValueSet);
	 * ldapValueEnumeration.close(); }
	 * 
	 * ldapEnumeration.close(); }
	 * 
	 * answer.close(); return resultMap;
	 * 
	 * } catch (NamingException e) { e.printStackTrace(); // restituisce la mappa
	 * parziale creata fino al comparsa dell'eccezione return null; } }
	 */

	public Map<String, Set<String>> roleInSubPilot(String filter, String pilotName, String topPilotName) {
		String btSearchbase = "dc=c3isp,dc=eu";
		String searchFilter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=cn=" + filter + ",ou=" + topPilotName
				+ ",ou=" + pilotName + ",ou=Pilots,dc=c3isp,dc=eu))";
		// String searchFilter = "(uid=user)";

		String[] searchAttributes_ = new String[] { "cn" };

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(searchAttributes_);

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		Map<String, Set<String>> resultMap = new HashMap<>();

		try {
			answer = ldapContext.search(btSearchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes ldapAttributes = result.getAttributes();
				NamingEnumeration<?> ldapEnumeration = ldapAttributes.getAll();
				if (!resultMap.containsKey("role")) {
					resultMap.put("role", new HashSet<String>());
				}
				resultMap.get("role").add(result.getName().split(",")[0].split("cn=")[1]);

				while (ldapEnumeration.hasMoreElements()) {
					Set<String> ldapValueSet = new HashSet<>();
					javax.naming.directory.Attribute ldapAttribute = (javax.naming.directory.Attribute) ldapEnumeration
							.next();

					NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll();
					while (ldapValueEnumeration.hasMoreElements()) {
						ldapValueSet.add((String) ldapValueEnumeration.next());
					}

					resultMap.put(ldapAttribute.getID(), ldapValueSet);
					ldapValueEnumeration.close();
				}

				ldapEnumeration.close();
			}

			answer.close();
			return resultMap;

		} catch (NamingException e) {
			e.printStackTrace();
			System.out.println("Exception in Attribute extraction");
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return null;
		}
	}

	/**
	 * Searchs into the LDAP servers the attributes values of a certain user
	 * 
	 * @param searchAttributesList List of attributes names of which we want to read
	 *                             its values
	 * @return a map containing each attribute name and its set of values (could be
	 *         just one)
	 */
	/*
	 * public Set<String> companyInKent(String filter) { String searchFilter =
	 * "(uid=" + filter + ")"; String searchbase =
	 * "ou=SME,ou=Pilots,dc=c3isp,dc=eu";
	 * 
	 * // Create the search controls SearchControls searchCtls = new
	 * SearchControls(); searchCtls.setReturningAttributes(new String[] { "o" });
	 * 
	 * // Specify the search scope
	 * searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	 * 
	 * // Now try a simple search and get some attributes // as defined in
	 * searchAttributes_ NamingEnumeration<SearchResult> answer;
	 * 
	 * String returnedValue = null; Set<String> organization = new HashSet<>(); try
	 * { answer = ldapContext.search(searchbase, searchFilter, searchCtls); while
	 * (answer.hasMoreElements()) { SearchResult result = answer.next(); Attributes
	 * ldapAttributes = result.getAttributes(); NamingEnumeration<?> ldapEnumeration
	 * = ldapAttributes.getAll();
	 * 
	 * while (ldapEnumeration.hasMoreElements()) { Set<String> ldapValueSet = new
	 * HashSet<>(); javax.naming.directory.Attribute ldapAttribute =
	 * (javax.naming.directory.Attribute) ldapEnumeration .next();
	 * 
	 * NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll(); while
	 * (ldapValueEnumeration.hasMoreElements()) { ldapValueSet.add((String)
	 * ldapValueEnumeration.next()); }
	 * 
	 * organization.addAll(ldapValueSet); } } answer.close();
	 * 
	 * return organization;
	 * 
	 * } catch (NamingException e) { e.printStackTrace(); // restituisce la mappa
	 * parziale creata fino al comparsa dell'eccezione return organization; } }
	 */

	public Set<String> companySubpilot(String filter, String pilotName) {
		String searchFilter = "(uid=" + filter + ")";
		String searchbase = "ou=" + pilotName + ",ou=Pilots,dc=c3isp,dc=eu";

		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setReturningAttributes(new String[] { "o" });

		// Specify the search scope
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Now try a simple search and get some attributes
		// as defined in searchAttributes_
		NamingEnumeration<SearchResult> answer;

		Set<String> organization = new HashSet<>();
		try {
			answer = ldapContext.search(searchbase, searchFilter, searchCtls);
			while (answer.hasMoreElements()) {
				SearchResult result = answer.next();
				Attributes ldapAttributes = result.getAttributes();
				NamingEnumeration<?> ldapEnumeration = ldapAttributes.getAll();

				while (ldapEnumeration.hasMoreElements()) {
					Set<String> ldapValueSet = new HashSet<>();
					javax.naming.directory.Attribute ldapAttribute = (javax.naming.directory.Attribute) ldapEnumeration
							.next();

					NamingEnumeration<?> ldapValueEnumeration = ldapAttribute.getAll();
					while (ldapValueEnumeration.hasMoreElements()) {
						ldapValueSet.add((String) ldapValueEnumeration.next());
					}

					organization.addAll(ldapValueSet);
				}
			}
			answer.close();

			return organization;

		} catch (NamingException e) {
			e.printStackTrace();
			System.out.println("Exception in LDAP organization");
			// restituisce la mappa parziale creata fino al comparsa dell'eccezione
			return organization;
		}
	}

	/**
	 * Updates the value of one or more attributes for a certain user
	 * 
	 * @param nameValueMap_ a map containing the attributes names and its value
	 * @throws NamingException
	 */
	public void update(String searchbase, Map<String, String> nameValueMap_) throws NamingException {

		for (Map.Entry<String, String> entry : nameValueMap_.entrySet()) {
			String attributeName = entry.getKey();
			String attributeValue = entry.getValue();

			ModificationItem mod[] = new ModificationItem[1];

			mod[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
					new BasicAttribute(attributeName, attributeValue));

			ldapContext.modifyAttributes(searchbase, mod);
		}

	}
}
