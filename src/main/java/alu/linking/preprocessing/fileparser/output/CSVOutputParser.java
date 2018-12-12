package alu.linking.preprocessing.fileparser.output;

import alu.linking.config.constants.Strings;

public class CSVOutputParser implements OutParser {

	@Override
	public String format(String subject, String predicate, String object) {
		return subject + Strings.CSV_DELIM.val + object;
	}

	@Override
	public void setHTML(boolean isHTML) {
		// TODO Auto-generated method stub

	}

	@Override
	public String format(String subject, String predicate, String object, boolean removeEmptyObjectLiterals) {
		return subject + Strings.CSV_DELIM.val + object;
	}

}
