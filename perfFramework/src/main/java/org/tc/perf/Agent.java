package org.tc.perf;

import static org.tc.perf.util.Utils.HOSTNAME;
import static org.tc.perf.util.Utils.sleepThread;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.tc.perf.cache.WorkQueueCache;
import org.tc.perf.process.ProcessState;
import org.tc.perf.work.Work;

/**
 * Agent is the process started on machines used in testing. This will take care
 * of {@link Work} alloted to this machine. It can spawn different processes and
 * is responsible for the log collection, etc.
 *
 * @author Himadri Singh
 *
 */

class Agent extends TestFramework {

	private static final Logger log = Logger.getLogger(Agent.class);
	private static final int pollPeriod = 1000;
	private static final int workTimeout = 300;
	private final Executor pool = Executors.newCachedThreadPool();

	/**
	 * This method keeps in polling {@link WorkQueueCache} for the {@link Work}
	 * alloted to this agent. The list of Work is extracted from the cache and
	 * each Work is executed in a separate thread. It updates the state of the
	 * Work i.e {@link ProcessState}.
	 *
	 * It also checks if Work is completed/aborted it should be removed in 500 *
	 * 200 ms.
	 *
	 */

	public void poll() {
		log.info("Connected Agents: " + getConnectedAgents());
		log.info(HOSTNAME + " Agent ready for work from master.");

		int i = 0;
		while (true) {
			List<Work> workList = workQueue.get(HOSTNAME);
			log.debug("Agent worklist: " + workList);
			for (Work work : workList) {
				ProcessState state = work.getState();

				/*
				 * Execute the work if not started in a separate thread.
				 */
				if (state.isNotStarted()) {
					pool.execute(new ProcessWork(work));
				}

				if ((state.isFailed() || state.isFinished()
						|| state.isTimeout() || state.isStarted())
						&& i++ > workTimeout) {
					log.error("Work is not being removed by Master. "
							+ "Seems there a failure at MasterControl? "
							+ "Clearing the Work State: " + work.getState());
					workQueue.remove(HOSTNAME);
					i = 0;
				}
			}
			/*
			 * Updates the state of the work which is checked by MasterController
			 */
			workQueue.update(HOSTNAME, workList);
			sleepThread(pollPeriod);
		}
	}

	/**
	 * Internal class to execute each Work in separate thread. This is done
	 * because each execution is blocking call so to execute multiple Work in
	 * same box needs to be executed in different threads.
	 *
	 * @author Himadri Singh
	 */
	private class ProcessWork implements Runnable {

		final Work work;

		public ProcessWork(Work work) {
			this.work = work;
		}

		public void run() {
			log.debug("Starting thread for processing work: " + work.getClass());
			/*
			 * Execute the work process
			 */
			work.doWork();
			/*
			 * If any running work fails, then mark the error state
			 * Running work should also be stopped and start cleanup
			 */
			ProcessState state = work.getState();
			if (state.isFailed())
				testCache.markErrorState(state.getFailureReason());
			log.info(work.getClass().getSimpleName() + " stopped with state " + state);
		}
	}
}
