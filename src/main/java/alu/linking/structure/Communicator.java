package alu.linking.structure;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Map;

public abstract class Communicator {
	protected final String http;
	protected final String baseURL;
	protected final String urlSuffix;
	protected OutputStream outStream = System.out;

	public Communicator(final String protocol, final String baseURL, final String urlSuffix) {
		this.http = protocol;
		this.baseURL = baseURL;
		this.urlSuffix = urlSuffix;
	}

	public abstract void get(final Map<String, String> mapParams) throws UnsupportedEncodingException, IOException, ConnectException, URISyntaxException;

	public abstract void post(final String queryText)
			throws UnsupportedEncodingException, IOException, URISyntaxException;

	public Communicator setOutputMethod(final OutputStream outStream) {
		this.outStream = outStream;
		return this;
	}

	public abstract Communicator params(final String[] params);

	public void get(String queryText) throws UnsupportedEncodingException, IOException {
		// TODO Auto-generated method stub

	}

}
