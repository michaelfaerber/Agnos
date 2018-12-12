package alu.linking.preprocessing.fileparser.input;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.Lists;

public class CSVParserGenericWrapper implements FileInParser<String>, AutoCloseable {

	private CSVParser parser = null;
	private List<String> header = null;

	@Override
	public void close() throws IOException {
		this.parser.close();
	}

	@Override
	public FileInParser create(Reader reader, Object... obj) throws IOException {
		CSVFormat csvFormat = null;
		if (obj != null && obj.length > 0) {
			// If you pass a single parameter and it is a Character, then that will be the
			// delimiter for the CSV file
			if (obj.length == 1 && obj[0] != null && obj[0] instanceof Character) {
				csvFormat = CSVFormat.DEFAULT.withDelimiter((Character) obj[0]);
			}
		}
		if (csvFormat == null) {
			csvFormat = CSVFormat.DEFAULT;
		}
		this.parser = new CSVParser(reader, csvFormat);
		return this;
	}

	@Override
	public List<String> getNext() {
		final Iterator<CSVRecord> it = this.parser.iterator();
		if (it.hasNext()) {
			final List<String> ret = Lists.newArrayList();
			for (String recordEntry : it.next()) {
				ret.add(recordEntry);
			}
			return ret;
		} else {
			return null;
		}
	}

	@Override
	public String toFormatString(Object... words) {
		String ret = words[0].toString();
		for (int i = 1; i < words.length; ++i) {
			ret += "_" + words[i].toString();
		}
		return ret;
	}

	@Override
	public List<String> withHeader(boolean hasHeader) {
		if (hasHeader) {
			this.header = getNext();
		}
		return this.header;
	}

	@Override
	public String toFormatString(String word) {
		return word;
	}

}
