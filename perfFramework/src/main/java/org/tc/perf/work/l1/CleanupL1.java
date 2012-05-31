package org.tc.perf.work.l1;

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
public class CleanupL1 extends AbstractL1 {

	private static final long serialVersionUID = 1L;

	public CleanupL1(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public void work() throws IOException {
		if (clientProcessMap.size() > 0){
			for (String processName : clientProcessMap.values())
				ProcessRegistry.getInstance().killProcess(processName);
			clientProcessMap.clear();
		}
		collectLogs(configuration.getClientLogLocation());
		SystemStatsCollector.getInstance().stop();
	}

}
