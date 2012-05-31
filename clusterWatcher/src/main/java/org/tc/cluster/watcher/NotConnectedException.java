package org.tc.cluster.watcher;

public class NotConnectedException extends Exception{
	private static final long serialVersionUID = 1L;
	private final Exception exception;
	private final String server;

	NotConnectedException(String server, Exception e){
		this.exception = e;
		this.server = server;
	}

	@Override
	public String getLocalizedMessage(){
		return String.format("Not Connected to server [%s].", server);
	}

	@Override
	public String getMessage(){
		return String.format("Not Connected to server [%s]. Caused by: ", server, exception.getMessage());
	}

	@Override
	public Throwable getCause(){
		return exception;
	}

}
