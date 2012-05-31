/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.springframework.cache.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration.CacheWriterFactoryConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.CopyStrategyConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.PinningConfiguration.Store;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration;

import com.terracotta.ehcache.perf.FakeWriteBehindFactory;

public class EhcacheConfigBuilder {

  private static EhcacheConfigBuilder builder = new EhcacheConfigBuilder();
  private final Configuration configuration;
  private final CacheConfiguration owners, pets, visits, petVisits, ownerPets, petTypes;
  private final TerracottaConfiguration tcConfig;
  private final TerracottaClientConfiguration clientConfig;
  private final NonstopConfiguration nonstop;
  private final CacheWriterConfiguration cacheWriterConfiguration;

  private EhcacheConfigBuilder(){
    configuration = new Configuration();
    configuration.setName("ehcacheperf");

    owners = new CacheConfiguration();
    pets = new CacheConfiguration();
    visits = new CacheConfiguration();
    ownerPets = new CacheConfiguration();
    petVisits = new CacheConfiguration();
    petTypes = new CacheConfiguration();

    owners.setName("owners");
    pets.setName("pets");
    visits.setName("visits");
    ownerPets.setName("ownerPets");
    petVisits.setName("petVisits");
    petTypes.setName("petTypes");

    PinningConfiguration pinningConfiguration = new PinningConfiguration();
    pinningConfiguration.store(Store.LOCALHEAP);
    petTypes.addPinning(pinningConfiguration);

    tcConfig = new TerracottaConfiguration();
    clientConfig = new TerracottaClientConfiguration();
    nonstop = new NonstopConfiguration();
    cacheWriterConfiguration = new CacheWriterConfiguration();

  }

  public void setMaxBytesLocalHeap(String localheap){
    if (localheap.startsWith("$")) return;
    configuration.setMaxBytesLocalHeap(localheap);
  }

  public void setMaxBytesLocalOffheap(String offheap){
    if (offheap.startsWith("$")) return;
    configuration.setMaxBytesLocalOffHeap(offheap);
  }

  public void setMaxBytesLocalDisk(String localdisk){
    if (localdisk.startsWith("$")) return;
    configuration.setMaxBytesLocalDisk(localdisk);
  }

  public static EhcacheConfigBuilder getInstance(){
    return builder;
  }

  public void setConsistency(String consistency){
    if (consistency.startsWith("$")) return;
    tcConfig.setConsistency(consistency);
  }

  public void setConcurrency(String concurrency) {
    if (concurrency.startsWith("$")) return;
    tcConfig.setConcurrency(Integer.parseInt(concurrency));
  }

  public void setSynchronousWrites(String synchronousWrites){
    if (synchronousWrites.startsWith("$")) return;
    tcConfig.setSynchronousWrites(Boolean.parseBoolean(synchronousWrites));
  }

  public void setValueMode(String valueMode){
    if (valueMode.startsWith("$")) return;
    tcConfig.setValueMode(valueMode);
  }

  public void setLocalKeyCache(String local){
    if (local.startsWith("$")) return;
    tcConfig.setLocalKeyCache(Boolean.parseBoolean(local));
  }

  public void setLocalKeyCacheSize(String size){
    if (size.startsWith("$")) return;
    tcConfig.setLocalKeyCacheSize(Integer.parseInt(size));
  }

  public void setStorageStrategy(String strategy){
    if (strategy.startsWith("$")) return;
    tcConfig.setStorageStrategy(strategy);
  }

  public void setTcConfigURL(String url){
    if (url.startsWith("$")) return;
     clientConfig.setUrl(url);
  }

  public void setRejoin(String rejoin){
    if (rejoin.startsWith("$")) return;
    clientConfig.setRejoin(Boolean.parseBoolean(rejoin));
  }

  public void setNonstopCache(String nonstop){
    if (nonstop.startsWith("$")) return;
    if (Boolean.parseBoolean(nonstop))
      tcConfig.addNonstop(this.nonstop);
  }

  public void setNonstopTimeout(String timeout){
    if (timeout.startsWith("$")) return;
    nonstop.setTimeoutMillis(Integer.parseInt(timeout));
  }

  public void setNonstopImmediateTimeout(String immediate){
    if (immediate.startsWith("$")) return;
    nonstop.setImmediateTimeout(Boolean.parseBoolean(immediate));
  }

