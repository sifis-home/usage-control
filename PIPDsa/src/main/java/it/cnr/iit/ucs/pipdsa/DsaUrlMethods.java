package it.cnr.iit.ucs.pipdsa;

public final class DsaUrlMethods {

	static private String dsaUrl;
	static private String dsaStatus;
	static private String dsaVersion;

	private DsaUrlMethods() {
	}

	static public String getDsaUrl() {
		return dsaUrl;
	}

	static public void setDsaUrl(String url) {
		dsaUrl = url;
	}

	static public String getDsaStatus() {
		return dsaStatus;
	}

	static public void setDsaStatus(String status) {
		dsaStatus = status;
	}

	static public String getDsaVersion() {
		return dsaVersion;
	}

	static public void setDsaVersion(String version) {
		dsaVersion = version;
	}

}
