package it.cnr.iit.ucsdht.properties;

import java.util.Map;

import it.cnr.iit.ucs.properties.components.PepProperties;

public class UCSDhtPepProperties implements PepProperties {

	// the identifier of the pep
	String pepId = "pep-default";

	// the topic name this pep is subscribed to
	String subTopicName = "topic-name-the-pep-is-subscribed-to";

	// the topic uuid this pep is subscribed to
	String subTopicUuid = "topic-uuid-the-pep-is-subscribed-to";

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
		return this.pepId;
	}

	public void setId(String pepId) {
		this.pepId = pepId;
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
	public String getSubTopicName() {
		return this.subTopicName;
	}

	@Override
	public String getSubTopicUuid() {
		return this.subTopicUuid;
	}

	public void setSubTopicName(String subTopicName) {
		this.subTopicName = subTopicName;
	}

	public void setSubTopicUuid(String subTopicUuid) {
		this.subTopicUuid = subTopicUuid;
	}

	@Override
	public String getCommandType() {
		return "ucs-command";
	}
}
