package alu.linking.preprocessing.surfaceform.query;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.constants.Strings;
import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.surfaceform.query.structure.LiteralEntityQuery;

public class HSFQuery extends LiteralEntityQuery {
	private static final String newline = Strings.NEWLINE.val;
	/*
	 * @param delimQueryResults delimiter for value of each query result entry
	 */
	private static final String delimQueryResults = Strings.QUERY_RESULT_DELIMITER.val;

	public HSFQuery(EnumModelType KG) {
		super(KG);
	}

	@Override
	protected BufferedWriter initAlternateChannelWriter() throws IOException {
		return null;
	}

	@Override
	protected String getQueryInputDir() {
		return FilePaths.DIR_QUERY_IN_HELPING_SURFACEFORM.getPath(KG);
	}

	@Override
	protected String getQueryOutDir() {
		return FilePaths.DIR_QUERY_OUT_HELPING_SURFACEFORM.getPath(KG);
	}

	@Override
	protected void outputMainChannel(String varName, String value, boolean hasNext, BufferedWriter writer)
			throws IOException {
		final String dynamicDelimQueryResults = (hasNext ? delimQueryResults : newline);
		writer.write(value + dynamicDelimQueryResults);
	}

	@Override
	protected void outputAlternateChannels(String varName, String value, boolean hasNext, List<BufferedWriter> writers)
			throws IOException {
		// Do nothing
	}
}
