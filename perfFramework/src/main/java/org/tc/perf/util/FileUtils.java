package org.tc.perf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.tc.perf.cache.DataCache;

/**
 *
 *
 * Utility class that helps in loading the files from disk to a specified cache.
 * It breaks the files into chunks and loads the {@link DataCache}
 *
 * @author Himadri Singh
 */
public class FileUtils {

	private static final Logger log = Logger.getLogger(FileUtils.class);
	private static final Format nf = NumberFormat.getNumberInstance();
	private static final int MAX_LENGTH = 64 * 1024; // 64KB

	private final DataCache data;

	public FileUtils(final DataCache cache) {
		this.data = cache;
	}

	/**
	 * download and extract the tar.gz file for terracotta kit
	 *
	 * @param location
	 *            location for the kit installation
	 */

	public File downloadExtractKit(final File location) throws IOException {
		log.info("Installing terracotta " + location.getAbsolutePath());
		String kitname = data.getKitName();
		File kit = new File(location, kitname);
		if (kit.isFile())
			log.info("Kit has been downloaded already: "
					+ kit.getAbsolutePath());
		else
			download(kitname, location);
		return extractKit(new File(location, kitname), location);
	}

	/**
	 * Gets the root directory of the file relative to a location.
	 *
	 * @param file
	 * @param location
	 * @return
	 */
	static File getRootDir(final File file, final File location) {
		File root = file;
		File parent = file.getParentFile();
		while (!(parent == null || parent.getAbsolutePath().equals(
				location.getAbsolutePath()))) {
			root = parent;
			parent = parent.getParentFile();
		}
		return root;
	}

	/**
	 * Reads terracotta tar.gz installation file and extracts to the local
	 * location
	 *
	 * @param kit
	 *            the name of the installation kit
	 * @param location
	 *            local directory to while needs to be extracted to
	 * @return the name of the root directory in the tar gzip file
	 * @throws IOException
	 */
	private File extractKit(final File kit, final File location)
			throws IOException {
		if (!kit.exists() || !kit.canRead())
			throw new IllegalArgumentException("Cannot open file for reading: "
					+ kit.getAbsolutePath());

		log.info("Extracting kit at " + location.getAbsolutePath());
		TarInputStream tin = new TarInputStream(new GZIPInputStream(
				new FileInputStream(kit)));
		TarEntry tarEntry = tin.getNextEntry();

		File extractLocation = getRootDir(
				new File(location, tarEntry.getName()), location);
		if (extractLocation.isDirectory()) {
			log.info("Extracted directory already exists: "
					+ extractLocation.getAbsolutePath());
			tin.close();
			return extractLocation;
		}

		while (tarEntry != null) {
			File destPath = new File(location, tarEntry.getName());
			log.info(tarEntry.getName());
			if (tarEntry.isDirectory()) {
				org.apache.commons.io.FileUtils.forceMkdir(destPath);
			} else {
				File parent = destPath.getParentFile();
				if (parent != null && !parent.exists())
					org.apache.commons.io.FileUtils.forceMkdir(parent);
				FileOutputStream fout = new FileOutputStream(destPath);
				tin.copyEntryContents(fout);
				fout.close();
			}
			tarEntry = tin.getNextEntry();
		}
		tin.close();
		log.info("Terracotta kit extracted at "
				+ extractLocation.getAbsolutePath());
		return extractLocation;
	}

	/**
	 * Download a file from the cache to a specified location
	 *
	 * Reads a byte array from the cache and write it out to a file at the
	 * specified location.
	 *
	 * @param filename
	 *            Name of the file to be downloaded (this will be used as the
	 *            key for the cache).
	 * @param dir
	 *            Destination directory to write the file to.
	 *
	 * @throws IOException
	 *             If there is an error writing to the target file
	 */
	public void download(final String filename, final File dir)
			throws IOException {
		int parts = data.getFilePartsCount(filename);
		log.info("Downloading " + filename);
		org.apache.commons.io.FileUtils.forceMkdir(dir);
		FileOutputStream fos = new FileOutputStream(new File(dir, filename));

		for (int i = 0; i < parts; i++) {
			byte[] file = data.getFilePart(filename, i);
			if (file != null) {
				fos.write(file);
			} else
				throw new IllegalStateException(filename + "." + i
						+ " can't be null.");
		}
		fos.close();
		if (log.isDebugEnabled())
			log.debug(filename + " has been saved to " + dir.getAbsolutePath());
	}

