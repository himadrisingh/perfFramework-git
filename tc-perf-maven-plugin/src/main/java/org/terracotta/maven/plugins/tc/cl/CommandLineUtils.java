/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.maven.plugins.tc.cl;

/*
 * The MIT License
 * 
 * Copyright (c) 2004, The Codehaus
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l </a>
 * @version $Id: CommandLineUtils.java 15287 2009-04-29 11:52:00Z ekulesho $
 */
public abstract class CommandLineUtils {
  static Map<Long, Process> processes = Collections.synchronizedMap(new HashMap<Long, Process>());

  static {
    Runtime.getRuntime().addShutdownHook(new Thread("CommandlineUtil shutdown") {
      public void run() {
        if (processes != null && processes.size() > 0) {
          synchronized (processes) {
            Process[] p = processes.values().toArray(new Process[processes.size()]);
            System.err.println("Destroying " + p.length + " processes");
            for (int i = 0; i < p.length; i++) {
              p[i].destroy();
            }
            System.err.println("Destroyed " + p.length + " processes");
          }
        }
      }
    });
  }

  public static class StringStreamConsumer implements StreamConsumer {
    private final StringBuffer string = new StringBuffer();

    private final String ls = System.getProperty("line.separator");

    public void consumeLine(String line) {
      string.append(line + ls);
    }

    public String getOutput() {
      return string.toString();
    }
  }

  public static Process executeCommandLine(Commandline cl, StreamConsumer systemOut, StreamConsumer systemErr)
      throws CommandLineException {
    return executeCommandLine(cl, null, systemOut, systemErr, false);
  }

