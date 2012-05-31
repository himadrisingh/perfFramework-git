/**
 *
 */
package org.tc.perf.work.l1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileUtils;
import org.tc.perf.util.TcConfigBuilder;
import org.tc.perf.work.AbstractWork;

/**
 * Work item to setup the L1.
 *
 * Downloads all client code to the L1 machine.
 */
public class SetupL1 extends AbstractWork {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(SetupL1.class);
	private static final String FW_PROP = "perf-framework.properties";

	public SetupL1(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public void work() throws IOException {
		File clientSetupLocation = configuration.getClientSetupLocation();

		FileUtils loader = new FileUtils(getDataCache());
		List<String> fileList = getDataCache().getTestLibs();
		loader.downloadAll(fileList, clientSetupLocation);

		File kit = loader.downloadExtractKit(configuration.getSetupLocation());
		log.info("Kit Path: " + kit.getAbsolutePath());
		List<String> kitLibs = new ArrayList<String>();
		for (File k : loader.getFiles(kit, configuration.getClasspathExclude())) {
			log.info("*** Adding kit libs to classpath: " + k.getPath());
			kitLibs.add(k.getPath());
		}
		getDataCache().setTestKitLibs(kitLibs);

		downloadLicense(loader, configuration.getClientSetupLocation());
		TcConfigBuilder tc = new TcConfigBuilder(configuration);
		File tcConfig = tc.createConfig(clientSetupLocation);

		FileOutputStream fos = new FileOutputStream(new File(
				clientSetupLocation, FW_PROP));
		Properties props = new Properties();
		props.putAll(configuration.getProps());
		props.put(configuration.getFwTcConfigParam(), tcConfig.getAbsolutePath());
		props.put(configuration.getFwNodeCountParam(),
				String.valueOf(configuration.getL1machines().size()));
		props.store(fos, "Perf Framework created this properties file.");
		fos.close();
		log.info("Created properties.xml file...");

	}
}
