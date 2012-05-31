/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.cache;

import com.terracotta.util.Stats;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheStatsProcessor implements CacheProcessor {

  private static CacheStatsProcessor _processor = new CacheStatsProcessor();

  private final AtomicInteger read, write;
  private final Stats readStat, writeStat;

  private CacheStatsProcessor(){
    read = new AtomicInteger();
    write = new AtomicInteger();

    readStat = new Stats();
    writeStat = new Stats();
  }

  public static CacheStatsProcessor getInstance(){
    return _processor;
  }

  public void processCache(CacheWrapper cacheWrapper) {
    if (cacheWrapper instanceof AbstractCacheWrapper){
      AbstractCacheWrapper wrapper = (AbstractCacheWrapper) cacheWrapper;
      read.addAndGet(wrapper.getReadCount());
      write.addAndGet(wrapper.getWriteCount());
      readStat.add(wrapper.getReadStats());
      writeStat.add(wrapper.getWriteStats());
    }
  }

  public int getRead() {
    return read.get();
  }

  public int getWrite() {
    return write.get();
  }

  public Stats getReadStat() {
    return readStat;
  }

  public Stats getWriteStat() {
    return writeStat;
  }

  public void reset(){
    read.set(0);
    write.set(0);
    readStat.reset();
    writeStat.reset();
  }

}