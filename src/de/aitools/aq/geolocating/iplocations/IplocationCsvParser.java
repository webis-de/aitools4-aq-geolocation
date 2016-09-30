package de.aitools.aq.geolocating.iplocations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.aitools.aq.geolocating.timezones.TimeZoneMap;

public abstract class IplocationCsvParser {
  
  protected final TimeZoneMap timeZoneMap;
  
  protected final Pattern filenamePattern;
  
  protected Charset charset;
  
  public IplocationCsvParser(
      final TimeZoneMap timeZoneMap, final String filenamePattern,
      final Charset charset) {
    if (timeZoneMap == null) { throw new NullPointerException(); }
    if (charset == null) { throw new NullPointerException(); }
    this.timeZoneMap = timeZoneMap;
    this.filenamePattern = Pattern.compile(filenamePattern);
    this.charset = charset;
  }
  
  public boolean isForFile(final File file) {
    return this.filenamePattern.matcher(file.getName()).matches();
  }
  
  public abstract Instant getFileInstant(final File file)
  throws IllegalArgumentException;
  
  public List<Record> parse(final File file) throws IOException {
    final List<Record> records = new ArrayList<>();
    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(
            new FileInputStream(file), this.charset))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        final Record record = this.parse(line);
        if (record != null) {
          records.add(record);
        }
      }
    }
    return records;
  }
  
  protected Record parse(final String line) {
    final String[] fields = line.split("\",\"");
    fields[0] = fields[0].replace("\"", "");
    fields[fields.length - 1] = fields[fields.length - 1].replace("\"", "");
    
    final Record record = this.parseWithoutTimeZone(fields);
    if (record == null) { return null; }

    try {
      record.timeZone = this.timeZoneMap.findTimeZone(
          record.longitude, record.latitude, record.countryCode);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    this.postProcessRecord(record, fields);
    
    if (record.timeZone == null) {
      if (!record.countryCode.equals("AQ")) {
        // We don't have time zones for Antarctica, so this is not an error that
        // the record still has no time zone
        System.err.println("NO TIME ZONE: " + line);
      }
      return null;
    }
    return record;
  }
  
  protected abstract Record parseWithoutTimeZone(final String[] fields)
  throws IllegalArgumentException;
  
  protected void postProcessRecord(final Record record, final String[] fields) {
    // Do nothing by default
  }
  
  public static class Record {
    
    public final long firstIp;
    
    public final long lastIp;
    
    public String countryCode;
    
    public String timeZone;
    
    public double latitude;
    
    public double longitude;
    
    public Record(
        final long firstIp, final long lastIp,
        final String countryCode,
        final double latitude, final double longitude) {
      this.firstIp = firstIp;
      this.lastIp = lastIp;
      this.countryCode = countryCode;
      this.latitude = latitude;
      this.longitude = longitude;
    }
    
    @Override
    public String toString() {
      return String.join("\t",
          String.valueOf(this.firstIp), String.valueOf(this.lastIp),
          this.countryCode,
          String.valueOf(this.latitude), String.valueOf(this.longitude));
    }
    
  }

}
