package no.kantega.debug.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import no.kantega.debug.util.Walkback;

import com.sun.jdi.event.LocatableEvent;

public class WalkbackPrinter {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
			"yyyy-MM-dd-HH_mm_ss_SSS-");
	private static AtomicInteger seqNo = new AtomicInteger();
	private File walkbackFolder = new File(System.getProperty(
			"walkback.folder", "walkback"));
	private String servletContext;

	public static String nextWalkbackName() {
		return FORMATTER.format(new Date()) + seqNo.incrementAndGet()
				+ ".walkback";
	}

	public File printWalkback(LocatableEvent event) {
		final String contents = Walkback.printWalkback(event);
		try {
			if(!walkbackFolder.exists()) {
				walkbackFolder.mkdirs();
			}
			File walkbackFile = new File(walkbackFolder, nextWalkbackName());
			new FileWriter(walkbackFile).append(contents).close();
			LoggerFactory.getLogger(this.getClass()).info("Written walkback to file: " + walkbackFile);
			return walkbackFile;
		} catch (IOException e) {
			LoggerFactory.getLogger(this.getClass()).error("Unable to write walkback to file " , e);
			return null;
		}
	}
	
	public String[] getWalkbacks() {
		return walkbackFolder.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File folder, String fileName) {
				return fileName.endsWith(".walkback");
			}
		});
	}
	
	public File getWalkbackFile(final String fileName) {
		return new File(walkbackFolder, fileName); 	
	}

	public void setServletContext(String contextPath) {
		this.servletContext = contextPath;
		
	}
}
