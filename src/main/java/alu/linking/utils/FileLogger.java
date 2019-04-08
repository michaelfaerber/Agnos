package alu.linking.utils;

import org.apache.log4j.Logger;

public class FileLogger extends Logger implements AutoCloseable {

	protected FileLogger(String name) {
		super(name);
	}

	@Override
	public void debug(Object message) {
		super.debug(message);
	}
	
	@Override
	public void warn(Object message) {
		// TODO Auto-generated method stub
		super.warn(message);
	}
	
	@Override
	public void error(Object message) {
		super.error(message);
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
