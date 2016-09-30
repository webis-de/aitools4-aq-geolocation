package de.aitools.aq.geolocating.iplocations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import de.aitools.aq.geolocating.timezones.TimeZoneMap;
import de.aitools.aq.geolocating.timezones.TimeZones;

public class IpligenceParser extends IplocationCsvParser {

  public IpligenceParser(final TimeZoneMap timeZoneMap) {
    super(timeZoneMap, "ipligence-(\\d){6}.csv", Charset.forName("ISO8859-1"));
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

    double latitude = Double.parseDouble(fields[12]);
    double longitude = Double.parseDouble(fields[13]);
    final String countryName = fields[3];
    final String offset = fields[6];
    final String stateCode = fields[7];
    final String cityName = fields[10];
    if (longitude == 0 && latitude == 0) { return null; }
    if (cityName.equals("FPO") || cityName.equals("APO")) { return null; }
    if (offset.isEmpty()) { return null; }
    
    // Bugfixing
    if (longitude >  0 && countryCode.equals("IM")) { longitude *= -1; }
    if (longitude >  0 && stateCode.equals("TZ")) { longitude *= -1; }
    if (longitude <  0 && countryCode.equals("TZ")) { longitude *= -1; }
    if (longitude == 0 && cityName.equals("GLADE SPRING")) { longitude = -81.773333; }
    if (longitude == 0 && cityName.equals("URBANA")) { longitude = -77.351389; }
    if (cityName.equals("MOGILEV")) { countryCode = "BY"; }
    if (cityName.equals("YEREVAN")) { countryCode = "AM"; }
    if (cityName.equals("S. ZENO NAVIGLIO") || cityName.equals("SAN ZENO NAVIGLIO")) { longitude = 10.2175; }
    if (longitude > 50 && cityName.equals("HYLLYKALLIO")) {
      double tmp = longitude;
      longitude = latitude;
      latitude = tmp;
    }
    if (countryName.equals("SERBIA")) { countryCode = "RS"; }
    if (countryName.equals("YUGOSLAVIA")) { countryCode = "RS"; }
    
    
    return new Record(firstIp, lastIp, countryCode, latitude, longitude);
  }
  
  @Override
  protected void postProcessRecord(final Record record, final String[] fields) {
    if (record.timeZone == null) {
      final String stateCode = fields[7];
      if (stateCode != null && !stateCode.isEmpty()) {
        // IPligence has sometimes the state code as country code
        // E.g., the channel islands have all "GB" as country
        try {
          record.timeZone = this.timeZoneMap.findTimeZone(
                  record.longitude, record.latitude, stateCode);
          if (record.timeZone != null) {
            record.countryCode = stateCode;
          }
        } catch (final IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }
    
    if (record.timeZone == null) {
      try {
        record.timeZone = this.timeZoneMap.findTimeZone(
            record.longitude, record.latitude);
        
        // Bugfixing the country code
        if (record.timeZone != null) {
          boolean allFine = false;
          switch (record.timeZone) {
          case "America/Puerto_Rico": // Puerto Rico has its country code
            if (record.countryCode.equals("US")) { record.countryCode = "PR"; allFine = true; }
            break;
          case "America/Curacao": // Carribean Netherlands
            if (record.countryCode.equals("AN")) { record.countryCode = "CW"; allFine = true; }
            break;
          case "America/Lower_Princes": // Carribean Netherlands
            if (record.countryCode.equals("AN")) { record.countryCode = "SX"; allFine = true; }
            break;
          case "America/Kralendijk": // Carribean Netherlands
            if (record.countryCode.equals("AN")) { record.countryCode = "BQ"; allFine = true; }
            break;
          case "Europe/Rome": // No idea what happened here...
            if (record.countryCode.equals("MG")) { record.countryCode = "IT"; allFine = true; }
            break;
          case "Australia/Sydney": // AS is American Samoa, not Australia...
            if (record.countryCode.equals("AS")) { record.countryCode = "AU"; allFine = true; }
            break;
          case "Australia/Brisbane": // AS is American Samoa, not Australia...
            if (record.countryCode.equals("AS")) { record.countryCode = "AU"; allFine = true; }
            break;
          case "Australia/Melbourne": // AS is American Samoa, not Australia...
            if (record.countryCode.equals("AS")) { record.countryCode = "AU"; allFine = true; }
            break;
          case "Australia/Perth": // AS is American Samoa, not Australia...
            if (record.countryCode.equals("AS")) { record.countryCode = "AU"; allFine = true; }
            break;
          case "Australia/Adelaide": // AS is American Samoa, not Australia...
            if (record.countryCode.equals("AS")) { record.countryCode = "AU"; allFine = true; }
            break;
          default:
            break;
          }
          if (!allFine) {
            if (!record.countryCode.equals("HM")) {
              // HM is part of Antarctica, so we should have no time zone
              System.err.println("NO TIME ZONE FOR " + record.countryCode
                  + ", BUT FOR "
                  + TimeZones.forId(record.timeZone).getCountryCode() 
                  + " (" + record.timeZone + "): " + String.join("|", fields));
            }
            record.timeZone = null;
          }
        }
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

}
