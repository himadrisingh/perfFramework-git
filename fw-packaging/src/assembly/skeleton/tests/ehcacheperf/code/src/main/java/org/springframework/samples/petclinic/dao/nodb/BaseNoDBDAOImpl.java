package org.springframework.samples.petclinic.dao.nodb;

import net.sf.ehcache.Cache;

import org.springframework.samples.petclinic.BaseEntity;
import org.springframework.samples.petclinic.CacheEntryAdapter;
import org.springframework.samples.petclinic.dao.Dao;

import com.terracotta.cache.CacheProcessor;
import com.terracotta.cache.CacheWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public abstract class BaseNoDBDAOImpl<K, V extends BaseEntity> implements Dao<K, V> {

  protected CacheWrapper<String, V> cache;
  protected CacheEntryAdapter<V>    adapter;
  protected int                     keyPaddingInBytes = 0;
  protected int                     valuePaddingInBytes = 0;
  protected boolean                 variableValue = false;
  private final Random              rnd = new Random();

  public BaseNoDBDAOImpl() {
    super();
  }

  public abstract void setCacheEntryAdapter();

  public void setVariableValue(String variableValue) {
    this.variableValue = Boolean.parseBoolean(variableValue);
  }

  private String getString(int b){
    /*
     * http://www.javamex.com/tutorials/memory/string_memory_usage.shtml
     * Minimum String memory usage (bytes) = 8 * (int) ((((no chars) * 2) + 45) / 8)
     */
    int no_of_chars = b / 2;

    byte[] bytes = new byte[no_of_chars];
    for (int i = 0; i < no_of_chars; i++) {
      bytes[i] = 0x32;
    }
    return new String(bytes);
  }

  protected String getPaddingString(int paddingInBytes){
    paddingInBytes = (variableValue) ? (paddingInBytes / 2) + rnd.nextInt(paddingInBytes + 1) : paddingInBytes;
    return getString(paddingInBytes);
  }

  public void setKeyPaddingInBytes(int keyPaddingInBytes) {
    this.keyPaddingInBytes = keyPaddingInBytes;
  }

  public void setValuePaddingInBytes(int valuePaddingInBytes) {
    this.valuePaddingInBytes = valuePaddingInBytes;
  }

  public void setCache(final CacheWrapper<String, V> cache) {
    this.cache = cache;
    setCacheEntryAdapter();
  }

  public void putInCache(V value) {
    cache.put(generateKey((K) value.getId()), value, adapter);
  }

  public List<V> findAll() {
    // TODO Auto-generated method stub
    return null;
  }

  protected String generateKey(K id) {
    return getString(keyPaddingInBytes) + id;
  }

  public void store(V value) {
    // TODO Auto-generated method stub
  }

  public void putInCache(Object owner, Collection<V> collection) {
    throw new UnsupportedOperationException();
  }

  /**
   * processes all associated caches associated with the input {@link CacheProcessor}
   *
   * @param cacheProcessor
   */
  public void processAssociatedCaches(CacheProcessor cacheProcessor) {
    if (cacheProcessor == null) return;
    cacheProcessor.processCache(cache);
  }

  public int getSize() {
    return cache.getSize();
  }

  protected boolean isSearchable() {
    Object underlyingCache = cache.getUnderlyingCache();
    if (underlyingCache instanceof Cache){
      return ((Cache)underlyingCache).isSearchable();
    }
    else
      return false;
  }
}
