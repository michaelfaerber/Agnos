package alu.linking.structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public interface Loggable {

	static void initLogging()
	{
        try {
        InputStream input = Loggable.class.getClassLoader().getResourceAsStream("log4j.properties");
        Properties prop = new Properties();
        prop.load(input);
        PropertyConfigurator.configure(prop);
    } catch (IOException e) {
        e.printStackTrace();
        System.out.println("ERROR: Unable to load subscriptionlog4j.properties");
    }
	}
	
	public default Logger getLogger() {
		//initLogging();
		//return LogFactory.getLog(getClass());
		//return Logger.getLogger(getClass());
		return LogManager.getLogger(getClass());
	}

	public default void warn(final String msg) {
		getLogger().warn(msg);
	}

	public default void error(final String msg) {
		getLogger().error(msg);
	}

}
