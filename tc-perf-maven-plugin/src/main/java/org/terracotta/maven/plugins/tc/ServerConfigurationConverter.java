package org.terracotta.maven.plugins.tc;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Convert server configuration to {@link ServerConfiguration}
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
 * @author Himadri Singh
 */

public class ServerConfigurationConverter extends AbstractConfigurationConverter {

  public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
    return ServerConfiguration.class.isAssignableFrom(type);
  }

  @SuppressWarnings("rawtypes")
  public Object fromConfiguration(ConverterLookup converterLookup, PlexusConfiguration configuration, Class type,
      Class baseType, ClassLoader classLoader, ExpressionEvaluator expressionEvaluator, ConfigurationListener listener)
      throws ComponentConfigurationException {
    try {
      String installationKit = configuration.getAttribute("installationKit", null);
      String tcConfig = configuration.getAttribute("tcConfig", null);
      String jvmArgs = configuration.getAttribute("jvmargs", null);
      String serversPerMirrorGroup = configuration.getAttribute("serversPerMirrorGroup", null);
      String persistenceEnabled = configuration.getAttribute("persistenceEnabled", null);
      String offheapSize = configuration.getAttribute("offheapSize", null);

      ServerConfiguration server = new ServerConfiguration();
      server.setInstallationKit(installationKit);
      server.setJvmArgs(jvmArgs);
      server.setTcConfig(tcConfig);
      server.setServersPerMirrorGroup(serversPerMirrorGroup);
      server.setPersistenceEnabled(persistenceEnabled);
      server.setOffheapSize(offheapSize);

      PlexusConfiguration[] agents = configuration.getChildren("agents");

      if (agents.length != 1) {
        throw new ComponentConfigurationException("Only one <agents> tag is expected.");
      }

      for (PlexusConfiguration agent : agents[0].getChildren("agent")) {
        server.addAgent(agent.getValue());
      }
      return server;
    } catch (NumberFormatException ex) {
      throw new ComponentConfigurationException(configuration, ex);
    }
  }
}
