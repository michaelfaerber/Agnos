package alu.linking.preprocessing.surfaceform.query;

import alu.linking.config.constants.FilePaths;
import alu.linking.config.kg.EnumModelType;

public class NP_HSFQuery extends HSFQuery {
	public NP_HSFQuery(EnumModelType KG) {
		super(KG);
	}

	@Override
	protected String getQueryInputDir() {
		return FilePaths.DIR_QUERY_IN_NP_HELPING_SURFACEFORM.getPath(KG);
	}

	@Override
	protected String getQueryOutDir() {
		return FilePaths.DIR_QUERY_OUT_NP_HELPING_SURFACEFORM.getPath(KG);
	}
}
