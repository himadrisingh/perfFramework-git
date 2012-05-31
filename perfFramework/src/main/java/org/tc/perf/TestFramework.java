package org.tc.perf;

import static org.tc.perf.util.Utils.FW_TC_CONFIG_URL;
import static org.tc.perf.util.Utils.sleepThread;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.tc.perf.cache.DataCache;
import org.tc.perf.cache.TestCache;
import org.tc.perf.cache.WorkQueueCache;
import org.tc.perf.process.ProcessState;
import org.tc.perf.util.Configuration;
import org.tc.perf.work.AbortWork;
import org.tc.perf.work.Work;
import org.terracotta.api.TerracottaClient;
import org.terracotta.cluster.ClusterInfo;
import org.terracotta.cluster.ClusterNode;

/**
 *
 * Abstract class which creates all the distributed cache required
 * by the framework. It also maintains the work queue and access to the test
 * data.
 *
 * @author Himadri Singh
 * @see WorkQueueCache
 * @see DataCache
 * @see TestCache
 */
abstract class TestFramework {

	private static final Logger log = Logger.getLogger(TestFramework.class);

	/*
	 * Not using toolkit BlockingQueue as we need to share our custom class Work
	 * and toolkit allows only literals. We can hydrate/dehydrate to make it
	 * work
	 */

	protected final WorkQueueCache workQueue;
	protected final TestCache testCache;

	public TestFramework() {
		this.workQueue = new WorkQueueCache();
		this.testCache = new TestCache();
	}

