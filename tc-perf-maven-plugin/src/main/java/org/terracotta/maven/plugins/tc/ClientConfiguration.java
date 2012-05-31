package org.terracotta.maven.plugins.tc;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClientConfiguration {

  private static final String NL = "\n";
  private static final String TAB = "\t";
  private String jvmArgs;
  private String className;
  private String arguments;
  private String fwTcConfigParam;
  private String fwNodeCountParam;
  private final List<String> agents;

  public ClientConfiguration() {
    this.agents = new ArrayList<String>();
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(String jvmArgs) {
    this.jvmArgs = jvmArgs;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getArguments() {
    return arguments;
  }

  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  public List<String> getAgents() {
    return agents;
  }

  public void addAgent(String agent) {
    this.agents.add(agent);
  }

  public String getFwTcConfigParam() {
    return fwTcConfigParam;
  }

  public void setFwTcConfigParam(String fwTcConfigParam) {
    this.fwTcConfigParam = fwTcConfigParam;
  }

  public String getFwNodeCountParam() {
    return fwNodeCountParam;
  }

  public void setFwNodeCountParam(String fwNodeCountParam) {
    this.fwNodeCountParam = fwNodeCountParam;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Client Configuration: ").append(NL);
    if (jvmArgs != null)
      sb.append(TAB).append("JVM Args: " + jvmArgs).append(NL);
    sb.append(TAB).append("Main-class: " + className).append(NL);
    if (arguments != null)
      sb.append(TAB).append("Program Args: " + arguments).append(NL);
    if (fwTcConfigParam != null)
      sb.append(TAB).append("fwTcConfigParam: " + fwTcConfigParam).append(NL);
    if (fwNodeCountParam != null)
      sb.append(TAB).append("fwNodeCountParam: " + fwNodeCountParam).append(NL);

    sb.append(TAB).append("Agents: " + agents).append(NL);
    return sb.toString();
  }

  public Properties getProperties(){
    Properties props = new Properties();
    if (jvmArgs != null)
      props.setProperty("l1_jvm_args", jvmArgs);
    props.setProperty("main-classname",className);
    if (arguments != null)
      props.setProperty("arguments",arguments);
    if (fwTcConfigParam != null)
      props.setProperty("fw-tc-config-param", fwTcConfigParam);
    if (fwNodeCountParam != null)
      props.setProperty("fw-node-count-param",fwNodeCountParam);

    StringBuilder sb = new StringBuilder();
    for (String agent : agents){
      sb.append(agent).append(" ");
    }
    props.setProperty("l1machines", sb.toString());
    return props;
  }
}
