package de.aitools.aq.geolocating.iplocations;

import de.aitools.aq.geolocating.collector.IpBlock;

public class IplocationIpBlock extends IpBlock {
  
  private String countryCode;

  private String timeZone;
  
  private double latitude;
  
  private double longitude;

  public IplocationIpBlock(
      final long firstIp,
      final long lastIp,
      final String countryCode,
      final String timeZone,
      final double latitude,
      final double longitude) {
    super(firstIp, lastIp);
    this.countryCode = countryCode;
    this.timeZone = timeZone;
    this.latitude = latitude;
    this.longitude = longitude;
  }
  
  public String getCountryCode() {
    return this.countryCode;
  }
  
  public String getTimeZone() {
    return this.timeZone;
  }
  
  public double getLatitude() {
    return this.latitude;
  }
  
  public double getLongitude() {
    return this.longitude;
  }
  
  public void setCountryCode(final String countryCode) {
    this.countryCode = countryCode;
  }
  
  public void setTimeZone(final String timeZone) {
    this.timeZone = timeZone;
  }
  
  public void setLatitude(final double latitude) {
    this.latitude = latitude;
  }
  
  public void setLongitude(final double longitude) {
    this.longitude = longitude;
  }

  @Override
  public String toString() {
    final StringBuilder output = new StringBuilder();
    output.append(this.getFirstIp()).append('\t');
    output.append(this.getLastIp()).append('\t');
    output.append(this.getCountryCode()).append('\t');
    output.append(this.getTimeZone()).append('\t');
    output.append(this.getLatitude()).append('\t');
    output.append(this.getLongitude());
    return output.toString();
  }
  
  @Override
  public IplocationIpBlock split(final long newLastIp) {
    return (IplocationIpBlock) super.split(newLastIp);
  }
  
  @Override
  public IplocationIpBlock clone() {
    return (IplocationIpBlock) super.clone();
  }
  
  public IplocationGeolocation toGeolocation(
      final IplocationIpBlocks blocks) {
    final IplocationGeolocation geolocation =
        new IplocationGeolocation();
    geolocation.setSource(blocks.getName());
    geolocation.setSourceTime(blocks.getTime());
    geolocation.setTimeZone(this.timeZone);
    geolocation.setCountryCode(this.countryCode);
    return geolocation;
  }

}
