package org.tc.perf;

import static org.tc.perf.util.Utils.HEADER;
import static org.tc.perf.util.Utils.LOG_EXT;
import static org.tc.perf.util.Utils.closeLogger;
import static org.tc.perf.util.Utils.updateLogLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.tc.cluster.watcher.ClusterWatcher;
import org.tc.cluster.watcher.ClusterWatcherExecutor;
import org.tc.cluster.watcher.mail.Mail;
import org.tc.cluster.watcher.util.ClusterWatcherProperties;
import org.tc.perf.cache.DataCache;
import org.tc.perf.process.ProcessRegistry;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileUtils;
import org.tc.perf.work.InitialCleanup;
import org.tc.perf.work.l1.CleanupL1;
import org.tc.perf.work.l1.SetupL1;
import org.tc.perf.work.l1.StartL1;
import org.tc.perf.work.l1.StopL1;
import org.tc.perf.work.l2.CleanupL2;
import org.tc.perf.work.l2.SetupL2;
import org.tc.perf.work.l2.StartL2;
import org.tc.perf.work.l2.StopL2;
import org.tc.perf.work.load.CleanupLoad;
import org.tc.perf.work.load.SetupLoad;
import org.tc.perf.work.load.StartLoad;

/**
 *
 * This is the test controller which distributes the Work to required hosts and
 * checks whether the alloted task was executed successfully or not. On any
 * error/exception, it starts the cleanup process and downloads the test logs to
 * local directory. <br/>
 * <br/>
 * This is the backbone of the test framework.
 *
 * @author Himadri Singh
 * @see #run()
 */
