package de.aitools.aq.geolocating.rir;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

public class RirIpBlockEntry implements Cloneable {
  
  private Instant start;
  
  private Set<String> countryCodes;
  
  private RirIpBlockEntry next;

  public RirIpBlockEntry(
      final Instant startTime,
      final String countryCode) {
    if (startTime == null) { throw new NullPointerException(); }
    if (countryCode == null) { throw new NullPointerException(); }
    this.start = startTime;
    this.countryCodes = new TreeSet<>();
    this.countryCodes.add(countryCode);
    this.next = null;
  }
  
  public static RirIpBlockEntry deserialize(
      final String blockString) {
    final String[] entries = blockString.split("\t", 2);
    final String thisEntry = entries[0];

    final String[] parts = thisEntry.split(":", 2);
    final Instant start = Instant.ofEpochMilli(Long.parseLong(parts[0]));
    final String[] countryCodes = parts[1].split(",");
    final RirIpBlockEntry entry = new RirIpBlockEntry(start, countryCodes[0]);
    for (int c = 1; c < countryCodes.length; ++c) {
      entry.countryCodes.add(countryCodes[c]);
    }
    
    if (entries.length == 2) {
      final String nextEntries = entries[1];
      entry.next = RirIpBlockEntry.deserialize(nextEntries);
    }
    
    return entry;
  } 
  
  public Set<String> getCountryCodes() {
    return this.countryCodes;
  }
  
  public Instant getStart() {
    return this.start;
  }
  
  public boolean hasNext() {
    return this.next != null;
  }
  
  public RirIpBlockEntry getNext() {
    return this.next;
  }
  
  public RirIpBlockEntry insert(
      final Instant start, final String countryCode) {
    final int comparison = start.compareTo(this.start);
    if (comparison < 0) {
      final RirIpBlockEntry block = new RirIpBlockEntry(start, countryCode);
      block.next = this;
      return block;
    } else if (comparison == 0) {
      if (this.countryCodes.contains(countryCode)) {
        return this;
      }
      
      // Add new country code
      this.countryCodes.add(countryCode);
      return this;

    } else { // start> this.start      
      if (this.next == null) {
        this.next = new RirIpBlockEntry(start, countryCode);
        return this;
      } else {
        this.next = this.next.insert(start, countryCode);
        return this;
      }
    }
  }
  
  public String serialize() {
    return this.toString();
  }

  @Override
  public String toString() {
    return this.toString(new StringBuilder());
  }

  private String toString(final StringBuilder output) {
    if (output.length() > 0) {
      output.append('\t');
    }
    output.append(this.start.toEpochMilli());
    output.append(':').append(String.join(",", this.countryCodes));
    if (this.next != null) {
      return this.next.toString(output);
    } else {
      return output.toString();
    }
  }
  
  @Override
  public RirIpBlockEntry clone() {
    try {
      final RirIpBlockEntry clone = (RirIpBlockEntry) super.clone();
      if (clone.next != null) {
        clone.next = clone.next.clone();
      }
      return clone;
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

}
