/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.cache;

import org.springframework.samples.petclinic.CacheEntryAdapter;

import com.terracotta.util.Stats;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractCacheWrapper<K, V> implements CacheWrapper<K, V> {

  private final Stats      readStats  = new Stats();
  private final Stats      writeStats = new Stats();

  private final AtomicInteger readCount  = new AtomicInteger();
  private final AtomicInteger writeCount = new AtomicInteger();

  abstract protected void putInCache(K key, V value, CacheEntryAdapter<V> adapter);

  public void put(K key, V value, CacheEntryAdapter<V> adapter) {
    long start = now();
    putInCache(key, value, adapter);
    long end = now();
    writeCount.incrementAndGet();
    writeStats.add(end - start);
  }

  abstract V getFromCache(K key, CacheEntryAdapter<V> adapter);

  public V get(K key, CacheEntryAdapter<V> adapter) {
    long start = now();
    V value = getFromCache(key, adapter);
    long end = now();
    readCount.incrementAndGet();
    readStats.add(end - start);
    return value;
  }

  private static long now() {
    return System.currentTimeMillis();
  }

  public Stats getReadStats() {
    return readStats;
  }

  public Stats getWriteStats() {
    return writeStats;
  }

  public int getReadCount() {
    return readCount.get();
  }

  public int getWriteCount() {
    return writeCount.get();
  }

  public void reset(){
    readCount.set(0);
    writeCount.set(0);
    readStats.reset();
    writeStats.reset();
  }

}
