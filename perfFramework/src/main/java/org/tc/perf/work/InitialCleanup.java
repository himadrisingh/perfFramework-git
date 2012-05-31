package org.tc.perf.work;

import static org.tc.perf.util.Utils.deleteDir;

import java.io.IOException;

import org.tc.perf.util.Configuration;

/**
 * Cleans up the local directory to remove any previous test artifacts or logs.
 *
 * @author Himadri Singh
 */
public class InitialCleanup extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public InitialCleanup(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public void work() throws IOException {
		deleteDir(configuration.getLocation());
	}
}
