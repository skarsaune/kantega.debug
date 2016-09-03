package no.kantega.debug.log;

import java.util.logging.Level;

public class Logging {
	
	private static Boolean supportsSlf4j;
	private static Boolean supportsLog4j;

	private static boolean supportsSlf4j() {
		if(supportsSlf4j == null) {
			try {
				supportsSlf4j = Class.forName("org.slf4j.LoggerFactory") != null;
			} catch (ClassNotFoundException e) {
				supportsSlf4j = false;
			}
		}
		return supportsSlf4j;
	}
	
	private static boolean supportsLog4j() {
		if(supportsLog4j == null) {
			try {
				supportsLog4j = Class.forName("org.apache.log4j.Logger") != null;
			} catch (ClassNotFoundException e) {
				supportsLog4j = false;
			}
		}
		return supportsSlf4j;
	} 

	
	public static void info(Class<?> klass, String msg, Object ... arguments) {
		if(supportsSlf4j()) {
			Slf4jLogging.slf4jInfo(klass, msg, arguments);
		} else if(supportsLog4j()) {
			Log4jLogging.log4jLog(klass, msg, org.apache.log4j.Level.INFO, arguments);
		} else {
			utilLog(klass, msg, Level.INFO, arguments );
		}
	}
	
	private static void utilLog(Class<?> klass, String msg,  Level level, Object ... arguments) {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(klass.getName());
		if(logger.isLoggable(level)) {
			logger.log(level, format(msg, arguments));
		}
		
	}

	static String format(final String msg, final Object ... arguments) {
		if(arguments.length > 0) {
			return String.format(msg.replace("{}", "%s"), arguments);
		}
		else {
			return msg;
		}
	}

	public static void debug(Class<?> klass, String msg, Object ... arguments) {
		if(supportsSlf4j()) {
			Slf4jLogging.slf4jDebug(klass, msg, arguments);
		}
	}
	
	public static void warn(Class<?> klass, String msg, Object ... arguments) {
		if(supportsSlf4j()) {
			Slf4jLogging.slf4jWarn(klass, msg, arguments);
		}
	}

	public static void error(Class<?> klass, String msg,
			Exception e) {
		if(supportsSlf4j()) {
			Slf4jLogging.slf4jError(klass, msg, e);
		} else if( supportsLog4j()) {
			Log4jLogging.log4jLogThrowable(klass, msg, e, org.apache.log4j.Level.ERROR);
		} else {
			
		}
		
	}

	public static void error(Class<?> class1, String msg) {
		if(supportsSlf4j()) {
			Slf4jLogging.slf4jError(class1, msg, null);
		} else if( supportsLog4j()) {
			Log4jLogging.log4jLog(class1, msg, org.apache.log4j.Level.ERROR);
		} else {
			utilLog(class1, msg, Level.SEVERE);
		}	
	}
	
	

}
