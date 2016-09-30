package de.aitools.aq.geolocating.iplocations;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import de.aitools.aq.geolocating.timezones.TimeZoneMap;

public class Ip2locationDb11Parser extends IplocationCsvParser {

  public Ip2locationDb11Parser(final TimeZoneMap timeZoneMap) {
    super(timeZoneMap, "ip2location-db11(lite)?-(\\d){6}.csv",
        Charset.forName("ISO8859-1"));
  }

  @Override
  public Instant getFileInstant(final File file)
  throws IllegalArgumentException{
    if (!this.isForFile(file)) {
      throw new IllegalArgumentException(file.getName());
    }
    final String filename = file.getName();
    

    final DateFormat dateFormat = new SimpleDateFormat("yyyyMM");
    dateFormat.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    final int dateBegin = filename.lastIndexOf('-') + 1;
    final String dateString = filename.substring(dateBegin, dateBegin + 6);
    try {
      return dateFormat.parse(dateString).toInstant();
    } catch (final ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected Record parseWithoutTimeZone(final String[] fields)
  throws IllegalArgumentException {
    final String ipRangeStartString = fields[0];
    final String ipRangeEndString = fields[1];
    String countryCode = fields[2];
    if (countryCode.isEmpty() || countryCode.equals("-")) { return null; }
    
    final long firstIp = Long.parseLong(ipRangeStartString);
    final long lastIp = Long.parseLong(ipRangeEndString);

    double latitude = Double.parseDouble(fields[6]);
    double longitude = Double.parseDouble(fields[7]);
    
    // Bugfixing
    final String cityName = fields[5];
    if (longitude > 0 && cityName.equals("Toyon")) { longitude *= -1; }
    
    return new Record(firstIp, lastIp, countryCode, latitude, longitude);
  }

}
