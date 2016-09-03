package no.kantega.debug.log;

import org.apache.log4j.Logger;

public class Log4jLogging {

	static void log4jLog(final Class<?> klass, String msg,  org.apache.log4j.Level level, final Object ... arguments) {
		Logger logger = Logger.getLogger(klass);
		if(logger.isEnabledFor(level)) {
			Logger.getLogger(klass).log(level, Logging.format(msg, arguments));
		}
		
	}

	static void log4jLogThrowable(Class<?> klass, String msg,
			Exception e, org.apache.log4j.Level error) {
		Logger.getLogger(klass).log(error, msg, e);
		
	}

}
