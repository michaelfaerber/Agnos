package alu.linking.preprocessing.dbpedia.spotlight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class SpotlightCommunicator {

	public static String lookup(final String text, final double conf) throws UnsupportedEncodingException, IOException, URISyntaxException {

		final String https = "https";
		final String baseURL = "api.dbpedia-spotlight.org";
		final String urlSuffix = "/en/annotate";
		final String textKeyword = "text=";
		// final String text = "<text>";
		final String confidenceKeyword = "confidence=";
		final String confidence = Double.toString(conf);
		final String query = textKeyword + text + "&" + confidenceKeyword + confidence;
		final URI uri = new URI(https, baseURL, urlSuffix, query, null);
		final URL obj = uri.toURL();
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		conn.setRequestProperty("accept", "application/json");
		conn.setDoOutput(true);
		conn.setRequestMethod("GET");

		final InputStreamReader is = new InputStreamReader(conn.getInputStream());
		try (final BufferedReader br = new BufferedReader(is)) {
			String line = null;
			String ret = "";
			while ((line = br.readLine()) != null) {
				ret += line;
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
