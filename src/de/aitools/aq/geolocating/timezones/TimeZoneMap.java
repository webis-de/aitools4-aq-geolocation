package de.aitools.aq.geolocating.timezones;

import java.io.IOException;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class TimeZoneMap extends CoordinatesMap {
  
  private static final String ATTRIBUTE_POLYGON = "the_geom";
  
  private static final String ATTRIBUTE_TIME_ZONE = "TZID";
  
  public static final double DEFAULT_TOLERANCE = 7.5; // 7.5 degrees = 360 / 24 / 2 = half of the distance between meridians
  
  private double tolerance;

  public TimeZoneMap() throws IOException {
    this(DEFAULT_TOLERANCE);
  }

  public TimeZoneMap(final double tolerance)
  throws IOException, IllegalArgumentException {
    super(TimeZoneMap.loadTimeZonesShapeFile(), ATTRIBUTE_POLYGON);
    this.setTolerance(tolerance);
  }
  
  public void setTolerance(final double tolerance)
  throws IllegalArgumentException {
    if (tolerance < 0.0) {
      throw new IllegalArgumentException("Negative tolerance: " + tolerance);
    }
    this.tolerance = tolerance;
  }
  
  public String findTimeZone(
      final double longitude, final double latitude)
  throws IOException, NullPointerException, IllegalStateException,
  ZoneRulesException {
    return this.findTimeZone(longitude, latitude, null);
  }
  
  public String findTimeZone(
      final double longitude, final double latitude, final String countryCode)
  throws IOException, NullPointerException, IllegalStateException,
  ZoneRulesException {
    return this.findTimeZone(new Coordinate(longitude, latitude), countryCode);
  }
  
  public String findTimeZone(final Coordinate coordinate)
  throws IOException, NullPointerException, IllegalStateException {
    return this.findTimeZone(coordinate, null);
  }
  
  public String findTimeZone(
      final Coordinate coordinate, final String countryCode)
  throws IOException, NullPointerException, IllegalStateException {
    final Point point = this.getGeometryFactory().createPoint(coordinate);
    final List<ZoneWithDistance> zoneList = new ArrayList<>();
    
    try (final FeatureIterator<SimpleFeature> featureIterator =
        this.find(coordinate, this.tolerance);) {
      while (featureIterator.hasNext()) {
        final SimpleFeature feature = featureIterator.next();
        final String zoneName = (String)
            feature.getAttribute(ATTRIBUTE_TIME_ZONE);
        if (zoneName.equals("uninhabited")) { continue; }
        if (countryCode != null) {
          
          switch (zoneName) {
          case "Europe/Simferopol": // IANA puts it to RU, others to UA
            if (!countryCode.equals("UA") && !countryCode.equals("RU")) { continue; }
            break;
          case "Pacific/Kiritimati": // IANA puts it to KI, but UM seems also reasonable
            if (!countryCode.equals("UM") && !countryCode.equals("KI")) { continue; }
            break;
          default:
            final String timeZoneCountryCode =
              TimeZones.forId(zoneName).getCountryCode();
            if (!countryCode.equals(timeZoneCountryCode)) { continue; }
            break;
          }
          
        }
        final double distance = ((MultiPolygon)
                feature.getAttribute(ATTRIBUTE_POLYGON)).distance(point);
        if (distance == 0.0) { return zoneName; }
        zoneList.add(new ZoneWithDistance(zoneName, distance));
      }
    }
    
    if (zoneList.isEmpty()) {
      return null;
    }

    Collections.sort(zoneList, new Comparator<ZoneWithDistance>() {
      @Override
      public int compare(
          final ZoneWithDistance o1, final ZoneWithDistance o2) {
        return Double.compare(o1.getDistance(), o2.getDistance());
      }
    });

    return zoneList.get(0).getZoneName();
  }
  
  public static SimpleFeatureSource loadTimeZonesShapeFile()
  throws IOException {
    return CoordinatesMap.loadShapeFile(
        CoordinatesMap.class.getResource("tz_world.shp"));
  }
  
  private static class ZoneWithDistance {
    
    private final String zoneName;
    
    private final double distance;
    
    private ZoneWithDistance(final String zoneName, final double distance) {
      this.zoneName = zoneName;
      this.distance = distance;
    }
    
    public String getZoneName() {
      return this.zoneName;
    }
    
    public double getDistance() {
      return this.distance;
    }
    
  }
  
  public static void main(String[] args) throws IOException {
    final TimeZoneMap map = new TimeZoneMap();
    System.out.println(map);
    
    System.out.println(map.findTimeZone(29.634084, 59.238281, "FI"));
    System.out.println(TimeZones.forId("Europe/Mariehamn").getCountryCode());
  }

}
