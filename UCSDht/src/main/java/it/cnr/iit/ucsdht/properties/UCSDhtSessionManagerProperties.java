package it.cnr.iit.ucsdht.properties;

import java.util.Map;

import it.cnr.iit.ucs.properties.components.SessionManagerProperties;

public class UCSDhtSessionManagerProperties implements SessionManagerProperties {

	private String dbUri = "jdbc:sqlite:file::memory:?cache=shared";

	@Override
	public String getName() {
		return "it.cnr.iit.ucs.sessionmanager.SessionManager";
	}

	@Override
	public Map<String, String> getAdditionalProperties() {
		return null;
	}

	@Override
	public String getId() {
		return "1";
	}

	@Override
	public String getDbUri() {
		return dbUri;
	}

	public void setDbUri(String dbUri) {
		this.dbUri = dbUri;
	}
}