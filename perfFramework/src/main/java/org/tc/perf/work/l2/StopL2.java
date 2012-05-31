package org.tc.perf.work.l2;

import static org.tc.perf.util.Utils.HOSTNAME;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessRegistry;
import org.tc.perf.util.Configuration;

/**
 *
 * Work item to stop a terracotta server. Builds the classpath and stops the L2
 * server. Keeps count of servers stopped on a box and keeps on incrementing the
 * port in server name i.e. HOSTNAME-PORT.
 *
 * @author Himadri Singh
 */

public class StopL2 extends AbstractL2 {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(StopL2.class);

	public StopL2(final Configuration configuration) {
		super(configuration);
	}

	private ProcessConfig getProcessConfig(String serverName) {
		File tcInstallDir = new File(getDataCache().getTcInstallDir());

		ArrayList<String> jvmArgs = new ArrayList<String>(
				configuration.getL2_jvmArgs());
		jvmArgs.add("-Dtc.install-root=" + tcInstallDir);

		ArrayList<String> args = new ArrayList<String>();
		args.add("-f");
		args.add("tc-config.xml");
		args.add("-n");
		args.add(serverName);

		ProcessConfig config = new ProcessConfig(tcStopMainClass, serverName + "-stop");
		config.setClasspath(
				new File(tcInstallDir, tcClasspath).getAbsolutePath())
				.setLocation(tcInstallDir)
				.setLogsDir(configuration.getServerLogLocation())
				.setJavaHome(configuration.getL2_javaHome())
				.setLogSnippet(tcStopLogSnippet)
				.setArguments(args);
		return config;
	}

	@Override
	public void work() {
		try {
			int i = runningServers.decrementAndGet();
			if (i < 0){
				 log.fatal("Running servers can't be -ve. Server never got started!");
				 runningServers.set(0);
			}
			String serverName = HOSTNAME + "-" + (9520 + i);
			ProcessRegistry.getInstance().killProcess(serverName);

			// FIXME: some problem with server shutdown, returning exit code 1 unnecessarily
			//			ProcessConfig config = getProcessConfig(serverName);
			//			execute(config);
		} catch (NullPointerException e) {
			if (e.getMessage().contains("TC_INSTALL_DIR"))
				log.warn("Terracotta Kit not found: " + e.getMessage());
		}
	}

}
