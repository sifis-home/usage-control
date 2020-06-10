package it.cnr.iit.ucs.pipldap.statics;

public final class LdapAuthorization {

	static private String host;
	static private String port;
	static private String bnddn;
	static private String password;

	private LdapAuthorization() {
	}

	public static String getHost() {
		return host;
	}

	public static void setHost(String host) {
		LdapAuthorization.host = host;
	}

	public static String getBnddn() {
		return bnddn;
	}

	public static void setBnddn(String bnddn) {
		LdapAuthorization.bnddn = bnddn;
	}

	public static String getPassword() {
		return password;
	}

	public static void setPassword(String password) {
		LdapAuthorization.password = password;
	}

	public static String getPort() {
		return port;
	}

	public static void setPort(String port) {
		LdapAuthorization.port = port;
	}

}
