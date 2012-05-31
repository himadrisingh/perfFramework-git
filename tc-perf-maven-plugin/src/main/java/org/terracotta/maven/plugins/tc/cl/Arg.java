/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.maven.plugins.tc.cl;

import java.io.File;

public interface Arg {
  void setValue(String value);

  void setLine(String line);

  void setFile(File value);

  String[] getParts();
}
