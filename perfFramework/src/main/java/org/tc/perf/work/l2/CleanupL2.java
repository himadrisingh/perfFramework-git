package org.tc.perf.work.l2;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.tc.perf.process.ProcessRegistry;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.SystemStatsCollector;

/**
 *
 * Starts cleanup process. gzips all the specified logs and uploads them to the
 * cache to be downloaded on MasterControl node.
 *
 * @author Himadri Singh
 */
public class CleanupL2 extends AbstractL2 {

	private static final long serialVersionUID = 1L;

	public CleanupL2(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public void work() throws FileNotFoundException, IOException {
		if (runningServers.get() > 0)
			ProcessRegistry.getInstance().killAllProcesses();
		collectLogs(configuration.getServerLogLocation());
		SystemStatsCollector.getInstance().stop();
	}

}
