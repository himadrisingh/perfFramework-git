/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.springframework.samples.petclinic;

/**
 * Simple JavaBean domain object adds a name property to <code>BaseEntity</code>. Used as a base class for objects
 * needing these properties.
 * 
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
public class NamedEntity extends BaseEntity {

  private String name;

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public String toString() {
    return this.getName();
  }

}
