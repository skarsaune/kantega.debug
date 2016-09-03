package no.kantega.debug.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import no.kantega.debug.log.Logging;
import no.kantega.debug.util.Walkback;

import com.sun.jdi.event.LocatableEvent;

public class WalkbackPrinter {
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
			"yyyy-MM-dd-HH_mm_ss_SSS-");
	private static AtomicInteger seqNo = new AtomicInteger();
	private File walkbackFolder = new File(System.getProperty(
			"walkback.folder", "walkback"));
	public static String nextWalkbackName() {
		return FORMATTER.format(new Date()) + seqNo.incrementAndGet()
				+ ".walkback";
	}

	@SuppressWarnings("resource")
	public File printWalkback(LocatableEvent event) {
		final String contents = Walkback.printWalkback(event);
		try {
			if(!walkbackFolder.exists()) {
				walkbackFolder.mkdirs();
			}
			File walkbackFile = new File(walkbackFolder, nextWalkbackName());
			new FileWriter(walkbackFile).append(contents).close();
			Logging.info(this.getClass(),"Written walkback to file: {}" , walkbackFile);
			return walkbackFile;
		} catch (IOException e) {
			Logging.error(this.getClass(),"Unable to write walkback to file " , e);
			return null;
		}
	}
	
	public String[] getWalkbacks() {
		String[] walkbacks = walkbackFolder.list(new FilenameFilter() {
			
			public boolean accept(File folder, String fileName) {
				return fileName.endsWith(".walkback");
			}
		});
		//not extistant or empty folder
		if(walkbacks == null) {
			return new String[0];
		}
		//show latest first
		Arrays.sort(walkbacks, Collections.reverseOrder());
		return walkbacks;
	}
	
	public File getWalkbackFile(final String fileName) {
		return new File(walkbackFolder, fileName); 	
	}

	public String getWalkback(String walkback) throws IOException {
		final File walkbackFile = getWalkbackFile(walkback);
		final FileInputStream fis = new FileInputStream(walkbackFile);
		byte[] buffer = new byte[(int)walkbackFile.length()];
		fis.read(buffer);
		fis.close();
		return new String(buffer);
	}

}
