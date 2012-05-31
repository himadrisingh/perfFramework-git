package org.tc.perf.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.StreamCopier;
import org.tc.perf.util.Utils;

/**
 *
 * Starts the process in a separate thread.{@link ProcessRegistry} keeps the
 * list of the ProcessThread started on the agent.
 *
 * Restarts the process if crashing is enabled
 * {@link Configuration#getCrashIntervals(String)}
 *
 * @author Himadri Singh
 */
public class ProcessThread implements Runnable {

	private static final Logger Log = Logger.getLogger(ProcessThread.class);

	private final ProcessRegistry registry = ProcessRegistry.getInstance();

	private final ProcessConfig config;
	private Process p;
	private final ProcessState state;
	private final AtomicBoolean destroyed = new AtomicBoolean(Boolean.FALSE);
	private final int totalCrashes;
	private int crashCount = 0;

	public ProcessThread(final ProcessConfig config, final ProcessState state) {
		this.config = config;
		this.state = state;
		this.totalCrashes = config.getCrashRepeatCount()
				* config.getCrashIntervals().size();
	}

	private List<String> getCmdList() {
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(config.getJavaHome() + "/bin/java");
		cmdList.addAll(config.getJvmArgs());
		cmdList.addAll(config.getDefaultArgs());
		cmdList.add("-cp");
		cmdList.add(quoteIfNeeded(config.getClasspath()));
		cmdList.add(config.getMainClass());
		cmdList.addAll(config.getArguments());
		return cmdList;
	}

	/**
	 * Merges <code>InputStream</code> to <code>OutputStream</code>, thus
	 * recording the process output to desired stream, here, to a file.
	 *
	 * @param in
	 *            InputStream to be copied
	 * @param out
	 *            OutputStream to which data needs to be copied to.
	 */
	public void merge(final InputStream in, final OutputStream out) {
		StreamCopier sc = new StreamCopier(in, out, state);
		sc.setLogSnippet(config.getLogSnippet());
		sc.start();
	}

	/**
	 * returns the ProcessState of the process. Needed to check whether process
	 * started as expected or not.
	 *
	 * @return ProcessState of the process
	 */

	public ProcessState getState() {
		return state;
	}

	/**
	 * Destroy the process running in the <code>ProcessThread</code>. It marks
	 * the process is being destroyed intentionally which maintains the state of
	 * the process i.e. process finished not failed.
	 */

	public void destroy() {
		if (p == null)
			return;
		Log.info("Destroying the process: " + getCmdList());
		destroyed.set(Boolean.TRUE);
		p.destroy();
	}

	private static String quoteIfNeeded(final String path) {
		if (path.indexOf(" ") > 0) {
			return "\"" + path + "\"";
		}
		return path;
	}

