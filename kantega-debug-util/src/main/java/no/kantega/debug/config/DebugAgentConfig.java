package no.kantega.debug.config;
/**
 * Configuration for a debug agent
 * controls what kinds of tests it should attempt to control
 * @author marska
 *
 */
public interface DebugAgentConfig {
	/**
	 * 
	 * @return Whether cause of NullPointers should be checked for 
	 */
	boolean shouldDiagnoseNullPointerExceptions();
	
	/**
	 * Whether walkback (stacktrace with values) should be emitted on handled events
	 * @return
	 */
	boolean shouldEmitWalkbackOnEvents();
	
	
}
