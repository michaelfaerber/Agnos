package alu.linking.launcher;

import alu.linking.config.kg.EnumModelType;
import alu.linking.executable.Pipeline;
import alu.linking.executable.preprocessing.setup.surfaceform.query.HSFQueryExecutor;
import alu.linking.executable.preprocessing.setup.surfaceform.query.NP_HSFQueryExecutor;
import alu.linking.executable.preprocessing.setup.surfaceform.query.NP_URLHSFQueryExecutor;
import alu.linking.executable.preprocessing.setup.surfaceform.query.SFQueryExecutor;
import alu.linking.preprocessing.embeddings.SSPEmbeddingGenerator;

public class LauncherSSPEmbeddingsMAG {

	private static final boolean UPDATE_QUERIES = false;

	public static void main(String[] args) {
		final EnumModelType KG = EnumModelType.MAG;
		final Pipeline pipeline = new Pipeline();
		if (UPDATE_QUERIES) {
			// Predicates: name
			pipeline.queue(new SFQueryExecutor(), KG);
			// Predicates: created, category
			pipeline.queue(new HSFQueryExecutor(), KG);
			// Empty
			pipeline.queue(new NP_HSFQueryExecutor(), KG);
			// Empty
			pipeline.queue(new NP_URLHSFQueryExecutor(), KG);
		}
		pipeline.queue(new SSPEmbeddingGenerator(KG), KG);
		try {
			pipeline.exec();
			System.out.println("Done!");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
