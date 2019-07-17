package alu.linking.api.debug.dbpedia;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.SpanImpl;
import org.aksw.gerbil.transfer.nif.data.TypedNamedEntity;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.atlas.json.io.parser.JSONParser;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Client for sending annotation requests to DBpedia
 * @author Kristian Noullet
 *
 */
public class SpotlightClient {

	private static final Logger LOGGER = Logger.getLogger(SpotlightClient.class);

	private static final String TYPE_PREFIX_URI_MAPPING[][] = new String[][] {
			{ "freebase", "http://rdf.freebase.com/ns/" }, { "dbpedia", "http://dbpedia.org/ontology/" } };

	private static final String DEFAULT_REQUEST_URL = "http://spotlight.dbpedia.org:80/rest";
	// private static final double DEFAULT_MIN_CONFIDENCE = -1;
	// private static final int DEFAULT_MIN_SUPPORT = -1;

	private static final String ANNOTATE_RESOURCE = "annotate";
	private static final String SPOT_RESOURCE = "spot";
	private static final String DISAMBIGUATE_RESOURCE = "disambiguate";

	private String serviceURL;
	// private double minConfidence = 0.2;
	// private int minSupport = 20;
	private Map<String, String> typePrefixToUriMapping;
	private DBpediaWebAnnotator annotator;

	public SpotlightClient(DBpediaWebAnnotator annotator) {
		this(DEFAULT_REQUEST_URL, annotator);
	}

	public SpotlightClient(String serviceURL, DBpediaWebAnnotator annotator) {
		this.serviceURL = serviceURL.endsWith("/") ? serviceURL : (serviceURL + "/");
		this.annotator = annotator;
		typePrefixToUriMapping = new HashMap<String, String>();
		for (int i = 0; i < TYPE_PREFIX_URI_MAPPING.length; ++i) {
			typePrefixToUriMapping.put(TYPE_PREFIX_URI_MAPPING[i][0], TYPE_PREFIX_URI_MAPPING[i][1]);
		}
	}

