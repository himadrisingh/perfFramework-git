/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 *
 */
package org.terracotta.maven.plugins.tc;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.tc.perf.BootStrap;

/**
 * Run perf framework Agent process
 *
 * @author Himadri Singh
 * @goal run-agent
 */
public class AgentRunMojo extends AbstractMojo {

  /**
   * tc-config url for the framework server
   *
   * @parameter expression="${frameworkTcConfigUrl}"
   * @required
   */
  private String frameworkTcConfigUrl;

  public AgentRunMojo() {
  }

  public AgentRunMojo(AgentRunMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    System.setProperty("fw.tc.config" , frameworkTcConfigUrl);
    getLog().info("---------------------------------------");
    getLog().info(" Starting Perf Framework Agent ");
    getLog().info("---------------------------------------");
    BootStrap.runAgent();
  }
}
