/**
 *
 */
package org.tc.perf.work.l2;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileUtils;
import org.tc.perf.util.TcConfigBuilder;
import org.tc.perf.work.AbstractWork;

/**
 *
 * Work item to set up the L2.
 *
 * Sets up the L2 by downloading and extracting the kit and building the
 * tc-config.xml
 *
 * @author Himadri Singh
 */

public class SetupL2 extends AbstractWork {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(SetupL2.class);

	public SetupL2(final Configuration configuration) {
		super(configuration);
	}

	@Override
	protected void work() throws IOException {
		log.info("Installing terracotta " + configuration.getLocation());
		FileUtils loader = new FileUtils(getDataCache());
		File kit = loader.downloadExtractKit(configuration.getSetupLocation());
		getDataCache().setTcInstallDir(kit.getAbsolutePath());
		downloadLicense(loader, kit);

		TcConfigBuilder builder = new TcConfigBuilder(configuration);
		builder.createConfig(kit);
	}
}
