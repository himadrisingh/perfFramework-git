/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.maven.plugins.tc.cl;


public class JavaShell extends Shell {

  public JavaShell(String executable) {
    setShellCommand(executable);
    setQuotedExecutableEnabled(true);
    // setShellArgs(new String[] { "/X", "/C" });
  }

}
