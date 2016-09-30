package de.aitools.aq.geolocating.collector;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "geolocationConsistency")
@XmlEnum(String.class)
public enum GeolocationConsistency {

  /**
   * The geolocations do not even agree on a country.
   */
	@XmlEnumValue("inconsistent")
	INCONSISTENT {
    @Override
    public String value() { return "inconsistent"; }
  },
	
  /**
   * The geolocations agree on country, but not on a time zone.
   */
	@XmlEnumValue("country-consistent")
	COUNTRY_CONSISTENT {
    @Override
    public String value() { return "country-consistent"; }
  },
	
  /**
   * The geolocations agree on country and time zone.
   */
	@XmlEnumValue("time-zone-consistent")
	TIME_ZONE_CONSISTENT {
    @Override
    public String value() { return "time-zone-consistent"; }
  };
	
	public abstract String value();
	
	public static GeolocationConsistency fromValue(final String value) {
    for (final GeolocationConsistency level
        : GeolocationConsistency.values()) {
      if (level.value().equals(value)) {
        return level;
      }
    }
    throw new IllegalArgumentException(value);
  }
}
