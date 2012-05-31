/**
 *
 */
package org.tc.perf.work.l1;

import static org.tc.perf.util.Utils.CLASSPATH_SEPARATOR;
import static org.tc.perf.util.Utils.FW_TC_CONFIG_URL;
import static org.tc.perf.util.Utils.HOSTNAME;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessConfig;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.SystemStatsCollector;
import org.terracotta.api.TerracottaClient;
import org.terracotta.coordination.Barrier;

/**
 * Starts the application/test/l1 process on the specified agent. Synchronizes
 * the startup using a clustered barrier. <br/>
 * <br/>
 * It also adds a unique id to each process via system property
 * <code>fw.node.id</code>.
 *
 * @author gautam, Himadri Singh
 *
 */
public class StartL1 extends AbstractL1 {

	private static final Logger log = Logger.getLogger(StartL1.class);
	private static final long serialVersionUID = 1L;

	public StartL1(final Configuration configuration) {
		super(configuration);
	}

	private ProcessConfig getProcessConfig() {

		TerracottaClient client = new TerracottaClient(FW_TC_CONFIG_URL);
		Barrier barrier = client.getToolkit().getBarrier(
				configuration.getUniqueId() + "-L1-barrier",
				configuration.getL1machines().size());

		int nodeId = 1;
		try {
			log.info("Waiting at barrier for L1s: "
					+ configuration.getL1machines().size());
			nodeId = barrier.await();
			log.info("Setting fw.node.id to " + nodeId);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}

		String mainClass = configuration.getMainClass();
		List<String> arguments = configuration.getArguments();
		List<String> fileList = getDataCache().getTestLibs();
		fileList.addAll(getDataCache().getTestKitLibs());

		StringBuilder classpath = new StringBuilder();
		for (String file : fileList) {
			classpath.append(file).append(CLASSPATH_SEPARATOR);
		}
		List<String> jvmArgs = new ArrayList<String>();
		jvmArgs.addAll(configuration.getL1_jvmArgs());
		jvmArgs.add("-Dfw.node.id=" + nodeId);
		jvmArgs.add("-Dnode-name=" + nodeId);

		ProcessConfig config = new ProcessConfig(mainClass, HOSTNAME
				+ "-client-" + nodeId);
		config.setClasspath(classpath.toString()).setArguments(arguments)
				.setLocation(configuration.getClientSetupLocation())
				.setLogsDir(configuration.getClientLogLocation())
				.setJvmArgs(jvmArgs).setJavaHome(configuration.getL1JavaHome());
		return config;
	}

	@Override
	protected void work() {
		log.info("Creating process config...");
		ProcessConfig config = getProcessConfig();
		int id = runningClients.getAndIncrement();
		if (id == 0) {
			log.info("Starting system stats collector...");
			SystemStatsCollector ssc = SystemStatsCollector.getInstance();
			ssc.updateLogLocation(configuration.getClientLogLocation());
			ssc.start();
		}

		clientProcessMap.put(id, config.getProcessName());
		if (configuration.getLoadmachines().size() > 0) {
			config.setLogSnippet(configuration.getClientLogCheck());
			log.info("loadmachines are configured. Timeout set to 120 secs to check client started successfully.");
		}
		execute(config);
	}

}
