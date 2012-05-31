package org.tc.perf.work;

import org.tc.perf.process.ProcessRegistry;
import org.tc.perf.util.Configuration;

/**
 * Work that kills all the process running on alloted host.
 *
 * @author Himadri Singh
 *
 */
public class AbortWork extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public AbortWork(Configuration configuration) {
		super(configuration);
	}

	@Override
	protected void work() {
		ProcessRegistry.getInstance().killAllProcesses();
	}

}
