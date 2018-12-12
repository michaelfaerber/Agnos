package alu.linking.executable.preprocessing.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Objects;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.structure.Communicator;
import alu.linking.structure.Executable;
import edu.stanford.nlp.io.StringOutputStream;

public class VirtuosoCommunicator extends Communicator implements Executable {

	public VirtuosoCommunicator() {
		super(Objects.VIRTUOSO_SERVER.protocol, Objects.VIRTUOSO_SERVER.baseURL, Objects.VIRTUOSO_SERVER.urlSuffix);
	}

	/**
	 * <b>WARNING</b>: Not yet fully developed.<br>
	 * Currently server returns http error code 400 (malformed query)<br>
	 * <br>
	 * See {@linkplain https://www.w3.org/TR/rdf-sparql-protocol/#SparqlQuery} <br>
	 * Section 2.2.1.11 for very long SPARQL queries<br>
	 * See {@linkplain vos.openlinksw.com/owiki/wiki/VOS/VOSSparqlProtocol}
	 * 
	 * @param text
	 * @param out
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void post(final String text, final Writer out) throws IOException, URISyntaxException {
		final String http = "http";
		final String baseURL = "km.aifb.kit.edu";
		final String urlSuffix = "/services/crunchbase-sparql";
		final String paramQueryKeyword = "query=";
		final String query = paramQueryKeyword + URLEncoder.encode(text, "UTF-8");
		final URI uri = new URI(http, baseURL, urlSuffix, null, null);
		final URL url = uri.toURL();
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("accept", "text/ntriples");
		conn.setDoOutput(true);

		conn.setInstanceFollowRedirects(false);
		conn.setUseCaches(false);
		// System.out.println(uri.getRawQuery());
		System.out.println(query);
		byte[] postData = query.getBytes(StandardCharsets.UTF_8);
		conn.setRequestProperty("charset", "utf-8");
		int postDataLength = postData.length;
		conn.setRequestProperty("Content-Type", "multipart/form-data");
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));

		try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				BufferedReader brReply = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				BufferedWriter bwOut = new BufferedWriter(out)) {
			// Post the data to the server
			wr.write(postData);
			String line = null;
			while ((line = brReply.readLine()) != null) {
				bwOut.write(line);
				bwOut.newLine();
			}

		}

	}

	/**
	 * Executes query from the input file <br>
	 * <b>Note</b>: This keeps the server output in memory as a string. It is
	 * advisable to use this method only for small expected output.
	 * 
	 * @param inputFile
	 *            file containing query
	 * @return server's output
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String lookup(final File inputFile) throws UnsupportedEncodingException, IOException, URISyntaxException {
		final StringOutputStream ret = new StringOutputStream();
		lookup(new FileInputStream(inputFile), ret);
		return ret.toString();
	}

	/**
	 * Note that this method just does a replace of the input file's path string, so
	 * it can be buggy / not work in some cases.<br>
	 * Only use if you know what you're doing.
	 * 
	 * @param inputFilePath
	 * @param automaticOutput
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String lookup(final EnumModelType KG, final String inputFilePath, final Boolean automaticOutput)
			throws UnsupportedEncodingException, IOException, URISyntaxException {
		if (Boolean.TRUE.equals(automaticOutput)) {
			final String inString = FilePaths.DIR_QUERY_IN.getPath(KG).replace("\\", "/");
			final String outString = FilePaths.DIR_QUERY_OUT.getPath(KG).replace("\\", "/");
			String outputFilePath = inputFilePath.replace(inString, outString);
			if (outputFilePath.equals(inputFilePath)) {
				// Error while replacing
				outputFilePath = inputFilePath.replace("\\", "/").replace(FilePaths.DIR_QUERY_IN.getPath(KG),
						FilePaths.DIR_QUERY_OUT.getPath(KG));
				if (outputFilePath.equals(inputFilePath)) {
					throw new IOException("Error - INPUT and OUTPUT paths are the same: " + outputFilePath);
				}
				// Creates output directories as they might not exist yet
				final File outFile = new File(outputFilePath);
				if (!outFile.exists()) {
					outFile.getParentFile().mkdirs();
				}
			}
			lookup(inputFilePath, outputFilePath);
			return "Output sent to " + outputFilePath;
		} else {
			return lookup(new File(inputFilePath));
		}
	}

	/**
	 * Reads query from inputFile, executes it on the server & outputs to outputFile
	 * 
	 * @param inputFile
	 *            input file
	 * @param outputFile
	 *            output file
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void lookup(final File inputFile, final File outputFile)
			throws UnsupportedEncodingException, IOException, URISyntaxException {
		lookup(new FileInputStream(inputFile), new FileOutputStream(outputFile));
	}

	public void lookup(final String inputPath, final String outputPath)
			throws UnsupportedEncodingException, IOException, URISyntaxException {
		lookup(new File(inputPath), new File(outputPath));
	}

	/**
	 * Executes query from the input stream and outputs server's output to the
	 * output stream<br>
	 * <b>Note</b>: This method is suitable for large outputs from the server-side,
	 * as it instantly writes out. <b>Note</b>: GET requests of size 1900 or greater
	 * are not allowed, therefore POST is called for such queries instead.
	 * 
	 * @param inText
	 * @param outQueryRet
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void lookup(final InputStream inText, final OutputStream outQueryRet)
			throws UnsupportedEncodingException, IOException, URISyntaxException {
		try (final BufferedReader brInText = new BufferedReader(new InputStreamReader(inText));
				final OutputStreamWriter bwOutText = new OutputStreamWriter(outQueryRet)) {
			final StringBuilder sbQuery = new StringBuilder("");
			String line = null;
			// Grab the query - no query should be too long to fit in memory
			while ((line = brInText.readLine()) != null) {
				sbQuery.append(line);
				sbQuery.append(Strings.NEWLINE);
			}
			final String sbQueryStr = sbQuery.toString();
			if (sbQueryStr == null)
				return;

			if (sbQueryStr.length() < 1_900)// 1_900 is set as the max limit for Virtuoso
			{
				get(sbQuery.toString(), bwOutText);
			} else {
				post(sbQueryStr.toString(), bwOutText);
			}
		}
	}

	public void get(final String text, final Writer out) throws UnsupportedEncodingException, IOException {
		try {
			final String paramQueryKeyword = "query=";
			final String query = paramQueryKeyword + text;
			final URI uri = new URI(http, baseURL, urlSuffix, query, null);
			final URL obj = uri.toURL();
			final HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
			conn.setRequestProperty("accept", "text/csv");
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setUseCaches(false);
			// GET
			conn.setRequestMethod("GET");
			try (final BufferedReader brReply = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					final BufferedWriter bwOut = new BufferedWriter(out)) {
				// Post the data to the server
				String line = null;
				while ((line = brReply.readLine()) != null) {
					bwOut.write(line);
					bwOut.newLine();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object exec(Object... o) throws Exception {
		if (o != null) {
			Class[] classes = new Class[o.length];
			for (int i = 0; i < o.length; ++i) {
				classes[i] = o[i].getClass();
			}
			final Method method = VirtuosoCommunicator.class.getDeclaredMethod(getExecMethod(), classes);
			System.out.println("[Method: " + method.getName() + "] Executing");
			Object ret = method.invoke(this, o);
			System.out.println("[Method: " + method.getName() + "] Executed");
			return ret;
		}

		return null;
	}

	@Override
	public boolean destroy() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getExecMethod() {
		return "lookup";
	}

	@Override
	public void get(String queryText) throws UnsupportedEncodingException, IOException {
		get(queryText, new OutputStreamWriter(outStream));
	}

	@Override
	public void post(String queryText) throws IOException, URISyntaxException {
		post(queryText, new OutputStreamWriter(outStream));
	}

	@Override
	public void get(Map<String, String> mapParams) throws UnsupportedEncodingException, IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Communicator params(String[] params) {
		return this;
	}

}
