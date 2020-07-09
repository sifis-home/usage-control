package it.cnr.iit.ucs.pipldap.statics;

import java.util.Map;

public final class CachedResult {

	public static Map<String, String> attributesToValues;
	public static boolean cached = false;

	private CachedResult() {
	}

	public static Map<String, String> getAttributesToValues() {
		return attributesToValues;
	}

	public static void setAttributesToValues(Map<String, String> attributesToValues) {
		CachedResult.attributesToValues = attributesToValues;
	}

	public static boolean isCached() {
		return cached;
	}

	public static void setCached(boolean cached) {
		CachedResult.cached = cached;
	}

}
