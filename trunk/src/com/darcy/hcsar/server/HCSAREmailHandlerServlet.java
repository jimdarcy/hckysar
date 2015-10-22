package com.darcy.hcsar.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.mail.internet.InternetAddress;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

public class HCSAREmailHandlerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger
			.getLogger(HCSAREmailHandlerServlet.class.getName());
	
	public static final String[] AGENCIES = { "HCSAR", "RVFD", "RADCLIFFFIRE",
		"TRACER", "VGFD", "UFD", "EFD", "TEST" };

	public static final String[] ADDRESSES = {
		"4380-fmYmNNSvXu9si8fk@alert.active911.com",
		"2012-imarBahWRVvcdSrU@alert.active911.com",
		"ZP7760-kUNRpgGJJ8Rtk32C@beta.active911.com",
		"TP7760-kUNRpgGJJ8Rtk32C@alert.active911.com",
		"9808-YMQqmYvYnvQvhYSf@alert.active911.com",
		"11307-yfhWu8sQvHigiePg@alert.active911.com",
		"10108-J9qYrkhspzm4F2zq@alert.active911.com",
	"DS7760-kUNRpgGJJ8Rtk32C@beta.active911.com" };

	public static void main(String[] args) {
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		log.severe("Beginning processing");
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage message = null;
		String incident = null;
		try{
			message = new MimeMessage(session, req.getInputStream());	
			if (message.getFrom().length > 0) {
				if (message.getFrom()[0] instanceof InternetAddress) {
					InternetAddress fromAddr = (InternetAddress) message
							.getFrom()[0];
					log.severe("Message from " + fromAddr.getAddress());
					if (!fromAddr.getAddress().equalsIgnoreCase("NO-REPLY@HCKY.ORG") && !fromAddr.getAddress().equalsIgnoreCase("jdarcy@gmail.com")){
						//forwardMessage(message,"jdarcy@gmail.com");
						MimeMessage fwdMessage = new MimeMessage(message);
						fwdMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("jdarcy@gmail.com","jdarcy@gmail.com"));
						fwdMessage.saveChanges();
						Transport.send(fwdMessage);
					}

				}
				
			}
		} catch (MessagingException e) {
			log.severe(e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			
			
			String subject = message.getSubject();
			log.severe("Message Subject: " + subject);
			if (subject.startsWith("Clear Report")) {
				log.severe("Found a clear report, so quit processing");
				return;
			}
			// log.severe(MessageUtils.findAttachment(message));
			String agency = req.getRequestURL().toString();
			agency = agency.trim();
			agency = agency.substring(agency.lastIndexOf('/') + 1,
					agency.indexOf('@'));
			if (agency.equalsIgnoreCase("rfd")) {
				agency = "RADCLIFFFIRE";
			}
			log.severe(agency);
			agency = agency.toUpperCase();
			if (ArrayUtils.contains(AGENCIES, agency)) {
				log.severe("Found agency " + agency);
				int beginIndex = subject.indexOf("#");
				log.severe("Index: " + beginIndex);
				String debugMode = DBLookup.getParamValue("debug");
				
				if (beginIndex > -1) {
					incident = subject.substring(beginIndex + 1);
					 MessageUtils.handleIncident(incident, agency, message);
					 
					log.severe(incident);
				}
			} else {
				// didn't find agency
				log.severe("Did not find agency: " + agency);
				// log.severe(message.toString());
				// forwardMessage(message);
			}
		} catch (MessagingException e) {
			log.severe(e.getMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void processInputStreamPart(BodyPart srcPart, MimeBodyPart tgtPart, String level)     throws MessagingException, IOException {
		String partType = srcPart.getContentType();                 
		//		LOG.info("Part " + level + " content type : " + partType);                 
		Object partContent = srcPart.getContent();                 
		//		LOG.info("Part " + level + " content : " + partContent);                 
		if (partType.startsWith("multipart/")) {  
			log.severe("multi");
			String subType = partType.substring(partType.indexOf('/') + 1);                         
			Multipart tgtPartMP = new MimeMultipart(subType);                         
			ByteArrayDataSource srcPartDS = new ByteArrayDataSource((InputStream) partContent, partType);
			Multipart srcPartMP = new MimeMultipart(srcPartDS);
			for (int j = 0; j < srcPartMP.getCount(); j++) {
				BodyPart srcChildPart = srcPartMP.getBodyPart(j);
				MimeBodyPart tgtChildPart = new MimeBodyPart();
				String childLevel = level + "." + j;
				this.processInputStreamPart(srcChildPart, tgtChildPart,	childLevel);
				tgtPartMP.addBodyPart(tgtChildPart);
			}                         
			tgtPart.setContent(tgtPartMP);
		} 
		else if (partType.startsWith("text/")) {
			log.severe("text");
			String data = new String(IOUtils.toByteArray(srcPart.getInputStream())); 
			tgtPart.setContent(data, partType);
		} 
		else {
			log.severe("other");
			String dataType = partType.substring(0, partType.indexOf(';'));	
			byte[] srcData  = IOUtils.toByteArray(srcPart.getInputStream());                         
			//				LOG.info("Data type = " + dataType);                         
			//				LOG.info("Data size = " + srcData.length);                         
			//				LOG.info("File name = " + srcPart.getFileName());                         
			tgtPart.setFileName(srcPart.getFileName());                         
			tgtPart.setDisposition(srcPart.getDisposition());                         
			DataSource tgtPartDS = new ByteArrayDataSource(srcData, dataType);
			tgtPart.setDataHandler(new DataHandler(tgtPartDS));
			//tgtPart.setContent(srcData, dataType);                 }     }

		}
	}
}