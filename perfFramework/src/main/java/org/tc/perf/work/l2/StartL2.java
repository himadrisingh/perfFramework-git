/**
 *
 */
package org.tc.perf.work.l2;

import static org.tc.perf.util.Utils.HOSTNAME;

import java.io.File;
import java.util.ArrayList;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.SystemStatsCollector;

/**
 *
 * Work item to start a terracotta server. Builds the classpath and starts the
 * L2 server. Keeps count of servers started on a box and keeps on incrementing
 * the port in server name i.e. HOSTNAME-PORT Thus, no each server on same box
 * can be started with unique name. PORT number starts from 9520.
 *
 * @author Himadri Singh
 */
public class StartL2 extends AbstractL2 {

	private static final long serialVersionUID = 1L;

	public StartL2(final Configuration configuration) {
		super(configuration);
	}

	private ProcessConfig getProcessConfig(String serverName) {
		File tcInstallDir = new File(getDataCache().getTcInstallDir());

		ArrayList<String> jvmArgs = new ArrayList<String>(
				configuration.getL2_jvmArgs());
		jvmArgs.add("-Dtc.install-root=" + tcInstallDir.getAbsolutePath());

		ArrayList<String> args = new ArrayList<String>();
		args.add("-f");
		args.add("tc-config.xml");
		args.add("-n");
		args.add(serverName);

		ProcessConfig config = new ProcessConfig(tcMainClass, serverName)
				.setClasspath(
						new File(tcInstallDir, tcClasspath).getAbsolutePath())
				.setArguments(args)
				.setLocation(tcInstallDir)
				.setJvmArgs(jvmArgs)
				.setLogsDir(configuration.getServerLogLocation())
				.setJavaHome(configuration.getL2_javaHome())
				.setLogSnippet(tcStartLogSnippet)
				.setCrashIntervals(configuration.getCrashIntervals(serverName))
				.setCrashRepeatCount(
						configuration.getCrashRepeatCount(serverName));
		return config;
	}

	@Override
	public void work() {
		SystemStatsCollector ssc = SystemStatsCollector.getInstance();
		ssc.updateLogLocation(configuration.getServerLogLocation());
		ssc.start();

		String serverName = HOSTNAME + "-" + (9520 + runningServers.getAndIncrement());
		ProcessConfig config = getProcessConfig(serverName);
		execute(config);
	}
}