  public void setNonstopTimeoutBehaviour(String behaviour){
    if (behaviour.startsWith("$")) return;
    TimeoutBehaviorConfiguration timeoutBehavior = new TimeoutBehaviorConfiguration();
    timeoutBehavior.setType(behaviour);
    nonstop.addTimeoutBehavior(timeoutBehavior);
  }

  public void setMaxWriteDelay(String delay){
    if (delay.startsWith("$")) return;
    cacheWriterConfiguration.setMaxWriteDelay(Integer.parseInt(delay));
  }

  public void setWriteBatching(String batching){
    if (batching.startsWith("$")) return;
    cacheWriterConfiguration.setWriteBatching(Boolean.parseBoolean(batching));
  }

  public void setWriteBatchSize(String size){
    if (size.startsWith("$")) return;
    cacheWriterConfiguration.setWriteBatchSize(Integer.parseInt(size));
  }

  public void setWriteCoalescing(String coalescing){
    if (coalescing.startsWith("$")) return;
    cacheWriterConfiguration.setWriteCoalescing(Boolean.parseBoolean(coalescing));
  }

  public void setWriteMode(String mode){
    if (mode.startsWith("$")) return;
    cacheWriterConfiguration.setWriteMode(mode);
  }

  // LOCAL HEAP

  public void setOwnersMaxBytesLocalHeap(String onheap){
    if (onheap.startsWith("$")) return;
    owners.setMaxBytesLocalHeap(onheap);
  }

  public void setPetsMaxBytesLocalHeap(String onheap){
    if (onheap.startsWith("$")) return;
    pets.setMaxBytesLocalHeap(onheap);
  }

  public void setVisitsMaxBytesLocalHeap(String onheap){
    if (onheap.startsWith("$")) return;
    visits.setMaxBytesLocalHeap(onheap);
  }

  public void setOwnerPetsMaxBytesLocalHeap(String onheap){
    if (onheap.startsWith("$")) return;
    ownerPets.setMaxBytesLocalHeap(onheap);
  }

  public void setPetVisitsMaxBytesLocalHeap(String onheap){
    if (onheap.startsWith("$")) return;
    petVisits.setMaxBytesLocalHeap(onheap);
  }

  public void setPetTypesMaxBytesLocalHeap(String onheap){
    if (onheap.startsWith("$")) return;
    petTypes.setMaxBytesLocalHeap(onheap);
  }

  // LOCAL DISK

  public void setOwnersMaxBytesLocalDisk(String maxBytesDisk){
    if (maxBytesDisk.startsWith("$")) return;
    owners.setMaxBytesLocalDisk(maxBytesDisk);
  }

  public void setPetsMaxBytesLocalDisk(String maxBytesDisk){
    if (maxBytesDisk.startsWith("$")) return;
    pets.setMaxBytesLocalDisk(maxBytesDisk);
  }

  public void setVisitsMaxBytesLocalDisk(String maxBytesDisk){
    if (maxBytesDisk.startsWith("$")) return;
    visits.setMaxBytesLocalDisk(maxBytesDisk);
  }

  public void setOwnerPetsMaxBytesLocalDisk(String maxBytesDisk){
    if (maxBytesDisk.startsWith("$")) return;
    ownerPets.setMaxBytesLocalDisk(maxBytesDisk);
  }

  public void setPetVisitsMaxBytesLocalDisk(String maxBytesDisk){
    if (maxBytesDisk.startsWith("$")) return;
    petVisits.setMaxBytesLocalDisk(maxBytesDisk);
  }

  public void setPetTypesMaxBytesLocalDisk(String maxBytesDisk){
    if (maxBytesDisk.startsWith("$")) return;
    petTypes.setMaxBytesLocalDisk(maxBytesDisk);
  }

  // MAX BYTES OFF HEAP

  public void setOwnersMaxBytesLocalOffHeap(String maxBytesOffHeap){
    if (maxBytesOffHeap.startsWith("$")) return;
    owners.setOverflowToOffHeap(true);
    owners.setMaxBytesLocalOffHeap(maxBytesOffHeap);
  }

  public void setPetsMaxBytesLocalOffHeap(String maxBytesOffHeap){
    if (maxBytesOffHeap.startsWith("$")) return;
    pets.setOverflowToOffHeap(true);
    pets.setMaxBytesLocalOffHeap(maxBytesOffHeap);
  }

