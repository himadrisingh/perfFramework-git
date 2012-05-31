/**
 *
 */
package org.tc.perf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.terracotta.api.TerracottaClient;
import org.terracotta.cluster.ClusterInfo;

/**
 * Class with simple utility methods
 *
 * @author Himadri Singh
 *
 */
public class Utils {
	private static Logger log = Logger.getLogger(Utils.class);
	private static final String rollingAppender = "rollingAppender";

	private Utils() {
	} // not meant to be instantiated

	/**
	 * Updates the framework log location to new path. This is necessary as the
	 * path is being recreated for every test.
	 *
	 * @param location
	 *            log location
	 */
	public static void updateLogLocation(String location) {
		try {
			Layout layout = new PatternLayout("%d %-5p [%c{1}] %m%n ");
			FileAppender fap = new RollingFileAppender(layout, location);
			fap.setName(rollingAppender);
			fap.setAppend(false);
			Logger log = Logger.getRootLogger();
			log.removeAppender(rollingAppender);
			log.addAppender(fap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the logger with added by updateLogLocation. Need to close the
	 * logger as it keeps the file handle and next time test cleanup will fail
	 * as it will can't delete the logs used by this process.
	 */

	public static void closeLogger() {
		Appender ap = Logger.getRootLogger().getAppender(rollingAppender);
		if (ap != null) {
			ap.close();
			Logger log = Logger.getRootLogger();
			log.removeAppender(rollingAppender);
		}
	}

	/**
	 * returns the hostname of the machine.
	 *
	 * @return hostname
	 */

	private static String getHostname() {
		ClusterInfo info = (new TerracottaClient(FW_TC_CONFIG_URL))
				.getToolkit().getClusterInfo();
		try {
			return info.getCurrentNode().getAddress().getCanonicalHostName();
		} catch (UnknownHostException e) {
			return "UNKNOWN";
		}
	}

	/**
	 * deletes the directory
	 *
	 * @param location
	 *            directory path
	 * @throws IOException
	 */
	public static void deleteDir(File location) throws IOException {
		log.info("Cleaning " + location.getAbsolutePath());
		FileUtils.deleteDirectory(location);
	}

	/**
	 * Pauses the thread
	 *
	 * @param millis
	 *            time in milliseconds
	 */
	public static void sleepThread(long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static Properties loadProperties(final String location)
			throws IllegalStateException {
		try {
			log.info("Loading properties from "
					+ (new File(location).getAbsolutePath()));
			FileInputStream fis = new FileInputStream(location);
			return loadProperties(fis);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Cannot find properties file: "
					+ location, e);
		}
	}

	public static Properties loadProperties(InputStream in) {
		Properties props = new Properties();
		try {
			props.load(in);
			in.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return props;
	}

	/**
	 * framework tc-config url pointing to framework terracotta server.
	 *
	 * default: localhost:8510
	 */
	public static final String FW_TC_CONFIG_URL = System.getProperty(
			"fw.tc.config", "localhost:8510");

	/**
	 * Classpath separator depending on unix or windows os.
	 */
	public static final String CLASSPATH_SEPARATOR = (System
			.getProperty("os.name").toLowerCase().indexOf("win") >= 0) ? ";"
			: ":";

	/**
	 * tc-config file name which will be created by framework.
	 */
	public static final String TC_CONFIG = "tc-config.xml";

	public static final String HOSTNAME = getHostname();
	public static final String LOG_EXT = "-log.tar.gz";
	public static final String HEADER = "===============================================================";

}
