package org.tc.perf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.tc.perf.cache.DataCache;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileUtils;
import org.tc.perf.work.ControllerWork;

/**
 *
 * Master class thats responsible for loading all the test artifacts to the
 * distributed cache and initiating the test process. It adds the
 * {@link MasterController} work to one of the agents.
 *
 * @author Himadri Singh
 */

public class Master extends TestFramework {

	private static Logger log = Logger.getLogger(Master.class);
	private final Configuration config;
	private DataCache data;

	public Master(Properties props) {
		this.config = new Configuration(props);
	}

	/**
	 * Loads test configs which are to run in the tests. These will be
	 * downloaded by {@link MasterController} on the local and reloaded for the
	 * each test case separately.
	 */

	private void loadTestCaseConfigs() {
		List<String> cases = config.getAllCases();
		if (cases.size() == 0) {
			throw new IllegalStateException("No test cases to run!!");
		}
		FileUtils loader = new FileUtils(data);
		for (String testCase : cases) {
			File file = new File(testCase);
			log.info("Loading test case: " + testCase);
			try {
				if (file.isDirectory()) {
					loader.uploadDirs(file, ".*properties");
				} else
					loader.uploadFile(new File(config.getRootdir(), testCase));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * It uploads the test jars, license and the terracotta kit specified in the
	 * configuration to the {@link DataCache}
	 */
	private void uploadTestJars() {
		log.info("Uploading jars/files ...");
		FileUtils loader = new FileUtils(data);
		List<String> include = config.getClasspathRegex();
		List<String> exclude = config.getClasspathExclude();
		List<String> l1Lib = null, loadLib = null;
		try {
			String location = config.getKitLocation();
			File kit = new File(location);
			log.info("Uploading terracotta kit: " + kit.getAbsolutePath());
			loader.uploadFile(kit);
			data.setKitName(kit.getName());

			File license = new File(config.getLicenseFileLocation());
			if (license.exists())
				loader.uploadFile(license);
			else
				log.warn("***** License file NOT found. EE version might not work. *****");

			log.info("Uploading test libs...");
			l1Lib = loader.uploadDirectories(config.getTestDirectories(),
					include, exclude);
			if (config.getLoadmachines().size() > 0) {
				log.info("Uploading load libs...");
				loadLib = loader.uploadDirectories(config.getLoadDirectories(),
						include, exclude);
			}
		} catch (IOException e) {
			throw new RuntimeException(
					"Failed to upload test artifacts to cache.", e);
		}
		data.setTestLibs(l1Lib);
		data.setLoadLibs(loadLib);
		log.info("Finished uploading all the required files.");
	}

	/**
	 * This method performs the basic checks and starts the test process. It
	 * adds the {@link ControllerWork} work to first l2 machine. That machine
	 * will act as the controller to the test. Master process can exit without
	 * worrying about the test. <br/>
	 * <br/>
	 * If {@link Configuration#isInline()} true, controller work will be
	 * executed by this JVM only.
	 */

	public void run() {
		log.info("#### Starting Test ####");
		checkConnectedAgents(config);
		checkForUsedAgents(config);
		data = DataCache.getInstance(config.getUniqueId());

		uploadTestJars();
		loadTestCaseConfigs();
		try {
			ControllerWork control = new ControllerWork(config);
			if (config.isInline())
				control.doWork();
			else {
				String host = config.getMasterController();
				executeWork(host, control, config.getWorkTimeout());
				log.info(String
						.format("Test has been initiated. %s would be controlling the test. ",
								host));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
