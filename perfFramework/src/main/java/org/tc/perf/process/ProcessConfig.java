package org.tc.perf.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * This class is used to define the arguments needed to start a java process.
 *
 * @author Himadri Singh
 */
public class ProcessConfig {

	private final List<String> defaultArgs = new ArrayList<String>(
			Arrays.asList("-XX:+HeapDumpOnOutOfMemoryError", "-verbose:gc",
					"-XX:+PrintGCTimeStamps", "-XX:+PrintGCDetails",
					"-Dcom.sun.management.jmxremote", "-showversion",
					"-Dcom.sun.management.jmxremote.ssl=false",
					"-Dtc.ssl.trustAllCerts=true",
					"-Dtc.ssl.disableHostnameVerifier=true",
					"-Dcom.sun.management.jmxremote.authenticate=false"));

	/**
	 * the main class to be start a java process.
	 */
	private final String mainClass;

	/**
	 * the classpath being used to start the process.
	 */
	private String classpath = ".";

	/**
	 * the list of program arguments
	 */
	private List<String> arguments = new ArrayList<String>();

	/**
	 * the location from where java process should be started so that relative
	 * paths, if any, are maintained.
	 */
	private File location = new File(".");

	/**
	 * relative path to the log directory
	 */
	private File logsDir = new File(".");

	/**
	 * the list of jvm arguments
	 */
	private List<String> jvmArgs = new ArrayList<String>();

	/**
	 * the log snippet that will mark the java process started successfully
	 */
	private String logSnippet = null;

	/**
	 * for custom java home settings.
	 */
	private String javaHome = System.getProperty("java.home");

	/**
	 * log filename that contains the console output.
	 */
	private final String consoleLog;

	/**
	 * process will be restarted if killed.
	 */

	private List<Integer> crashIntervals = new ArrayList<Integer>();

	/**
	 * Repeat crashing of server -1 : indefinitely
	 */
	private int crashRepeatCount = -1;

	private final String verboseGcLog;

	private final String processName;

	/**
	 * Process Config requires main-class name and process name. processName
	 * will be used for various loggings.
	 *
	 * @param mainClass
	 *            main-class name to execute the java process
	 * @param processName
	 *            unique process name
	 */
	public ProcessConfig(String mainClass, String processName) {
		this.mainClass = mainClass;
		String userHome = System.getProperty("user.home");
		if (userHome != null && !userHome.isEmpty())
			defaultArgs.add("-XX:HeapDumpPath=" + userHome + File.separator
					+ "perf-fw-heapdumps");
		this.processName = processName;
		this.consoleLog = processName + "-console.log";
		this.verboseGcLog = processName + "-verbose-gc.log";
	}

	public String getClasspath() {
		return classpath;
	}

	public ProcessConfig setClasspath(String classpath) {
		this.classpath = classpath;
		return this;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public ProcessConfig setArguments(List<String> arguments) {
		this.arguments = arguments;
		return this;
	}

	public File getLocation() {
		return location;
	}

	public ProcessConfig setLocation(File location) {
		this.location = location;
		return this;
	}

	public File getLogsDir() {
		return logsDir;
	}

	public ProcessConfig setLogsDir(File logsDir) {
		this.logsDir = logsDir;
		this.defaultArgs.add("-Xloggc:"
				+ new File(logsDir, this.verboseGcLog).getAbsolutePath());
		return this;
	}

	public List<String> getJvmArgs() {
		return jvmArgs;
	}

	public ProcessConfig setJvmArgs(List<String> jvmArgs) {
		this.jvmArgs = jvmArgs;
		return this;
	}

	public String getLogSnippet() {
		return logSnippet;
	}

	public ProcessConfig setLogSnippet(String logSnippet) {
		this.logSnippet = logSnippet;
		return this;
	}

	public String getMainClass() {
		return mainClass;
	}

	public List<String> getDefaultArgs() {
		return defaultArgs;
	}

	public String getJavaHome() {
		return javaHome;
	}

	public ProcessConfig setJavaHome(String javaHome) {
		this.javaHome = javaHome;
		return this;
	}

	public File getConsoleLog() {
		return new File(logsDir, consoleLog);
	}

	public File getVerboseGcLog() {
		return new File(logsDir, verboseGcLog);
	}

	public List<Integer> getCrashIntervals() {
		return crashIntervals;
	}

	public ProcessConfig setCrashIntervals(List<Integer> crashIntervals) {
		this.crashIntervals = crashIntervals;
		return this;
	}

	public int getCrashRepeatCount() {
		return crashRepeatCount;
	}

	public ProcessConfig setCrashRepeatCount(int crashRepeatCount) {
		this.crashRepeatCount = crashRepeatCount;
		return this;
	}

	public String getProcessName() {
		return processName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(mainClass).append(" ");
		for (String arg : arguments)
			sb.append(arg).append(" ");
		return sb.toString();
	}

}