  public void setVisitsMaxBytesLocalOffHeap(String maxBytesOffHeap){
    if (maxBytesOffHeap.startsWith("$")) return;
    visits.setOverflowToOffHeap(true);
    visits.setMaxBytesLocalOffHeap(maxBytesOffHeap);
  }

  public void setPetTypesMaxBytesLocalOffHeap(String maxBytesOffHeap){
    if (maxBytesOffHeap.startsWith("$")) return;
    petTypes.setOverflowToOffHeap(true);
    petTypes.setMaxBytesLocalOffHeap(maxBytesOffHeap);
  }

  public void setPetVisitsMaxBytesLocalOffHeap(String maxBytesOffHeap){
    if (maxBytesOffHeap.startsWith("$")) return;
    petVisits.setOverflowToOffHeap(true);
    petVisits.setMaxBytesLocalOffHeap(maxBytesOffHeap);
  }

  public void setOwnerPetsMaxBytesLocalOffHeap(String maxBytesOffHeap){
    if (maxBytesOffHeap.startsWith("$")) return;
    ownerPets.setOverflowToOffHeap(true);
    ownerPets.setMaxBytesLocalOffHeap(maxBytesOffHeap);
  }

  // MAX ELEMENTS IN MEMORY

  public void setOwnersMaxElementsInMemory(String maxElementsInMemory){
    if (maxElementsInMemory.startsWith("$")) return;
    owners.setMaxEntriesLocalHeap(Integer.parseInt(maxElementsInMemory));
  }

  public void setVisitsMaxElementsInMemory(String maxElementsInMemory){
    if (maxElementsInMemory.startsWith("$")) return;
    visits.setMaxEntriesLocalHeap(Integer.parseInt(maxElementsInMemory));
  }

  public void setPetsMaxElementsInMemory(String maxElementsInMemory){
    if (maxElementsInMemory.startsWith("$")) return;
    pets.setMaxEntriesLocalHeap(Integer.parseInt(maxElementsInMemory));
  }

  public void setPetTypesMaxElementsInMemory(String maxElementsInMemory){
    if (maxElementsInMemory.startsWith("$")) return;
    petTypes.setMaxEntriesLocalHeap(Integer.parseInt(maxElementsInMemory));
  }

  public void setPetVisitsMaxElementsInMemory(String maxElementsInMemory){
    if (maxElementsInMemory.startsWith("$")) return;
    petVisits.setMaxEntriesLocalHeap(Integer.parseInt(maxElementsInMemory));
  }

  public void setOwnerPetsMaxElementsInMemory(String maxElementsInMemory){
    if (maxElementsInMemory.startsWith("$")) return;
    ownerPets.setMaxEntriesLocalHeap(Integer.parseInt(maxElementsInMemory));
  }

  public void setOwnersMaxElementsOnDisk(String maxElementsOnDisk){
    if (maxElementsOnDisk.startsWith("$")) return;
    owners.setMaxElementsOnDisk(Integer.parseInt(maxElementsOnDisk));
  }

  public void setVisitsMaxElementsOnDisk(String maxElementsOnDisk){
    if (maxElementsOnDisk.startsWith("$")) return;
    visits.setMaxElementsOnDisk(Integer.parseInt(maxElementsOnDisk));
  }

  public void setPetsMaxElementsOnDisk(String maxElementsOnDisk){
    if (maxElementsOnDisk.startsWith("$")) return;
    pets.setMaxElementsOnDisk(Integer.parseInt(maxElementsOnDisk));
  }

  public void setPetTypesMaxElementsOnDisk(String maxElementsOnDisk){
    if (maxElementsOnDisk.startsWith("$")) return;
    petTypes.setMaxElementsOnDisk(Integer.parseInt(maxElementsOnDisk));
  }

  public void setPetVisitsMaxElementsOnDisk(String maxElementsOnDisk){
    if (maxElementsOnDisk.startsWith("$")) return;
    petVisits.setMaxElementsOnDisk(Integer.parseInt(maxElementsOnDisk));
  }

  public void setOwnerPetsMaxElementsOnDisk(String maxElementsOnDisk){
    if (maxElementsOnDisk.startsWith("$")) return;
    ownerPets.setMaxElementsOnDisk(Integer.parseInt(maxElementsOnDisk));
  }

  public void setOwnersTTI(String tti){
    if (tti.startsWith("$")) return;
    owners.setTimeToIdleSeconds(Integer.parseInt(tti));
  }

