package com.darcy.hcsar.server;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import model.IncidentModel;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MessageUtils {
	private static final Logger log = Logger.getLogger(MessageUtils.class
			.getName());

	public static String parseNewMessage(String inMessage) {
		// log.severe(inMessage);
		// log.severe(Integer.toString(StringUtils.countMatches(inMessage,
		// "\n")));
		// log.severe(inMessage.indexOf("Incident#"));
		String message = "";

		inMessage = inMessage.substring(inMessage.indexOf("Nature:"));
		StringTokenizer st = new StringTokenizer(inMessage, "\n");
		boolean end = false;
		while (st.hasMoreElements() && !end) {
			String token = st.nextToken();
			if (token.startsWith("Street Notes")) {
				end = true;
			}
			message += token + "\n";
		}
		log.severe("Before: " + message);
		message = message.replaceAll("<b>", "");
		message = message.replaceAll("</b>", "");
		// message = StringUtils.remove(message, "</b>");

		log.severe("After: " + message);
		return message;
	}

	public static String parseMessage(String inMessage) {
		Document doc = Jsoup.parse(inMessage);
		Elements e = doc.getAllElements();
		Iterator it = e.iterator();
		IncidentModel im = new IncidentModel();
		int crossIndex = -1;
		int natureIndex = -1;
		int businessIndex = -1;
		int addtAddrIndex = -1;
		int notesIndex = -1;
		int dateIndex = -1;
		int timeIndex = -1;

		int index = 0;
		log.severe("HTML Element count: " + e.size());
		while (it.hasNext()) {
			Element currentElement = (Element) it.next();
			// log.severe("Element(" + index + "): " + currentElement.html());
			if (currentElement.html().trim().startsWith("Cross:"))
				crossIndex = index;
			else if (currentElement.html().trim().startsWith("Nature:"))
				natureIndex = index;
			else if (!currentElement.html().trim().startsWith("Business:")) {
				if (currentElement.html().trim().startsWith("Date:")) {
					dateIndex = index;
				} else if (currentElement.html().trim()
						.startsWith("Time&nbsp;Out:"))
					timeIndex = index;
				else if (currentElement.html().trim().startsWith("Business:")) {
					businessIndex = index;
				} else if (currentElement.html().trim()
						.startsWith("Addt&nbsp;Address:"))
					addtAddrIndex = index;
				else if (currentElement.html().trim().startsWith("Notes:")) {
					notesIndex = index;
				}
			}
			index++;
		}
		if (crossIndex != -1) {
			im.setAddress(e.get(crossIndex + 1).html()
					.replaceAll("&nbsp;", " "));
		}
		if (natureIndex != -1) {
			im.setNature(e.get(natureIndex + 1).html()
					.replaceAll("&nbsp;", " "));
		}
		String cross = new String();
		if (natureIndex - crossIndex > 1) {
			for (int i = natureIndex - 1; i > crossIndex + 1; i--) {
				if (cross.length() == 0) {
					cross = e.get(i).html().replaceAll("&nbsp;", " ").trim();
				} else {
					cross = cross + " & "
							+ e.get(i).html().replaceAll("&nbsp;", " ").trim();
				}
			}
		}

		if (cross.length() > 0) {
			im.setCross(cross);
		}
		Date date = null;
		if ((dateIndex != -1) && (timeIndex != -1) && (dateIndex != -1)
				&& (timeIndex == -1)) {
			DateFormat df = new SimpleDateFormat("mm/dd/yy");
			try {
				date = df.parse(e.get(dateIndex + 1).html());
				im.setTimeOut(date);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		} else if ((dateIndex != -1) && (timeIndex != -1) && (dateIndex != -1)
				&& (timeIndex != -1)) {
			DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm");
			try {
				date = df.parse(e.get(dateIndex + 1).html() + " "
						+ e.get(timeIndex + 1).html());
				im.setTimeOut(date);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		}
		String business = new String();
		if ((addtAddrIndex != -1) && (businessIndex != -1)
				&& (addtAddrIndex - businessIndex > 1)) {
			for (int i = addtAddrIndex - 1; i > businessIndex; i--) {
				if (business.length() == 0)
					business = e.get(i).html().replaceAll("&nbsp;", " ").trim();
				else {
					business = business + "\n"
							+ e.get(i).html().replaceAll("&nbsp;", " ").trim();
				}
			}
		}
		if (business.length() > 0) {
			im.setBusiness(business);
		}

		String addtAddress = new String();
		List<String> values = new ArrayList<String>();
		if ((notesIndex != -1) && (addtAddrIndex != -1)
				&& (notesIndex - addtAddrIndex > 1)) {

			for (int i = notesIndex - 1; i > addtAddrIndex; i--) {
				// if (addtAddress.length() == 0)
				values.add(e.get(i).html().replaceAll("&nbsp;", " ").trim());
				// else {
				// addtAddress = addtAddress + "\n" +
				// e.get(i).html().replaceAll("&nbsp;", " ").trim();
				// }
			}
		}
		if (values.size() > 0) {
			Collections.reverse(values);
			im.setAddtAddress(StringUtils.join(values, ' '));
		}
		// if (addtAddress.length() > 0) {
		// im.setAddtAddress(addtAddress);
		// }

		return im.toString();
	}

	public static void sendEmail(String toAddress, String body, String subject)
			throws UnsupportedEncodingException, MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress("noreply@hckysar.appspotmail.com",
				"Control"));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
				toAddress, "Active911"));
		msg.setSubject(subject);
		msg.setText(body);
		log.severe("Ready to send: " + body);
		Transport.send(msg);
	}

	public static String findAttachment(Part p) throws IOException,
			MessagingException {
		Object o = p.getContent();
		log.severe("Starting");
		// log.severe(o.getClass().getName());
		if ((o instanceof Multipart)) {
			// log.severe("This is a Multipart");
			Multipart mp = (Multipart) o;
			int count = mp.getCount();
			// log.severe("Count: " + count);
			for (int i = 0; i < count; i++) {

				Object part = mp.getBodyPart(i).getContent();
				if (part instanceof String) {
					 log.severe("String: " + part.toString());
						return part.toString();
					
				}
				log.severe(part.getClass().getName());
				if ((part instanceof InputStream && !(part instanceof org.apache.geronimo.mail.util.Base64DecoderStream))) {
					return convertISToString((InputStream) part);
				}
				if ((part instanceof String)) {
					// log.severe(part.toString());
					if (((String) part).indexOf("</div></div>") > -1) {
						return (String) part;
					}
				}
			}
		} else if ((o instanceof InputStream)) {
			return convertISToString((InputStream) o);
		}

		return null;
	}

	public static String inspectMessage(Part p) throws IOException,
			MessagingException {
		if (p instanceof Message) {

			// Call methos writeEnvelope
			log.severe("Message");
		}
		log.severe("----------------------------");
		log.severe("CONTENT-TYPE: " + p.getContentType());

		// check if the content is plain text
		if (p.isMimeType("text/plain")) {
			log.severe("This is plain text");
			log.severe("---------------------------");
			log.severe((String) p.getContent());
		}
		// check if the content has attachment
		else if (p.isMimeType("multipart/*")) {
			log.severe("This is a Multipart");
			log.severe("---------------------------");
			Multipart mp = (Multipart) p.getContent();
			int count = mp.getCount();
			for (int i = 0; i < count; i++) {
				log.severe(mp.getBodyPart(i).getClass().getName());
				if (mp.getBodyPart(i) instanceof javax.mail.internet.MimeBodyPart) {
					MimeBodyPart mimeBodyPart = (MimeBodyPart) mp
							.getBodyPart(i);
					log.severe(mimeBodyPart.getContent().getClass().getName());
					if (mimeBodyPart.getContent() instanceof MimeMultipart) {
						return findAttachment(mimeBodyPart);

					}else{
						log.severe("Don't know what to do");
					}
					
					// log.severe(mimeBodyPart.getContent().toString());
				}

			}
		}

		else {
			Object o = p.getContent();
			if (o instanceof String) {
				log.severe("This is a string");
				log.severe("---------------------------");
				log.severe((String) o);
			} else if (o instanceof InputStream) {
				log.severe("This is just an input stream");
				log.severe("---------------------------");
				InputStream is = (InputStream) o;
				is = (InputStream) o;
				int c;
				while ((c = is.read()) != -1)
					System.out.write(c);
			} else {
				log.severe("This is an unknown type");
				log.severe("---------------------------");
				log.severe(o.toString());
			}
		}

		return "";
	}

	private static String convertISToString(InputStream is) {
		log.severe("starting conversion");

		String output = new String();
		try {
			int c;
			while ((c = is.read()) != -1) {
				// int c;
				output = output + (char) c;
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// log.severe(output);
		return output;

	}

	public static void handleIncident(String incident, String agency,
			MimeMessage message) {
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query.Filter incidentIdFilter = new Query.FilterPredicate("incidentId",
				Query.FilterOperator.EQUAL, incident);
		Query.Filter incidentAgencyFilter = new Query.FilterPredicate("agency",
				Query.FilterOperator.EQUAL, agency);
		Query.CompositeFilter filter = new Query.CompositeFilter(
				Query.CompositeFilterOperator.AND,
				Arrays.asList(new Query.Filter[] { incidentIdFilter,
						incidentAgencyFilter }));

		Query q = new Query("Incident").setFilter(filter);

		PreparedQuery pq = ds.prepare(q);
		Entity result = pq.asSingleEntity();
		String messageString = null;

		if (result == null) {
			log.severe("Did not find incident " + incident);

			Key entryKey = KeyFactory.createKey("DispatchEntry", incident);

			Entity e = new Entity("Incident", entryKey);
			e.setProperty("incidentId", incident);
			e.setProperty("agency", agency);

			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			int index = ArrayUtils.indexOf(HCSAREmailHandlerServlet.AGENCIES,
					agency);
			log.severe("calling send alarm");
			boolean processingSuccess = sendAlarm(incident, message,
					HCSAREmailHandlerServlet.ADDRESSES[index]);
			if (processingSuccess) {
				datastore.put(e);
				log.severe("stored");
			}

		} else {
			log.severe("Found incident " + incident);
		}
	}

	public static boolean sendAlarm(String incident, MimeMessage message,
			String toAddress) {
		String messageString = null;
		//
		try {
			messageString = inspectMessage(message);
			// log.severe(messageString);
		} catch (IOException e1) {
			e1.printStackTrace();
			log.severe(e1.getMessage());
			return false;
		} catch (MessagingException e1) {
			e1.printStackTrace();
			log.severe(e1.getMessage());
			return false;
		} catch (Throwable e1) {
			e1.printStackTrace();
			log.severe(e1.getMessage());
			return false;
		}
		log.severe("!!!!");
		log.severe(messageString);
		log.severe(toAddress);
		if (messageString != null) {
			String body = parseNewMessage(messageString);
			log.severe("Body: " + body);
			try {
				sendEmail(toAddress, body, "Incident: " + incident);
				sendEmail("jdarcy@gmail.com", body, "Incident: " + incident);
			} catch (UnsupportedEncodingException e) {
				log.severe(e.getMessage());
				e.printStackTrace();
				return false;
			} catch (MessagingException e) {
				log.severe(e.getMessage());
				return false;
			}
		}

		return true;
	}
}