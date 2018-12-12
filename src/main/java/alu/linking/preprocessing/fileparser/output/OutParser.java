package alu.linking.preprocessing.fileparser.output;

import org.jsoup.Jsoup;

public interface OutParser {
	public String format(String subject, String predicate, String object);

	public void setHTML(final boolean isHTML);

	public static String removeIllegalChars(final String spo, final String replacement, final boolean isHTML) {
		// String ret = spo.replace("\\", "\\\\");
		// ret = ret.replace("\"", "");
		// return ret;
		String ret = spo;
		// Remove all tags
		// Remove all newlines...
		if (isHTML) {
			ret = Jsoup.parse(ret).text();
		}
		ret = ret.replaceAll("\n", " ");
		if (isHTML) {
			ret = ret.replaceAll("\\p{Punct}", " ");
		}
		ret = ret.replace("<", " ").replace(">", " ").replaceAll("  ", replacement);
		return ret;
	}

	public String format(String subject, String predicate, String object, boolean removeEmptyObjectLiterals);
}
