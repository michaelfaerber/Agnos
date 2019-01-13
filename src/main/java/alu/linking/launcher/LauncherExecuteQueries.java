package alu.linking.launcher;

import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.Pipeline;
import alu.linking.executable.preprocessing.setup.surfaceform.query.HSFQueryExecutor;
import alu.linking.executable.preprocessing.setup.surfaceform.query.NP_HSFQueryExecutor;
import alu.linking.executable.preprocessing.setup.surfaceform.query.NP_URLHSFQueryExecutor;
import alu.linking.executable.preprocessing.setup.surfaceform.query.SFQueryExecutor;

public class LauncherExecuteQueries {

	public static void main(String[] args) {
		Pipeline pipeline = new Pipeline();
		final EnumModelType KG = EnumModelType.CRUNCHBASE2;
		pipeline.queue(new SFQueryExecutor(), KG);
		pipeline.queue(new HSFQueryExecutor(), KG);
		pipeline.queue(new NP_HSFQueryExecutor(), KG);
		pipeline.queue(new NP_URLHSFQueryExecutor(), KG);
		try {
			pipeline.exec();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
