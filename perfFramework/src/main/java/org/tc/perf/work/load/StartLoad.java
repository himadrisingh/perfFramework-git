/**
 *
 */
package org.tc.perf.work.load;

import static org.tc.perf.util.Utils.CLASSPATH_SEPARATOR;

import java.util.List;

import org.tc.perf.process.ProcessConfig;
import org.tc.perf.util.Configuration;
import org.tc.perf.work.AbstractWork;

/**
 * @author Himadri Singh
 *
 */
public class StartLoad extends AbstractWork {

	private static final long serialVersionUID = 1L;

	public StartLoad(final Configuration configuration) {
		super(configuration);
	}

	private ProcessConfig getProcessConfig() {
		String mainClass = configuration.getLoadMainClass();
		List<String> arguments = configuration.getLoadArguments();

		List<String> fileList = getDataCache().getLoadLibs();
		StringBuilder classpath = new StringBuilder();
		for (String file : fileList) {
			classpath.append(file).append(CLASSPATH_SEPARATOR);
		}
		List<String> jvmArgs = configuration.getLoadJvmArgs();

		ProcessConfig config = new ProcessConfig(mainClass, "load");
		config.setClasspath(classpath.toString()).setArguments(arguments)
				.setLocation(configuration.getLoadSetupLocation())
				.setLogsDir(configuration.getLoadLogLocation())
				.setJvmArgs(jvmArgs);
		return config;
	}

	@Override
	protected void work() {
		ProcessConfig config = getProcessConfig();
		execute(config);
	}

}
