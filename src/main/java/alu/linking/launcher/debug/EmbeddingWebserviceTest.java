package alu.linking.launcher.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;

import alu.linking.disambiguation.scorers.embedhelp.EntitySimilarityService;

public class EmbeddingWebserviceTest {

	public static void main(String[] args) {
		try {
			// sendGet();
			// basicGet();
			entityServiceTest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void entityServiceTest() {
		final EntitySimilarityService similarityService = new EntitySimilarityService();
		System.out.println("Starting...");
		System.out.println(similarityService.similarity("<http://www.wikidata.org/entity/Q59537246>",
				"<http://www.wikidata.org/entity/Q862231>"));
	}

	private static void foo() {
		final String baseURL = "http://localhost:3030/model";
		final String arg1 = "?url1=";
		final String arg2 = "?url2=";

		final String entity1 = "http://www.wikidata.org/entity/Q30074017";
		final String entity2 = "http://www.wikidata.org/entity/Q55684531";

		// API Calls!
		final StringBuilder sbURL = new StringBuilder(baseURL);
		sbURL.append(arg1);
		sbURL.append(entity1);
		sbURL.append("&");
		sbURL.append(arg2);
		sbURL.append(entity2);
		System.out.println("Prepaaaaaare");
		org.jsoup.nodes.Document doc = null;
		try {
			doc = Jsoup.connect(sbURL.toString()).get();
			System.out.println(doc.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static void sendGet() throws Exception {
		final String baseURL = "http://localhost:3030/model";
		final String arg1 = "?url1=";
		final String arg2 = "?url2=";

		final String entity1 = "http://www.wikidata.org/entity/Q30074017";
		final String entity2 = "http://www.wikidata.org/entity/Q55684531";

		// API Calls!
		final StringBuilder sbURL = new StringBuilder(baseURL);
		sbURL.append(arg1);
		sbURL.append(entity1);
		sbURL.append("&");
		sbURL.append(arg2);
		sbURL.append(entity2);

		HttpGet request = new HttpGet(sbURL.toString());

		// add request headers
		request.addHeader("custom-key", "mkyong");
		request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");

		final CloseableHttpClient httpClient = HttpClients.createDefault();
		try (CloseableHttpResponse response = httpClient.execute(request)) {

			// Get HttpResponse Status
			System.out.println(response.getStatusLine().toString());

			HttpEntity entity = response.getEntity();
			Header headers = entity.getContentType();
			System.out.println(headers);

			if (entity != null) {
				// return it as a String
				String result = EntityUtils.toString(entity);
				System.out.println(result);
			}

		}

	}

	private static void basicGet() throws UnsupportedEncodingException, IOException {
		final String baseURL = "http://localhost:3030/model?";
		final String arg1 = "url1=";
		final String arg2 = "url2=";

		final String entity1 = "http://www.wikidata.org/entity/Q64";
		final String entity2 = "http://www.wikidata.org/entity/Q64";

		// API Calls!
		final StringBuilder sbURL = new StringBuilder(baseURL);
		sbURL.append(arg1);
		sbURL.append(entity1);
		sbURL.append("&");
		sbURL.append(arg2);
		sbURL.append(entity2);

		final URL url = new URL(sbURL.toString());
		Number retVal = 0d;
		final StringBuilder sbRet = new StringBuilder();

		try (final InputStream is = url.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
			for (String line; (line = reader.readLine()) != null;) {
				sbRet.append(line);
				// System.out.println(line);
			}
		}
		try {
			retVal = Double.valueOf(sbRet.toString().trim());
			System.out.println("#1 - Retval = " + retVal);
		} catch (NumberFormatException nfe) {
			retVal = 0d;
			System.out.println("ERROR:" + nfe.getMessage());
		}

		System.out.println("#2 - Retval = " + retVal);
	}
}
