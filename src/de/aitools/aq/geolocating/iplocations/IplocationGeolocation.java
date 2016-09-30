package de.aitools.aq.geolocating.iplocations;

import java.time.Instant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.aitools.aq.geolocating.jaxb.XmlInstantAdapter;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "iplocationGeolocalisation", propOrder = {
    "sourceTime", "countryCode", "timeZone"
})
public class IplocationGeolocation {
  
  private String source;
  
  private Instant sourceTime;
  
  private String countryCode;
  
  private String timeZone;
  
  public IplocationGeolocation() {
    this.source = null;
    this.sourceTime = null;
    this.countryCode = null;
    this.timeZone = null;
  }

  @XmlAttribute(required = true)
  public String getSource() {
    return this.source;
  }

  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  public Instant getSourceTime() {
    return this.sourceTime;
  }

  @XmlAttribute(required = true)
  public String getCountryCode() {
    return this.countryCode;
  }

  @XmlAttribute(required = true)
  public String getTimeZone() {
    return this.timeZone;
  }
  
  public void setSource(final String source) {
    this.source = source;
  }
  
  public void setSourceTime(final Instant sourceTime) {
    this.sourceTime = sourceTime;
  }
  
  public void setCountryCode(final String countryCode) {
    this.countryCode = countryCode;
  }
  
  public void setTimeZone(final String timeZone) {
    this.timeZone = timeZone;
  }

}
