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
 * Kill test running on perf framework
 *
 * @author Himadri Singh
 * @goal kill
 */
public class MasterKillMojo extends AbstractMojo {

  /**
   * tc-config url for the framework server
   *
   * @parameter expression="${frameworkTcConfigUrl}"
   * @required
   */
  private String frameworkTcConfigUrl;

  /**
   * test unique id, which needs to be killed.
   * tc-perf:list lists the running tests containing test unique ids
   *
   * @parameter expression="${fw.testId}"
   * @required
   */
  private String testId;


  public MasterKillMojo() {
  }

  public MasterKillMojo(MasterKillMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    System.setProperty("fw.tc.config" , frameworkTcConfigUrl);
    getLog().info("------------------------------------------------------------------------");
    getLog().info("Starting Perf Framework - Kill tests with ID: " + testId);
    getLog().info("------------------------------------------------------------------------");
    BootStrap.killTest(testId);
  }
}
