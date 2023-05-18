package it.cnr.iit.ucsdht.properties;

import java.util.Map;

import it.cnr.iit.ucs.properties.components.PepProperties;

public class UCSDhtPepProperties implements PepProperties {

	@Override
	public String getName() {
		return "it.cnr.iit.pepdht.PEPDht";
	}

	@Override
	public Map<String, String> getAdditionalProperties() {
		return null;
	}

	@Override
	public String getId() {
		return "0";
	}

	@Override
	public String getUri() {
		return "http://localhost:9400";
	}

	@Override
	public String getRevokeType() {
		return "HARD";
	}

	@Override
	public String getApiOngoingEvaluation() {
		return "onGoingEvaluation";
	}

	@Override
	public String getApiTryAccessResponse() {
		return "tryAccessResponse";
	}

	@Override
	public String getApiStartAccessResponse() {
		return "startAccessResponse";
	}

	@Override
	public String getApiEndAccessResponse() {
		return "endAccessResponse";
	}

}
