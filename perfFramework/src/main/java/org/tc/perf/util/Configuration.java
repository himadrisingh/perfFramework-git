package org.tc.perf.util;

import static java.io.File.separatorChar;
import static org.tc.perf.util.Utils.HEADER;
import static org.tc.perf.util.Utils.HOSTNAME;
import static org.tc.perf.util.Utils.loadProperties;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * Loads configuration file. Set default values. <br/>
 * <br/>
 * Keys can be specified for specific hostname too. <br/>
 * <br/>
 * Priority level of the parameter: key.hostname > key <br/>
 * the value for key.hostname will override the value for key
 *
 * @author Himadri Singh
 */
public class Configuration implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(Configuration.class);
	private static final String DOT = ".";
	private static final String NL = "\n\t";
	private static final String defnFileName = "test.defn";

	public static final Configuration EMPTY = new Configuration();

	private final String rootDir;
	private final Properties props;
	private final String uniqueId, testName;
	private final List<String> allCases;
	private final String testCase;
	private final boolean inline;

	/*
	 * Test Data
	 */
	private static enum Status {
		RUNNING, FINISHED, FAILED;
	}

	private String resultLog = "NA";
	private Status status;
	private Date startTime, endTime;
	private String errorMsg = "Unknown";

	/**
	 * Just for empty Configuration
	 */
	private Configuration() {
		props = null;
		allCases = null;
		testCase = null;
		uniqueId = null;
		testName = null;
		rootDir = null;
		inline = false;
	}

	public Configuration(final Properties properties) {
		props = new Properties();
		props.putAll(loadProperties(Configuration.class
				.getResourceAsStream("/hidden.properties")));
		props.putAll(properties);

		testName = getRequiredString("test");
		allCases = toList(getRequiredString("cases"));
		rootDir = System.getProperty("tests.root.dir", "tests") + separatorChar
				+ testName;
		inline = getBoolean("inline", false);

		try {
			loadProperties(getRootdir() + separatorChar + defnFileName);
			log.info("Loaded test definition: " + testName);
		} catch (IllegalStateException ise) {
			log.warn("No particular test definition found.");
		}
		log.info("Properties: " + props);
		log.info("Log location: " + getLocation().getAbsolutePath());

		uniqueId = UUID.randomUUID().toString();
		testCase = null;
	}

	/**
	 * Copy constructor. This is being used to create a copy of
	 * {@link Configuration} when loading a new test case so that original
	 * properties are entact.
	 *
	 * @param config
	 *            Configuration
	 * @param testcase
	 *            current test case
	 */
	public Configuration(final Configuration config, final String testcase) {
		this.props = new Properties(config.props);
		this.testName = config.testName;
		this.allCases = config.allCases;
		this.uniqueId = config.uniqueId;
		this.rootDir = config.rootDir;
		this.inline = config.inline;
		this.testCase = testcase;
		try {
			log.info("Loading properties for test case: " + testcase);
			loadTestCase(getLocation() + "/" + testcase);
		} catch (IllegalStateException ise) {
			log.warn("No particular config found for testcase: " + testcase);
		}
	}

	private List<File> getDirectories(final List<String> dirNames) {
		List<File> directories = new ArrayList<File>();
		for (String dir : dirNames) {
			File d = new File(getRootdir(), dir);
			if (d.isDirectory()) {
				log.info("Test directories found: " + d.getAbsolutePath());
				directories.add(d);
			} else
				log.warn("Can't find directory: " + d.getAbsolutePath() + " : "
						+ dir);
		}

		if (directories.size() == 0)
			throw new IllegalStateException(
					"Test jar directories cant be zero.");

		return directories;
	}

	/**
	 * @return the list of jvm arguments for load process.
	 */
	public List<String> getLoadJvmArgs() {
		return toList(getString("load_jvm_args", ""));
	}

	/**
	 * @return main class name for load process.
	 */
	public String getLoadMainClass() {
		return (getLoadmachines().size() > 0) ? getRequiredString("load-main-classname")
				: "";
	}

	/**
	 * @return program arguments for the load process, if any.
	 */
	public List<String> getLoadArguments() {
		return toList(getString("load-arguments", ""));
	}

	/**
	 * @return properties
	 */
	public Properties getProps() {
		return props;
	}

	/**
	 * Returns terracotta kit location. If nothing specifies, searches for
	 * terracotta*.tar.gz files in the root directory. If multiple found uses,
	 * first one only. Will print the file being used.
	 *
	 * @return kit location
	 */
	public String getKitLocation() {
		String kit = getString("kit.location", "");
		if (!kit.equals(""))
			return kit;
		File current = new File(".");
		String[] tars = current.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches("terracotta.*tar.gz");
			}
		});
		if (tars.length == 0)
			throw new IllegalStateException("No terracotta kits found!!");
		log.info("Terracotta kit (terracotta.*.tar.gz) found: " + tars[0]);
		return tars[0];
	}

	/**
	 * Get the location of the license file on the system
	 *
	 * @return the licenseFileLocation
	 */
	public String getLicenseFileLocation() {
		return getString("kit.licenseLocation", "terracotta-license.key");
	}

	/**
	 * @return directories containing the test process jars.
	 */
	public List<File> getTestDirectories() {
		return getDirectories(toList(getRequiredString("directories")));
	}

	/**
	 * @return directories containing the load process jars.
	 */
	public List<File> getLoadDirectories() {
		return getDirectories(toList(getRequiredString("load_directories")));
	}

	/**
	 * @return list of regex of the files to be included in the classpath
	 */
	public List<String> getClasspathRegex() {
		return toList(getString("classpath", ".*jar .*xml .*properties"));
	}

	/**
	 * excludes the ehcache-* or terracotta-* jars to be included in the
	 * classpath. These jars will be replaced by one found in the terracotta
	 * installation kit.
	 *
	 * @return list of regex of the files to be excluded in the classpath.
	 */
	public List<String> getClasspathExclude() {
		return toList(getString("classpath.exclude", ""));
	}

	/**
	 * @return list of the regex of the files to be included in the log
	 *         collection.
	 */
	public List<String> getLogRegex() {
		return toList(getString("log.collection.ext",
				".*log .*log\\.[0-9]* .*xml .*txt"));
	}

	/**
	 * @return main class for the test process
	 */
	public String getMainClass() {
		return getRequiredString("main-classname");
	}

	/**
	 * @return java home to be used for test process
	 */
	public String getL1JavaHome() {
		return getString("l1.java.home", System.getProperty("java.home"));
	}

	/**
	 * @return java home to be used for terracotta server
	 */
	public String getL2_javaHome() {
		return getString("l2.java.home", System.getProperty("java.home"));
	}

	/**
	 * @return log snippet from the app logs that makes sure that it started
	 *         successfully
	 */
	public String getClientLogCheck() {
		return getString("client.log.check",
				"Connection successfully established");
	}

	/**
	 * @return unique id for each test case
	 */
	public String getUniqueId() {
		return uniqueId;
	}

	private File getDirectory(File parent, String name) {
		File f = new File(parent, name);
		try {
			FileUtils.forceMkdir(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}

	/**
	 * @return framework local directory
	 */
	public File getLocation() {
		String log = getString("logLocation",
				System.getProperty("user.home", "target/"))
				+ "/perfTests/framework2.0/local";
		return new File(log);
	}

	/**
	 * location to save the results on the local.
	 *
	 * @return {@link #getLocation()}/results/
	 */

	public File getResultLocation() {
		return getDirectory(getLocation(), "results");
	}

	/**
	 * base directory for setup location on the local.
	 *
	 * @return {@link #getLocation()}/setup/
	 */
	public File getSetupLocation() {
		return getDirectory(getLocation(), "setup");
	}

	/**
	 * setup location for the clients. All application/test jars are downloaded
	 * here.
	 *
	 * @return {@link #getSetupLocation()}/client/
	 */
	public File getClientSetupLocation() {
		return getDirectory(getSetupLocation(), "client");
	}

	/**
	 * setup location for the clients. Terracotta kit is downloaded and
	 * extracted here.
	 *
	 * @return {@link #getSetupLocation()}/server/
	 */
	public File getServerSetupLocation() {
		return getDirectory(getSetupLocation(), "server");
	}

	/**
	 * setup location for the clients. All load process jars are downloaded
	 * here.
	 *
	 * @return {@link #getSetupLocation()}/load/
	 */
	public File getLoadSetupLocation() {
		return getDirectory(getSetupLocation(), "load");
	}

	/**
	 * base directory for all the logs
	 *
	 * @return {@link #getLocation()}/logs/
	 */
	public File getLogLocation() {
		return getDirectory(getLocation(), "logs");
	}

	/**
	 * Client logs directory.
	 *
	 * @return {@link #getLogLocation()}/client/
	 */
	public File getClientLogLocation() {
		return getDirectory(getLogLocation(), "client");
	}

	/**
	 * Server logs directory.
	 *
	 * @return {@link #getLogLocation()}/server/
	 */
	public File getServerLogLocation() {
		return getDirectory(getLogLocation(), "server");
	}

	/**
	 * Load logs directory.
	 *
	 * @return {@link #getLogLocation()}/load/
	 */
	public File getLoadLogLocation() {
		return getDirectory(getLogLocation(), "load");
	}

	/**
	 * @return list of program arguments for the test process
	 */
	public List<String> getArguments() {
		return toList(getString("arguments", ""));
	}

	/**
	 * @return list of machines to be used for terracotta server
	 */
	public List<String> getL2machines() {
		return toList(getRequiredString("l2machines").toLowerCase());
	}

	/**
	 * @return list of machines to be used for test process
	 */
	public List<String> getL1machines() {
		return toList(getRequiredString("l1machines").toLowerCase());
	}

	/**
	 * @return list of machines to be used for load process
	 */
	public List<String> getLoadmachines() {
		return toList(getString("loadmachines", "").toLowerCase());
	}

	/**
	 * @return list of all machines (unique)
	 */
	public Set<String> getAllmachines() {
		Set<String> allmachines = new HashSet<String>();
		allmachines.addAll(getL1machines());
		allmachines.addAll(getL2machines());
		allmachines.addAll(getLoadmachines());
		return allmachines;
	}

	/**
	 * Since we have multiple processes running on same box and specified
	 * redundantly in the configuration. This method provides the collection of
	 * unique L2 agents. Use this if we want to run a particular work only once
	 * on an agent.
	 *
	 * @return the collection of unique L2 agents.
	 */
	public Set<String> getUniqueL2Machines() {
		return new HashSet<String>(getL2machines());
	}

	/**
	 * Since we have multiple processes running on same box and specified
	 * redundantly in the configuration. This method provides the collection of
	 * unique L1 agents. Use this if we want to run a particular work only once
	 * on an agent.
	 *
	 * @return the collection of unique L1 agents.
	 */
	public Set<String> getUniqueL1Machines() {
		return new HashSet<String>(getL1machines());
	}

	/**
	 * Since we have multiple processes running on same box and specified
	 * redundantly in the configuration. This method provides the collection of
	 * unique load agents. Use this if we want to run a particular work only
	 * once on an agent.
	 *
	 * @return the collection of unique load agents.
	 */
	public Set<String> getUniqueLoadMachines() {
		return new HashSet<String>(getLoadmachines());
	}

	/**
	 * @return Number of server to be included in one mirror group.
	 */
	public int getServersPerMirrorGroup() {
		return getInteger("serversPerMirrorGroup", 1);
	}

	/**
	 * @return Distributed Garbage Collection to be enabled on the server or
	 *         not.
	 */
	public boolean isDgcEnabled() {
		return getBoolean("dgc.enabled", true);
	}

	/**
	 *
	 * @return Distributed Garbage Collection to be set in tc-config.xml
	 */
	public int getDgcInterval() {
		return getInteger("dgc.interval", 300);
	}

	/**
	 * @return Persistence mode to be used. <br/>
	 *         true if permanent-store <br/>
	 *         false if temporary-swap-only<br/>
	 */
	public String getPersistence() {
		return (getBoolean("persistence.enabled", false)) ? "permanent-store"
				: "temporary-swap-only";
	}

	/**
	 * @return true if BigMemory on terracotta server is enabled.
	 */
	public boolean isOffheapEnabled() {
		return getBoolean("l2.offheap.enabled", false);
	}

	public String getOffheapMaxDataSize() {
		return getString("l2.offheap.maxDataSize", "1g");
	}

	public List<String> getL1_jvmArgs() {
		return toList(getString("l1_jvm_args", ""));
	}

	public List<String> getL2_jvmArgs() {
		return toList(getString("l2_jvm_args", ""));
	}

	public boolean isClearLogs() {
		return getBoolean("logs.clear", true);
	}

	public String getFwTcConfigParam() {
		return getString("fw-tc-config-param", "");
	}

	public String getFwNodeCountParam() {
		return getString("fw-node-count-param", "");
	}

	/**
	 * Returns list of the intervals at which server needs to be crashed. Eg.
	 * Interval can be "server.restart.localhost-9520: 300 200 100". Server will
	 * be crashed after 300 secs then 200 secs after that then 100 secs after
	 * that.
	 *
	 * @param serverName
	 * @return list of integers crash interval converted from string
	 */
	public List<Integer> getCrashIntervals(String serverName) {
		List<String> list = toList(getString("server.restart." + serverName, ""));
		String port = serverName.substring(serverName.indexOf("952"));
		if (list.size() == 0)
			list = toList(getString("server.restart.localhost-" + port, ""));
		List<Integer> intervals = new ArrayList<Integer>();
		for (String i : list) {
			try {
				intervals.add(Integer.parseInt(i));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return intervals;
	}

	public int getCrashRepeatCount(String serverName) {
		return getInteger("server.restart.repeat", -1);
	}

	public String getServerDataDir(String serverName) {
		return getString("server.data.dir." + serverName, "");
	}

	private List<String> toList(final String value) {
		List<String> list;
		// If the string is null or empty, return an empty array.
		if (value == null || value.trim().length() == 0)
			list = new ArrayList<String>();
		else {
			list = Arrays.asList(value.replace("localhost", HOSTNAME)
					.split(" "));
		}
		return Collections.unmodifiableList(list);
	}

	private Boolean getBoolean(final String key, final boolean defaultValue) {
		return Boolean.valueOf(getString(key, String.valueOf(defaultValue)));
	}

	private Integer getInteger(final String key, final int defaultValue) {
		return Integer.valueOf(getString(key, String.valueOf(defaultValue)));
	}

	private String getString(final String key, final String defaultValue) {
		String value = getProperty(key);
		if (value == null || value.trim().length() == 0) {
			log.debug("Key not found in Properties: " + key
					+ " , Using defaults: " + defaultValue);
			props.setProperty(key, defaultValue);
			value = defaultValue;
		}
		return value.trim()
				.replaceAll("\\$\\{testcase.properties\\}", testCase);
	}

	/**
	 * key.hostname > key
	 *
	 * @param key
	 * @return
	 */
	private String getProperty(final String key) {
		String val;
		if ((val = props.getProperty(key + DOT + HOSTNAME)) != null)
			return val;
		return props.getProperty(key);
	}

	private String getRequiredString(final String key) {
		String prop = getProperty(key);
		if (prop == null) {
			throw new IllegalStateException("Required property not found: "
					+ key);
		}
		return prop.trim();
	}

	public String getUser() {
		return getString("user", "unknown");
	}

	public String getTestName() {
		return testName;
	}

	public String getTestCase() {
		return testCase;
	}

	public List<String> getAllCases() {
		return allCases;
	}

	public int getWorkTimeout() {
		return getInteger("work.timeout", 600);
	}

	public String getMasterController() {
		return getL2machines().get(0);
	}

	public void setStatusRunning() {
		this.startTime = new Date();
		this.status = Status.RUNNING;
	}

	public void setStatusFinished() {
		this.endTime = new Date();
		this.status = Status.FINISHED;
	}

	public void setStatusFailed(String errorMsg) {
		this.errorMsg = errorMsg;
		this.endTime = new Date();
		this.status = Status.FAILED;
		log.error(HEADER);
		log.error("Failure!!");
		log.error(this.errorMsg);
		log.error(HEADER);
	}

	public boolean isRunning() {
		return Status.RUNNING.equals(this.status);
	}

	public boolean isFailed() {
		return Status.FAILED.equals(this.status);
	}

	public boolean isFinished() {
		return Status.FINISHED.equals(this.status);
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getResultLog() {
		return resultLog;
	}

	public void setResultLog(String resultLog) {
		this.resultLog = resultLog;
		if (!isFailed())
			setStatusFinished();
	}

	public String getRootdir() {
		return rootDir;
	}

	public boolean isInline() {
		return inline;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Test ID: ").append(uniqueId).append(NL)
				.append("Test Name: ").append(testName).append(NL)
				.append("Test Case: ").append(testCase).append(NL)
				.append("L2: ").append(getL2machines()).append(NL)
				.append("L1: ").append(getL1machines()).append(NL)
				.append("Status: ").append(status).append(NL).append(NL)
				.append("Start Time: ").append(startTime).append(NL);
		if (!isInline())
			sb.append("Controller: ").append(getMasterController());
		if (!isRunning())
			sb.append("End Time: ").append(endTime).append(NL)
					.append("Results Log: ").append(resultLog).append(NL);

		if (isFailed())
			sb.append("Failure Reason: ").append(errorMsg).append(NL);

		return sb.toString();
	}

	public void loadTestCase(String testcaseFilePath) {
		this.props.putAll(loadProperties(testcaseFilePath));

	}

}