	/**
	 * Finds the list of the names of files matching the regex in the specified
	 * directory.
	 *
	 * @param dir
	 *            directory to be searched
	 * @param includeRegex
	 *            regex for the files to be included. Must match the patterns
	 *            defined by {@link Pattern}
	 * @return list of the names of the files
	 */
	@SuppressWarnings("unchecked")
	public List<File> getFiles(final File dir, final List<String> includeRegex) {
		return getFiles(dir, includeRegex, Collections.EMPTY_LIST);
	}

	/**
	 * Finds the list of the names of files matching the regex in the specified
	 * directory.
	 *
	 * @param dir
	 *            directory to be searched
	 * @param includeRegex
	 *            regex for the files to be included. Must match the pattern
	 *            defined by {@link Pattern}
	 * @param excludeRegex
	 *            regex for the files to be excluded. Must match the pattern
	 *            defined by {@link Pattern}
	 * @return list of the names of the files
	 */
	public List<File> getFiles(final File dir, final List<String> includeRegex,
			final List<String> excludeRegex) {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				log.debug(name);
				for (String p : excludeRegex) {
					if (name.matches(p)) {
						log.info("Excluding: " + name);
						return Boolean.FALSE;
					}
				}
				for (String p : includeRegex) {
					if (name.matches(p))
						return Boolean.TRUE;
				}
				return new File(dir, name).isDirectory();
			}
		};
		List<File> fileList = new ArrayList<File>();
		File[] files = dir.listFiles(filter);
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					fileList.addAll(getFiles(f, includeRegex, excludeRegex));
				} else {
					fileList.add(f);
				}
			}
		} else {
			log.warn("No files found to zip for patterns " + includeRegex
					+ " in dir: " + dir.getAbsolutePath());
		}
		return fileList;
	}

	/**
	 * Uploads all files matching the specified pattern into the cache
	 *
	 * Looks through all files in the directory list and collects all files that
	 * match the specified pattern. The selected files are then uploaded to the
	 * cache one by one.
	 *
	 * @param dirnames
	 *            List of directory paths to search
	 * @param includeRegex
	 *            regex for the files to be included. Must match the pattern
	 *            defined by {@link Pattern}
	 *
	 * @return a list of uploaded files
	 *
	 * @throws IOException
	 *             Error reading any of the files.
	 */
	public List<String> uploadDirectories(final List<File> dirnames,
			final List<String> includeRegex, final List<String> excludeRegex)
			throws IOException {
		List<String> uploadedFiles = new ArrayList<String>();
		for (File dir : dirnames) {
			List<File> list = getFiles(dir, includeRegex, excludeRegex);
			if (list.isEmpty()) {
				log.info("No files found in " + dir);
				continue;
			}
			log.info(String.format("Uploading %s files from directory: %s ",
					list.size(), dir.getAbsolutePath()));
			for (File file : list) {
				uploadedFiles.add(uploadFile(file));
			}
		}
		return uploadedFiles;
	}

	/**
	 * Upload all the files in the specified list of directories matching the
	 * regex
	 *
	 * @param dirnames
	 *            list of the directories to search for
	 * @param includeRegex
	 *            regex for the files to be included. Must match the pattern
	 *            defined by {@link Pattern}
	 * @return list of the names of the files uploaded
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public List<String> uploadDirectories(final List<File> dirnames,
			final List<String> includeRegex) throws IOException {
		return uploadDirectories(dirnames, includeRegex, Collections.EMPTY_LIST);
	}

	/**
	 * Upload all the files in the specified directory matching the regex
	 *
	 * @param dir
	 *            list of the directories to search for
	 * @param regex
	 *            regex for the files to be included. Must match the pattern
	 *            defined by {@link Pattern}
	 * @return list of the names of the files uploaded
	 * @throws IOException
	 */
	public List<String> uploadDirs(final File dir, final String regex)
			throws IOException {
		List<File> list = new ArrayList<File>();
		list.add(dir);
		List<String> patterns = new ArrayList<String>();
		patterns.add(regex);
		return uploadDirectories(list, patterns);
	}

	/**
	 * Download all specified files.
	 *
	 * Downloads all the specified files from the cache and writes them out to
	 * the destination directory.
	 *
	 * @param files
	 *            Names of the files to be downloaded (file names must also be
	 *            keys to the cached file data).
	 * @param destDir
	 *            Target directory where the files will be written.
	 *
	 * @throws IOException
	 *             Error writing out the file.
	 */
	public void downloadAll(final List<String> files, final File destDir)
			throws IOException {
		for (String file : files) {
			download(file, destDir);
		}
	}

	/**
	 * Gzips the files in the directories specified by the pattern into a single
	 * file.
	 *
	 * @param dir
	 *            directories to be searched
	 * @param regex
	 *            regex for the files to be included. Must match the pattern
	 *            defined by {@link Pattern}
	 * @param gzip
	 *            final gzip file name
	 * @throws IOException
	 *             Error reading the file.
	 */

	public void gzipFiles(final File dir, final List<String> regex,
			final File gzip) throws FileNotFoundException, IOException {
		log.info(String.format("Gzipping [%s] into [%s] ...",
				dir.getAbsolutePath(), gzip.getName()));
		TarOutputStream out = new TarOutputStream(new GZIPOutputStream(
				new FileOutputStream(gzip)));
		out.setLongFileMode(TarOutputStream.LONGFILE_GNU);

		List<File> files = getFiles(dir, regex);

		for (File file : files) {
			if (!file.exists())
				continue;
			log.debug(file.getName());
			FileInputStream in = new FileInputStream(file);

			TarEntry te = new TarEntry(file.getParentFile().getName()
					+ File.separator + file.getName());
			te.setSize(file.length());
			out.putNextEntry(te);
			int count = 0;
			byte[] buf = new byte[1024];
			while ((count = in.read(buf, 0, 1024)) != -1) {
				out.write(buf, 0, count);
			}
			out.closeEntry();
			in.close();
		}
		out.finish();
		out.close();
		log.info("Logs gzipped to " + gzip.getName());
	}

	/**
	 * Upload a single file to the cache. Breaks into chunks of 500KB. Each
	 * chunk is stored with separate key i.e. FileName.<part-number>
	 *
	 * @param file
	 *            A file to upload
	 * @throws IOException
	 *             Error reading the file.
	 */

	public String uploadFile(final File file) throws IOException {

		if (!file.exists() || !file.canRead())
			throw new IllegalArgumentException("Cannot open file for reading: "
					+ file.getAbsolutePath());

		InputStream is = new FileInputStream(file);
		long length = file.length();
		log.info(String.format("[%s bytes] %s", nf.format(length),
				file.getName()));

		int largeFileDots = 25;
		int total = 0;
		int index = 0;
		int expected = (int) Math.ceil((double) length / MAX_LENGTH);

		if (expected > largeFileDots)
			log.info("Progress: (Expect " + expected + " dots)");
		while (total < length) {
			// Read MAX_LENGTH (500KB) of data
			byte[] bytes;
			if (length - total < MAX_LENGTH)
				bytes = new byte[(int) (length - total)];
			else
				bytes = new byte[MAX_LENGTH];

			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
			total += offset;
			data.putFileParts(file.getName(), index, bytes);

			if (expected > largeFileDots) {
				// print a progress bar for large files
				if (index > 0 && index % 10 == 0)
					System.out.print(" ");
				System.out.print(".");
			}
			index++;
		}
		if (expected > largeFileDots)
			System.out.print("\n");

		if (total < length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}
		is.close();
		data.putFilePartsCount(file.getName(), index);
		log.debug(String.format("Uploaded %s to the cache %s in %d parts. ",
				file.getName(), data.getName(), index));
		return file.getName();
	}
	//
	// public static void main (String[] ag) throws FileNotFoundException,
	// IOException{
	// FileUtils fu = new FileUtils(null);
	// String[] regex = new String[]{ ".*java", ".*properties" };
	// fu.gzipFiles(new File("src"), Arrays.asList(regex), new
	// File("src.tar.gz"));
	// }
}
