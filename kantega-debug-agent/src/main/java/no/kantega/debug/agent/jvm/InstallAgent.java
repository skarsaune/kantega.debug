package no.kantega.debug.agent.jvm;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * @author marska
 *         <p>
 *         Install the decentipede agent in a target JVM identified by its pid
 *         The agent is implemented as the same jar I'm in
 */
public class InstallAgent {

    private static final Logger logger = LoggerFactory.getLogger(InstallAgent.class);

    public static void installAgent(final int pid) throws IOException, AttachNotSupportedException, URISyntaxException, AgentLoadException, AgentInitializationException  {

//    	try {
//        logger.info("Attaching to JVM with pid: {}", pid);
//        Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
//        Method attachMethod = vmClass.getDeclaredMethod("attach", String.class);
//        Object vm=attachMethod.invoke(null, pid);
//        Method loadAgentMethod = vmClass.getDeclaredMethod("loadAgent", String.class, String.class);
//        loadAgentMethod.invoke(vm, getMyJar().toString(), "decentipede.installed");
//        Method detachAgentMethod = vmClass.getDeclaredMethod("detach");
//    	} catch (Throwable e) {
//    		System.err.println(e);
//    	}
        
        final VirtualMachine vm = attach(pid);
        try {
            if (vm.getAgentProperties().containsValue("decentipede.installed")) {
                logger.warn("JVM agent already installed, aborting");
            } else {

                final String agentPath = getMyJar().toString();
                logger.info("Installing Decentipede JVM agent witn path: {} in JVM with pid: {}", agentPath, pid);
                vm.loadAgent(agentPath, "decentipede.installed");
//                setUpDebuggerIfRequrired(vm);
            }
        } finally {
            try {
                vm.detach();
            } catch (Exception e) {
                logger.info("Exception detaching VM for installing agent");
            }
        }
    }

    public static void setUpDebuggerIfRequrired(final VirtualMachine vm) throws IOException, AgentLoadException,
            AgentInitializationException {

        if(isDebuggerRunning(vm)) {
            logger.info("Debugger is running");
        } else {
            final String jdwpOptions = "jdwp=transport=dt_socket,server=y,suspend=n,address=" + allocateFreePort();
            logger.info("Setting up jdwp with options: {}" , jdwpOptions);
            vm.loadAgentLibrary("jdwp", jdwpOptions);
        }
    }

	private static boolean isDebuggerRunning(final VirtualMachine vm)
			throws IOException {
	    final String cmdLine = String.valueOf(vm.getAgentProperties().get("sun.jvm.args"));
	    return vm.getAgentProperties().get("jdwp") != null || cmdLine.contains("jdwp");
	}


    /**
     * Find path to the jarfile we are in, so it can be installed as an agent in target VM
     *
     * @return
     * @throws URISyntaxException
     */
    static File getMyJar() throws URISyntaxException {
        return new File(DecentipedeJvmAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    /**
     * Figure out a free port number
     *
     * @return
     */
    private static int allocateFreePort() throws IOException {
        ServerSocket sock = null;
        try {
            return new ServerSocket(0).getLocalPort();
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private static VirtualMachine attach(final int pid) throws IOException, AttachNotSupportedException {
        return VirtualMachine.attach(String.valueOf(pid));
    }
}
