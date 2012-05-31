package org.tc.perf.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.tc.perf.work.Work;

/**
 * Work Queue that store list of {@link Work} that needs to be executed on
 * particular host. Work is tied to the hostname of the agents.
 *
 * @author Himadri Singh
 *
 */
public class WorkQueueCache {

	private final static Logger log = Logger.getLogger(WorkQueueCache.class);
	private final static String WORK_QUEUE = "WORK_QUEUE";
	private final Cache workQueue;

	public WorkQueueCache() {
		this.workQueue = CacheGenerator.getCache(WORK_QUEUE);
	}

	/**
	 * puts the work into the cache for particular host. If already exists, it
	 * adds the list of the work.
	 *
	 * @param host
	 *            hostname
	 * @param work
	 *            work to be executed
	 */
	public synchronized void put(final String host, final Work work) {
		List<Work> workList = get(host);
		if (workList.isEmpty())
			workList = new ArrayList<Work>();
		workList.add(work);
		workQueue.put(new Element(host, workList));
	}

	/**
	 * updates the list of the work in the current state, only if hostname
	 * exists.
	 *
	 * @param host
	 *            hostname
	 * @param workList
	 *            updated work list
	 */
	public void update(final String host, final List<Work> workList) {
		workQueue.replace(new Element(host, workList));
	}

	/**
	 * Remove the Work for this host in the work queue for error state
	 */

	public void remove(final String host) {
		log.info("Removing from work queue cache: " + host + " Success: "
				+ workQueue.remove(host));
	}

	/**
	 * clears the work queue
	 */
	public void clear() {
		workQueue.removeAll();
	}

	/**
	 * Gets work items from the common work cache for a host CopyOnRead list.
	 *
	 * @param host
	 *            The hostname for the machine is doing the work
	 *
	 * @return the Work instance for this host. If no work was found, null will
	 *         be returned.
	 */

	@SuppressWarnings("unchecked")
	public synchronized List<Work> get(final String host) {
		Element e = workQueue.get(host);
		if (e != null) {
			Serializable val = e.getValue();
			if (val instanceof List<?>)
				return new ArrayList<Work>((List<Work>) val);
			else
				throw new IllegalStateException(
						"List of work expected but got " + val.getClass());
		} else
			return Collections.emptyList();
	}

	/**
	 * Dumps the work queue cache state
	 */
	public void dumpState(){
		List<String> keys = workQueue.getKeys();
		log.info("Dumping WorkQueueCache State: ");
		for (String key : keys){
			StringBuilder sb = new StringBuilder();
			for (Work w : get(key))
				sb.append(w).append(" ");
			log.info(String.format("--> [ %s , ( %s) ]", key, sb.toString()));
		}
	}

	/**
	 * @return true if work queue is empty
	 */
	public boolean isEmpty() {
		return (workQueue.getSize() == 0);
	}

}
