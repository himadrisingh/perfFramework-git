/**
 *
 */
package org.tc.perf.work.l1;

import org.apache.log4j.Logger;
import org.tc.perf.process.ProcessRegistry;
import org.tc.perf.util.Configuration;

/**
 * Kill all the running client processes on the agent.
 *
 * @author gautam, Himadri Singh
 */
public class StopL1 extends AbstractL1 {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(StopL1.class);

	public StopL1(final Configuration configuration) {
		super(configuration);
	}

	@Override
	protected void work() {
		int id = runningClients.decrementAndGet();
		ProcessRegistry registry = ProcessRegistry.getInstance();
		String processName = clientProcessMap.remove(id);
		log.info("Killing process: " + processName);
		if (registry.killProcess(processName))
			log.info("Done Killing...");
		else
			log.error(processName + " Process was not killed.");
	}

}
