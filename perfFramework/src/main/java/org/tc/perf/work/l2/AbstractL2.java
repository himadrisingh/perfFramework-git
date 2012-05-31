package org.tc.perf.work.l2;

import java.util.concurrent.atomic.AtomicInteger;

import org.tc.perf.process.ProcessRegistry;
import org.tc.perf.process.ProcessThread;
import org.tc.perf.util.Configuration;
import org.tc.perf.work.AbstractWork;

public abstract class AbstractL2 extends AbstractWork {

	private static final long serialVersionUID = 1L;

	/**
	 * Register terracotta server process configs to this registry. When
	 * stopping servers, unregister with {@link ProcessRegistry} too as
	 * it is also checked, if server restarts are enabled.
	 *
	 * @see ProcessThread for server restarts.
	 */
	protected static final AtomicInteger runningServers = new AtomicInteger();

	protected static final String tcClasspath = "/lib/tc.jar";
	protected static final String tcMainClass = "com.tc.server.TCServerMain";
	protected static final String tcStopMainClass = "com.tc.admin.TCStop";
	protected static final String tcStartLogSnippet = "Available Max Runtime";
	protected static final String tcStopLogSnippet = "stopping it";

	public AbstractL2(Configuration configuration) {
		super(configuration);
	}

}
