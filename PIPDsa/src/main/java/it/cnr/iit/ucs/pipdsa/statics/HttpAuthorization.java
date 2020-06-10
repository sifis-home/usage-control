package it.cnr.iit.ucs.pipdsa.statics;

import java.util.Base64;

public final class HttpAuthorization {

	static private String restUsername;
	static private String restPassword;

	private HttpAuthorization() {
	}

	static public String base64() {
		String credentials = restUsername + ":" + restPassword;
		return Base64.getEncoder().encodeToString(credentials.getBytes());
	}

	static public String getRestUsername() {
		return restUsername;
	}

	static public void setRestUsername(String username) {
		restUsername = username;
	}

	static public String getRestPassword() {
		return restPassword;
	}

	static public void setRestPassword(String password) {
		restPassword = password;
	}

}
