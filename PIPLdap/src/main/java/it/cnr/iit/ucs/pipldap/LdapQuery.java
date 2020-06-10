package it.cnr.iit.ucs.pipldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import it.cnr.iit.ucs.pipldap.statics.LdapAuthorization;
import it.cnr.iit.utility.errorhandling.Reject;

public final class LdapQuery {

	private static String url;
	private static Integer port;
	private static String password;
	private static String bnddn;

	private static String dc = new String();
	private static String cn = new String();
	private static String ou = new String();

	private static LdapConnection connection;

	private static final String NOT_FOUND = "not found";

	private static Logger log = Logger.getLogger(LdapQuery.class.getName());

	private LdapQuery() {
	}

	static public void init() {
		url = LdapAuthorization.getHost();
		port = Integer.parseInt(LdapAuthorization.getPort());
		password = LdapAuthorization.getPassword();
		bnddn = LdapAuthorization.getBnddn();

		setDc(bnddn);
		setCn(bnddn);
		setOu(bnddn);

		connection = new LdapNetworkConnection(url, port);
		try {
			connection.bind(bnddn, password);
		} catch (LdapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static Map<String, Map<String, String>> queryForAll(String filter, String... attributes) {
		Reject.ifBlank(filter);

		Map<String, Map<String, String>> userAttrs = new HashMap<>();
		List<String> attrsList = Arrays.asList(attributes);
		attrsList = attrsList.stream().filter(el -> !el.equals("uid")).collect(Collectors.toList());

		try {

			SearchRequest req = new SearchRequestImpl();
			req.setScope(SearchScope.ONELEVEL);
			req.addAttributes(attributes);
			req.setTimeLimit(0);
			req.setBase(new Dn(filter));
			req.setFilter("(objectClass=*)");

			// Process the request
			SearchCursor searchCursor = connection.search(req);

			while (searchCursor.next()) {
				Response response = searchCursor.get();

				// process the SearchResultEntry
				if (response instanceof SearchResultEntry) {
					Entry resultEntry = ((SearchResultEntry) response).getEntry();

					if (resultEntry.containsAttribute(attributes)) {

						String uid = resultEntry.getAttributes().stream().filter(el -> el.getId().contains("uid"))
								.map(el -> el.get().getString()).findFirst().orElse(NOT_FOUND);

						if (uid.equals(NOT_FOUND)) {
							continue;
						}

						Map<String, String> attrAndValue = new HashMap<>();
						for (String attr : attrsList) {
							String value;
							value = resultEntry.getAttributes().stream().filter(el -> el.getId().contains(attr))
									.map(el -> el.get().getString()).findFirst().orElse(NOT_FOUND);
							attrAndValue.put(attr, value);
						}
						userAttrs.put(uid, attrAndValue);

					}
				}
			}

			searchCursor.close();
			connection.close();

//			userAttrs.entrySet().stream().forEach(
//					entry -> entry.getValue().entrySet().stream().forEach(el -> log.severe("attribute for user "
//							+ entry.getKey() + ": key -> " + el.getKey() + ", value -> " + el.getValue())));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return userAttrs;
	}

	private static String setDc(String searchString) {
		Reject.ifBlank(searchString);

		List<String> dcElements = Arrays.asList(searchString.split("dc"));
		String dc = "dc" + dcElements.stream().filter(el -> !el.startsWith("ou=") && !el.startsWith("cn="))
				.collect(Collectors.joining("dc"));
		log.severe("setDc() -> " + dc);
		return dc;
	}

	private static String setCn(String searchString) {
		Reject.ifBlank(searchString);

		if (!searchString.contains("cn=")) {
			return "";
		}
		String cn = "cn" + searchString.split("cn")[1].split(",")[0];
		log.severe("setCn() -> " + cn);
		return cn;
	}

	private static String setOu(String searchString) {
		Reject.ifBlank(searchString);
		String ou = searchString.split(cn + ",")[1].split("," + dc)[0];
		log.severe("setOu() -> " + ou);
		return ou;
	}

}
