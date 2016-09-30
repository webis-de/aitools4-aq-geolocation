package de.aitools.aq.geolocating.rir;

import java.time.Instant;

import de.aitools.aq.geolocating.collector.IpBlock;

public class RirIpBlock extends IpBlock {
  
  private RirIpBlockEntry first;

  public RirIpBlock(
      final long firstIp,
      final long lastIp) {
    super(firstIp, lastIp);
    this.first = null;
  }
  
  public boolean hasFirst() {
    return this.first != null;
  }
  
  public RirIpBlockEntry getFirst() {
    return this.first;
  }
  
  public void setFirst(final RirIpBlockEntry first) {
    this.first = first;
  }
  
  public void insert(
      final Instant start, final String countryCode) {
    if (this.first == null) {
      this.first = new RirIpBlockEntry(start, countryCode);
    } else {
      this.first = this.first.insert(start, countryCode);
    }
  }

  @Override
  public String toString() {
    final String range = this.getFirstIp() + "\t" + this.getLastIp();
    if (this.first == null) {
      return range;
    } else {
      return range + "\t" + this.first;
    }
  }
  
  @Override
  public RirIpBlock split(final long newLastIp) {
    return (RirIpBlock) super.split(newLastIp);
  }
  
  @Override
  public RirIpBlock clone() {
    final RirIpBlock clone = (RirIpBlock) super.clone();
    if (clone.first != null) {
      clone.first = clone.first.clone();
    }
    return clone;
  }

}
