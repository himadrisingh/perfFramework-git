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
 * List all tests running on perf framework
 *
 * @author Himadri Singh
 * @goal list
 */
public class MasterListMojo extends AbstractMojo {

  /**
   * tc-config url for the framework server
   *
   * @parameter expression="${frameworkTcConfigUrl}"
   * @required
   */
  private String frameworkTcConfigUrl;

  public MasterListMojo() {
  }

  public MasterListMojo(MasterListMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    System.setProperty("fw.tc.config" , frameworkTcConfigUrl);
    getLog().info("------------------------------------------------------------------------");
    getLog().info("Starting Perf Framework - List running tests ");
    getLog().info("------------------------------------------------------------------------");
    BootStrap.listTests();
  }
}