	protected String request(String inputText, String requestUrl) throws RuntimeException, IOException {
		String parameters;
		try {
			parameters = "text=" + URLEncoder.encode(inputText, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Exception while encoding request data.", e);
			throw new RuntimeException("Exception while encoding request data.", e);
		}
		HttpPost request = null;
		try {
			request = annotator.createPostRequest(requestUrl);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Couldn't create HTTP request.", e);
		}
		HttpEntity entity = new StringEntity(parameters, "UTF-8");
		request.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
		request.addHeader(HttpHeaders.ACCEPT, "application/json");
		request.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
		request.setEntity(entity);
		entity = null;
		CloseableHttpResponse response = null;
		InputStream is = null;
		try {
			try {
				response = annotator.getClient().execute(request);
			} catch (java.net.SocketException e) {
				if (e.getMessage().contains(DBpediaWebAnnotator.CONNECTION_ABORT_INDICATING_EXCPETION_MSG)) {
					LOGGER.error("It seems like the annotator has needed too much time and has been interrupted.");
					throw new RuntimeException(
							"It seems like the annotator has needed too much time and has been interrupted.", e);
				} else {
					LOGGER.error("Exception while sending request.", e);
					throw new RuntimeException("Exception while sending request.", e);
				}
			} catch (Exception e) {
				LOGGER.error("Exception while sending request.", e);
				throw new RuntimeException("Exception while sending request.", e);
			}
			StatusLine status = response.getStatusLine();
			if ((status.getStatusCode() < 200) || (status.getStatusCode() >= 300)) {
				LOGGER.error("Response has the wrong status: " + status.toString());
				LOGGER.error("Response was: " + IOUtils.toString(response.getEntity().getContent()));
				throw new RuntimeException("Response has the wrong status: " + status.toString());
			}
			entity = response.getEntity();
			try {
				return IOUtils.toString(entity.getContent(), "UTF-8");
			} catch (Exception e) {
				LOGGER.error("Couldn't parse the response.", e);
				throw new RuntimeException("Couldn't parse the response.", e);
			}
		} finally {
			IOUtils.closeQuietly(is);
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (IOException e1) {
				}
			}
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
				}
			}
			annotator.closeRequest(request);
		}
	}

	public List<TypedNamedEntity> annotateSavely(Document document) throws IOException {
		try {
			return annotate(document);
		} catch (RuntimeException e) {
			LOGGER.error("Error while requesting DBpedia Spotlight to annotate text. Returning null.", e);
			return null;
		}
	}

	public List<TypedNamedEntity> annotate(Document document) throws RuntimeException, IOException {
		String response = request(document.getText(), serviceURL + ANNOTATE_RESOURCE);
		return parseAnnotationResponse(response);
	}

	protected List<TypedNamedEntity> parseAnnotationResponse(String response) {
		List<TypedNamedEntity> markings = new ArrayList<TypedNamedEntity>();

		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) new JSONObject(response);
			LOGGER.info("DBpedia Spotlight response: " + jsonObject);
		} catch (JSONException jsonEx) {
			LOGGER.error("Could not parse response - DBpedia Spotlight response: " + response);
			return null;
		}

		JSONArray resources = (JSONArray) jsonObject.get("Resources");
		JSONObject resource;
		int start;
		int length;
		String uri = null;
		Set<String> types;
		String typeStrings[], uriParts[];
		if (resources != null) {
			for (Object res : resources) {
				resource = (JSONObject) res;
				start = Integer.parseInt((String) resource.get("@offset"));
				length = ((String) resource.get("@surfaceForm")).length();
				try {
					uri = URLDecoder.decode((String) resource.get("@URI"), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					LOGGER.error("Error while parsing DBpedia Spotlight response. Returning null.", e);
					return null;
				}
				// create Types set
				typeStrings = ((String) resource.get("@types")).split(",");
				types = new HashSet<String>(typeStrings.length);
				for (int i = 0; i < typeStrings.length; ++i) {
					uriParts = typeStrings[i].split(":");
					uriParts[0] = uriParts[0].toLowerCase();
					if (typePrefixToUriMapping.containsKey(uriParts[0])) {
						types.add(typePrefixToUriMapping.get(uriParts[0]) + uriParts[1]);
					} else {
						types.add(typeStrings[i]);
					}
				}
				markings.add(new TypedNamedEntity(start, length, uri, types));
			}
		}

		return markings;
	}

	public List<Span> spotSavely(Document document) throws IOException {
		try {
			return spot(document);
		} catch (RuntimeException e) {
			LOGGER.error("Error while requesting DBpedia Spotlight to spot text. Returning null.", e);
			return null;
		}
	}

	public List<Span> spot(Document document) throws RuntimeException, IOException {
		String response = request(document.getText(), serviceURL + SPOT_RESOURCE);
		return parseSpottingResponse(response);
	}

	protected List<Span> parseSpottingResponse(String response) {
		List<Span> markings = new ArrayList<Span>();

		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) new JSONObject(response);
			LOGGER.info("DBpedia Spotlight JSON response: " + jsonObject);
		} catch (JSONException jsonEx) {
			LOGGER.error("Could not parse response - DBpedia Spotlight response: " + response);
			return null;
		}

		jsonObject = (JSONObject) jsonObject.get("annotation");
		// If there are surface forms
		if (jsonObject.keySet().contains("surfaceForm")) {
			Object surfaceFormContainer = jsonObject.get("surfaceForm");
			// If there is an array of surface forms
			if (surfaceFormContainer instanceof JSONArray) {
				JSONArray resources = (JSONArray) jsonObject.get("surfaceForm");
				JSONObject resource;
				if (resources != null) {
					for (Object res : resources) {
						resource = (JSONObject) res;
						addSpan(resource, markings);
					}
				}
			} else {
				// If there is only one surface form
				addSpan((JSONObject) surfaceFormContainer, markings);
			}
		}

		return markings;
	}

	protected static void addSpan(JSONObject resource, List<Span> markings) {
		int start = Integer.parseInt((String) resource.get("@offset"));
		int length = ((String) resource.get("@name")).length();
		markings.add(new SpanImpl(start, length));
	}

	public List<TypedNamedEntity> disambiguate(Document document) throws RuntimeException, IOException {
		String text = document.getText();
		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><annotation text=\"");
		try {
			requestBuilder.append(URLEncoder.encode(document.getText(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Exception while encoding request data.", e);
			throw new RuntimeException("Exception while encoding request data.", e);
		}
		requestBuilder.append("\">");

		List<Span> spans = document.getMarkings(Span.class);
		int start;
		for (Span span : spans) {
			start = span.getStartPosition();
			requestBuilder.append("<surfaceForm name=\"");
			try {
				requestBuilder.append(URLEncoder.encode(text.substring(start, start + span.getLength()), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Exception while encoding request data.", e);
				throw new RuntimeException("Exception while encoding request data.", e);
			}
			requestBuilder.append("\" offset=\"");
			requestBuilder.append(start);
			requestBuilder.append("\" />");
		}
		requestBuilder.append("</annotation>");

		String response = request(requestBuilder.toString(), serviceURL + DISAMBIGUATE_RESOURCE);
		return parseAnnotationResponse(response);
	}

	public List<TypedNamedEntity> disambiguateSavely(Document document) throws IOException {
		try {
			return disambiguate(document);
		} catch (RuntimeException e) {
			LOGGER.error("Error while requesting DBpedia Spotlight to spot text. Returning null.", e);
			return null;
		}
	}
}