package alu.linking.preprocessing.webcrawler;

import java.io.IOException;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
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
	public CrawlerConnection call() {
		callFinish = false;
		try {
			// System.out.println(Thread.currentThread().getName() + " - Executing: " +
			// url);
			final Connection connection = Jsoup.connect(url).userAgent(USER_AGENT).followRedirects(false);
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
			} else if (htmlDocument != null && htmlDocument.body() == null) {
				System.out.println("No body for: " + url);
			} else {
				System.out.println("HTML Doc is null for: " + url);
			}
			this.textResult = ret;
			callFinish = true;
			return this;
		} catch (IOException ioe) {
		}
		callFinish = true;
		return null;

	}

	private List<String> searchForLinks(Document htmlDocument) {
		final List<String> links = Lists.newArrayList();
		Elements linksOnPage = htmlDocument.select("a[href]");
		for (Element link : linksOnPage) {
			links.add(link.absUrl("href"));
		}
		return links;
	}

	@Override
	public String getText() {
		if (!callFinish) {
			call();
		}
		return this.textResult;
	}

}
