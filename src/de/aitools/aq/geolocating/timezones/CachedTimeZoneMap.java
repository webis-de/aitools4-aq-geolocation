package de.aitools.aq.geolocating.timezones;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

public class CachedTimeZoneMap extends TimeZoneMap {
  
  private final Map<String, String> cachedTimeZones;

  public CachedTimeZoneMap()
  throws IOException {
    this(TimeZoneMap.DEFAULT_TOLERANCE);
  }

  public CachedTimeZoneMap(final double tolerance)
  throws IOException, IllegalArgumentException {
    super(tolerance);
    this.cachedTimeZones = new HashMap<>();
  }
  
  @Override
  public String findTimeZone(
      final Coordinate coordinate, final String countryCode)
  throws IOException, NullPointerException, IllegalStateException {
    final String key = String.format(
        "%s %s %s", countryCode, coordinate.x, coordinate.y);
    if (this.cachedTimeZones.containsKey(key)) {
      return this.cachedTimeZones.get(key);
    } else {
      final String timeZone = super.findTimeZone(coordinate, countryCode);
      this.cachedTimeZones.put(key, timeZone);
      return timeZone;
    }
  }

}
