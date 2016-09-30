package de.aitools.aq.geolocating.collector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.aitools.aq.geolocating.iplocations.IplocationGeolocation;
import de.aitools.aq.geolocating.jaxb.Jaxbs;
import de.aitools.aq.geolocating.jaxb.XmlInstantAdapter;
import de.aitools.aq.geolocating.timezones.TimeZones;
import de.aitools.aq.geolocating.timezones.TimeZones.TimeZone;

/**
 * Class that stores the golocation informations collected by a
 * {@link GeolocationCollector}.
 * <p>
 * It holds the time span of the RIR entry in which the IP address falls at the
 * specific time instant, all IPlocation geolocations during that time,
 * candidates for country and time zone based on RIR data, the overall
 * consistency of all geolocations, and a country code and time zone if there
 * is are single most-likely ones respectively.
 * </p><p>
 * Outside of the {@link GeolocationCollector}, only the getter methods should
 * be used.
 * </p>
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "geolocations", propOrder = {
    "ip", "instant", "countryCode", "timeZone", "consistency",
    "rirStart", "rirEnd",
    "rirCountryCodeCandidates", "rirTimeZoneCandidates",
    "iplocationGeolocations"
})
public class Geolocations {
  
  private String ip;
  
  private Instant instant;
  
  private String countryCode;
  
  private String timeZone;
  
  private GeolocationConsistency consistency;
  
  private Instant rirStart;
  
  private Instant rirEnd;
  
  private Set<String> rirCountryCodeCandidates;
  
  private Set<String> rirTimeZoneCandidates;
  
  private List<IplocationGeolocation> iplocationGeolocations;
  
  private int nextIplocationIndex;
  
  public Geolocations() {
    this.ip = null;
    this.instant = null;
    this.countryCode = null;
    this.timeZone = null;
    this.consistency = null;
    this.rirStart = null;
    this.rirEnd = null;
    this.rirCountryCodeCandidates = new HashSet<>(1);
    this.rirTimeZoneCandidates = new HashSet<>();
    this.iplocationGeolocations = new ArrayList<>();
    this.nextIplocationIndex = -1;
  }

  /**
   * Gets the IP address that should be geolocated as a String.
   */
  @XmlAttribute(required = true)
  public String getIp() {
    return this.ip;
  }

  /**
   * Gets the time instant at which the IP address should be geolocated.
   */
  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  public Instant getInstant() {
    return this.instant;
  }


  /**
   * Gets the single most-likely country code for the IP at the time instant,
   * or null if there is no single most-likely one.
   */
  @XmlAttribute(required = false)
  public String getCountryCode() {
    return this.countryCode;
  }


  /**
   * Gets the single most-likely time zone for the IP at the time instant,
   * or null if there is no single most-likely one.
   */
  @XmlAttribute(required = false)
  public String getTimeZone() {
    return this.timeZone;
  }


  /**
   * Gets the consistency level of all geolocations.
   * <p>
   * This is {@link GeolocationConsistency#COUNTRY_CONSISTENT} if all
   * geolocations refer to the same country or
   * {@link GeolocationConsistency#TIME_ZONE_CONSISTENT} if all additionally
   * refer to the same time zone.
   * It is {@link GeolocationConsistency#INCONSISTENT} else.
   * </p>
   */
  @XmlAttribute(required = true)
  public GeolocationConsistency getConsistency() {
    return this.consistency;
  }


  /**
   * Gets the instant at which corresponding RIR entry starts
   */
  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  public Instant getRirStart() {
    return this.rirStart;
  }


  /**
   * Gets the instant at which correpsonding RIR entry ends, or
   * {@link Instant#MAX} if it is still ongoing.
   */
  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  public Instant getRirEnd() {
    return this.rirEnd;
  }

  /**
   * Gets possible candidates for the country according to RIR.
   * <p>
   * The RIR data has some flaws were different registry files assign the
   * same IP addresses at the same time to different countries. All such
   * different countries are listed here. 
   * </p>
   */
  @XmlElement(name = "rirCountryCodeCandidate", required = false)
  public Set<String> getRirCountryCodeCandidates() {
    return this.rirCountryCodeCandidates;
  }


  /**
   * Gets possible candidates for the time zone according to RIR.
   * <p>
   * This is just all time zones for all country code candidates (see
   * {@link #getRirCountryCodeCandidates()}).
   * </p>
   */
  @XmlElement(name = "rirTimeZoneCandidate", required = false)
  public Set<String> getRirTimeZoneCandidates() {
    return this.rirTimeZoneCandidates;
  }


  /**
   * Gets all IPlocation geolocations for the IP address that fall within
   * {@link #getRirStart()} and {@link #getRirEnd()}.
   */
  @XmlElement(name = "iplocationGeolocation", required = false)
  public List<IplocationGeolocation> getIplocationGeolocations() {
    return this.iplocationGeolocations;
  }


  /**
   * Gets the index of the first {@link IplocationGeolocation} within
   * {@link #getIplocationGeolocations()} that is after {@link #getInstant()},
   * or {@link #getIplocationGeolocations()}<tt>.size()</tt> if there is no
   * such geolocation in the list. 
   */
  @XmlAttribute(required = true)
  public int getNextIplocationIndex() {
    return this.nextIplocationIndex;
  }


  /**
   * Gets the IPlocation geolocation from {@link #getIplocationGeolocations()}
   * directly before {@link #getInstant()} or null if it does not exist. 
   */
  public IplocationGeolocation getIplocationGeolocationBefore() {
    final int index = this.getNextIplocationIndex() - 1;
    if (index < 0) {
      return null;
    } else {
      return this.iplocationGeolocations.get(index);
    }
  }


  /**
   * Gets the IPlocation geolocation from {@link #getIplocationGeolocations()}
   * directly after {@link #getInstant()} or null if it does not exist.
   */
  public IplocationGeolocation getIplocationGeolocationAfter() {
    final int index = this.getNextIplocationIndex();
    if (index == this.iplocationGeolocations.size()) {
      return null;
    } else {
      return this.iplocationGeolocations.get(index);
    }
  }

  public void setIp(final String ip) {
    this.ip = ip;
  }

  public void setInstant(final Instant instant) {
    this.instant = instant;
  }

  public void setCountryCode(final String countryCode) {
    this.countryCode = countryCode;
  }

  public void setTimeZone(final String timeZone) {
    this.timeZone = timeZone;
  }

  public void setConsistency(final GeolocationConsistency consistency) {
    this.consistency = consistency;
  }
  
  public void setRirStart(final Instant rirStart) {
    this.rirStart = rirStart;
  }
  
  public void setRirEnd(final Instant rirEnd) {
    this.rirEnd = rirEnd;
  }
  
  public void setNextIplocationIndex(final int nextIplocationIndex) {
    this.nextIplocationIndex = nextIplocationIndex;
  }
  
  public void addIplocation(
      final List<IplocationGeolocation> iplocationGeolocations) {
    if (this.instant == null) {
      throw new IllegalStateException(
          "Instant must be set before a call to addIplocation");
    }
    
    this.getIplocationGeolocations().addAll(iplocationGeolocations);

    Collections.sort(this.getIplocationGeolocations(),
        new Comparator<IplocationGeolocation>() {
          @Override
          public int compare(
              final IplocationGeolocation o1,
              final IplocationGeolocation o2) {
            return o1.getSourceTime().compareTo(o2.getSourceTime());
          }
        });
    

    this.nextIplocationIndex = 0;
    for (final IplocationGeolocation geolocation
        : this.getIplocationGeolocations()) {
      if (geolocation.getSourceTime().isAfter(this.instant)) {
        break;
      }
      ++this.nextIplocationIndex;
    }
  }
  
  public void setRirGeolocations(final Iterable<String> countryCodeCandidates) {
    this.rirCountryCodeCandidates.clear();
    this.rirTimeZoneCandidates.clear();
    
    for (final String countryCode : countryCodeCandidates) {
      final List<TimeZone> timeZones = TimeZones.forCountryCode(countryCode);
      if (!timeZones.isEmpty()) { // Something like EU or GB
        this.rirCountryCodeCandidates.add(countryCode);
      }
      for (final TimeZone timeZone : timeZones) {
        this.rirTimeZoneCandidates.add(timeZone.getId().toString());
      }
    }
    
    if (this.rirCountryCodeCandidates.isEmpty()) {
      // Better only EU than nothing
      for (final String countryCode : countryCodeCandidates) {
        this.rirCountryCodeCandidates.add(countryCode);
      }
    }
  }
  
  @Override
  public String toString() {
    try {
      return Jaxbs.toString(this);
    } catch (final JAXBException e) {
      // Should not be possible
      throw new RuntimeException(e);
    }
  }
  

}
