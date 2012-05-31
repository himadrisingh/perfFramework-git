/*
 * Copyright 2002-2004 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.springframework.jdbc.datasource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Proxy for a target DataSource, fetching actual JDBC Connections lazily, i.e. not until first creation of a Statement.
 * Connection initialization properties like auto-commit mode, transaction isolation and read-only mode will be kept and
 * applied to the actual JDBC Connection as soon as an actual Connection is fetched (if ever). Consequently, commit and
 * rollback calls will be ignored if no Statements have been created.
 * <p/>
 * <p>
 * This DataSource proxy allows to avoid fetching JDBC Connections from a pool unless actually necessary. JDBC
 * transaction control can happen without fetching a Connection from the pool or communicating with the database; this
 * will be done lazily on first creation of a JDBC Statement.
 * <p/>
 * <p>
 * Lazy fetching of physical JDBC Connections is particularly beneficial in a generic transaction demarcation
 * environment. It allows you to demarcate transactions on all methods that could potentially perform data access,
 * without paying a performance penalty if no actual data access happens.
 * <p/>
 * <p>
 * <b>This DataSource proxy gives you behavior analogous to JTA and a transactional JNDI DataSource (as provided by the
 * J2EE server), even with a local transaction strategy like DataSourceTransactionManager or
 * HibernateTransactionManager.</b> It does not add value with Spring's JtaTransactionManager as transaction strategy.
 * <p/>
 * <p>
 * Lazy fetching of JDBC Connections is also recommended for read-only operations with Hibernate, in particular if the
 * chances of resolving the result in the second-level cache are high.</b> This avoids the need to communicate with the
 * database at all for such read-only operations. You will get the same effect with non-transactional reads, but lazy
 * fetching of JDBC Connections allows you to still perform reads in transactions.
 * <p/>
 * <p>
 * <b>NOTE:</b> This DataSource proxy needs to return wrapped Connections to handle lazy fetching of an actual JDBC
 * Connection. Therefore, the returned Connections cannot be cast to a native JDBC Connection type like
 * OracleConnection, or to a connection pool implementation type. Use a corresponding NativeJdbcExtractor to retrieve
 * the native JDBC Connection.
 * 
 * @author Juergen Hoeller
 * @see ConnectionProxy
 * @see DataSourceTransactionManager
 * @see org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor
 * @since 1.1.4
 */
public class FasterLazyConnectionDataSourceProxy extends DelegatingDataSource {

  private final static Class[] constructorParams = { InvocationHandler.class };

  private Boolean              defaultAutoCommit;

  private Integer              defaultTransactionIsolation;
  private Class                proxyClass;

  /**
   * Create a new LazyConnectionDataSourceProxy.
   * 
   * @see #setTargetDataSource
   */
  public FasterLazyConnectionDataSourceProxy() {
    Class<?>[] interfaces = new Class[] { ConnectionProxy.class };

    /*
     * Look up or generate the designated proxy class.
     */
    proxyClass = Proxy.getProxyClass(ConnectionProxy.class.getClassLoader(), interfaces);

  }

  /**
   * Create a new LazyConnectionDataSourceProxy.
   * 
   * @param targetDataSource the target DataSource
   */
  public FasterLazyConnectionDataSourceProxy(DataSource targetDataSource) {
    this();
    setTargetDataSource(targetDataSource);
    afterPropertiesSet();
  }

