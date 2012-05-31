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
 * Clear all tests on perf framework
 *
 * @author Himadri Singh
 * @goal clear-framework
 */
public class MasterClearMojo extends AbstractMojo {

  /**
   * Hidden method to clear the framework
   * It has super powers!
   *
   * @parameter expression="${frameworkTcConfigUrl}"
   * @required
   */
  private String frameworkTcConfigUrl;

  public MasterClearMojo() {
  }

  public MasterClearMojo(MasterClearMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());
  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    System.setProperty("fw.tc.config" , frameworkTcConfigUrl);
    getLog().info("------------------------------------------------------------------------");
    getLog().info("Starting Perf Framework - clearing all ");
    getLog().info("------------------------------------------------------------------------");
    BootStrap.clearFramework();
  }
}