  public static Process executeCommandLine(Commandline cl, InputStream systemIn, StreamConsumer systemOut,
      StreamConsumer systemErr, boolean spawn) throws CommandLineException {
    Process p = cl.execute();

    StreamFeeder inputFeeder = null;
    if (systemIn != null) {
      inputFeeder = new StreamFeeder(systemIn, p.getOutputStream());
      inputFeeder.start();
    }

    StreamPumper outputPumper = new StreamPumper(p.getInputStream(), systemOut);
    outputPumper.start();

    StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), systemErr);
    errorPumper.start();

    if (spawn) {
      try {
        Thread.sleep(500L); // for some reason process don't start without this delay
      } catch (InterruptedException ex) {
        throw new CommandLineException("Error while executing external command, process killed.", ex);
      }
      return p;
    }

    processes.put(new Long(cl.getPid()), p);
    try {
      p.waitFor();

      if (inputFeeder != null) {
        synchronized (inputFeeder) {
          if (!inputFeeder.isDone()) {
            inputFeeder.wait();
          }
        }
      }

      synchronized (outputPumper) {
        if (!outputPumper.isDone()) {
          outputPumper.wait();
        }
      }

      synchronized (errorPumper) {
        if (!errorPumper.isDone()) {
          errorPumper.wait();
        }
      }

      return p;

    } catch (InterruptedException ex) {
      killProcess(cl.getPid());
      throw new CommandLineException("Error while executing external command, process killed.", ex);
    } finally {
      if (inputFeeder != null) {
        inputFeeder.close();
      }

      outputPumper.close();
      errorPumper.close();

      processes.remove(new Long(cl.getPid()));
    }
  }

  public static Properties getSystemEnvVars() throws IOException {
    return getSystemEnvVars(true);
  }

  /**
   * Return the shell environment variables. If <code>caseSensitive == true</code>, then envar keys will all be
   * upper-case.
   * 
   * @param caseSensitive Whether environment variable keys should be treated case-sensitively.
   * @return Properties object of (possibly modified) envar keys mapped to their values.
   * @throws IOException
   */
  public static Properties getSystemEnvVars(boolean caseSensitive) throws IOException {
    Process p = null;

    Properties envVars = new Properties();

    Runtime r = Runtime.getRuntime();

    String os = System.getProperty("os.name").toLowerCase();

    // If this is windows set the shell to command.com or cmd.exe with correct
    // arguments.
    if (os.indexOf("windows") != -1) {
      if (os.indexOf("95") != -1 || os.indexOf("98") != -1 || os.indexOf("Me") != -1) {
        p = r.exec("command.com /c set");
      } else {
        p = r.exec("cmd.exe /c set");
      }
    } else {
      p = r.exec("env");
    }

    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

    String line;

    String lastKey = null;
    String lastVal = null;

    while ((line = br.readLine()) != null) {
      int idx = line.indexOf('=');

      if (idx > 1) {
        lastKey = line.substring(0, idx);

        if (!caseSensitive) {
          lastKey = lastKey.toUpperCase();
        }

        lastVal = line.substring(idx + 1);

        envVars.setProperty(lastKey, lastVal);
      } else if (lastKey != null) {
        lastVal += "\n" + line;

        envVars.setProperty(lastKey, lastVal);
      }
    }

    return envVars;
  }

  /**
   * Kill a process launched by executeCommandLine methods Doesn't work correctly on windows, only the cmd process will
   * be destroy but not the sub process (<a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4770092">Bug
   * ID 4770092</a>)
   * 
   * @param pid The pid of command return by Commandline.getPid()
   */
  public static void killProcess(long pid) {
    Process p = processes.get(new Long(pid));

    if (p != null) {
      p.destroy();
      System.out.println("killed.");
      processes.remove(new Long(pid));
    } else {
      System.out.println("don't exist.");
    }
  }

  public static boolean isAlive(long pid) {
    return (processes.get(new Long(pid)) != null);
  }

  public static String[] translateCommandline(String toProcess) throws Exception {
    if (toProcess == null || toProcess.length() == 0) {
      return new String[0];
    }

    // parse with a simple finite state machine

    final int normal = 0;
    final int inQuote = 1;
    final int inDoubleQuote = 2;
    int state = normal;
    StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
    Vector<String> v = new Vector<String>();
    StringBuffer current = new StringBuffer();

    while (tok.hasMoreTokens()) {
      String nextTok = tok.nextToken();
      switch (state) {
        case inQuote:
          if ("\'".equals(nextTok)) {
            state = normal;
          } else {
            current.append(nextTok);
          }
          break;
        case inDoubleQuote:
          if ("\"".equals(nextTok)) {
            state = normal;
          } else {
            current.append(nextTok);
          }
          break;
        default:
          if ("\'".equals(nextTok)) {
            state = inQuote;
          } else if ("\"".equals(nextTok)) {
            state = inDoubleQuote;
          } else if (" ".equals(nextTok)) {
            if (current.length() != 0) {
              v.addElement(current.toString());
              current.setLength(0);
            }
          } else {
            current.append(nextTok);
          }
          break;
      }
    }

    if (current.length() != 0) {
      v.addElement(current.toString());
    }

    if (state == inQuote || state == inDoubleQuote) {
      throw new CommandLineException("unbalanced quotes in " + toProcess);
    }

    String[] args = new String[v.size()];
    v.copyInto(args);
    return args;
  }

  /**
   * <p>
   * Put quotes around the given String if necessary.
   * </p>
   * <p>
   * If the argument doesn't include spaces or quotes, return it as is. If it contains double quotes, use single quotes -
   * else surround the argument by double quotes.
   * </p>
   * 
   * @throws CommandLineException if the argument contains both, single and double quotes.
   */
  public static String quote(String argument) throws CommandLineException {
    return quote(argument, false, false, true);
  }

  /**
   * <p>
   * Put quotes around the given String if necessary.
   * </p>
   * <p>
   * If the argument doesn't include spaces or quotes, return it as is. If it contains double quotes, use single quotes -
   * else surround the argument by double quotes.
   * </p>
   * 
   * @throws CommandLineException if the argument contains both, single and double quotes.
   */
  public static String quote(String argument, boolean wrapExistingQuotes) throws CommandLineException {
    return quote(argument, false, false, wrapExistingQuotes);
  }

  public static String quote(String argument, boolean escapeSingleQuotes, boolean escapeDoubleQuotes,
      boolean wrapExistingQuotes) throws CommandLineException {
    if (argument.indexOf("\"") > -1) {
      if (argument.indexOf("\'") > -1) {
        throw new CommandLineException("Can't handle single and double quotes in same argument");
      } else {
        if (escapeSingleQuotes) {
          return "\\\'" + argument + "\\\'";
        } else if (wrapExistingQuotes) {
          return '\'' + argument + '\'';
        }
      }
    } else if (argument.indexOf("\'") > -1) {
      if (escapeDoubleQuotes) {
        return "\\\"" + argument + "\\\"";
      } else if (wrapExistingQuotes) {
        return '\"' + argument + '\"';
      }
    } else if (argument.indexOf(" ") > -1) {
      if (escapeDoubleQuotes) {
        return "\\\"" + argument + "\\\"";
      } else {
        return '\"' + argument + '\"';
      }
    }

    return argument;
  }

  public static String toString(String[] line) {
    // empty path return empty string
    if (line == null || line.length == 0) {
      return "";
    }

    // path containing one or more elements
    final StringBuffer result = new StringBuffer();
    for (int i = 0; i < line.length; i++) {
      if (i > 0) {
        result.append(' ');
      }
      try {
        result.append(quote(line[i]));
      } catch (Exception e) {
        System.err.println("Error quoting argument: " + e.getMessage());
      }
    }
    return result.toString();
  }

}
