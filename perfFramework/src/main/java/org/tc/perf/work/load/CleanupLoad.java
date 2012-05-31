package org.tc.perf.work.load;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.tc.perf.util.Configuration;
import org.tc.perf.work.AbstractWork;

/**
 *
 * Starts cleanup process. gzips all the specified logs and uploads them to the
 * cache to be downloaded on MasterControl node.
 *
 * @author Himadri Singh
 */
public class CleanupLoad extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public CleanupLoad(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public void work() throws FileNotFoundException, IOException {
		collectLogs(configuration.getLoadLogLocation());
	}

}
