package org.springframework.cache.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Alex Snaps
 */
public class TcEhCacheManagerFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

  protected final Log  logger             = LogFactory.getLog(getClass());
  private boolean      shared             = false;

  private String       cacheManagerName;
  private CacheManager cacheManager;

  private String       clustered          = "true";
  private boolean      standalone         = false;

  private String       concurrency, consistency, synchronousWrites, valueMode, localKeyCache, localKeyCacheSize,
      storageStrategy, statistics, transactionalMode, expressTerracottaUrl;

  private String       owners_tti, owners_ttl, owners_capacity, owners_inMemory, owners_localHeap, owners_localOffheap, owners_localDisk;
  private String       pets_tti, pets_ttl, pets_inMemory, pets_capacity, pets_localHeap, pets_localOffheap, pets_localDisk;
  private String       visits_tti, visits_ttl, visits_inMemory, visits_capacity, visits_localHeap, visits_localOffheap, visits_localDisk;
  private String       petVisits_tti, petVisits_ttl, petVisits_inMemory, petVisits_capacity, petVisits_localHeap, petVisits_localOffheap, petVisits_localDisk;
  private String       ownerPets_tti, ownerPets_ttl, ownerPets_inMemory, ownerPets_capacity, ownerPets_localHeap, ownerPets_localOffheap, ownerPets_localDisk;
  private String       petTypes_tti, petTypes_ttl, petTypes_capacity, petTypes_inMemory, petTypes_localHeap, petTypes_localOffheap, petTypes_localDisk;

  private String       localHeap, localDisk, localOffheap, localCacheEnabled;
  private String       searchEnabled, writeBehindEnabled, copyStrategy, copyOnRead, copyOnWrite, maxWriteDelay, writeBatching,
      writeBatchSize, writeCoalescing, writeMode;
  private String       nonstopEnabled, nonstopTimeoutMillis, nonstopImmediateTimeout, nonstopTimeoutBehavior, rejoin;


  /**
   * Set whether the EHCache CacheManager should be shared (as a singleton at the VM level) or independent (typically
   * local within the application). Default is "false", creating an independent instance.
   *
   * @see net.sf.ehcache.CacheManager#create()
   * @see net.sf.ehcache.CacheManager#CacheManager()
   */
  public void setShared(boolean shared) {
    this.shared = shared;
  }

  /**
   * Set the name of the EHCache CacheManager (if a specific name is desired).
   *
   * @see net.sf.ehcache.CacheManager#setName(String)
   */
  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }

  public void afterPropertiesSet() throws CacheException {
    logger.info("Initializing EHCache CacheManager");
    if (this.shared) {
      // Shared CacheManager singleton at the VM level.
        this.cacheManager = CacheManager.create(getConfiguration());
    } else {
      // Independent CacheManager instance (the default).
        this.cacheManager = new CacheManager(getConfiguration());
    }
    if (this.cacheManagerName != null) {
      this.cacheManager.setName(this.cacheManagerName);
    }

    logger.info(this.cacheManager.getActiveConfigurationText());

  }

  public void setCopyStrategy(final String copyStrategy) {
    this.copyStrategy = copyStrategy;
  }

  private Configuration getConfiguration() {
    EhcacheConfigBuilder builder = EhcacheConfigBuilder.getInstance();
    builder.setClustered(clustered);

    builder.setMaxBytesLocalDisk(localDisk);
    builder.setMaxBytesLocalHeap(localHeap);
    builder.setMaxBytesLocalOffheap(localOffheap);
    builder.setLocalCacheEnabled(localCacheEnabled);

    builder.setOwnersMaxElementsInMemory(owners_inMemory);
    builder.setOwnersMaxElementsOnDisk(owners_capacity);
    builder.setOwnersTTI(owners_tti);
    builder.setOwnersTTL(owners_ttl);
    builder.setOwnersMaxBytesLocalHeap(owners_localHeap);
    builder.setOwnersMaxBytesLocalDisk(owners_localDisk);
    builder.setOwnersMaxBytesLocalOffHeap(owners_localOffheap);

    builder.setPetsMaxElementsInMemory(pets_inMemory);
    builder.setPetsMaxElementsOnDisk(pets_capacity);
    builder.setPetsTTI(pets_tti);
    builder.setPetsTTL(pets_ttl);
    builder.setPetsMaxBytesLocalHeap(pets_localHeap);
    builder.setPetsMaxBytesLocalDisk(pets_localDisk);
    builder.setPetsMaxBytesLocalOffHeap(pets_localOffheap);

    builder.setVisitsMaxElementsInMemory(visits_inMemory);
    builder.setVisitsMaxElementsOnDisk(visits_capacity);
    builder.setVisitsTTI(visits_tti);
    builder.setVisitsTTL(visits_ttl);
    builder.setVisitsMaxBytesLocalDisk(visits_localDisk);
    builder.setVisitsMaxBytesLocalHeap(visits_localHeap);
    builder.setVisitsMaxBytesLocalOffHeap(visits_localOffheap);

    builder.setOwnerPetsMaxElementsInMemory(ownerPets_inMemory);
    builder.setOwnerPetsMaxElementsOnDisk(ownerPets_capacity);
    builder.setOwnerPetsTTI(ownerPets_tti);
    builder.setOwnerPetsTTL(ownerPets_ttl);
    builder.setOwnerPetsMaxBytesLocalHeap(ownerPets_localHeap);
    builder.setOwnerPetsMaxBytesLocalDisk(ownerPets_localDisk);
    builder.setOwnerPetsMaxBytesLocalOffHeap(ownerPets_localOffheap);

    builder.setPetTypesMaxElementsInMemory(petTypes_inMemory);
    builder.setPetTypesMaxElementsOnDisk(petTypes_capacity);
    builder.setPetTypesTTI(petTypes_tti);
    builder.setPetTypesTTL(petTypes_ttl);
    builder.setPetTypesMaxBytesLocalHeap(petTypes_localHeap);
    builder.setPetTypesMaxBytesLocalDisk(petTypes_localDisk);
    builder.setPetTypesMaxBytesLocalOffHeap(petTypes_localOffheap);

    builder.setPetVisitsMaxElementsInMemory(petVisits_inMemory);
    builder.setPetVisitsMaxElementsOnDisk(pets_capacity);
    builder.setPetVisitsTTI(petVisits_tti);
    builder.setPetVisitsTTL(petVisits_ttl);
    builder.setPetVisitsMaxBytesLocalHeap(petVisits_localHeap);
    builder.setPetVisitsMaxBytesLocalDisk(petVisits_localDisk);
    builder.setPetVisitsMaxBytesLocalOffHeap(petVisits_localOffheap);

    builder.setTransactionalMode(transactionalMode);
    builder.setSynchronousWrites(synchronousWrites);
    builder.setConcurrency(concurrency);
    builder.setConsistency(consistency);
    builder.setLocalKeyCache(localKeyCache);
    builder.setLocalKeyCacheSize(localKeyCacheSize);
    builder.setValueMode(valueMode);
    builder.setStorageStrategy(storageStrategy);
    builder.setStatistics(statistics);

    builder.setCopyOnRead(copyOnRead);
    builder.setCopyOnWrite(copyOnWrite);
    builder.setCopyStrategyClass(copyStrategy);

    builder.enableSearch(searchEnabled);

    builder.setWriteBehind(writeBehindEnabled);
    builder.setMaxWriteDelay(maxWriteDelay);
    builder.setWriteBatching(writeBatching);
    builder.setWriteBatchSize(writeBatchSize);
    builder.setWriteCoalescing(writeCoalescing);
    builder.setWriteMode(writeMode);

    builder.setRejoin(rejoin);
    builder.setNonstopCache(nonstopEnabled);
    builder.setNonstopTimeout(nonstopTimeoutMillis);
    builder.setNonstopTimeoutBehaviour(nonstopTimeoutBehavior);
    builder.setNonstopImmediateTimeout(nonstopImmediateTimeout);

    builder.setTcConfigURL(expressTerracottaUrl);

    return builder.getEhcacheConfiguration();
  }

  public void setBulkLoad(boolean c){
    if (isClustered()) {
      String[] caches = cacheManager.getCacheNames();
      for (String name : caches){
        Cache cache = cacheManager.getCache(name);
        if(!name.equals("petTypes") && isCacheClustered(cache)) {
          logger.info("Setting cache [" + name + "] bulk load: " + c);
          cache.setNodeBulkLoadEnabled(c);
        }
      }
    }
  }

  private boolean isCacheClustered(Cache cache) {
    TerracottaConfiguration tcConfig = cache.getCacheConfiguration().getTerracottaConfiguration();
    if(tcConfig == null) {
      return false;
    }
    return tcConfig.isClustered();
  }


  public void waitUntilBulkLoadComplete(){
    if (isClustered()) {
      String[] caches = cacheManager.getCacheNames();
      for (String name : caches){
        Cache cache = cacheManager.getCache(name);
        if(isCacheClustered(cache)) {
          logger.info("Waiting for cache [" + name + "] to complete bulk load.");
          cache.waitUntilClusterBulkLoadComplete();
        }
      }
    }
  }


  public String getStorageStrategy() {
    return storageStrategy;
  }

  public void setStorageStrategy(String storageStrategy) {
    this.storageStrategy = storageStrategy.trim();
  }

  public String getConcurrency() {
    return concurrency;
  }

  public void setConcurrency(String concurrency) {
    this.concurrency = concurrency.trim();
  }

  public void setValueMode(String valueMode) {
    this.valueMode = valueMode.trim();
  }

  public void setLocalKeyCache(String localKeyCache) {
    this.localKeyCache = localKeyCache.trim();
  }

  public void setLocalKeyCacheSize(String localKeyCacheSize) {
    this.localKeyCacheSize = localKeyCacheSize.trim();
  }

  public String getVisits_tti() {
    return visits_tti;
  }

  public void setVisits_tti(String visitsTti) {
    visits_tti = visitsTti.trim();
  }

  public String getVisits_ttl() {
    return visits_ttl;
  }

  public void setVisits_ttl(String visitsTtl) {
    visits_ttl = visitsTtl.trim();
  }

  public String getOwners_tti() {
    return owners_tti;
  }

  public void setOwners_tti(String ownersTti) {
    owners_tti = ownersTti.trim();
  }

  public String getOwners_ttl() {
    return owners_ttl;
  }

  public void setOwners_ttl(String ownersTtl) {
    owners_ttl = ownersTtl.trim();
  }

  public String getPets_tti() {
    return pets_tti;
  }

  public void setPets_tti(String petsTti) {
    pets_tti = petsTti.trim();
  }

  public String getPets_ttl() {
    return pets_ttl;
  }

  public void setPets_ttl(String petsTtl) {
    pets_ttl = petsTtl.trim();
  }

  public String getPetVisits_tti() {
    return petVisits_tti;
  }

  public void setPetVisits_tti(String petVisitsTti) {
    petVisits_tti = petVisitsTti.trim();
  }

  public String getPetVisits_ttl() {
    return petVisits_ttl;
  }

  public void setPetVisits_ttl(String petVisitsTtl) {
    petVisits_ttl = petVisitsTtl.trim();
  }

  public String getOwnerPets_tti() {
    return ownerPets_tti;
  }

  public void setOwnerPets_tti(String ownerPetsTti) {
    ownerPets_tti = ownerPetsTti.trim();
  }

  public String getOwnerPets_ttl() {
    return ownerPets_ttl;
  }

  public void setOwnerPets_ttl(String ownerPetsTtl) {
    ownerPets_ttl = ownerPetsTtl.trim();
  }

  public String getPetTypes_tti() {
    return petTypes_tti;
  }

  public void setPetTypes_tti(String petTypesTti) {
    petTypes_tti = petTypesTti.trim();
  }

  public String getPetTypes_ttl() {
    return petTypes_ttl;
  }

  public void setPetTypes_ttl(String petTypesTtl) {
    petTypes_ttl = petTypesTtl.trim();
  }

  public String getOwners_capacity() {
    return owners_capacity;
  }

  public void setOwners_capacity(String ownersCapacity) {
    owners_capacity = ownersCapacity.trim();
  }

  public String getPets_capacity() {
    return pets_capacity;
  }

  public void setPets_capacity(String petsCapacity) {
    pets_capacity = petsCapacity.trim();
  }

  public String getVisits_capacity() {
    return visits_capacity;
  }

  public void setVisits_capacity(String visitsCapacity) {
    visits_capacity = visitsCapacity.trim();
  }

  public String getPetVisits_capacity() {
    return petVisits_capacity;
  }

  public void setPetVisits_capacity(String petVisitsCapacity) {
    petVisits_capacity = petVisitsCapacity.trim();
  }

  public String getOwnerPets_capacity() {
    return ownerPets_capacity;
  }

  public void setOwnerPets_capacity(String ownerPetsCapacity) {
    ownerPets_capacity = ownerPetsCapacity.trim();
  }

  public String getPetTypes_capacity() {
    return petTypes_capacity;
  }

  public void setPetTypes_capacity(String petTypesCapacity) {
    petTypes_capacity = petTypesCapacity.trim();
  }

  public String getOwners_inMemory() {
    return owners_inMemory;
  }

  public void setOwners_inMemory(String ownersInMemory) {
    owners_inMemory = ownersInMemory.trim();
  }

  public String getPets_inMemory() {
    return pets_inMemory;
  }

  public void setPets_inMemory(String petsInMemory) {
    pets_inMemory = petsInMemory.trim();
  }

  public String getVisits_inMemory() {
    return visits_inMemory;
  }

  public void setVisits_inMemory(String visitsInMemory) {
    visits_inMemory = visitsInMemory.trim();
  }

  public String getStatistics() {
    return statistics;
  }

  public void setStatistics(String statistics) {
    this.statistics = statistics.trim();
  }

  public boolean isStandalone() {
    return standalone;
  }

  public void setStandalone(boolean standalone) {
    this.standalone = standalone;
  }

  public String getPetVisits_inMemory() {
    return petVisits_inMemory;
  }

  public void setPetVisits_inMemory(String petVisitsInMemory) {
    petVisits_inMemory = petVisitsInMemory.trim();
  }

  public String getOwnerPets_inMemory() {
    return ownerPets_inMemory;
  }

  public void setOwnerPets_inMemory(String ownerPetsInMemory) {
    ownerPets_inMemory = ownerPetsInMemory.trim();
  }

  public String getPetTypes_inMemory() {
    return petTypes_inMemory;
  }

  public boolean isClustered() {
    return Boolean.valueOf(clustered);
  }

  public void setClustered(final boolean clustered) {
    this.clustered = Boolean.toString(clustered);
  }

  public void setPetTypes_inMemory(String petTypesInMemory) {
    petTypes_inMemory = petTypesInMemory.trim();
  }

  public void setExpressTerracottaUrl(String expressTerracottaUrl) {
    this.expressTerracottaUrl = expressTerracottaUrl.trim();
  }

  public void setMaxWriteDelay(final String maxWriteDelay) {
    this.maxWriteDelay = maxWriteDelay.trim();
  }

  public void setWriteMode(String writeMode) {
    this.writeMode = writeMode.trim();
  }

  public void setWriteBatching(final String writeBatching) {
    this.writeBatching = writeBatching.trim();
  }

  public void setWriteBatchSize(final String writeBatchSize) {
    this.writeBatchSize = writeBatchSize.trim();
  }

  public void setWriteCoalescing(final String writeCoalescing) {
    this.writeCoalescing = writeCoalescing.trim();
  }

  public CacheManager getCacheManager(){
    return this.cacheManager;
  }

  public Object getObject() {
    return this.cacheManager;
  }

  public Class getObjectType() {
    return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
  }

  public boolean isSingleton() {
    return true;
  }

  public void destroy() {
    logger.info("Shutting down EHCache CacheManager");
    this.cacheManager.shutdown();
  }

  public void setTransactionalMode(String transactionalMode) {
    this.transactionalMode = transactionalMode.trim();
  }

  public String getTransactionalMode() {
    return transactionalMode;
  }

  public String getCopyOnWrite() {
    return copyOnWrite;
  }

  public void setCopyOnWrite(String copyOnWrite) {
    this.copyOnWrite = copyOnWrite.trim();
  }

  public String getCopyOnRead() {
    return copyOnRead;
  }

  public void setCopyOnRead(String copyOnRead) {
    this.copyOnRead = copyOnRead.trim();
  }

  public String getNonstopEnabled() {
    return nonstopEnabled;
  }

  public void setNonstopEnabled(String nonstopEnabled) {
    this.nonstopEnabled = nonstopEnabled.trim();
  }

  public String getNonstopTimeoutMillis() {
    return nonstopTimeoutMillis;
  }

  public void setNonstopTimeoutMillis(String nonstopTimeoutMillis) {
    this.nonstopTimeoutMillis = nonstopTimeoutMillis.trim();
  }

  public String getNonstopImmediateTimeout() {
    return nonstopImmediateTimeout;
  }

  public void setNonstopImmediateTimeout(String nonstopImmediateTimeout) {
    this.nonstopImmediateTimeout = nonstopImmediateTimeout.trim();
  }

  public String getNonstopTimeoutBehavior() {
    return nonstopTimeoutBehavior;
  }

  public void setNonstopTimeoutBehavior(String nonstopTimeoutBehavior) {
    this.nonstopTimeoutBehavior = nonstopTimeoutBehavior.trim();
  }

  public void setConsistency(String consistency) {
    this.consistency = consistency.trim();
  }

  public void setSynchronousWrites(String synchronousWrites) {
    this.synchronousWrites = synchronousWrites.trim();
  }

  public String getRejoin() {
    return rejoin;
  }

  public void setRejoin(String rejoin) {
    this.rejoin = rejoin.trim();
  }

  public String getSearchEnabled() {
    return searchEnabled;
  }

  public void setSearchEnabled(String searchEnabled) {
    this.searchEnabled = searchEnabled.trim();
  }

  public String getWriteBehindEnabled() {
    return writeBehindEnabled;
  }

  public void setWriteBehindEnabled(String writeBehindEnabled) {
    this.writeBehindEnabled = writeBehindEnabled.trim();
  }

  public String getOwners_localHeap() {
    return owners_localHeap;
  }

  public void setOwners_localHeap(String owners_localHeap) {
    this.owners_localHeap = owners_localHeap.trim();
  }

  public String getOwners_localOffheap() {
    return owners_localOffheap;
  }

  public void setOwners_localOffheap(String owners_localOffheap) {
    this.owners_localOffheap = owners_localOffheap.trim();
  }

  public String getOwners_localDisk() {
    return owners_localDisk;
  }

  public void setOwners_localDisk(String owners_localDisk) {
    this.owners_localDisk = owners_localDisk.trim();
  }

  public String getPets_localHeap() {
    return pets_localHeap;
  }

  public void setPets_localHeap(String pets_localHeap) {
    this.pets_localHeap = pets_localHeap.trim();
  }

  public String getPets_localOffheap() {
    return pets_localOffheap;
  }

  public void setPets_localOffheap(String pets_localOffheap) {
    this.pets_localOffheap = pets_localOffheap.trim();
  }

  public String getPets_localDisk() {
    return pets_localDisk;
  }

  public void setPets_localDisk(String pets_localDisk) {
    this.pets_localDisk = pets_localDisk.trim();
  }

  public String getVisits_localHeap() {
    return visits_localHeap;
  }

  public void setVisits_localHeap(String visits_localHeap) {
    this.visits_localHeap = visits_localHeap.trim();
  }

  public String getVisits_localOffheap() {
    return visits_localOffheap;
  }

  public void setVisits_localOffheap(String visits_localOffheap) {
    this.visits_localOffheap = visits_localOffheap.trim();
  }

  public String getVisits_localDisk() {
    return visits_localDisk;
  }

  public void setVisits_localDisk(String visits_localDisk) {
    this.visits_localDisk = visits_localDisk.trim();
  }

  public String getPetVisits_localHeap() {
    return petVisits_localHeap;
  }

  public void setPetVisits_localHeap(String petVisits_localHeap) {
    this.petVisits_localHeap = petVisits_localHeap.trim();
  }

  public String getPetVisits_localOffheap() {
    return petVisits_localOffheap;
  }

  public void setPetVisits_localOffheap(String petVisits_localOffheap) {
    this.petVisits_localOffheap = petVisits_localOffheap.trim();
  }

  public String getPetVisits_localDisk() {
    return petVisits_localDisk;
  }

  public void setPetVisits_localDisk(String petVisits_localDisk) {
    this.petVisits_localDisk = petVisits_localDisk.trim();
  }

  public String getOwnerPets_localHeap() {
    return ownerPets_localHeap;
  }

  public void setOwnerPets_localHeap(String ownerPets_localHeap) {
    this.ownerPets_localHeap = ownerPets_localHeap.trim();
  }

  public String getOwnerPets_localOffheap() {
    return ownerPets_localOffheap;
  }

  public void setOwnerPets_localOffheap(String ownerPets_localOffheap) {
    this.ownerPets_localOffheap = ownerPets_localOffheap.trim();
  }

  public String getOwnerPets_localDisk() {
    return ownerPets_localDisk;
  }

  public void setOwnerPets_localDisk(String ownerPets_localDisk) {
    this.ownerPets_localDisk = ownerPets_localDisk.trim();
  }

  public String getPetTypes_localHeap() {
    return petTypes_localHeap;
  }

  public void setPetTypes_localHeap(String petTypes_localHeap) {
    this.petTypes_localHeap = petTypes_localHeap.trim();
  }

  public String getPetTypes_localOffheap() {
    return petTypes_localOffheap;
  }

  public void setPetTypes_localOffheap(String petTypes_localOffheap) {
    this.petTypes_localOffheap = petTypes_localOffheap.trim();
  }

  public String getPetTypes_localDisk() {
    return petTypes_localDisk;
  }

  public void setPetTypes_localDisk(String petTypes_localDisk) {
    this.petTypes_localDisk = petTypes_localDisk.trim();
  }

  public String getLocalHeap() {
    return localHeap;
  }

  public void setLocalHeap(String localHeap) {
    this.localHeap = localHeap.trim();
  }

  public String getLocalDisk() {
    return localDisk;
  }

  public void setLocalDisk(String localDisk) {
    this.localDisk = localDisk.trim();
  }

  public String getLocalOffheap() {
    return localOffheap;
  }

  public void setLocalOffheap(String localOffheap) {
    this.localOffheap = localOffheap.trim();
  }

  public String getLocalCacheEnabled() {
    return localCacheEnabled;
  }

  public void setLocalCacheEnabled(String localCacheEnabled) {
    this.localCacheEnabled = localCacheEnabled.trim();
  }


}
