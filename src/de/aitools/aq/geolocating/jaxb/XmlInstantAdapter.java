package de.aitools.aq.geolocating.jaxb;

import java.time.Instant;
import java.util.GregorianCalendar;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class XmlInstantAdapter
extends XmlAdapter<XMLGregorianCalendar, Instant> {

  @Override
  public Instant unmarshal(final XMLGregorianCalendar calendar)
  throws Exception {
    return XmlInstantAdapter.calendarToInstant(calendar);
  }

  @Override
  public XMLGregorianCalendar marshal(final Instant instant)
  throws Exception {
    return XmlInstantAdapter.instantToCalendar(instant);
  }
  
  public static Instant calendarToInstant(final XMLGregorianCalendar calendar) {
    if (calendar == null) { return null; }
    return calendar.toGregorianCalendar().toInstant();
  }
  
  public static XMLGregorianCalendar instantToCalendar(final Instant instant) {
    if (instant == null) { return null; }
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(instant.toEpochMilli());
    try {
      return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
    } catch (final DatatypeConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

}
