package alu.linking.executable.preprocessing.setup.surfaceform.query;

import org.apache.commons.logging.Log;
import org.apache.log4j.Logger;

import alu.linking.config.kg.EnumModelType;
import alu.linking.preprocessing.surfaceform.query.HSFQuery;
import alu.linking.structure.Executable;

public class HSFQueryExecutor implements Executable {

	@Override
	public void init() {

	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		final EnumModelType KG;
		EnumModelType KGhelper = EnumModelType.DEFAULT;
		if (o.length > 0) {
			for (Object ob : o) {
				if (ob instanceof EnumModelType) {
					KGhelper = (EnumModelType) ob;
					break;
				}
			}
		}
		KG = KGhelper;
		KGhelper = null;
		new HSFQuery(KG).execQueries();
		return null;
	}

	@Override
	public boolean destroy() {
		return false;
	}
}
