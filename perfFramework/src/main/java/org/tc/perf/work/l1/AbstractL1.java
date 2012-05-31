package org.tc.perf.work.l1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.tc.perf.util.Configuration;
import org.tc.perf.work.AbstractWork;

public abstract class AbstractL1 extends AbstractWork {

	private static final long serialVersionUID = 1L;

	protected static final AtomicInteger runningClients = new AtomicInteger();
	protected static final Map<Integer, String> clientProcessMap = new ConcurrentHashMap<Integer, String>();


	public AbstractL1(Configuration configuration) {
		super(configuration);
	}


}
