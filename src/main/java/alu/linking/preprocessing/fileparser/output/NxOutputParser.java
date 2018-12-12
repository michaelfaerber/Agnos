package alu.linking.preprocessing.fileparser.output;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.namespace.XSD;
import org.semanticweb.yars.nx.util.NxUtil;

import alu.linking.config.constants.Strings;

public class NxOutputParser implements OutParser {
	private boolean isHTML = false;

	public static String format(Node[] line) {
		return format(line[0], line[1], line[2]);
	};

	@Override
	public String format(final String inputSubject, final String inputPredicate, final String inputObject,
			final boolean removeEmptyObjectLiterals) {
		final Node subj;
		String subject = inputSubject, predicate = inputPredicate, object = inputObject;
		final String initObj = object;
		// subject = NxUtil.escapeForMarkup(OutParser.removeIllegalChars(subject, ""));
		subject = NxUtil.escapeForMarkup(subject);
		// predicate = NxUtil.escapeForMarkup(OutParser.removeIllegalChars(predicate));
		predicate = NxUtil.escapeForMarkup(predicate);
		object = NxUtil.escapeForMarkup(OutParser.removeIllegalChars(object, " ", isHTML));
		if (subject.startsWith("http://")) {
			subj = new Resource(subject, false);
		} else {
			// subj = new BNode(Strings.RDF_BLANK_NODE_PREFIX.val + subject, true);
			subj = new Resource(Strings.RDF_BLANK_NODE_PREFIX.val + subject, false);
			// subj = new BNode(subject, true);
		}

		if (removeEmptyObjectLiterals && (object == null || object.length() == 0)) {
			return null;
		}

		return format(subj, new Resource(predicate), new Literal(object, XSD.STRING));
	}

	public static String format(Node subject, Node predicate, Node object) {
		return subject.toN3() + " " + predicate.toN3() + " " + object.toN3() + " .";
	}

	@Override
	public String format(String subject, String predicate, String object) {
		return format(subject, predicate, object, true);
	}

	@Override
	public void setHTML(boolean isHTML) {
		this.isHTML = isHTML;
	}
}