  public void setVisitsTTI(String tti){
    if (tti.startsWith("$")) return;
    visits.setTimeToIdleSeconds(Integer.parseInt(tti));
  }

  public void setPetsTTI(String tti){
    if (tti.startsWith("$")) return;
    pets.setTimeToIdleSeconds(Integer.parseInt(tti));
  }

  public void setPetTypesTTI(String tti){
    if (tti.startsWith("$")) return;
    petTypes.setTimeToIdleSeconds(Integer.parseInt(tti));
  }

  public void setPetVisitsTTI(String tti){
    if (tti.startsWith("$")) return;
    petVisits.setTimeToIdleSeconds(Integer.parseInt(tti));
  }

  public void setOwnerPetsTTI(String tti){
    if (tti.startsWith("$")) return;
    ownerPets.setTimeToIdleSeconds(Integer.parseInt(tti));
  }

  public void setOwnersTTL(String ttl){
    if (ttl.startsWith("$")) return;
    owners.setTimeToLiveSeconds(Integer.parseInt(ttl));
  }

  public void setVisitsTTL(String ttl){
    if (ttl.startsWith("$")) return;
    visits.setTimeToLiveSeconds(Integer.parseInt(ttl));
  }

  public void setPetsTTL(String ttl){
    if (ttl.startsWith("$")) return;
    pets.setTimeToLiveSeconds(Integer.parseInt(ttl));
  }

  public void setPetTypesTTL(String ttl){
    if (ttl.startsWith("$")) return;
    petTypes.setTimeToLiveSeconds(Integer.parseInt(ttl));
  }

  public void setPetVisitsTTL(String ttl){
    if (ttl.startsWith("$")) return;
    petVisits.setTimeToLiveSeconds(Integer.parseInt(ttl));
  }

  public void setOwnerPetsTTL(String ttl){
    if (ttl.startsWith("$")) return;
    ownerPets.setTimeToLiveSeconds(Integer.parseInt(ttl));
  }

  public void setClustered(String clustered){
    if (clustered.startsWith("$")) return;
    if (Boolean.parseBoolean(clustered)){
      owners.addTerracotta(tcConfig);
      pets.addTerracotta(tcConfig);
      visits.addTerracotta(tcConfig);
      petTypes.addTerracotta(tcConfig);
      petVisits.addTerracotta(tcConfig);
      ownerPets.addTerracotta(tcConfig);

      configuration.addTerracottaConfig(clientConfig);
    }
  }

  public void setCopyStrategyClass(String clazz){
    if (clazz.startsWith("$")) return;
    CopyStrategyConfiguration copyStrategyConfiguration = new CopyStrategyConfiguration();
    copyStrategyConfiguration.setClass(clazz);
    ownerPets.addCopyStrategy(copyStrategyConfiguration);
  }

  public void setWriteBehind(String behind){
    if (behind.startsWith("$")) return;
    if (Boolean.parseBoolean(behind)) {
      CacheWriterFactoryConfiguration cacheWriterFactoryConfiguration = new CacheWriterFactoryConfiguration();
      cacheWriterFactoryConfiguration.setClass(FakeWriteBehindFactory.class.toString());
      cacheWriterConfiguration.addCacheWriterFactory(cacheWriterFactoryConfiguration);

      owners.addCacheWriter(cacheWriterConfiguration);
      pets.addCacheWriter(cacheWriterConfiguration);
      visits.addCacheWriter(cacheWriterConfiguration);
      petTypes.addCacheWriter(cacheWriterConfiguration);
      ownerPets.addCacheWriter(cacheWriterConfiguration);
      petVisits.addCacheWriter(cacheWriterConfiguration);
    }
  }

  public void setCopyOnWrite(String copyOnWrite){
    if (copyOnWrite.startsWith("$")) return;
    owners.setCopyOnWrite(Boolean.parseBoolean(copyOnWrite));
    pets.setCopyOnWrite(Boolean.parseBoolean(copyOnWrite));
    visits.setCopyOnWrite(Boolean.parseBoolean(copyOnWrite));
    petTypes.setCopyOnWrite(Boolean.parseBoolean(copyOnWrite));
    ownerPets.setCopyOnWrite(Boolean.parseBoolean(copyOnWrite));
    petVisits.setCopyOnWrite(Boolean.parseBoolean(copyOnWrite));
  }