	/**
	 * Clears the work queue and aborts the all the current tasks.
	 *
	 * @param test
	 *            test configuration
	 * @param abort
	 *            if abort is true, it will the processes running on the agents
	 *            too
	 * @see AbortWork
	 */
	protected void clearAll(Configuration test, boolean abort) {
		Set<String> hosts = test.getAllmachines();
		// Clear the work queue first.
		log.info("Clearing work queue...");
		for (String h : hosts) {
			workQueue.remove(h);
			if (abort)
				workQueue.put(h, new AbortWork(Configuration.EMPTY));
		}
		try {
			waitForWorkCompletion(hosts, test.getWorkTimeout());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds work items to the {@link WorkQueueCache} for a host and waits for
	 * its completion.
	 *
	 * @param hosts
	 *            The list of hostnames of the {@link Agent} that will execute
	 *            the work
	 * @param work
	 *            The work to be executed
	 * @param timeout
	 *            time after which work will timeout and mark it as a failure
	 *            case.
	 * @see #waitForWorkCompletion(Collection, int)
	 */

	protected void executeWork(final Collection<String> hosts, final Work work, int timeout)
			throws Exception {
		for (String host : hosts)
			workQueue.put(host, work);
		waitForWorkCompletion(hosts, timeout);
	}

	/**
	 * Adds work items to the {@link WorkQueueCache} for a host and waits for
	 * its completion.
	 *
	 * @param host
	 *            The hostname of the {@link Agent} that will execute
	 *            the work
	 * @param work
	 *            The work to be executed
	 * @param timeout
	 *            time after which work will timeout and mark it as a failure
	 *            case.
	 * @see #waitForWorkCompletion(Collection, int)
	 */

	protected void executeWork(final String host, final Work work, int timeout)
			throws Exception {
		List<String> hosts = new ArrayList<String>();
		hosts.add(host);
		executeWork(hosts, work, timeout);
	}

	/**
	 * Adds work items to the {@link WorkQueueCache} for a host and waits for
	 * its completion. No timeout is set, it will wait till the process finishes
	 * or crashes.
	 *
	 * @param hosts
	 *            The list of hostnames of the {@link Agent} that will execute
	 *            the work
	 * @param work
	 *            The work to be executed
	 * @see #waitForWorkCompletion(Collection, int)
	 */

	protected void executeWorkTillFinish(final Collection<String> hosts,
			final Work work) throws Exception {
		for (String host : hosts)
			workQueue.put(host, work);
		waitForWorkCompletion(hosts, -1);
	}

	/**
	 * Check to make sure that the new test machines doesnt overlaps with the
	 * previous one.
	 *
	 * @throws IllegalStateException
	 *             if being used by any other test
	 */

	protected void checkForUsedAgents(Configuration config) {
		Set<String> superList = config.getAllmachines();

		List<Configuration> tests = testCache.getAllTests();
		for (Configuration test : tests) {
			if (!test.isRunning())
				continue;

			Set<String> machinesUsed = test.getAllmachines();
			machinesUsed.retainAll(superList);
			if (machinesUsed.size() > 0) {
				throw new IllegalStateException(machinesUsed
						+ " is/are being used by test id: "
						+ test.getUniqueId());
			}
		}
	}

	/**
	 * Checks for connected Agents
	 *
	 * @throws IllegalStateException
	 *             if required agents are not connected
	 */

	protected void checkConnectedAgents(Configuration config) {
		Collection<String> agents = getConnectedAgents();
		if (agents.isEmpty()) {
			throw new IllegalStateException(
					"No Agents are found connected to f/w terracotta server: "
							+ FW_TC_CONFIG_URL
							+ ". Are you sure Agents are running?");
		}

		List<String> superList = new ArrayList<String>(config.getAllmachines());
		if (superList.removeAll(agents) && superList.size() > 0) {
			throw new IllegalStateException(
					"Not all agents required by this test are connected to fw server: "
							+ FW_TC_CONFIG_URL + ".\n\tMissing Agents List: "
							+ superList + "\n\tConnected Agents List: "
							+ agents);
		}
	}

	/**
	 * Returns the list of connected agents to <code>FW_TC_CONFIG_URL</code>
	 *
	 * @return {@link List} list of connected agents
	 */

	protected List<String> getConnectedAgents() {
		ClusterInfo info = (new TerracottaClient(FW_TC_CONFIG_URL)).getToolkit()
				.getClusterInfo();
		Collection<ClusterNode> listOfAgents = info.getClusterTopology()
				.getNodes();

		// Remove Master Node from the agent lists
		listOfAgents.remove(info.getCurrentNode());

		List<String> agentNames = new ArrayList<String>();
		for (ClusterNode agent : listOfAgents) {
			try {
				agentNames
						.add((agent.getAddress().getCanonicalHostName().toLowerCase()));
				log.debug("Agents found: "
						+ agent.getAddress().getCanonicalHostName().toLowerCase());
			} catch (UnknownHostException e) {
				log.error("Unknown Agent found " + agent, e);
			}
		}
		return agentNames;
	}

	/**
	 * Wait for work to finish/fail/timeout on the list of hosts. It keeps on
	 * polling {@link WorkQueueCache} for the {@link Work} alloted to the hosts.
	 * {@link Agent} will be executing the work and updating the
	 * {@link ProcessState} of the work, which is monitored here. <br/>
	 * Once each work reaches in started or finished state it moves to the next
	 * phase.<br/>
	 * Timeout and failed state throws Exception.
	 *
	 * @param hosts
	 *            The list of hosts to monitor.
	 * @param timeoutInSecs
	 *            A timeout in seconds for the jobs to complete. timeoutInSecs =
	 *            -1 for Infinite wait.
	 *
	 * @throws TimeoutException
	 *             If the work doesn't complete within the specified timeout
	 *
	 * @throws Exception
	 *             If work running on agent fails with some exception
	 */
	private void waitForWorkCompletion(final Collection<String> hosts,
			final int timeoutInSecs) throws Exception {
		TimeUnit.SECONDS.sleep(2);

		List<String> pendingHosts = new ArrayList<String>(hosts);
		Calendar timeout = Calendar.getInstance();
		timeout.add(Calendar.SECOND, timeoutInSecs);

		// TODO: Check why worklist are not updated
		// checking error state only when executeWorkTillFinish is called.
		while (!pendingHosts.isEmpty() && !workQueue.isEmpty()
				&& !(testCache.isErrorState() && timeoutInSecs < 0)) {

			for (String host : hosts) {
				List<Work> workList = workQueue.get(host);
				Iterator<Work> iter = workList.iterator();

				while (iter.hasNext()) {
					Work work = iter.next();
					ProcessState state = work.getState();
					log.debug(work + " : " + state);
					if (state.isStarted() || state.isFinished()) {
						iter.remove();
					}

					if (state.isTimeout()) {
						workQueue.remove(host);
						throw new TimeoutException("Test job failed since "
								+ host + " didnt completed the task due to "
								+ state.getFailureReason());
					}
					if (state.isFailed()) {
						workQueue.remove(host);
						throw new Exception("Job execution Failed. Reason: "
								+ state.getFailureReason());
					}
				}
				if (workList.isEmpty()) {
					workQueue.remove(host);
					pendingHosts.remove(host);
					if (pendingHosts.size() > 0){
						log.debug("===========================================");
						for (String h: pendingHosts)
							log.debug("Pending hosts: " + h + " : " + workQueue.get(h));
						log.debug("===========================================");
					}
				}
				sleepThread(1000);
			}
		}

		if (testCache.isErrorState() && timeoutInSecs < 0) {
			for (String host : hosts)
				workQueue.remove(host);
		}

		if (timeoutInSecs > 0 && Calendar.getInstance().after(timeout)) {
			log.error("Timed out waiting for jobs to finish on: " + hosts);
			throw new TimeoutException("Exceeded timeout of " + timeoutInSecs
					+ " seconds");
		}
	}
}
