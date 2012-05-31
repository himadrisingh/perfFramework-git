package org.tc.perf.cache;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * Cache which stores all the jars/logs/kit for a particular cache.
 *
 * @author Himadri Singh
 *
 */
public class DataCache {
	private final static String TEST_LIBS = "TEST_FILES";
	private final static String KIT_LIBS = "KIT_FILES";
	private final static String LOAD_LIBS = "LOAD_FILES";
	private final static String TC_INSTALL_DIR = "KIT_PATH";
	private final static String KIT_NAME = "KIT_NAME";


	private final Cache data;

	private DataCache(String cacheName) {
		data = CacheGenerator.getCache(cacheName);
	}

	public static DataCache getInstance(String cacheName){
		return new DataCache(cacheName);
	}

	@SuppressWarnings("unchecked")
	private List<String> getList(String key) {
		Element e = data.get(key);
		if (e == null)
			return new ArrayList<String>();
		Object files = e.getValue();
		if (files instanceof List<?>)
			return (List<String>) files;
		else
			throw new UnsupportedOperationException(
					"Expected to find a List<String> in file list cache but found "
							+ files.getClass() + " instead.");
	}

	/**
	 *
	 * @return The name of jars to used int the application/l1 classpath.
	 */
	public List<String> getTestLibs() {
		return getList(TEST_LIBS);
	}

	/**
	 *
	 * @return The name of jars to used int the load process classpath.
	 */
	public List<String> getLoadLibs() {
		return getList(LOAD_LIBS);
	}

	public List<String> getTestKitLibs() {
		return getList(KIT_LIBS);
	}

	public void setTestLibs(List<String> value) {
		put(TEST_LIBS, value);
	}

	public void setTestKitLibs(List<String> value) {
		put(KIT_LIBS, value);
	}

	public void setLoadLibs(List<String> value) {
		put(LOAD_LIBS, value);
	}

	private void put(String key, List<String> value) {
		if (value == null)
			return;
		data.put(new Element(key, value));
	}

	public void setTcInstallDir(String installDir) {
		data.put(new Element(TC_INSTALL_DIR, installDir));
	}

	/**
	 *
	 * @return path to terracota install dir extracted locally.
	 */
	public String getTcInstallDir() {
		Element e = data.get(TC_INSTALL_DIR);
		if (e == null) {
			throw new NullPointerException("TC_INSTALL_DIR is null");
		}
		return (String) e.getValue();
	}

	public void setKitName(String name) {
		data.put(new Element(KIT_NAME, name));
	}

	/**
	 *
	 * @return the name of terracotta installation kit
	 */
	public String getKitName() {
		Element e = data.get(KIT_NAME);
		if (e == null) {
			throw new NullPointerException("KIT not uploaded.");
		}
		return (String) e.getValue();
	}

	/**
	 * The files loaded to the cache are stored in chunks. The method returns
	 * the number of chunks a file is stored.
	 *
	 * @param filename
	 *            name of the file
	 * @return number of chunks
	 * @throws FileNotFoundException
	 *             file is not stored the cache
	 */
	public int getFilePartsCount(String filename) throws FileNotFoundException {
		Element element = data.get(filename);
		if (element == null) {
			throw new FileNotFoundException(String.format(
					"Cannot find %s in the cache.", filename));
		}
		return (Integer) element.getValue();
	}

	/**
	 * Returns the file chunk #
	 * @param filename file name
	 * @param part chunk number
	 * @return bytes of chunk
	 * @see #getFilePartsCount(String)
	 */
	public byte[] getFilePart(String filename, int part) {
		Element e = data.get(filename + "." + part);
		return (e != null) ? (byte[]) e.getValue() : null;
	}

	public void putFilePartsCount(String filename, int count){
		data.put(new Element(filename, count));
	}

	public void putFileParts(String filename, int count, byte[] part){
		data.put(new Element(filename + "." + count, part));
	}

	public String getName(){
		return data.getName();
	}

	public static void removeInstance(String uniqueId) {
		CacheGenerator.removeCache(uniqueId);
	}

}
