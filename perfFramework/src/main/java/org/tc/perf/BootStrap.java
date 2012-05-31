package org.tc.perf;

import static org.tc.perf.util.Utils.HOSTNAME;
import static org.tc.perf.util.Utils.loadProperties;

import org.apache.log4j.Logger;
import org.tc.perf.work.Work;

/**
 * BootStrap class is used to <li>Start the test process</li> <li>Start Agent</li>
 * <li>
 * List running tests</li> <li>Stop a running test</li> <li>Take perf framework
 * state dump for debugging purposes.</li>
 *
 * @author Himadri Singh
 *
 */
public class BootStrap {

	private static final Logger log = Logger.getLogger(BootStrap.class);

	private static enum CMD {
		AGENT, MASTER, LIST, KILL, DUMP
	};

	private static final String tcConfigSample = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "  <tc:tc-config xsi:schemaLocation=\"http://www.terracotta.org/config "
			+ "http://www.terracotta.org/schema/terracotta-4.xsd\">\n"
			+ "    <servers>\n"
			+ "      <server host=\"%i\" name=\"%i\">\n"
			+ "        <dso-port>8510</dso-port>\n"
			+ "      <server>\n"
			+ "    <servers>\n" + "  </tc:tc-config>\n";

	/**
	 * Start Master process using the configuration provided.
	 *
	 * @see Master#run()
	 * @param props
	 *            configuration file path to be used to load the test.
	 */

	public static final void runMaster(final String props) {
		if (props == null) {
			printHelp();
			return;
		}
		log.info(String.format(
				"Starting performance framework master on %s ...", HOSTNAME));
		Master master = new Master(loadProperties(props));
		master.run();
	}

	/**
	 * Start the agent. It will connect to the terracotta server and ready for
	 * any work alloted to it.
	 *
	 * @see Agent
	 * @see Work
	 */
	public static final void runAgent() {
		log.info(String.format("Starting Test Framework agent on %s...",
				HOSTNAME));
		Agent agent = new Agent();
		agent.poll();
	}

	/**
	 * prints the help message.
	 */
	public static final void printHelp() {
		log.info("Welcome to Terracotta distributed test framework.");
		log.info("Need to start a Terracotta server at port 8510 for framework.");
		log.info("Usage: \t"
				+ BootStrap.class.getName()
				+ "  [ MASTER <test-configuration-file>  |  AGENT  |  LIST  |  KILL <test-id> | DUMP ] ");
		log.info("MASTER \t\tStarts master process to load the test.");
		log.info("\t\t\tIt needs <test-configuration-file> as argument.");
		log.info("AGENT \t\tStarts agent process. ");
		log.info("LIST \t\tLists the tests running (unique ids) in the framework");
		log.info("\t\t\talong with the machines being used.");
		log.info("KILL \t\tKills the test. Needs the test unique id.\n");
		log.info("DUMP \t\tDumps the state of perf framework for debugging purpose.\n");
		log.info("Sample tc-config to start terracotta-server at port 8510:\n\n"
				+ tcConfigSample);
	}

	/**
	 * @see MasterController#listRunningTests()
	 */
	public static final void listTests() {
		//FIXME: restrict using null here
		new MasterController(null).listRunningTests();
	}
	/**
	 * @see MasterController#clearFramework()
	 */
	public static final void clearFramework() {
		new MasterController(null).clearFramework();
	}

	/**
	 * @see MasterController#dumpState()
	 */
	public static final void dumpState(){
		new MasterController(null).dumpState();
	}

	/**
	 * @see MasterController#killTest(String)
	 * @param uniqueId
	 *            unique id alloted to the test
	 */
	public static final void killTest(String uniqueId) {
		if (uniqueId == null) {
			printHelp();
			return;
		}
		new MasterController(null).killTest(uniqueId.trim());
	}

	public static void main(final String[] args) {
		if (args.length > 0) {
			CMD cmd = CMD.valueOf(args[0]);

			switch (cmd) {
			case MASTER:
				if (args.length < 2){
					printHelp();
					System.exit(1);
				}
				runMaster(args[1]);
				break;
			case AGENT:
				runAgent();
				break;
			case KILL:
				if (args.length < 2){
					printHelp();
					System.exit(1);
				}
				killTest(args[1]);
				break;
			case LIST:
				listTests();
				break;
			default:
				printHelp();
			}
		} else
			printHelp();
	}
}
