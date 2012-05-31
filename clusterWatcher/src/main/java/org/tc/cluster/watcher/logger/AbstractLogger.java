package org.tc.cluster.watcher.logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

abstract class AbstractLogger implements StatsLogger {

	protected static final String SEP = " : ";
	protected static final String NL = "\n";
	private static final SimpleDateFormat format = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private ArrayList<String> headers, stats;
	private final CsvStatsLogger csv;
	private final AtomicBoolean logHeader = new AtomicBoolean(false);

	public AbstractLogger(String file) {
		csv = new CsvStatsLogger(file);
	}

	private String getTime() {
		return format.format(new Date());
	}

	public void logStats(ServerStat serverStat) throws NotConnectedException {
		if (!logHeader.get()) {
			logHeader.set(true);
			headers = getHeaders(serverStat);
			headers.add(0, "Time");
			csv.header(headers.toArray(new String[headers.size()]));
		}
		stats = getStats(serverStat);
		stats.add(0, getTime());
		csv.log(stats.toArray(new String[stats.size()]));
	}

	public String snapshot() {
		StringBuilder builder = new StringBuilder();
		Iterator<String> h = headers.iterator();
		Iterator<String> s = stats.iterator();
		for (; h.hasNext() && s.hasNext();) {
			builder.append(h.next()).append(SEP).append(s.next()).append(NL);
		}
		return builder.toString();
	}

	public void close() {
		try {
			csv.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}

	abstract ArrayList<String> getHeaders(ServerStat stat)
			throws NotConnectedException;

	abstract ArrayList<String> getStats(ServerStat stat)
			throws NotConnectedException;
}