  public void setCopyOnRead(String copyOnRead){
    if (copyOnRead.startsWith("$")) return;
    owners.setCopyOnRead(Boolean.parseBoolean(copyOnRead));
    pets.setCopyOnRead(Boolean.parseBoolean(copyOnRead));
    visits.setCopyOnRead(Boolean.parseBoolean(copyOnRead));
    petTypes.setCopyOnRead(Boolean.parseBoolean(copyOnRead));
    ownerPets.setCopyOnRead(Boolean.parseBoolean(copyOnRead));
    petVisits.setCopyOnRead(Boolean.parseBoolean(copyOnRead));
  }

  public void setStatistics(String stats){
    if (stats.startsWith("$")) return;
    owners.setStatistics(Boolean.parseBoolean(stats));
    pets.setStatistics(Boolean.parseBoolean(stats));
    visits.setStatistics(Boolean.parseBoolean(stats));
    petTypes.setStatistics(Boolean.parseBoolean(stats));
    ownerPets.setStatistics(Boolean.parseBoolean(stats));
    petVisits.setStatistics(Boolean.parseBoolean(stats));
  }

  public void setTransactionalMode(String transactionalMode){
    if(transactionalMode.startsWith("$")) return;
    owners.setTransactionalMode(transactionalMode);
    visits.setTransactionalMode(transactionalMode);
    pets.setTransactionalMode(transactionalMode);
    petTypes.setTransactionalMode(transactionalMode);
    ownerPets.setTransactionalMode(transactionalMode);
    petVisits.setTransactionalMode(transactionalMode);

    setCopyOnRead(String.valueOf(true));
    setCopyOnWrite(String.valueOf(true));
    setConsistency(Consistency.STRONG.toString());
  }

  public void setLocalCacheEnabled(String cacheEnabled){
    if (cacheEnabled.startsWith("$")) return;
    tcConfig.setLocalCacheEnabled(Boolean.parseBoolean(cacheEnabled));
  }

  public void enableSearch(String search){
    if (search.startsWith("$")) return;

    if (Boolean.parseBoolean(search)) {

      SearchAttribute first = new SearchAttribute();
      first.setName("firstName");
      first.setExpression("value.getFirstName()");

      SearchAttribute last = new SearchAttribute();
      last.setName("lastName");
      last.setExpression("value.getLastName()");

      SearchAttribute acc = new SearchAttribute();
      acc.setName("account");
      acc.setExpression("value.getAccount()");

      SearchAttribute add = new SearchAttribute();
      add.setName("address");
      add.setExpression("value.getAddress()");

      SearchAttribute tele = new SearchAttribute();
      tele.setName("telephone");
      tele.setExpression("value.getTelephone()");

      SearchAttribute city = new SearchAttribute();
      city.setName("city");
      city.setExpression("value.getCity()");

      SearchAttribute petCount = new SearchAttribute();
      petCount.setName("petCount");
      petCount.setExpression("value.getPets().size()");

      Searchable o = new Searchable();
      o.addSearchAttribute(first);
      o.addSearchAttribute(last);
      o.addSearchAttribute(acc);
      o.addSearchAttribute(add);
      o.addSearchAttribute(tele);
      o.addSearchAttribute(city);
      o.addSearchAttribute(petCount);
      owners.searchable(o);

      SearchAttribute visitCount = new SearchAttribute();
      visitCount.setName("visitCount");
      visitCount.setExpression("value.getVisits().size()");

      SearchAttribute birthDate = new SearchAttribute();
      birthDate.setName("birthDate");
      birthDate.setExpression("value.getBirthDate()");

      Searchable p = new Searchable();
      p.addSearchAttribute(birthDate);
      p.addSearchAttribute(visitCount);
      pets.addSearchable(p);

      SearchAttribute date = new SearchAttribute();
      date.setName("date");
      date.setExpression("value.getDate()");

      SearchAttribute desc = new SearchAttribute();
      desc.setName("description");
      desc.setExpression("value.getDescription()");

      Searchable v = new Searchable();
      v.addSearchAttribute(date);
      v.addSearchAttribute(desc);
      visits.addSearchable(v);
    }
  }

  public Configuration getEhcacheConfiguration(){
    configuration.addCache(owners);
    configuration.addCache(pets);
    configuration.addCache(visits);
    configuration.addCache(petTypes);
    configuration.addCache(ownerPets);
    configuration.addCache(petVisits);
    return configuration;
  }

}
