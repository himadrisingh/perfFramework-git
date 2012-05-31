package org.tc.perf.work;

import org.tc.perf.MasterController;
import org.tc.perf.util.Configuration;

/**
 * This class starts the {@link MasterController} in a separate thread and notifies the
 * Master that the test has been initiated.
 *
 * @author Himadri Singh
 * @see MasterController
 */
public class ControllerWork extends AbstractWork {

	private static final long serialVersionUID = 1L;
	private final MasterController control;

	public ControllerWork(Configuration configuration) {
		super(configuration);
		control = new MasterController(configuration);
	}

	@Override
	protected void work() {
		new Thread(control, "MasterControl").start();
	}

}
