package alu.linking.preprocessing.webcrawler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

public class Crawler {
	private Set<String> pagesVisited = new HashSet<String>();
	private List<String> pagesToVisit = new LinkedList<String>();
	private List<String> foundLinks = Lists.newArrayList();

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

	private String nextUrl() {
		String nextUrl;
		do {
			nextUrl = this.pagesToVisit.remove(0);
		} while (this.pagesVisited.contains(nextUrl));
		this.pagesVisited.add(nextUrl);
		return nextUrl;
	}

	/**
	 * Simply visits website and grabs the text.
	 * 
	 * @param url
	 *            website to visit
	 * @return text content of website, NULL if any error occurs
	 */
	public String crawlSimple(final String url) {
		return crawlSimple(url, false);
	}

	/**
	 * Simply visits website and grabs the text and saves links if saveLinks flag is
	 * set to TRUE.
	 * 
	 * @param url
	 *            page to visit
	 * @param saveLinks
	 *            whether to save links
	 * @return
	 * @throws Exception
	 */
	public String crawlSimple(final String url, final boolean saveLinks) {
		return new CrawlerConnection(url, saveLinks).call().getText();
	}

	public CrawlerConnection createConnection(final String url, final boolean saveLinks) {
		return new CrawlerConnection(url, saveLinks);
	}

	public CrawlerConnection createConnection(final String url) {
		return new CrawlerConnection(url);
	}

	public List<String> getSavedLinks() {
		return this.foundLinks;
	}
}
