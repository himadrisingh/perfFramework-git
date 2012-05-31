package org.tc.cluster.watcher.logger;
import static org.tc.cluster.watcher.util.ClusterWatcherProperties.LOG;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.management.ObjectName;

import org.tc.cluster.watcher.NotConnectedException;
import org.tc.cluster.watcher.ServerStat;

import com.tc.management.beans.TCServerInfoMBean;
import com.tc.statistics.StatisticData;
import com.tc.stats.api.DSOMBean;

public class SystemStatsLogger extends AbstractLogger {

	private static final String MEMORY_USED = "memory used";
	private static final String CPU_USAGE   = "cpu usage";
	private static final String CHANNEL_ID	= "channelID";

	public SystemStatsLogger(String sysLog){
		super(sysLog);
	}

	private int getAvgCpu(StatisticData[] stats){
		if (stats == null)
			return 0;
		double cpu = 0.0;
		for (StatisticData s : stats){
			try{
				cpu += Double.parseDouble(s.getData().toString());
			} catch (NumberFormatException e){
				e.printStackTrace();
			}
		}
		return (int) (cpu * 100/stats.length);
	}

	@Override
	ArrayList<String> getHeaders(ServerStat stat) throws NotConnectedException{
		DSOMBean dso = stat.getDsoMbean();
		TCServerInfoMBean info = stat.getInfoBean();
		Map l2Stats = info.getStatistics();

		Map<ObjectName, Map> l1Stats = dso.getL1Statistics();
		ArrayList<String> headers = new ArrayList<String>();
		for (int i = 0; i < l2Stats.keySet().size(); i++){
		headers.add("L2_CPU");
		headers.add("L2MemoryUsed");
		}
		for (ObjectName k : l1Stats.keySet()){
			String client = String.format("ClientID[%s]",k.getKeyProperty(CHANNEL_ID));
			headers.add(client + "_CPU");
			headers.add(client + "_MemoryUsed");
		}
		return headers;
	}

	@Override
	ArrayList<String> getStats(ServerStat stat) throws NotConnectedException{
		try {
			DSOMBean dso = stat.getDsoMbean();
			TCServerInfoMBean info = stat.getInfoBean();
			Map<ObjectName, Map> l1Stats = dso.getL1Statistics();
			ArrayList<String> data = new ArrayList<String>();

			Map l2Stats = info.getStatistics();
			data.add(getAvgCpu((StatisticData[])l2Stats.get(CPU_USAGE)) + "%");
			data.add(String.valueOf(l2Stats.get(MEMORY_USED)));

			Iterator<ObjectName> itr = l1Stats.keySet().iterator();
			while (itr.hasNext()){
				Map l1StatsMap = l1Stats.get(itr.next());
				if (l1StatsMap != null){
					data.add(getAvgCpu((StatisticData[])l1StatsMap.get(CPU_USAGE)) + "%");
					data.add(String.valueOf(l1StatsMap.get(MEMORY_USED)));
				}
			}
			return (data);
		}
		catch (UndeclaredThrowableException e){
			LOG.error(e.getLocalizedMessage());
		}
		return new ArrayList<String>();
	}
}
