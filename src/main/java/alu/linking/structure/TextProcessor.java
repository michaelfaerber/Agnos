package alu.linking.structure;

import java.io.IOException;
import java.util.concurrent.Callable;

public interface TextProcessor extends Callable<TextProcessor> {
	public String getText() throws IOException;

	// public InputStream getInputStream();
}
