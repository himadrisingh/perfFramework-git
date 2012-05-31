package org.tc.cluster.watcher.util;

import static java.io.File.separator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class ClusterWatcherProperties {

	private static final String NL = "\n\t";
	public static final String LOGS_DIR = "logs.dir";
	public static Logger LOG = Logger.getLogger("ClusterWatcher");

	private final Properties props = new Properties();
	private String smtpServer, recipients, smtpUsername, smtpPasswd;
	private String systemStatsLog, terracottaStatsLog, dgcStatsLog, opsStatsLog;

	private int mailInterval;
	private int initialRetries;

	private int lowTxnThreshold;
	private long probeInterval;
	private long oneActiveMaxCheck;
	private long missingClientMaxCheck;
	private long lowTxnMaxCheck;
	private long clusterDownMaxCheck, serverDownMaxCheck;
	private long clientCount;
	private String tcConfig;
	private boolean checkClusterDown;
	private boolean checkWriteTps;

	private void refresh() {
		lowTxnThreshold = getPropertyAsInt("low.txr.threshold", 5);
		probeInterval = toMillis(getProperty("probe.interval", "5s"));
		oneActiveMaxCheck = getPropertyAsInt("one.active.max.check", 24);
		missingClientMaxCheck = getPropertyAsInt("missing.client.max.check", 24);
		lowTxnMaxCheck = getPropertyAsInt("low.txr.max.check", 24);
		clusterDownMaxCheck = getPropertyAsInt("cluster.down.max.check", 24);
		serverDownMaxCheck = getPropertyAsInt("server.down.max.check", 24);
		clientCount = getPropertyAsInt("clientcount", 0);
		tcConfig = getProperty("tc-config.url", "localhost:9510");
		checkClusterDown = getBoolean("cluster.down.check", false);
		initialRetries = getPropertyAsInt("initial.retries", 6);
		checkWriteTps = getBoolean("check.write.tps", false);

		String logs = getProperty(LOGS_DIR, "logs");
		systemStatsLog = logs + separator + "system.stats.csv";
		terracottaStatsLog = logs + separator + "tc.stats.csv";
		dgcStatsLog = logs + separator + "dgc.stats.csv";
		opsStatsLog = logs + separator + "ops.log";

		smtpServer = getProperty("smtp.host");
		recipients = getProperty("recipients");
		smtpUsername = getProperty("smtp.username");
		smtpPasswd = getProperty("smtp.password");
		mailInterval = getPropertyAsInt("mail.interval", 6 * 3600);

		updateLogLocation(logs + separator + "cluster-watcher.log");
	}

	private void updateLogLocation(String logs){
		try {
			Layout layout = new PatternLayout("%d %-5p [%c{1}] %m%n ");
			FileAppender fap = new RollingFileAppender(layout, logs);
			LOG.removeAllAppenders();
			LOG.addAppender(fap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ClusterWatcherProperties(String propertyFile) {
		try {
			props.clear();
			InputStream fis = new FileInputStream(propertyFile);
			props.load(fis);
			IOUtils.closeQuietly(fis);
			props.putAll(System.getProperties());
			refresh();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ClusterWatcherProperties(Properties p) {
		props.clear();
		props.putAll(p);
		refresh();
		LOG.info("Loaded properties: " + props);
	}

	private String getProperty(String key) {
		return props.getProperty(key);
	}

	private String getProperty(String key, String defaultVal) {
		return props.getProperty(key, defaultVal);
	}

	private int getPropertyAsInt(String name, int defaultVal) {
		return Integer.parseInt(getProperty(name, String.valueOf(defaultVal))
				.trim());
	}

/*	private double getPropertyAsDouble(String name, double defaultVal) {
		return Double.parseDouble(getProperty(name, String.valueOf(defaultVal))
				.trim());
	}

	private long getPropertyAsLong(String name, long defaultVal) {
		return Long.parseLong(getProperty(name, String.valueOf(defaultVal))
				.trim());
	}*/

	private boolean getBoolean(String key, boolean defaultVal) {
		String value = getProperty(key);
		if (value != null) {
			return Boolean.valueOf(value);
		}
		return Boolean.valueOf(defaultVal);
	}

	private long toMillis(String time) {
		String[] parts = time.trim().split(":");
		long millis = 0L;
		for (String part : parts) {
			String value = part.substring(0, part.length() - 1).trim();
			if (part.endsWith("h")) {
				millis += Integer.valueOf(value) * 60 * 60 * 1000L;
			} else if (part.endsWith("m")) {
				millis += Integer.valueOf(value) * 60 * 1000L;
			} else if (part.endsWith("s")) {
				millis += Integer.valueOf(value) * 1000L;
			} else {
				millis += Integer.valueOf(part);
			}
		}
		return millis;
	}

	public String getSmtpServer() {
		return smtpServer;
	}

	public String getRecipients() {
		return recipients;
	}

	public String getSmtpUsername() {
		return smtpUsername;
	}

	public String getSmtpPasswd() {
		return smtpPasswd;
	}

	public String getSystemStatsLog() {
		return systemStatsLog;
	}

	public String getTerracottaStatsLog() {
		return terracottaStatsLog;
	}

	public int getMailInterval() {
		return mailInterval;
	}

	public int getInitialRetries() {
		return initialRetries;
	}

	public int getLowTxnThreshold() {
		return lowTxnThreshold;
	}

	public long getProbeInterval() {
		return probeInterval;
	}

	public long getOneActiveMaxCheck() {
		return oneActiveMaxCheck;
	}

	public long getMissingClientMaxCheck() {
		return missingClientMaxCheck;
	}

	public long getLowTxnMaxCheck() {
		return lowTxnMaxCheck;
	}

	public long getClusterDownMaxCheck() {
		return clusterDownMaxCheck;
	}

	public long getClientCount() {
		return clientCount;
	}

	public String getTcConfig() {
		return tcConfig;
	}

	public boolean isCheckClusterDown() {
		return checkClusterDown;
	}

	public String getDgcStatsLog() {
		return dgcStatsLog;
	}

	public String getOpsStatsLog() {
		return opsStatsLog;
	}

	public boolean isCheckWriteTps() {
		return checkWriteTps;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ClusterWatcher Properties - ").append(NL)
				.append("tc-config url: ").append(tcConfig).append(NL)
				.append("Client Count: ").append(clientCount).append(NL)
				.append("Initial Retries (5s): ").append(initialRetries)
				.append(NL).append("Probe interval: ").append(probeInterval)
				.append(" ms").append(NL)
				.append("One Active Server Max Check: ")
				.append(oneActiveMaxCheck).append(NL)
				.append("Missing Clients Max Check: ")
				.append(missingClientMaxCheck).append(NL)
				.append("Cluster Down Max Check: ").append(clusterDownMaxCheck)
				.append(NL).append("Low Txn Max Check: ")
				.append(lowTxnMaxCheck).append(NL)
				.append("Low Txn Threshold: ").append(lowTxnThreshold)
				.append(NL).append("Check Cluster Down: ")
				.append(checkClusterDown);
		return sb.toString();
	}

	public long getServerDownMaxCheck() {
		return serverDownMaxCheck;
	}
}
