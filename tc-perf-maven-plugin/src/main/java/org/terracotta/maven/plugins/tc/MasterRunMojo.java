/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 *
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.tc.perf.BootStrap;
import org.tc.perf.Master;
import org.tc.perf.MasterController;
import org.terracotta.maven.plugins.tc.cl.CommandLineException;
import org.terracotta.maven.plugins.tc.cl.CommandLineUtils;
import org.terracotta.maven.plugins.tc.cl.Commandline;
import org.terracotta.maven.plugins.tc.cl.JavaShell;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;

/**
 * Run perf framework Master process
 *
 * @author Eugene Kuleshov
 * @goal run
 * @requiresDependencyResolution compile
 * @configurator override
 */
public class MasterRunMojo extends AbstractMojo {

  /**
   * Project name
   *
   * @parameter expression="${project.artifactId}"
   */

  private String testName;

  /**
   * Project name
   *
   * @parameter expression="${project.build.finalName}"
   */

  private String buildName;

  /**
   * Project name
   *
   * @parameter expression="${project.packaging}"
   */

  private String packaging;

  /**
   * tc-config url for the framework server
   *
   * @parameter expression="${frameworkTcConfigUrl}"
   * @required
   */
  private String frameworkTcConfigUrl;

  /**
   * Test jars
   *
   * @parameter expression="${project.runtimeClasspathElements}"
   * @required
   * @readonly
   */
  private List<String> classpathElements;

  /**
   * Classpath for plugin
   *
   * @parameter expression="${plugin.artifacts}"
   * @required
   */
  private List<Artifact> pluginArtifacts;

  /**
   * Path to the java executable to use to spawned processes
   *
   * @parameter expression="${jvm}"
   * @optional
   */
  private String jvm;

  /**
   * Path to the java executable to use to spawned processes
   *
   * @parameter expression="${recipients}"
   * @optional
   */
  private String recipients;

  /**
   * JVM Arguments for the {@link Master} java process
   *
   * @parameter expression="${jvmargs}"
   * @optional
   */
  private String jvmargs;

  /**
   * Working directory for the spawned java processes
   *
   * @parameter expression="${workingDirectory}" default-value="${basedir}"
   */
  private File workingDirectory;

  /**
   * Build directory
   *
   * @parameter expression="${project.build.directory}"
   */
  private File buildDirectory;

  /**
   * Location of the DSO config (tc-config.xml)
   *
   * @parameter expression="${config}" default-value="${basedir}/tc-config.xml"
   */
  private File config;

  /**
   * Location of specific perf framework properties
   *
   * @parameter expression="${frameworkProperties}"
   * @optional
   */
  private String frameworkProperties;

  /**
   * To run {@link MasterController} in line with maven command or in background.
   *
   * @parameter expression="${inline}"
   * @optional
   */
  private String inline;

  /**
   * Configuration for the perf framework {@link Master} processes to start. <br>
   * <br>
   *
   * <pre>
   *  &lt;client jvmargs=&quot;-Xmx1g&quot;
   *    className=&quot;com.terracotta.EhCachePerfTest&quot;
   *    arguments=&quot;src/main/resources/mvn-tc-run.node.properties&quot;
   *    fw-tc-config-param=&quot;expressTerracottaUrl&quot;
   *    fw-node-count-param=&quot;numOfNodes&quot; &gt;
   *  &lt;agents&gt;
   *   &lt;agent&gt;agent01&lt;/agent&gt;
   *   &lt;agent&gt;agent02&lt;/agent&gt;
   *  &lt;/agents&gt;
   * </pre>
   *
   * @parameter expression="${client}"
   * @required
   */
  private ClientConfiguration client;

  /**
   * Configuration for the perf framework {@link Master} processes to start. <br>
   * <br>
   *
   * <pre>
   * &lt;server jvmargs=&quot;-Xms1g -Xmx1g&quot;
   *    tcConfig=&quot;tc-config.xml&quot;
   *    installationKit=&quot;terracotta-xxx.tar.gz&quot;&gt;
   *  &lt;agents&gt;
   *   &lt;agent&gt;agent01&lt;/agent&gt;
   *   &lt;agent&gt;agent02&lt;/agent&gt;
   *  &lt;/agents&gt;
   * &lt;/server&gt;
   * </pre>
   *
   * @parameter expression="${server}"
   * @required
   */
  private ServerConfiguration server;

  private boolean waitForCompletion() {
    return true;
  }

  public MasterRunMojo() {
  }

  public MasterRunMojo(MasterRunMojo mojo) {
    setLog(mojo.getLog());
    setPluginContext(mojo.getPluginContext());

    this.config = mojo.config;
    this.workingDirectory = mojo.workingDirectory;
    this.jvm = mojo.jvm;
    this.classpathElements = mojo.classpathElements;
    this.pluginArtifacts = mojo.pluginArtifacts;
  }

