package org.tc.cluster.watcher.mail;

import static org.tc.cluster.watcher.util.ClusterWatcherProperties.LOG;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public class Mail {

	private final String host, recipients;
	private SMTPAuthenticator auth = null;

	public Mail(String smtp, String recipients){
		if (smtp == null || recipients == null)
			throw new IllegalArgumentException("smtp.host|recipients can't be null!!");

		this.host = smtp;
		this.recipients = recipients;
		LOG.debug("Mail settings: SMTP host:  " + smtp + " , Recipients: " + recipients);
	}

	public void setAuthentication(String username, String password){
		this.auth = new SMTPAuthenticator(username, password);
		LOG.info("Setting mail authentication...");
	}

	/**
	 * Send mail to recipients@host
	 *
	 * @param subject
	 * 				subject of the mail
	 * @param content
	 * 				formatted content of mail body
	 * @throws MessagingException
	 */
	public void send(String subject, String content) throws MessagingException  {
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", host);

		Session session;
		if (auth != null){
			properties.put("mail.smtp.auth", "true");
			LOG.info("Using authenticated mail...");
			session = Session.getDefaultInstance(properties, auth);
		}
		else
			session = Session.getDefaultInstance(properties);

		String localhost = "";
		try {
			localhost = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		MimeMessage message = new MimeMessage(session);
		message.addRecipients(Message.RecipientType.TO, recipients);
		message.setSubject(String.format("[%s] %s", localhost ,subject));
		message.setText(content);
		Transport.send(message);
		LOG.info("Mail sent to " + recipients);
	}

	/**
	* SimpleAuthenticator is used to do simple authentication
	* when the SMTP server requires it.
	*/
	private static class SMTPAuthenticator extends javax.mail.Authenticator
	{
		private final String SMTP_AUTH_USER, SMTP_AUTH_PWD;

		public SMTPAuthenticator(String username, String password) {
			this.SMTP_AUTH_USER = username;
			this.SMTP_AUTH_PWD = password;
		}

	    @Override
		public PasswordAuthentication getPasswordAuthentication()
	    {
	        String username = SMTP_AUTH_USER;
	        String password = SMTP_AUTH_PWD;
	        return new PasswordAuthentication(username, password);
	    }
	}
}
