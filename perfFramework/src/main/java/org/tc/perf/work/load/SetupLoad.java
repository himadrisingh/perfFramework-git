/**
 *
 */
package org.tc.perf.work.load;

import java.io.IOException;
import java.util.List;

import org.tc.perf.util.Configuration;
import org.tc.perf.util.FileUtils;
import org.tc.perf.work.AbstractWork;

/**
 * Work item to setup the L1.
 *
 * Downloads all client code to the L1 machine.
 */
public class SetupLoad extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public SetupLoad(final Configuration configuration) {
		super(configuration);
	}

	@Override
	public void work() throws IOException {
		FileUtils loader = new FileUtils(getDataCache());
		List<String> fileList = getDataCache().getLoadLibs();
		loader.downloadAll(fileList, configuration.getLoadSetupLocation());
	}
}
