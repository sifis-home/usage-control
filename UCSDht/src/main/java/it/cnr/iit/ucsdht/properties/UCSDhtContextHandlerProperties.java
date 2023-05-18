package it.cnr.iit.ucsdht.properties;

import java.util.Map;

import it.cnr.iit.ucs.properties.components.ContextHandlerProperties;

public class UCSDhtContextHandlerProperties implements ContextHandlerProperties {

	@Override
	public String getName() {
		return "it.cnr.iit.ucs.contexthandler.ContextHandler";
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
	public String getUri() {
		return "http://localhost:9998";
	}

}