  private Commandline createCommandLine() {
    String executable;
    if (jvm != null && jvm.length() > 0) {
      executable = jvm;
    } else {
      String os = System.getProperty("os.name");
      if (os.indexOf("Windows") > -1) {
        executable = System.getProperty("java.home") + "/bin/java.exe";
      } else {
        executable = System.getProperty("java.home") + "/bin/java";
      }
    }

    JavaShell javaShell = new JavaShell(executable);

    Commandline cmd = new Commandline(javaShell);
    cmd.createArgument().setLine(createJvmArguments());
    return cmd;
  }

  private String createJvmArguments() {
    StringBuilder sb = new StringBuilder(jvmargs != null ? jvmargs : "");
    return sb.toString();
  }

  private String createPluginClasspath() {
    String classpath = "";
    for (Artifact artifact : pluginArtifacts) {
      if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
        String groupId = artifact.getGroupId();
        // XXX workaround to shorten the classpath
        if (!groupId.startsWith("org.apache.maven") //
            && !"org.codehaus.cargo".equals(groupId) //
            && !"org.springframework".equals(groupId)) {
          classpath += artifact.getFile().getAbsolutePath() + File.pathSeparator;
        }
      }
    }
    return classpath;
  }

  private String createFrameworkPropertiesAsFile(File target) throws IOException {
    Properties props = new Properties();
    props.putAll(server.getProperties());
    props.putAll(client.getProperties());
    String pname = "fw-default.properties";
    props.put("cases", pname);
    props.put("test", testName);
    props.put("directories", "frameworkTestJars");
    props.put("kit.location", createTerracottaKit(target));
    if (inline == null)
      props.put("inline", "true");
    else
      props.put("inline", String.valueOf(inline));

    if (recipients != null)
      props.put("recipients", recipients);
    if (frameworkProperties != null)
      props.putAll(loadProperties(frameworkProperties));

    File fwProps = null;
    try {
      fwProps = new File(target, pname);
      FileOutputStream fos = new FileOutputStream(fwProps);
      props.store(fos, "Created by perf framework for Master Process.");
      fos.close();
    } catch (IOException e) {
      throw new RuntimeException("Failed to create framework properties.", e);
    }

    return fwProps.getAbsolutePath();
  }

  private Properties loadProperties(final String location) throws IllegalStateException {
    try {
      getLog().debug("Loading properties from " + (new File(location).getAbsolutePath()));
      FileInputStream fis = new FileInputStream(location);
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      return props;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot find properties file: " + location, e);
    }
  }

  private String quoteIfNeeded(String path) {
    if (path.indexOf(" ") > 0) {
      return "\"" + path + "\"";
    }
    return path;
  }

  class ForkedProcessStreamConsumer implements StreamConsumer {
    private final String prefix;

    public ForkedProcessStreamConsumer(String prefix) {
      this.prefix = prefix;
    }

    public void consumeLine(String msg) {
      getLog().info("[" + prefix + "] " + msg);
    }
  }

  public String getServerStatus(String jmxUrl) throws MalformedURLException, IOException {
    getLog().debug("Connecting to DSO server at " + jmxUrl);
    JMXServiceURL url = new JMXServiceURL(jmxUrl);
    JMXConnector jmxc = null;
    try {
      jmxc = JMXConnectorFactory.connect(url, null);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      TCServerInfoMBean serverMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO,
          TCServerInfoMBean.class, false);

      return serverMBean.getHealthStatus();
    } finally {
      if (jmxc != null) {
        try {
          jmxc.close();
        } catch (IOException ex) {
          getLog().error("Error closing jmx connection", ex);
        }
      }
    }
  }

  void log(String msg, Exception ex) {
    if (getLog().isDebugEnabled()) {
      getLog().error(msg, ex);
    } else {
      getLog().error(msg);
    }
  }

  private Startable createFrameworkStartable(String props) throws IOException {
    Commandline cmd = createCommandLine();

    if (workingDirectory != null) {
      cmd.setWorkingDirectory(workingDirectory);
    }

    cmd.createArgument().setLine("-Dfw.tc.config=" + frameworkTcConfigUrl);
    cmd.createArgument().setLine("-Dtests.root.dir=" + buildDirectory.getPath());
    cmd.createArgument().setValue("-cp");
    cmd.createArgument().setValue(quoteIfNeeded(createPluginClasspath()));
    cmd.createArgument().setValue(BootStrap.class.getCanonicalName());
    cmd.createArgument().setValue("MASTER");
    cmd.createArgument().setValue(props);

    return new CmdStartable("perfFwMaster", cmd);
  }

  public void setJvmargs(String jvmargs) {
    this.jvmargs = jvmargs;
  }

  public void setJvm(String jvm) {
    this.jvm = jvm;
  }

  public static interface Startable {
    public void start(boolean wait);

    public void stop();

    public String getNodeName();
  }

  public class CmdStartable implements Startable {
    private final String nodeName;
    private final Commandline cmd;

    public CmdStartable(String nodeName, Commandline cmd) {
      this.nodeName = nodeName;
      this.cmd = cmd;
    }

    public void start(boolean wait) {
      try {
        StreamConsumer streamConsumer = new ForkedProcessStreamConsumer(nodeName);
        CommandLineUtils.executeCommandLine(cmd, null, streamConsumer, streamConsumer, !wait);
      } catch (CommandLineException e) {
        getLog().error("Failed to start node " + nodeName, e);
      }
    }

    public void stop() {
      // ignore
    }

    public String getNodeName() {
      return nodeName;
    }

    @Override
    public String toString() {
      return cmd.toString();
    }

  }

  // TODO: Get dependencies of org.terracotta only
  private String createTerracottaKit(File target) throws IOException {
    File dest = new File("terracotta-kit/lib");
    if (dest.exists())
      FileUtils.deleteDirectory(dest);
    FileUtils.forceMkdir(dest);
    for (Artifact artifact : pluginArtifacts) {
      if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
        String groupId = artifact.getGroupId();
        // TODO: XXX workaround to shorten the classpath
        if (!groupId.startsWith("org.apache.maven") //
            && !"org.codehaus.plexus".equals(groupId)) {
          if (groupId.equals("org.terracotta")
              && (artifact.getArtifactId().equals("terracotta") || artifact.getArtifactId().equals("terracotta-ee"))) {
            File tcJar = new File(dest, "tc.jar");
            // if tc.jar doesn't exists copy the terracotta artifact as tc.jar
            if (!tcJar.isFile())
              FileUtils.copyFile(artifact.getFile(), new File(dest, "tc.jar"));
            else // if tc.jar exists then overwrite the copy with terracotta-ee artifact
            if (artifact.getArtifactId().equals("terracotta-ee"))
              FileUtils.copyFile(artifact.getFile(), new File(dest, "tc.jar"));
          } else
            FileUtils.copyFileToDirectory(artifact.getFile(), dest);
        }
      }
    }

    File gzip = new File(target, "terracotta.tar.gz");
    URI relativePath = new File(".").toURI().relativize(dest.toURI());
    File dir = new File(relativePath.toString());

    TarOutputStream out = new TarOutputStream(new GZIPOutputStream(new FileOutputStream(gzip)));
    out.setLongFileMode(TarOutputStream.LONGFILE_GNU);

    File[] files = dir.listFiles();

    for (File file : files) {
      // File logFile = new File(dir + FILE_SEPARATOR + file);
      if (!file.exists())
        continue;
      getLog().debug(file.getPath());

      TarEntry te = new TarEntry(file);
      te.setSize(file.length());
      out.putNextEntry(te);
      int count = 0;
      byte[] buf = new byte[1024];
      FileInputStream in = new FileInputStream(file);
      while ((count = in.read(buf, 0, 1024)) != -1) {
        out.write(buf, 0, count);
      }
      in.close();
      out.closeEntry();
    }
    out.finish();
    out.close();
    FileUtils.deleteDirectory(dest.getParentFile());
    getLog().info("Gzipped the kit: " + gzip.getPath());
    return gzip.getAbsolutePath();
  }

  @SuppressWarnings("unchecked")
  private void copyDependencies(File target) throws IOException {
    File dependencyDir = new File(target, "frameworkTestJars");
    if (dependencyDir.exists())
      FileUtils.deleteDirectory(dependencyDir);
    FileUtils.forceMkdir(dependencyDir);

    getLog().info("Copying dependencies and class-path files to " + dependencyDir.getAbsolutePath());
    String name = buildDirectory + File.separator + buildName + "." + packaging;
    classpathElements.add(name);
    for (String elements : classpathElements) {
      File f = new File(elements);
      if (f.isFile()) {
        getLog().debug("Copying " + elements);
        FileUtils.copyFileToDirectory(f, dependencyDir);
      } else if (f.isDirectory()) {
        Iterator<File> it = FileUtils.iterateFiles(f, new IOFileFilter() {
          public boolean accept(File dir, String name) {
            if (name.endsWith(".class"))
              return false;
            return true;
          }

          public boolean accept(File file) {
            if (file.getName().endsWith(".class"))
              return false;
            return true;
          }
        }, TrueFileFilter.INSTANCE);

        while (it.hasNext()) {
          File fi = it.next();
          getLog().debug("Copying " + fi.getName());
          FileUtils.copyFileToDirectory(fi, dependencyDir);
        }

      }
    }

  }

  public void execute() throws MojoExecutionException, MojoFailureException {
    File target = new File (buildDirectory, testName);
    if (target.exists())
      try {
        FileUtils.deleteDirectory(target);
        FileUtils.forceMkdir(target);
      } catch (IOException e1) {
        throw new MojoFailureException("Cannot create temp dir", e1);
      }

    getLog().info("------------------------------------------------------------------------");
    try {
      copyDependencies(target);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage());
    }
    getLog().info("------------------------------------------------------------------------");
    getLog().info("Starting Perf Framework - Master process ");
    getLog().info("------------------------------------------------------------------------");

    getLog().info(server.toString());
    getLog().info(client.toString());

    Startable startable;
    try {
      String props = createFrameworkPropertiesAsFile(target);
      getLog().debug("Created " + props);
      startable = createFrameworkStartable(props);
      getLog().debug("Startable: " + startable);
      startable.start(waitForCompletion());
    } catch (IOException e) {
      e.printStackTrace();
    }
    getLog().info("Finished completion of the Perf Framework - Master process");
  }
}