	/**
	 * create backup of the file is exists, so that the new process dont
	 * overwrite the old file. Used to create back up for console logs and
	 * verbose gc logs of the previous process.
	 *
	 * @param log
	 */
	private void createBackupIfExists(File log) {
		if (log.exists()) {
			String name = String.format("%d-%s", System.currentTimeMillis(),
					log.getName());
			File old = new File(log.getParentFile(), name);
			Log.info(String.format("Backing up %s to %s", log.getName(),
					old.getName()));
			try {
				FileUtils.copyFile(log, old);
			} catch (IOException e) {
				Log.error("Not able create the copy of the file: "
						+ log.getName());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Process is started using {@link Runtime#exec(String)}. A separate
	 * {@link CrashThread} is started if crashing is enabled. The output streams
	 * are copied into a file using {@link StreamCopier}. {@link ProcessState}
	 * is marked STARTED if {@link ProcessConfig#getLogSnippet()} is found in
	 * the logs.
	 *
	 * @param crashInterval
	 *            interval in secs after which process needs to be
	 *            stopped/crashed.
	 * @return process exit code
	 * @throws IOException
	 * @throws InterruptedException
	 */

	private int startProcess(int crashInterval) throws IOException,
			InterruptedException {
		List<String> cmdList = getCmdList();
		Log.info("Process command: " + cmdList);
		FileUtils.forceMkdir(config.getLogsDir());
		p = Runtime.getRuntime().exec(cmdList.toArray(new String[0]), null,
				config.getLocation());

		Thread crash = null;
		if (crashCount < totalCrashes && crashInterval > 0) {
			crash = new Thread(new CrashThread(p, config, crashInterval),
					"CrashThread");
			crash.start();
		}

		createBackupIfExists(config.getConsoleLog());
		createBackupIfExists(config.getVerboseGcLog());

		FileOutputStream fos = new FileOutputStream(config.getConsoleLog());
		merge(p.getInputStream(), fos);
		merge(p.getErrorStream(), fos);

		state.markInitialized();
		Log.debug("Log check: " + config.getLogSnippet());

		int returnCode = p.waitFor();
		IOUtils.closeQuietly(p.getInputStream());
		IOUtils.closeQuietly(p.getOutputStream());
		IOUtils.closeQuietly(p.getErrorStream());
		IOUtils.closeQuietly(fos);
		p.destroy();
		Log.info(config.getProcessName() + " exited with code: " + returnCode);
		if (crash != null) {
			crash.interrupt();
		}
		return returnCode;
	}

	/**
	 * Do not restart if no crash intervals are specified or if process is
	 * crashed for config.getCrashIntervals().size() *
	 * config.getCrashRepeatCount()
	 *
	 * crashRepeatCount -ve means that it will be repeated indefinitely.
	 *
	 * @return
	 */
	private int getNextCrashInterval() {
		int size = config.getCrashIntervals().size();
		if (size == 0 || crashCount == size * config.getCrashRepeatCount())
			return -1;
		return config.getCrashIntervals().get(crashCount % size);
	}

	/**
	 * Restart the process, config restart is enabled or returnCode = 11
	 * (terracotta server zap request). Do not restart if exit code == 2 or 1
	 * even if config restart
	 *
	 * @param returnCode
	 * @return
	 */

	private boolean doRestart(int returnCode) {
		switch (returnCode) {
		case 11:
			return Boolean.TRUE;
		case 2:
		case 1:
			return Boolean.FALSE;
		}

		// Crash count should be incremented only when its being crashed without
		// exit code 11
		crashCount++;
		Log.info(String.format("Crash count: %d Total crashes: %d", crashCount,
				totalCrashes));
		if (totalCrashes == 0)
			return Boolean.FALSE;
		return !destroyed.get()
				&& registry.isRegistered(config.getProcessName())
				&& (crashCount <= totalCrashes);
	}

	public void run() {
		try {
			registry.register(config.getProcessName(), this);
			int returnCode = startProcess(getNextCrashInterval());

			if (doRestart(returnCode)) {
				Log.info("Restarting server in 5 secs.");
				Utils.sleepThread(5000);
				run();
			} else if (returnCode == 0 || destroyed.get()) {
				state.markFinished();
			} else {
				state.markFailed(String.format("%s exited with %d exit-code.",
						getCmdList(), returnCode));
				Log.info(String.format("%s exited with %d exit-code.",
						getCmdList(), returnCode));
			}
		} catch (Exception e) {
			state.markFailed(e.getMessage());
		}
		registry.unregister(config.getProcessName());
	}

	public ProcessConfig getProcessConfig() {
		return config;
	}

	/**
	 * Crash thread which kills the process running to simulate the server
	 * crashes for HA tests.
	 *
	 * @author Himadri Singh
	 *
	 */
	private class CrashThread implements Runnable {

		private static final int SEC = 1000;
		private final Process p;
		private final ProcessConfig config;
		private final int crashInterval;

		public CrashThread(Process process, ProcessConfig config,
				int crashInterval) {
			this.p = process;
			this.config = config;
			this.crashInterval = crashInterval;
		}

		public void run() {
			Log.info("Starting crash thread to crash the process after "
					+ crashInterval + " secs.");
			try {
				Thread.sleep(crashInterval * SEC);
			} catch (InterruptedException e) {
				Log.info("Crash thread exited.");
				return;
			}
			Log.info("Crash thread destroying the process " + config);
			p.destroy();
		}

	}

}