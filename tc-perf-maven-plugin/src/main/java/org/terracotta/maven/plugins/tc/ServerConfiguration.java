package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ServerConfiguration {

  private static final String NL = "\n";
  private static final String TAB = "\t";
  private String jvmArgs;
  private String tcConfig;
  private String installationKit;
  private String serversPerMirrorGroup;
  private String persistenceEnabled;
  private String offheapSize;
  private final List<String> agents;

  public ServerConfiguration() {
    agents = new ArrayList<String>();
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(String jvmArgs) {
    this.jvmArgs = jvmArgs;
  }

  public String getTcConfig() {
    return tcConfig;
  }

  public void setTcConfig(String tcConfig) {
    this.tcConfig = tcConfig;
  }

  public String getInstallationKit() {
    return installationKit;
  }

  public void setInstallationKit(String installationKit) {
    this.installationKit = installationKit;
  }

  public List<String> getAgents() {
    return agents;
  }

  public void addAgent(String agent) {
    this.agents.add(agent);
  }

  public String getServersPerMirrorGroup() {
    return serversPerMirrorGroup;
  }

  public void setServersPerMirrorGroup(String serversPerMirrorGroup) {
    this.serversPerMirrorGroup = serversPerMirrorGroup;
  }

  public String getPersistenceEnabled() {
    return persistenceEnabled;
  }

  public void setPersistenceEnabled(String persistenceEnabled) {
    this.persistenceEnabled = persistenceEnabled;
  }

  public String getOffheapSize() {
    return offheapSize;
  }

  public void setOffheapSize(String offheapSize) {
    this.offheapSize = offheapSize;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Server Configuration: ").append(NL);
    if (jvmArgs != null)
      sb.append(TAB).append("JVM Args: " + jvmArgs).append(NL);

    if (installationKit != null)
      sb.append(TAB).append("Installation Kit: " + installationKit).append(NL);

    if (tcConfig != null)
      sb.append(TAB).append("tc-config: " + tcConfig).append(NL);

    if (offheapSize != null)
      sb.append(TAB).append("Offheap: ").append(offheapSize).append(NL);

    if (persistenceEnabled != null)
      sb.append(TAB).append("Persistence: ").append(persistenceEnabled).append(NL);

    if (serversPerMirrorGroup != null)
      sb.append(TAB).append("Server per Mirror Group: ").append(serversPerMirrorGroup).append(NL);

    sb.append(TAB).append("Agents: " + agents).append(NL);
    return sb.toString();
  }

  public Properties getProperties() {
    Properties props = new Properties();
    if (jvmArgs != null)
      props.setProperty("l2_jvm_args", jvmArgs);
    if (installationKit != null)
      props.setProperty("kit.location", installationKit);
    if (tcConfig != null)
      props.setProperty("tc-config", tcConfig);
    if (offheapSize != null){
      props.setProperty("l2.offheap.enabled", String.valueOf(true));
      props.setProperty("l2.offheap.maxDataSize", offheapSize);
    }
    if (persistenceEnabled != null)
      props.setProperty("persistence.enabled", persistenceEnabled);
    if (serversPerMirrorGroup != null)
      props.setProperty("serversPerMirrorGroup", serversPerMirrorGroup);

    StringBuilder sb = new StringBuilder();
    for (String agent : agents) {
      sb.append(agent).append(" ");
    }
    props.setProperty("l2machines", sb.toString());
    return props;
  }

}
