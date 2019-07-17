package alu.linking.utils;

public class CBURLTransformer {
	public static String toCustomKG(final String line) {
		String comparableLine = line;
		comparableLine = comparableLine.replace("https://www.crunchbase.com/", "http://linked-crunchbase.org/api/");
		comparableLine = comparableLine.replace("crunchbase.com/", "linked-crunchbase.org/api/");
		comparableLine = comparableLine.replace("organization", "organizations");
		comparableLine = comparableLine.replace("person", "people");
		comparableLine = comparableLine.replace("acquisition", "acquisitions");
		comparableLine += "#id";
		return comparableLine;
	}

	public static String fromCustomKG(final String line) {
		String comparableLine = line;
		comparableLine = comparableLine.replace("http://linked-crunchbase.org/api/", "https://www.crunchbase.com/");
		comparableLine = comparableLine.replace("organizations", "organization");
		comparableLine = comparableLine.replace("people", "person");
		comparableLine = comparableLine.replace("acquisitions", "acquisition");
		if (comparableLine.endsWith("#id")) {
			comparableLine = comparableLine.substring(0, comparableLine.length() - 3);
		}
		return comparableLine;
	}

	/**
	 * Checks if passed URL is a URL from the original CB website
	 * 
	 * @param url
	 * @return
	 */
	public static boolean isCrunchbaseURL(final String url) {
		String transformed = toCustomKG(url);
		final String ending = "#id";
		// Same url -> not a crunchbase url
		// Aka. changes -> it's a crunchbase url
		if (transformed.endsWith(ending)) {
			return !url.equals(transformed.substring(0, transformed.length() - ending.length()));
		} else {
			return !url.equals(transformed);
		}
	}
}
