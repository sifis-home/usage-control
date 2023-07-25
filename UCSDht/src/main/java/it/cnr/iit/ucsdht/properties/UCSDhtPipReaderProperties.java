package it.cnr.iit.ucsdht.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.cnr.iit.ucs.properties.components.PipProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UCSDhtPipReaderProperties implements PipProperties {

	private String id;
	private String name = "it.cnr.iit.ucs.pipreader.PIPReader";
	private String journalProtocol = "file";
	private String journalPath = "/tmp/ucf";
	List<Map<String, String>> attributes = new ArrayList<>();
	private long refreshRate;

	private Map<String, String> additionalProperties = new HashMap<>();

	@Override
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String getJournalProtocol() {
		return this.journalProtocol;
	}

	public void setJournalProtocol(String journalProtocol) {
		this.journalProtocol = journalProtocol;
	}

	@Override
	public String getJournalPath() {
		return this.journalPath;
	}

	public void setJournalPath(String journalPath) {
		this.journalPath = journalPath;
	}

	@Override
	public List<Map<String, String>> getAttributes() {
		return new ArrayList<>(attributes);
	}

	public void addAttribute(String id, String category, String dataType, String filePath) {
		Map<String,String> attributeMap = new HashMap<>();
		attributeMap.put("ATTRIBUTE_ID", id);
		attributeMap.put("CATEGORY", category);
		attributeMap.put("EXPECTED_CATEGORY", category);
		attributeMap.put("DATA_TYPE", dataType);
		attributeMap.put("FILE_PATH", filePath);

		this.attributes.add(attributeMap);
	}

	public void addAttribute(String id, String category, String dataType, String filePath, String expectedCategory) {
		Map<String,String> attributeMap = new HashMap<>();
		attributeMap.put("ATTRIBUTE_ID", id);
		attributeMap.put("CATEGORY", category);
		attributeMap.put("EXPECTED_CATEGORY", expectedCategory);
		attributeMap.put("DATA_TYPE", dataType);
		attributeMap.put("FILE_PATH", filePath);

		this.attributes.add(attributeMap);
	}

	@Override
	public long getRefreshRate() {
		return refreshRate;
	}

	public void setRefreshRate(long refreshRate) {
		this.refreshRate = refreshRate;
	}

	@Override
	@JsonIgnore
	public boolean isMultiAttribute() {
		return this.attributes.size() > 1;
	}

	@Override
	@JsonIgnore
	public Map<String, String> getJournalAdditionalProperties() {
		return null;
	}

	@Override
	public Map<String, String> getAdditionalProperties() {
		return additionalProperties;
	}

	public void setAdditionalProperties(Map<String, String> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}
}
