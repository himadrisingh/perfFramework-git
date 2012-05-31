package org.tc.cluster.watcher.logger;

import java.util.ArrayList;
import java.util.Arrays;

import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

import com.tc.stats.api.DSOMBean;

public class TerracottaStatsLogger extends AbstractLogger {

	private static final String[] headers = new String[] { "LiveObjectCount",
			"WriteTxnRate", "L2DiskFaultRate", "ObjectFaultRate",
			"ObjectFlushRate", "OffheapFaultRate", "OffheapFlushRate",
			"Broadcast", "OffheapMap", "OffheapObject", "PendingTxns" };

	public TerracottaStatsLogger(String terracottaStatsLog) {
		super(terracottaStatsLog);
	}

	@Override
	ArrayList<String> getStats(ServerStat serverStat) throws NotConnectedException{
		DSOMBean bean;
		bean = serverStat.getDsoMbean();
		ArrayList<String> ArrayList = new ArrayList<String>();
		ArrayList.add(String.valueOf(bean.getLiveObjectCount()));
		ArrayList.add(String.valueOf(bean.getTransactionRate()));
		ArrayList.add(String.valueOf(bean.getL2DiskFaultRate()));
		ArrayList.add(String.valueOf(bean.getObjectFaultRate()));
		ArrayList.add(String.valueOf(bean.getObjectFlushRate()));
		ArrayList.add(String.valueOf(bean.getOffHeapFaultRate()));
		ArrayList.add(String.valueOf(bean.getOffHeapFlushRate()));
		ArrayList.add(String.valueOf(bean.getBroadcastRate()));
		ArrayList.add(String.valueOf(bean.getOffheapMapAllocatedMemory()));
		ArrayList.add(String.valueOf(bean.getOffheapObjectAllocatedMemory()));
		ArrayList.add(String.valueOf(bean.getPendingTransactionsCount()));
		return ArrayList;
	}

	@Override
	ArrayList<String> getHeaders(ServerStat stat) throws NotConnectedException {
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(headers));
		return list;
	}



}
