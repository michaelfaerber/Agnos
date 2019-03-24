package alu.linking.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.NIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.NIFDocumentParser;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.TypedSpan;
import org.aksw.gerbil.transfer.nif.data.TypedNamedEntity;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class DBpediaWebAnnotator {
	public static void main(final String[] args) {
		final String inputPath = "C:\\Users\\Kris\\Desktop\\jar_out\\evaluation\\"
				// + "kore50-nif_angelina.ttl"
				//+ "kore50-nif.ttl"
				+ "kore50-nif_desire_dylan.ttl"
		//
		;
		final File inputFile = new File(inputPath);
		final String dbpediaURL =
				// "http://www.dbpedia-spotlight.org/rest/"
				"http://model.dbpedia-spotlight.org/en/"
		// "http://www.dbpedia-spotlight.org/"
		;

		final TurtleNIFDocumentParser parser = new TurtleNIFDocumentParser();
		final Document document;
		try {
			final InputStream inputStream = new FileInputStream(inputFile);
			document = parser.getDocumentFromNIFStream(inputStream);
			// final List<MeaningSpan> markings = new
			// DBpediaWebAnnotator(dbpediaURL).performD2KBTask(document);
			new SpotlightClient(dbpediaURL, new DBpediaWebAnnotator(dbpediaURL))//
			//.disambiguate(document)
			//.annotateSavely(document)
			.annotate(document)
			;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final String CONNECTION_ABORT_INDICATING_EXCPETION_MSG = "Software caused connection abort";
	protected CloseableHttpClient client;

	private static final Logger LOGGER = Logger.getLogger(DBpediaWebAnnotator.class);

	private static final String DOCUMENT_URI = "http://www.aksw.org/gerbil/NifWebService/request_";

	private String url;
	private int documentCount = 0;
	private NIFDocumentCreator nifCreator = new TurtleNIFDocumentCreator();
	private NIFDocumentParser nifParser = new TurtleNIFDocumentParser();

	private List<Header> additionalHeader = new LinkedList<Header>();

	public DBpediaWebAnnotator(final String dbpediaURL) {
		// Create a client
		this.client = HttpClientBuilder.create().build();
		this.url = dbpediaURL;
	}

	@SuppressWarnings("unchecked")
	protected <T extends Marking> List<T> performAnnotation(Document document, Class<T> resultClass)
			throws RuntimeException {
		document = request(document);
		if (document != null) {
			return document.getMarkings(resultClass);
		} else {
			return Collections.EMPTY_LIST;
		}
	}

	public Document request(Document document) throws RuntimeException {
		// give the document a URI
		document.setDocumentURI(DOCUMENT_URI + documentCount);
		++documentCount;
		LOGGER.info("Started request for " + document.getDocumentURI() + " - "
				+ (document.getText().length() > 20 ? (document.getText().substring(0, 20) + "...")
						: document.getText()));
		// create NIF document
		String nifDocument = nifCreator.getDocumentAsNIFString(document);
		System.out.println(nifDocument);
		HttpEntity entity = new StringEntity(nifDocument, "UTF-8");
		// send NIF document
		HttpPost request = null;
		try {
			request = createPostRequest(url);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Couldn't create HTTP request.", e);
		}
		request.setEntity(entity);
		request.addHeader(HttpHeaders.CONTENT_TYPE, nifCreator.getHttpContentType() + ";charset=UTF-8");
		request.addHeader(HttpHeaders.ACCEPT, nifParser.getHttpContentType());
		request.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
		for (Header header : getAdditionalHeader()) {
			request.addHeader(header);
		}

		entity = null;
		CloseableHttpResponse response = null;
		Document responseDoc = null;
		try {
			response = sendRequest(request, true);
			// receive NIF document
			entity = response.getEntity();
			// read response and parse NIF
			try {
				responseDoc = nifParser.getDocumentFromNIFStream(entity.getContent());
			} catch (Exception e) {
				LOGGER.error("Couldn't parse the response.", e);
				throw new RuntimeException("Couldn't parse the response.", e);
			}
			closeRequest(request);
		} finally {
			if (entity != null) {
				try {
					EntityUtils.consume(entity);
				} catch (IOException e1) {
				}
			}
			IOUtils.closeQuietly(response);
		}
		LOGGER.info("Finished request for " + document.getDocumentURI());
		return responseDoc;
	}

	void closeRequest(HttpPost request) {
		// Used for logging purposes in GERBIL
	}

	public HttpPost createPostRequest(String url) {
		return new HttpPost(url);
	}

	protected CloseableHttpResponse sendRequest(HttpUriRequest request) throws RuntimeException {
		return sendRequest(request, false);
	}

	protected CloseableHttpResponse sendRequest(HttpUriRequest request, boolean retry) throws RuntimeException {
		CloseableHttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (NoHttpResponseException e) {
			if (retry) {
				LOGGER.warn("Got no response from the server (\"{}\"). Retrying...", e);
				return sendRequest(request, false);
			} else {
				LOGGER.error("Got no response from the server.", e);
				throw new RuntimeException("Got no response from the server.", e);
			}
		} catch (RequestAbortedException e) {
			LOGGER.error("It seems like the annotator has needed too much time and has been interrupted.");
			throw new RuntimeException("It seems like the annotator has needed too much time and has been interrupted.",
					e);
		} catch (java.net.SocketException e) {
			if (e.getMessage().contains(CONNECTION_ABORT_INDICATING_EXCPETION_MSG)) {
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
			try {
				response.close();
			} catch (IOException e) {
			}
			throw new RuntimeException("Response has the wrong status: " + status.toString());
		}
		return response;
	}

	/**
	 * @return the additionalHeader
	 */
	public List<Header> getAdditionalHeader() {
		return additionalHeader;
	}

	public List<Meaning> performC2KB(Document document) throws RuntimeException {
		return performAnnotation(document, Meaning.class);
	}

	public List<MeaningSpan> performD2KBTask(Document document) throws RuntimeException {
		return performAnnotation(document, MeaningSpan.class);
	}

	public List<Span> performRecognition(Document document) throws RuntimeException {
		return performAnnotation(document, Span.class);
	}

	public List<MeaningSpan> performA2KBTask(Document document) throws RuntimeException {
		return performAnnotation(document, MeaningSpan.class);
	}

	public List<TypedSpan> performTyping(Document document) throws RuntimeException {
		return performAnnotation(document, TypedSpan.class);
	}

	public List<TypedNamedEntity> performTask1(Document document) throws RuntimeException {
		return performAnnotation(document, TypedNamedEntity.class);
	}

	public List<TypedNamedEntity> performTask2(Document document) throws RuntimeException {
		return performAnnotation(document, TypedNamedEntity.class);
	}

	public List<TypedSpan> performRT2KBTask(Document document) throws RuntimeException {
		return performAnnotation(document, TypedSpan.class);
	}

	public CloseableHttpClient getClient() {
		return this.client;
	}

}