  /**
   * Set the default auto-commit mode to expose when no target Connection has been fetched yet (-> actual JDBC
   * Connection default not known yet).
   * <p>
   * If not specified, the default gets determined by checking a target Connection on startup. If that check fails, the
   * default will be determined lazily on first access of a Connection.
   * 
   * @see java.sql.Connection#getAutoCommit
   */
  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    this.defaultAutoCommit = new Boolean(defaultAutoCommit);
  }

  /**
   * Set the default transaction isolation level to expose when no target Connection has been fetched yet (-> actual
   * JDBC Connection default not known yet).
   * <p>
   * If not specified, the default gets determined by checking a target Connection on startup. If that check fails, the
   * default will be determined lazily on first access of a Connection.
   * 
   * @see java.sql.Connection#getTransactionIsolation
   */
  public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
    this.defaultTransactionIsolation = new Integer(defaultTransactionIsolation);
  }

  public void afterPropertiesSet() {
    super.afterPropertiesSet();

    // Determine default auto-commit and transaction isolation
    // via a Connection from the target DataSource, if possible.
    if (this.defaultAutoCommit == null || this.defaultTransactionIsolation == null) {
      try {
        Connection con = getTargetDataSource().getConnection();
        try {
          checkDefaultConnectionProperties(con);
        } finally {
          con.close();
        }
      } catch (SQLException ex) {
        // logger.warn("Could not retrieve default auto-commit and transaction isolation settings", ex);
      }
    }
  }

  /**
   * Check the default connection properties (auto-commit, transaction isolation), keeping them to be able to expose
   * them correctly without fetching an actual JDBC Connection from the target DataSource.
   * <p>
   * This will be invoked once on startup, but also for each retrieval of a target Connection. If the check failed on
   * startup (because the database was down), we'll lazily retrieve those settings.
   * 
   * @param con the Connection to use for checking
   * @throws SQLException if thrown by Connection methods
   */
  protected synchronized void checkDefaultConnectionProperties(Connection con) throws SQLException {
    if (this.defaultAutoCommit == null) {
      this.defaultAutoCommit = new Boolean(con.getAutoCommit());
    }
    if (this.defaultTransactionIsolation == null) {
      this.defaultTransactionIsolation = new Integer(con.getTransactionIsolation());
    }
  }

  /**
   * Expose the default auto-commit value.
   */
  protected Boolean defaultAutoCommit() {
    return defaultAutoCommit;
  }

  /**
   * Expose the default transaction isolation value.
   */
  protected Integer defaultTransactionIsolation() {
    return defaultTransactionIsolation;
  }

  /**
   * Return a Connection handle that lazily fetches an actual JDBC Connection when asked for a Statement (or
   * PreparedStatement or CallableStatement).
   * <p>
   * The returned Connection handle implements the ConnectionProxy interface, allowing to retrieve the underlying target
   * Connection.
   * 
   * @return a lazy Connection handle
   * @see ConnectionProxy#getTargetConnection
   */
  public Connection getConnection() {
    return getNewProxy(new LazyConnectionInvocationHandler());
  }

  /**
   * Return a Connection handle that lazily fetches an actual JDBC Connection when asked for a Statement (or
   * PreparedStatement or CallableStatement).
   * <p>
   * The returned Connection handle implements the ConnectionProxy interface, allowing to retrieve the underlying target
   * Connection.
   * 
   * @param username the per-Connection username
   * @param password the per-Connection password
   * @return a lazy Connection handle
   * @see ConnectionProxy#getTargetConnection
   */
  public Connection getConnection(String username, String password) {
    return getNewProxy(new LazyConnectionInvocationHandler(username, password));

  }

  private Connection getNewProxy(final InvocationHandler h) {
    final Object result;/*
                         * Invoke its constructor with the designated invocation handler.
                         */
    try {
      Constructor cons = proxyClass.getConstructor(constructorParams);
      result = cons.newInstance(new Object[] { h });
    } catch (NoSuchMethodException e) {
      throw new InternalError(e.toString());
    } catch (IllegalAccessException e) {
      throw new InternalError(e.toString());
    } catch (InstantiationException e) {
      throw new InternalError(e.toString());
    } catch (InvocationTargetException e) {
      throw new InternalError(e.toString());
    }
    return (Connection) result;
  }

  /**
   * Invocation handler that defers fetching an actual JDBC Connection until first creation of a Statement.
   */
  private class LazyConnectionInvocationHandler implements InvocationHandler {

    private String     username;

    private String     password;

    private Boolean    readOnly = Boolean.FALSE;

    private Integer    transactionIsolation;

    private Boolean    autoCommit;

    private boolean    closed   = false;

    private Connection target;

    public LazyConnectionInvocationHandler() {
      this.autoCommit = defaultAutoCommit();
      this.transactionIsolation = defaultTransactionIsolation();
    }

    public LazyConnectionInvocationHandler(String username, String password) {
      this();
      this.username = username;
      this.password = password;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on ConnectionProxy interface coming in...

      // Handle getTargetConnection method: return underlying connection.
      if (method.getName().equals("getTargetConnection")) { return getTargetConnection(); }

      if (!hasTargetConnection()) {
        // No physical target Connection kept yet ->
        // resolve transaction demarcation methods without fetching
        // a physical JDBC Connection until absolutely necessary.

        if (method.getName().equals("isReadOnly")) {
          return this.readOnly;
        } else if (method.getName().equals("setReadOnly")) {
          this.readOnly = (Boolean) args[0];
          return null;
        } else if (method.getName().equals("getTransactionIsolation")) {
          if (this.transactionIsolation != null) { return this.transactionIsolation; }
          // Else fetch actual Connection and check there,
          // because we didn't have a default specified.
        } else if (method.getName().equals("setTransactionIsolation")) {
          this.transactionIsolation = (Integer) args[0];
          return null;
        } else if (method.getName().equals("getAutoCommit")) {
          if (this.autoCommit != null) { return this.autoCommit; }
          // Else fetch actual Connection and check there,
          // because we didn't have a default specified.
        } else if (method.getName().equals("setAutoCommit")) {
          this.autoCommit = (Boolean) args[0];
          return null;
        } else if (method.getName().equals("commit")) {
          // Ignore: no statements created yet.
          return null;
        } else if (method.getName().equals("rollback")) {
          // Ignore: no statements created yet.
          return null;
        } else if (method.getName().equals("getWarnings")) {
          return null;
        } else if (method.getName().equals("clearWarnings")) {
          return null;
        } else if (method.getName().equals("isClosed")) {
          return new Boolean(this.closed);
        } else if (method.getName().equals("close")) {
          // Ignore: no target connection yet.
          this.closed = true;
          return null;
        } else if (this.closed) {
          // Connection proxy closed, without ever having fetched a
          // physical JDBC Connection: throw corresponding SQLException.
          throw new SQLException("Illegal operation: connection is closed");
        }
      }

      // Target Connection already fetched,
      // or target Connection necessary for current operation ->
      // invoke method on target connection.
      try {
        return method.invoke(getTargetConnection(), args);
      } catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }

    /**
     * Return whether the proxy currently holds a target Connection.
     */
    protected boolean hasTargetConnection() {
      return (this.target != null);
    }

    /**
     * Return the target Connection, fetching it and initializing it if necessary.
     */
    protected Connection getTargetConnection() throws SQLException {
      if (this.target == null) {
        // Fetch physical Connection from DataSource.
        this.target = (this.username != null) ? getTargetDataSource().getConnection(this.username, this.password)
            : getTargetDataSource().getConnection();

        // If we still lack default connection properties, check them now.
        checkDefaultConnectionProperties(this.target);

        // Apply kept transaction settings, if any.
        if (this.readOnly.booleanValue()) {
          this.target.setReadOnly(this.readOnly.booleanValue());
        }
        if (this.transactionIsolation != null && !this.transactionIsolation.equals(defaultTransactionIsolation())) {
          this.target.setTransactionIsolation(this.transactionIsolation.intValue());
        }
        if (this.autoCommit != null && this.autoCommit.booleanValue() != this.target.getAutoCommit()) {
          this.target.setAutoCommit(this.autoCommit.booleanValue());
        }
      }

      return this.target;
    }
  }

}