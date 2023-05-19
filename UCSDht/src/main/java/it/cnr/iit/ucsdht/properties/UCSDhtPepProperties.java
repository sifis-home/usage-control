package it.cnr.iit.ucsdht.properties;

import java.util.Map;

import it.cnr.iit.ucs.properties.components.PepProperties;

public class UCSDhtPepProperties implements PepProperties {

	@Override
	public String getName() {
		return "it.cnr.iit.ucsdht.PEPDhtUCSSide";
	}

	@Override
	public Map<String, String> getAdditionalProperties() {
		return null;
	}

	@Override
	public String getId() {
		return "pep-0";
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

	@Override
	public String getPubTopicName() {
		return "topic-name";
	}

	@Override
	public String getPubTopicUuid() {
		return "topic-uuid-the-pep-is-subscribed-to";
	}

	@Override
	public String getCommandType() {
		return "ucs-command";
	}

	//todo: implement the setter
}
