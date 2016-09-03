package no.kantega.debug.log;

import org.slf4j.LoggerFactory;

public class Slf4jLogging {

	static void slf4jWarn(Class<?> klass, String msg, Object[] arguments) {
		LoggerFactory.getLogger(klass).debug(msg, arguments);
		
	}

	static void slf4jDebug(Class<?> klass, String msg, Object[] arguments) {
		LoggerFactory.getLogger(klass).debug(msg, arguments);
	}

	static void slf4jInfo(Class<?> klass, String msg, Object ... arguments) {
		LoggerFactory.getLogger(klass).info(msg, arguments);
	}

	static void slf4jError(Class<?> klass, String msg, Exception e) {
		LoggerFactory.getLogger(klass).error(msg, e);
		
	}

}
