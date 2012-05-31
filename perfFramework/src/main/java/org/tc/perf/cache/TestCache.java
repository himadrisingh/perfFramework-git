package org.tc.perf.cache;

import static org.tc.perf.util.Utils.HOSTNAME;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.tc.perf.BootStrap;
import org.tc.perf.MasterController;
import org.tc.perf.util.Configuration;

/**
 * The cache which stores the current running tests and saved test results for a
 * day which can be listed by {@link BootStrap#listTests()}
 *
 * @author Himadri Singh
 *
 */

public class TestCache {

	private final static String RUNNING_TESTS = "RUNNING_TESTS";
	private final static int DAY = 60 * 60 * 24;
	private final static String ERROR = "ERROR";

	/**
	 * Returns the cache containing the running tests unique id and machines
	 * used by them.
	 *
	 * @return cache containing running tests
	 */

	private final Cache testCache;

	public TestCache() {
		this.testCache = CacheGenerator.getCache(RUNNING_TESTS);

	}

	/**
	 *
	 * @param uniqueId unique test id
	 * @return Configuration for the test id
	 */
	public Configuration getTest(String uniqueId) {
		Element e = testCache.get(uniqueId);
		if (e != null) {
			return (Configuration) e.getValue();
		}
		return null;
	}

	/**
	 * Stores the configuration to be used for the test.
	 * Key being {@link Configuration#getUniqueId()}
	 * @param config Configuration that needs be stored
	 */
	public void putTest(Configuration config) {
		if (config == null)
			return;

		testCache.put(new Element(config.getUniqueId(), config));
	}

	/**
	 * Saves test which can be listed by
	 * {@link MasterController#listRunningTests()} Saved test stays in the
	 * framework for a day.
	 *
	 * @param config Test configuration
	 */

	public void saveTest(Configuration config) {
		if (config == null)
			return;
		// Removing for the key test unique id
		testCache.remove(config.getUniqueId());
		// Saving with unique result name
		Element e = new Element(config.getResultLog(), config);
		e.setTimeToLive(DAY);
		testCache.put(e);
	}

	/**
	 *
	 * @return list of tests in the framework
	 */
	public List<Configuration> getAllTests() {
		List<Configuration> list = new ArrayList<Configuration>();

		@SuppressWarnings("unchecked")
		List<String> tests = testCache.getKeys();
		for (String test : tests){
			Object obj = testCache.get(test).getValue();
			if (obj instanceof Configuration)
				list.add((Configuration) testCache.get(test).getValue());
			else
				System.out.println(test + " : " + obj.toString());
		}

		return list;
	}

	public boolean isErrorState(){
		Element e = testCache.get(ERROR);
		return (e != null);
	}

	public String getErrorStateMessage() {
		Element e = testCache.get(ERROR);
		if (e != null){
			return (String) e.getValue();
		}
		throw new IllegalStateException("Not in error state!");
	}

	public void markErrorState(String msg){
		testCache.put(new Element(ERROR, msg + " on " + HOSTNAME));
	}

	public void clearErrorState() {
		testCache.remove(ERROR);
	}

	public void clear() {
		testCache.removeAll();
	}

}