public class MasterController extends TestFramework implements Runnable,
		Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(MasterController.class);

	private final Configuration testConfig;
	private transient ClusterWatcherExecutor cwExecutor;

	public MasterController(Configuration config) {
		this.testConfig = config;
	}

	/**
	 * Executes the whole test process for list of test configs to be executed.
	 * The test process includes <li>{@link InitialCleanup}</li> <li>
	 * {@link SetupL2}</li> <li>{@link SetupL1}</li><li>{@link StartL2}</li> <li>
	 * {@link StartL1}</li> <li>{@link StopL1}</li><li>{@link StopL2}</li> <li>
	 * {@link CleanupL1}</li><li>{@link CleanupL2}</li> <li>Collect logs and
	 * download them on local</li>
	 */
	public void run() {
		log.info("Connected agents: " + getConnectedAgents());
		try {
			org.apache.commons.io.FileUtils.forceDelete(testConfig.getLocation());
		} catch (FileNotFoundException fe) {
			// no-op
		} catch (Exception e) {
			e.printStackTrace();
		}
		FileUtils loader = new FileUtils(DataCache.getInstance(testConfig
				.getUniqueId()));

		this.cwExecutor = new ClusterWatcherExecutor();

		for (String testcase : testConfig.getAllCases()) {
			log.info(HEADER);
			log.info(" Starting Test Process...");
			log.info(HEADER);
			try {
				loader.download(testcase, testConfig.getLocation());
			} catch (IOException e1) {
				log.error("Not able to download the test config: " + testcase,
						e1);
				continue;
			}
			Configuration config = new Configuration(testConfig, testcase);
			config.setStatusRunning();
			testCache.putTest(config);

			log.info(HEADER);
			log.info(String.format(" Running test case : %s (Test ID: %s)",
					testcase, config.getUniqueId()));
			log.info(HEADER);
			log.info(config);
			try {
				initialCleanup(config);
				updateLogLocation(new File(config.getResultLocation(),
						"framework.log").getAbsolutePath());
				testSetup(config);
				runTestProcess(config);
			} catch (Exception e) {
				log.error("Testcase Failed.");
				config.setStatusFailed(e.getMessage());
				workQueue.clear();
			} finally {
				stopTest(config);
			}

			log.info(HEADER);
			log.info("TestCase Summary: ");
			log.info(config);
			log.info(HEADER);
			mailLogs(config);
			closeLogger();
		}
		log.info("All TestCases Finished.");
		DataCache.removeInstance(testConfig.getUniqueId());
	}

	/**
	 * Sends a summary of the test phase. The location of the logs is specified
	 * in the mail.
	 *
	 * @param config
	 *            test configuration
	 */
	private void mailLogs(Configuration config) {
		String smtp = config.getProps().getProperty("smtp.host");
		String recipients = config.getProps().getProperty("recipients");

		if (smtp == null || recipients == null) {
			log.warn("Mail can't be configured. Not sending report.");
			return;
		}
		Mail mail = new Mail(smtp, recipients);
		try {
			mail.send(config.getTestName() + " : " + config.getTestCase(),
					config.toString());
		} catch (MessagingException e) {
			// e.printStackTrace();
		}
	}

	private void initialCleanup(Configuration config) throws Exception {
		int timeout = config.getWorkTimeout();
		// Initial Cleanup on all the boxes
		log.info("Executing Initial Cleanup:");
		executeWork(config.getAllmachines(), new InitialCleanup(config), timeout);
	}

	private void testSetup(Configuration config) throws Exception {
		int timeout = config.getWorkTimeout();
		// Setup L2
		log.info("Setup L2 on all l2_machines: " + config.getUniqueL2Machines());
		executeWork(config.getUniqueL2Machines(), new SetupL2(config), timeout);

		// Setup L1
		log.info("Setup L1 on all l1_machines: " + config.getUniqueL1Machines());
		executeWork(config.getUniqueL1Machines(), new SetupL1(config), timeout);

		if (config.getLoadmachines().size() > 0) {
			// Setup Load
			log.info("Setup Load on all load_machines: "
					+ config.getUniqueLoadMachines());
			executeWork(config.getUniqueLoadMachines(), new SetupLoad(config), timeout);
		}
	}

	/**
	 * Execute test process. If some work fails, skip rest of the steps and
	 * start cleanup.
	 *
	 * @param config
	 *
	 * @throws Exception
	 */

	private void runTestProcess(Configuration config) throws Exception {
		int timeout = config.getWorkTimeout();
		log.info("Executing test process...");
		// Start L2
		// Sorted list to make sure first one becomes active always
		ArrayList<String> sortedList = new ArrayList<String>(config.getL2machines());
		Collections.sort(sortedList, String.CASE_INSENSITIVE_ORDER);
		for (String l2: sortedList){
			log.info("Start L2 on l2_machines: " + l2);
			executeWork(l2, new StartL2(config), timeout);
		}
		cwExecutor.start(getCWProperties(config));

		// Start l1
		log.info("Start L1 on all l1_machines: " + config.getL1machines());
		if (config.getLoadmachines().size() > 0) {
			executeWork(config.getL1machines(), new StartL1(config), timeout);
			// Start load
			log.info("Start Load on all load_machines: "
					+ config.getLoadmachines());
			executeWorkTillFinish(config.getLoadmachines(), new StartLoad(
					config));
		} else
			executeWorkTillFinish(config.getL1machines(), new StartL1(config));
	}

	private void stopTest(Configuration config){
		workQueue.clear();
		cleanup(config);
		collectLogs(config);
		if (testCache.isErrorState()){
			config.setStatusFailed(testCache.getErrorStateMessage());
			testCache.clearErrorState();
		}
		testCache.saveTest(config);
		clearAll(config, true);
	}

	private void cleanup(Configuration config) {
		int timeout = config.getWorkTimeout();
		if (cwExecutor != null)
			cwExecutor.stop();

		try {
			// Stop L1, , If no load then it should be stopped already.
			log.info("Stopping L1 on all l1_machines: "
					+ config.getL1machines());
			executeWork(config.getL1machines(), new StopL1(config), timeout);
		} catch (Exception e) {
			log.error("Stopping L1 failed: " + e.getMessage());
		}

		if (config.getLoadmachines().size() > 0) {
			try {
				log.info("Cleanup Load on all load_machines: "
						+ config.getUniqueLoadMachines());
				executeWork(config.getUniqueLoadMachines(), new CleanupLoad(
						config), timeout);
			} catch (Exception e) {
				log.error("Cleanup Load failed: " + e.getMessage());
			}
		}

		try {
			// Stop L2
			log.info("Stopping L2 on all l2_machines: "
					+ config.getL2machines());
			executeWork(config.getL2machines(), new StopL2(config), timeout);
		} catch (Exception e) {
			log.error("Stopping L2 failed: " + e.getMessage());
		}

		try {
			log.info("Cleanup L1 on all l1_machines: "
					+ config.getUniqueL1Machines());
			executeWork(config.getUniqueL1Machines(), new CleanupL1(config), timeout);
		} catch (Exception e) {
			log.error("Cleanup L1 failed: " + e.getMessage());
		}

		try {
			log.info("Cleanup L2 on all l2_machines: "
					+ config.getUniqueL2Machines());
			executeWork(config.getUniqueL2Machines(), new CleanupL2(config), timeout);
		} catch (Exception e) {
			log.error("Cleanup L2 failed: " + e.getMessage());
		}

		// Kill any lingering process started on the agent
		ProcessRegistry.getInstance().killAllProcesses();
	}

	/**
	 * Start downloading logs to the local and create a gzip file for all the
	 * logs.
	 *
	 * @param config
	 *            test configuration
	 */
	private void collectLogs(Configuration config) {
		FileUtils loader = new FileUtils(DataCache.getInstance(config
				.getUniqueId()));

		for (String l2 : config.getL2machines()){
			try {
				loader.download("server-" + l2 + LOG_EXT, config.getResultLocation());
			} catch (IOException e) {
				log.error("Failed to download logs for the test. " + e.getMessage());
			}
		}

		for (String l1 : config.getL1machines()){
			try {
				loader.download("client-" + l1 + LOG_EXT, config.getResultLocation());
			} catch (IOException e) {
				log.error("Failed to download logs for the test. " + e.getMessage());
			}
		}

		if (config.getLoadmachines().size() > 0) {
			for (String load : config.getLoadmachines()){
				try {
					loader.download("load-" + load + LOG_EXT, config.getResultLocation());
				} catch (IOException e) {
					log.error("Failed to download logs for the test. " + e.getMessage());
				}
			}
		}


		List<String> regex = Arrays.asList(".*gz", ".*csv", ".*log");
		String finalZipName = config.getTestName() + "-" + config.getTestCase()
				+ "-" + config.getUniqueId() + LOG_EXT;

		File results = new File(finalZipName);
		try {
			loader.gzipFiles(config.getResultLocation(), regex, results);
			config.setResultLog(results.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the properties which can be consumed by {@link ClusterWatcher}
	 * @param config test configuration
	 * @return cluster watcher properties
	 */

	private ClusterWatcherProperties getCWProperties(Configuration config) {
		Properties props = new Properties();
		props.putAll(config.getProps());
		StringBuilder tcConfig = new StringBuilder();
		for (String l2 : config.getL2machines())
			tcConfig.append(l2).append(":9510,");

		props.setProperty("tc-config.url", tcConfig.toString());
		props.setProperty("clientcount",
				String.valueOf(config.getL1machines().size()));
		props.setProperty(ClusterWatcherProperties.LOGS_DIR, config
				.getResultLocation().getAbsolutePath());
		return new ClusterWatcherProperties(props);
	}

	/**
	 * Lists all the registered tests running in the framework.
	 *
	 * @see BootStrap#listTests()
	 */

	public void listRunningTests() {
		List<Configuration> tests = testCache.getAllTests();
		log.info("List of running tests in the framework.\n");
		log.info(HEADER);
		log.info("S.No\tTest");
		log.info(HEADER);
		int i = 1;
		for (Configuration test : tests) {
			log.info(String.format("%d.\t%s\n", i++, test));
		}
	}

	/**
	 * Stops the test, collects the logs and download them to the local. Removes
	 * the test from running test lists.
	 *
	 * @param testUniqueId
	 *            unique id alloted to the test
	 * @see BootStrap#killTest(String)
	 */

	public void killTest(String testUniqueId) {
		Configuration config = testCache.getTest(testUniqueId);
		if (config == null) {
			log.info("Test not running for id: " + testUniqueId);
			return;
		}
		stopTest(config);
	}

	/**
	 * Clears all test running on the framework. <br/><br/>Use it CAUTIOUSLY.
	 *
	 * @see BootStrap#clearFramework()
	 */
	public void clearFramework() {
		List<Configuration> tests = testCache.getAllTests();
		log.info("Clearing all tests.\n");
		for (Configuration test : tests) {
			killTest(test.getUniqueId());
		}
		workQueue.clear();
		testCache.clear();
	}

	/**
	 * Dumps state of the perf framework.
	 *
	 * @see BootStrap#dumpState()
	 */

	public void dumpState() {
		listRunningTests();
		workQueue.dumpState();
	}
}