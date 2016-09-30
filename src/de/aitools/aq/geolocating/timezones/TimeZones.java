package de.aitools.aq.geolocating.timezones;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vividsolutions.jts.geom.Coordinate;

public class TimeZones {
  
  private static final Pattern COORDINATE_PATTERN = Pattern.compile("[+-]\\d*");
  
  private static final List<TimeZone> ALL = new ArrayList<>();
  
  private static final Map<String, TimeZone> BY_ID =
      new HashMap<>();
  
  private static final Map<String, List<TimeZone>> BY_COUNTRY_CODE =
      new HashMap<>();
  
  private TimeZones() { }
  
  public static List<TimeZone> all() {
    return Collections.unmodifiableList(ALL);
  }
  
  public static TimeZone forId(final String id) throws NoSuchElementException {
    TimeZones.init();
    final TimeZone timeZone = BY_ID.get(id);
    if (timeZone == null) {
      throw new NoSuchElementException(id);
    } else {
      return timeZone;
    }
  }
  
  public static TimeZone forId(final ZoneId id) throws NoSuchElementException {
    return TimeZones.forId(id.getId());
  }
  
  public static List<TimeZone> forCountryCode(final String countryCode) {
    TimeZones.init();
    final List<TimeZone> timeZones = BY_COUNTRY_CODE.get(countryCode);
    if (timeZones == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(timeZones);
    }
  }
  
  private static synchronized void init() {
    if (ALL.isEmpty()) {
      final InputStream zoneTab =
          TimeZones.class.getResourceAsStream("zone.tab");
      
      try (final BufferedReader reader
          = new BufferedReader(new InputStreamReader(zoneTab))) {
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("#") || line.trim().isEmpty()) { continue; }
          final TimeZone timeZone = TimeZone.parseFromZoneTab(line);
          ALL.add(timeZone);
          
          BY_ID.put(timeZone.getId().toString(), timeZone);
          
          final String countryCode = timeZone.getCountryCode();
          List<TimeZone> timeZonesByCountryCode =
              BY_COUNTRY_CODE.get(countryCode);
          if (timeZonesByCountryCode == null) {
            timeZonesByCountryCode = new ArrayList<>();
            BY_COUNTRY_CODE.put(countryCode, timeZonesByCountryCode);
          }
          timeZonesByCountryCode.add(timeZone);
        }
      } catch (final IOException e) {
        throw new RuntimeException("ERROR loading timezone data", e);
      }
      
      final InputStream backward =
          TimeZones.class.getResourceAsStream("backward");
      
      try (final BufferedReader reader
          = new BufferedReader(new InputStreamReader(backward))) {
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (line.startsWith("#") || line.trim().isEmpty()) { continue; }
          final String[] parts = line.split("\\s+");
          final String target = parts[1];
          final String old = parts[2];
          try {
            BY_ID.put(old, TimeZones.forId(target));
          } catch (final NoSuchElementException nse) {
            // No country zones, just ignore them
          }
        }
      } catch (final IOException e) {
        throw new RuntimeException("ERROR loading timezone data", e);
      }
    }
  }
  
  public static class TimeZone {
    
    private final ZoneId id;
    
    private final String countryCode;
    
    private final String[] nameParts;
    
    private final Coordinate coordinates;
    
    private TimeZone(
        final String name, final String countryCode, String coordinatesString) {
      if (countryCode == null) { throw new NullPointerException(); }
      this.id = ZoneId.of(name);
      this.countryCode = countryCode;
      this.nameParts = name.split("/");
      
      this.coordinates = TimeZone.parseCoordinates(coordinatesString);
    }
    
    public ZoneId getId() {
      return this.id;
    }
    
    public String getCountryCode() {
      return this.countryCode;
    }
    
    public boolean hasStateName() {
      return this.nameParts.length > 2;
    }
    
    public String getStateName() throws NoSuchElementException {
      if (this.hasStateName()) {
        return this.nameParts[1];
      } else {
        throw new NoSuchElementException();
      }
    }
    
    public String getCityName() {
      return this.nameParts[this.nameParts.length - 1];
    }
    
    public Coordinate getCoordinates() {
      return this.coordinates;
    }
    
    public ZoneOffset getOffset(final long millisecondsSinceEpoch) {
      return this.getOffset(Instant.ofEpochMilli(millisecondsSinceEpoch));
    }
    
    public ZoneOffset getOffset(final Instant date) {
      return this.id.getRules().getOffset(date);
    }
    
    public ZoneOffset getOffsetWithoutDaylightSavingTime(
        final long millisecondsSinceEpoch) {
      return this.getOffsetWithoutDaylightSavingTime(
          Instant.ofEpochMilli(millisecondsSinceEpoch));
    }
    
    public ZoneOffset getOffsetWithoutDaylightSavingTime(final Instant date) {
      return this.id.getRules().getStandardOffset(date);
    }
    
    @Override
    public String toString() {
      return this.getId().getId();
    }
    
    private static TimeZone parseFromZoneTab(final String tzRecord) {
      final String[] parts = tzRecord.split("\t");
      final String countryCode = parts[0];
      final String coordinatesString = parts[1];
      final String name = parts[2];
      
      return new TimeZone(name, countryCode, coordinatesString);
    }
    
    private static Coordinate parseCoordinates(final String coordinatesString) {
      final Matcher matcher = COORDINATE_PATTERN.matcher(coordinatesString);
      if (!matcher.find() || matcher.start() != 0) {
        throw new IllegalArgumentException(coordinatesString);
      }
      final int splitIndex = matcher.end();
      final double latitude =
          TimeZone.parseDegrees(coordinatesString.substring(0, splitIndex));
      if (!matcher.find() || matcher.start() != splitIndex
          || matcher.end() != coordinatesString.length()) {
        throw new IllegalArgumentException(coordinatesString);
      }
      final double longitude =
          TimeZone.parseDegrees(coordinatesString.substring(splitIndex));
      
      return new Coordinate(longitude, latitude);
    }
    
    private static final double parseDegrees(final String iso6709String) {
      double degrees = 0;

      int integralEnd = iso6709String.indexOf('.');
      if (integralEnd < 0) { integralEnd = iso6709String.length(); }
      
      if (integralEnd >= 7) {
        degrees += Double.parseDouble(
            iso6709String.substring(integralEnd - 2)) / 60 / 60;
        degrees += Double.parseDouble(
            iso6709String.substring(integralEnd - 4, integralEnd - 2)) / 60;
        degrees += Double.parseDouble(
            iso6709String.substring(1, integralEnd - 4));
      } else if (integralEnd >= 5) { 
        degrees += Double.parseDouble(
            iso6709String.substring(integralEnd - 2)) / 60;
        degrees += Double.parseDouble(
            iso6709String.substring(1, integralEnd - 2));
      } else {
        degrees += Double.parseDouble(
            iso6709String.substring(1));
      }

      if (iso6709String.charAt(0) == '-') {
        degrees *= -1;
      }
      
      return degrees;
    }
    
  }

}
