package org.tc.perf.work;

import static org.tc.perf.util.Utils.HEADER;
import static org.tc.perf.util.Utils.HOSTNAME;
import static org.tc.perf.util.Utils.LOG_EXT;
import static org.tc.perf.util.Utils.deleteDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.tc.perf.cache.DataCache;
import org.tc.perf.process.ProcessConfig;
import org.tc.perf.process.ProcessState;
import org.tc.perf.process.ProcessThread;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileUtils;

/**
 * Abstract base class for work
 *
 * Base class for {@link Work} implementations. Maintains the configuration and
 * completion state of the work at hand.
 *
 * @see Work for more details.
 */

public abstract class AbstractWork implements Work {

	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(AbstractWork.class);

	protected final Configuration configuration;
	private final ProcessState state = new ProcessState();

	public AbstractWork(final Configuration configuration) {
		this.configuration = configuration;
	}

	// DataCache is not serializable
	// Nor does FileUtils
	protected DataCache getDataCache() {
		return DataCache.getInstance(configuration.getUniqueId());
	}

	/**
	 * {@inheritDoc}
	 */
	public void doWork() {
		try {
			state.markInitialized();
			work();
			if (!state.isTimeout() && !state.isFailed())
				state.markFinished();
		} catch (Exception e) {
			e.printStackTrace();
			state.markFailed(e.getMessage());
		}

	}

	/**
	 * Abstract method for actual work implementation.
	 */
	protected abstract void work() throws Exception;

	/**
	 * Set the internal state of this work as complete.
	 */
	public ProcessState getState() {
		return this.state;
	}

	protected void execute(ProcessConfig config) {
		ProcessThread thread = new ProcessThread(config, state);
		thread.run();
	}

	protected void collectLogs(final File logLocation) throws IOException {
		List<String> logRegex= configuration.getLogRegex();
		FileUtils loader = new FileUtils(getDataCache());
		File gzip = new File(logLocation + "-" + HOSTNAME + LOG_EXT);
		loader.gzipFiles(logLocation, logRegex, gzip);
		loader.uploadFile(gzip);
		if (gzip.delete() && log.isDebugEnabled())
			log.debug("Removed local tar file.");
		if (configuration.isClearLogs())
			try {
				deleteDir(logLocation);
			} catch (Exception e) {
				log.error("Not able to clear local logs: " + e.getMessage());
			}
	}

	/**
	 * Download a license file from the cache to the local filesystem
	 *
	 * @param loader
	 * @param licensePath
	 */

	protected void downloadLicense(final FileUtils loader,
			final File licensePath) {
		String licenseFileName = (new File(
				configuration.getLicenseFileLocation())).getName();
		try {
			loader.download(licenseFileName, licensePath);
		} catch (FileNotFoundException e) {
			log.warn(HEADER);
			log.warn("No license file was found.");
			log.warn("You may see failures if you use enterprise features.");
			log.warn(HEADER);
		} catch (IOException e) {
			log.warn(
					"Error downloading the license file from the cache. You may see errors if you use enterprise features.",
					e);
		}
	}

	@Override
	public String toString(){
		return this.getClass().getName() + ":" + getState();
	}
}
