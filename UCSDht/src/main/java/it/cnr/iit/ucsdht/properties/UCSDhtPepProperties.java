package it.cnr.iit.ucsdht.properties;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.cnr.iit.ucs.properties.components.PepProperties;

public class UCSDhtPepProperties implements PepProperties {

	private String name = "it.cnr.iit.ucsdht.PEPDhtUCSSide";

	// the identifier of the pep
	private String pepId = "pep-default";

	// the topic name this pep is subscribed to
	private String subTopicName = "topic-name-the-pep-is-subscribed-to";

	// the topic uuid this pep is subscribed to
	private String subTopicUuid = "topic-uuid-the-pep-is-subscribed-to";

	private String commandType = "ucs-command";


	@Override
	public String getName() {
		return this.name;
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

	@JsonIgnore
	@Override
	public String getUri() {
		return "http://localhost:9400";
	}

	@JsonIgnore
	@Override
	public String getRevokeType() {
		return "HARD";
	}

	@JsonIgnore
	@Override
	public String getApiOngoingEvaluation() {
		return "onGoingEvaluation";
	}

	@JsonIgnore
	@Override
	public String getApiTryAccessResponse() {
		return "tryAccessResponse";
	}

	@JsonIgnore
	@Override
	public String getApiStartAccessResponse() {
		return "startAccessResponse";
	}

	@JsonIgnore
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
		return this.commandType;
	}
}
