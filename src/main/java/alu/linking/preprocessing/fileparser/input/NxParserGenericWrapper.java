package alu.linking.preprocessing.fileparser.input;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class NxParserGenericWrapper implements FileInParser<Node> {
	private NxParser parser = null;
	private List<Node> header = null;

	@Override
	public void close() {
		//No need to close NxParser
	}

	@Override
	public List<Node> getNext() {
		return parser.hasNext() ? Arrays.asList(parser.next()) : null;
	}

	@Override
	public FileInParser<Node> create(Reader reader, Object... params) {
		//Ignores parameters
		this.parser = new NxParser(reader);
		return this;
	}

	@Override
	public String toFormatString(Object... words) {
		String ret;
		if (words[0] instanceof Node) {
			ret = toFormatString(((Node) words[0]));
		} else {
			ret = words[0].toString();
		}
		for (int i = 1; i < words.length; ++i) {
			String word;
			if (words[i] instanceof Node) {
				word = toFormatString((Node) words[i]);
			} else {
				word = words[i].toString();
			}
			ret += "_" + word;
		}
		return ret;
	}

	@Override
	public List<Node> withHeader(boolean hasHeader) {
		if (hasHeader) {
			this.header = getNext();
		}
		return this.header;
	}

	@Override
	public String toFormatString(Node word) {
		return word.toN3();
	}
}
