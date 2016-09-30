package de.aitools.aq.geolocating.collector;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import de.aitools.aq.geolocating.iplocations.IplocationGeolocation;
import de.aitools.aq.geolocating.iplocations.IplocationIpBlocks;
import de.aitools.aq.geolocating.rir.RirIpBlock;
import de.aitools.aq.geolocating.rir.RirIpBlockEntry;
import de.aitools.aq.geolocating.rir.RirIpBlocks;

/**
 * Collects {@link Geolocations} from RIR and IPlocation data for IP addresses
 * at certain time instants.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 * @param <G> The geolocation implementation to use.
 */
public class GeolocationCollector<G extends Geolocations> {
  
  private final Supplier<G> geolocationsFactory;
  
  private final List<IplocationIpBlocks> iplocations;
  
  private final RirIpBlocks rir;
  
  /**
   * Creates a new {@link GeolocationCollector} using the data in the
   * iplocationsDirectory and the rirDirectory.
   * <p>
   * Geolocations are created using the given factory method. If you are unsure
   * about this, use {@link #create(File, File)}.
   * </p>
   * @param iplocationsDirectory Directory containing the parsed IPlocation
   * databases (see {@link IplocationIpBlocks#main(String[])})
   * @param rirDirectory Directory containing the parsed RIR database (see
   * {@link RirIpBlocks#main(String[])})
   * @param geolocationsFactory Method to create new {@link Geolocations}
   * @throws IOException If an error occurred reading the RIR or IPlocation
   * databases 
   */
  public GeolocationCollector(
      final File iplocationsDirectory, final File rirDirectory,
      final Supplier<G> geolocationsFactory)
  throws IOException {
    if (geolocationsFactory == null) { throw new NullPointerException(); }
    this.geolocationsFactory = geolocationsFactory;
    this.iplocations = IplocationIpBlocks.deserializeAll(
        iplocationsDirectory);
    this.rir = RirIpBlocks.deserialize(
        new File(rirDirectory, RirIpBlocks.RIR_FILENAME));
  }

  /**
   * Creates a new {@link GeolocationCollector} using the data in the
   * iplocationsDirectory and the rirDirectory.
   * @param iplocationsDirectory Directory containing the parsed IPlocation
   * databases (see {@link IplocationIpBlocks#main(String[])})
   * @param rirDirectory Directory containing the parsed RIR database (see
   * {@link RirIpBlocks#main(String[])})
   * @throws IOException If an error occurred reading the RIR or IPlocation
   * databases 
   */
  public static GeolocationCollector<Geolocations> create(
      final File iplocationsDirectory, final File rirDirectory)
  throws IOException {
    return new GeolocationCollector<>(
        iplocationsDirectory, rirDirectory, () -> new Geolocations());
  }
  
  /**
   * Collects all {@link Geolocations} for given IP address at given time
   * instant.
   * @param address The IP address that should be geolocated
   * @param time The time at which the address should be geolocated
   * @return The collected Geolocations
   */
  public G collect(final InetAddress address, final Instant time) {
    return this.collect(IpBlock.addressToLong(address), time);
  }
  
  protected G collect(final long ip, final Instant time) {
    final G geolocalisations =
        this.collectGeolocations(ip, time);
    if (geolocalisations.getRirStart() != null) {
      GeolocationCollector.setCountryAndTimeZone(geolocalisations);
      GeolocationCollector.setConsistency(geolocalisations);
    }
    
    return geolocalisations;
  }
  
  private static void setCountryAndTimeZone(
      final Geolocations geolocalisations) {
    
    final Set<String> rirCountryCodeCandidates =
        geolocalisations.getRirCountryCodeCandidates();
    final Set<String> rirTimeZoneCandidates =
        geolocalisations.getRirTimeZoneCandidates();

    final IplocationGeolocation before =
        geolocalisations.getIplocationGeolocationBefore();
    final IplocationGeolocation after =
        geolocalisations.getIplocationGeolocationAfter();

    if (before != null) {
      if (after != null) { // before != null, after != null
        // take if both agree with RIR
        if (before.getCountryCode().equals(after.getCountryCode())
            && rirCountryCodeCandidates.contains(before.getCountryCode())) {
          geolocalisations.setCountryCode(before.getCountryCode());
        }
        if (before.getTimeZone().equals(after.getTimeZone())
            && rirTimeZoneCandidates.contains(before.getTimeZone())) {
          geolocalisations.setTimeZone(before.getTimeZone());
        }
      } else { // before != null, after == null
        // take if agrees with RIR
        if (rirCountryCodeCandidates.contains(before.getCountryCode())) {
          geolocalisations.setCountryCode(before.getCountryCode());
        }
        if (rirTimeZoneCandidates.contains(before.getTimeZone())) {
          geolocalisations.setTimeZone(before.getTimeZone());
        }
      }
    } else {
      if (after != null) { // before == null, after != null
        // take if agrees with RIR
        if (rirCountryCodeCandidates.contains(after.getCountryCode())) {
          geolocalisations.setCountryCode(after.getCountryCode());
        }
        if (rirTimeZoneCandidates.contains(after.getTimeZone())) {
          geolocalisations.setTimeZone(after.getTimeZone());
        }
      } else { // before == null, after == null
        // take from RIR if unique
        if (rirCountryCodeCandidates.size() == 1) {
          geolocalisations.setCountryCode(
              rirCountryCodeCandidates.iterator().next());
        }
        if (rirTimeZoneCandidates.size() == 1) {
          geolocalisations.setTimeZone(
              rirTimeZoneCandidates.iterator().next());
        }
      }
    }
    
  }
  
