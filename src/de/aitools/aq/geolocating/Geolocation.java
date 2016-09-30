package de.aitools.aq.geolocating;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import de.aitools.aq.geolocating.collector.GeolocationCollector;
import de.aitools.aq.geolocating.collector.Geolocations;

/**
 * Class to hold geolocation information: country and time zone.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="geolocation")
public class Geolocation {
  
  private String countryCode;
  
  private String timeZone;
  
  /**
   * Creates a geolocation without country code and time zone set.
   * <p>
   * You have to set both by using {@link #setCountryCode(String)} and
   * {@link #setTimeZone(String)} before you can serialize this object via JAXB.
   * </p>
   */
  public Geolocation() {
    this.countryCode = null;
    this.timeZone = null;
  }
  
  /**
   * Creates a geolocation from given {@link Geolocations} (as produced by a
   * {@link GeolocationCollector}).
   * <p>
   * This just takes the country and time zone from the geolocations.
   * </p>
   * @param geolocations The input geolocations
   */
  public Geolocation(final Geolocations geolocations)
  throws NullPointerException {
    this.setCountryCode(geolocations.getCountryCode());
    this.setTimeZone(geolocations.getTimeZone());
  }
  
  @XmlAttribute(required = true)
  public String getCountryCode() {
    return this.countryCode;
  }

  @XmlAttribute(required = true)
  public String getTimeZone() {
    return this.timeZone;
  }
  
  public void setCountryCode(final String countryCode) {
    if (countryCode == null) { throw new NullPointerException(); }
    this.countryCode = countryCode;
  }
  
  public void setTimeZone(final String timeZone) {
    if (timeZone == null) { throw new NullPointerException(); }
    this.timeZone = timeZone;
  }


}
