package org.terracotta.maven.plugins.tc;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Convert server configuration to {@link ClientConfiguration}
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
 *
 * @author Himadri Singh
 */

public class ClientConfigurationConverter extends AbstractConfigurationConverter {

  public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
    return ClientConfiguration.class.isAssignableFrom(type);
  }

  @SuppressWarnings("rawtypes")
  public Object fromConfiguration(ConverterLookup converterLookup, PlexusConfiguration configuration, Class type,
      Class baseType, ClassLoader classLoader, ExpressionEvaluator expressionEvaluator, ConfigurationListener listener)
      throws ComponentConfigurationException {
    try {
      String className = configuration.getAttribute("className");
      String arguments = configuration.getAttribute("arguments", null);
      String jvmArgs = configuration.getAttribute("jvmargs", null);
      String fwTcConfigParam = configuration.getAttribute("fw-tc-config-param", null);
      String fwNodeCountParam = configuration.getAttribute("fw-node-count-param", null);

      if (className == null)
        throw new ComponentConfigurationException(
            "Main class name is expected. <client className=\"main.class.name\" >");

      ClientConfiguration client = new ClientConfiguration();
      client.setArguments(arguments);
      client.setJvmArgs(jvmArgs);
      client.setClassName(className);
      client.setFwNodeCountParam(fwNodeCountParam);
      client.setFwTcConfigParam(fwTcConfigParam);

      PlexusConfiguration[] agents = configuration.getChildren("agents");
      if (agents.length != 1) {
        throw new ComponentConfigurationException("Only one <agents> tag is expected.");
      }
      for (PlexusConfiguration agent : agents[0].getChildren("agent")) {
        client.addAgent(agent.getValue());
      }
      return client;
    } catch (NumberFormatException ex) {
      throw new ComponentConfigurationException(configuration, ex);
    }
  }

}
