package alu.linking.preprocessing.webcrawler;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;

import alu.linking.config.constants.Numbers;
import alu.linking.structure.TextProcessor;

public class CrawlerConnection implements TextProcessor {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

	private final String url;
	private final boolean saveLinks;
	private List<String> foundLinks;
	private String textResult = null;
	private boolean callFinish = false;

	public CrawlerConnection(final String url) {
		this(url, false);
	}

	public CrawlerConnection(final String url, final boolean saveLinks) {
		this.url = url;
		this.saveLinks = saveLinks;
	}

	@Override
	public CrawlerConnection call() throws IOException {
		callFinish = false;
//		 System.out.println(Thread.currentThread().getName() + " - Executing: " +
//		 url);
		try {
			final Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);// .followRedirects(false);
			final Document htmlDocument = connection.get();
			connection.timeout(Numbers.WEBCRAWLER_CONNECTIONS_TIMEOUT_MS.val.intValue());

			if (connection.response().statusCode() == 200) // 200 is the HTTP OK status code
															// indicating that everything is great.
			{
				// System.out.println("Visiting: " + url);
			}
			Response conn_rep = null;
			String conn_type = null;
			if (connection == null || (conn_rep = connection.response()) == null
					|| (conn_type = conn_rep.contentType()) == null || !conn_type.contains("text/html")) {
				callFinish = true;
				return null;
			}

			if (saveLinks) {
				final List<String> links = searchForLinks(htmlDocument);
				this.foundLinks = links;
			}
			final Element htmlBody;
			String ret = "";
			final String htmlTitle;
			if ((htmlDocument != null) && (htmlTitle = htmlDocument.title()) != null) {
				ret += "<title>" + htmlTitle + "</title>\n";
			}
			if (htmlDocument != null && (htmlBody = htmlDocument.body()) != null) {
				ret += htmlBody.text();
				// System.out.println("Found website for: " + url);
			} else if (htmlDocument != null && htmlDocument.body() == null) {
				System.out.println("No body for: " + url);
			} else {
				System.out.println("HTML Doc is null for: " + url);
			}
			this.textResult = ret;
			callFinish = true;
			return this;
		} catch (HttpStatusException httpCodeError) {
			// do nothing about 404 etc
		} catch (UnknownHostException uhe) {
			// DNS
		} catch (SocketException | SocketTimeoutException socketException) {
			// ignore socket exceptions
		} catch (SSLProtocolException sslProtocolException) {
			// SSL protocol exception
		} catch (SSLHandshakeException sslHandshakeException) {
			// handshake exception
		} catch (UnsupportedMimeTypeException mimeException) {
			// Mime exception
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		callFinish = true;
		return null;
//
	}

	private List<String> searchForLinks(Document htmlDocument) {
		final List<String> links = Lists.newArrayList();
		Elements linksOnPage = htmlDocument.select("a[href]");
		for (Element link : linksOnPage) {
			links.add(link.absUrl("href"));
		}
		return links;
	}

	public List<String> getLinks() throws IOException {
		if (!callFinish) {
			call();
		}
		return this.foundLinks;
	}

	@Override
	public String getText() throws IOException {
		if (!callFinish) {
			call();
		}
		return this.textResult;
	}

}