  private static void setConsistency(final Geolocations geolocalisations) {
    geolocalisations.setConsistency(GeolocationConsistency.INCONSISTENT);

    final Set<String> countryCodes =
        GeolocationCollector.getConsistentCountryCodes(geolocalisations);
    if (countryCodes.size() == 1) {
      geolocalisations.setConsistency(
          GeolocationConsistency.COUNTRY_CONSISTENT);
      
      final Set<String> timeZones =
          GeolocationCollector.getConsistentTimeZones(geolocalisations);
      if (timeZones.size() == 1) {
        geolocalisations.setConsistency(
            GeolocationConsistency.TIME_ZONE_CONSISTENT);
      }
    }
  }
  
  private static Set<String> getConsistentCountryCodes(
      final Geolocations geolocalisations) {
    final Set<String> consistent =
        new HashSet<>(geolocalisations.getRirCountryCodeCandidates());
    
    for (final IplocationGeolocation geolocalisation
        : geolocalisations.getIplocationGeolocations()) {
      final String countryCode = geolocalisation.getCountryCode();
      if (!consistent.contains(countryCode)) {
        consistent.clear();
        return consistent;
      } else {
        if (consistent.size() > 1) {
          consistent.clear();
          consistent.add(countryCode);
        }
      }
    }
    
    return consistent;
  }
  
  private static Set<String> getConsistentTimeZones(
      final Geolocations geolocalisations) {
    final Set<String> consistent =
        new HashSet<>(geolocalisations.getRirTimeZoneCandidates());
    
    for (final IplocationGeolocation geolocalisation
        : geolocalisations.getIplocationGeolocations()) {
      final String timeZone = geolocalisation.getTimeZone();
      if (!consistent.contains(timeZone)) {
        consistent.clear();
        return consistent;
      } else {
        if (consistent.size() > 1) {
          consistent.clear();
          consistent.add(timeZone);
        }
      }
    }
    
    return consistent;
  }
  
  /*
   * Collect Geolocations
   */
  
  private G collectGeolocations(
      final long ip, final Instant time) {
    
    final G geolocations = this.getRirGeolocation(ip, time);
    
    final Instant start = geolocations.getRirStart();
    if (start != null) {
      Instant end = geolocations.getRirEnd();
      if (end == null) { end = Instant.MAX; }
      geolocations.addIplocation(
          this.getIplocationGeolocations(ip, start, end));
    }
    
    return geolocations;
  }
  
  private G getRirGeolocation(
      final long ip, final Instant time) {
    final G geolocations = this.geolocationsFactory.get();
    geolocations.setIp(IpBlock.longToIp(ip));
    geolocations.setInstant(time);
    
    final RirIpBlock block = this.rir.getBlock(ip);
    if (block == null) { return geolocations; }
    RirIpBlockEntry entry = block.getFirst();
    if (entry == null) { return geolocations; }
    if (time.compareTo(entry.getStart()) < 0) { return geolocations; }

    RirIpBlockEntry nextEntry = entry.getNext();
    while (nextEntry != null && nextEntry.getStart().compareTo(time) <= 0) {
      entry = nextEntry;
      nextEntry = entry.getNext();
    }

    geolocations.setRirStart(entry.getStart());
    if (nextEntry != null) {
      geolocations.setRirEnd(nextEntry.getStart());
    }
    geolocations.setRirGeolocations(entry.getCountryCodes());
    return geolocations;
  }

  private List<IplocationGeolocation> getIplocationGeolocations(
      final long ip, final Instant start, final Instant end) {
    final List<IplocationGeolocation> geolocations = new
        ArrayList<>(this.iplocations.size());
    for (final IplocationIpBlocks blocks : this.iplocations) {
      if (start.compareTo(blocks.getTime()) <= 0
          && blocks.getTime().compareTo(end) <= 0) {
        final IplocationGeolocation geolocation =
            blocks.getGeolocation(ip);
        if (geolocation != null) {
          geolocations.add(geolocation);
        }
      }
    }
    return geolocations;
  }

}
