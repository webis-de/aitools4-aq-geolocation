package de.aitools.aq.geolocating;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.aitools.aq.geolocating.jaxb.XmlInetAddressAdapter;
import de.aitools.aq.geolocating.jaxb.XmlInstantAdapter;

/**
 * Class that holds a {@link Geolocation} for an {@link InetAddress} at a given
 * {@link Instant}.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="geolocalization")
public class Geolocalization {
  
  private InetAddress address;
  
  private Instant time;
  
  private Geolocation geolocation;
  
  public Geolocalization() {
    this.address = null;
    this.time = null;
    this.geolocation = null;
  }
  
  public Geolocalization(final InetAddress address, final Instant time)
  throws NullPointerException {
    this.setAddress(address);
    this.setTime(time);
  }
  
  public Geolocalization(final InetAddress address, final Instant time,
      final Geolocation geolocation)
  throws NullPointerException {
    this(address, time);
    this.setGeolocation(geolocation);
  }

  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInetAddressAdapter.class)
  public InetAddress getAddress() {
    return address;
  }

  @XmlAttribute(required = true)
  @XmlJavaTypeAdapter(XmlInstantAdapter.class)
  public Instant getTime() {
    return time;
  }

  @XmlElement(required = true)
  public Geolocation getGeolocation() {
    return geolocation;
  }
  
  public void setAddress(final InetAddress address)
  throws NullPointerException {
    if (address == null) { throw new NullPointerException(); }
    this.address = address;
  }
  
  public void setTime(final Instant time)
  throws NullPointerException {
    if (time == null) { throw new NullPointerException(); }
    this.time = time;
  }
  
  public void setGeolocation(final Geolocation geolocation)
  throws NullPointerException {
    if (geolocation == null) { throw new NullPointerException(); }
    this.geolocation = geolocation;
  }
  
  @Override
  public String toString() {
    final Geolocation geolocation = this.getGeolocation();
    final StringBuilder output = new StringBuilder();
    output.append(this.getAddress().getHostAddress());
    output.append('\t').append(this.getTime().toString());
    if (geolocation != null) {
      output.append('\t').append(geolocation.getTimeZone());
      output.append('\t').append(geolocation.getCountryCode());
    }
    return output.toString();
  }
  
  /**
   * Deserializes a Geolocalization serialized by {@link #toString()}.
   */
  public static Geolocalization parse(final String string)
  throws IllegalArgumentException {
    try {
      final String[] fields = string.split("\t");
      if (fields.length != 2 && fields.length != 4) {
        throw new IllegalArgumentException(string);
      }
      final Geolocalization geolocalization = new Geolocalization();
      geolocalization.setAddress(InetAddress.getByName(fields[0]));
      geolocalization.setTime(Instant.parse(fields[1]));
      if (fields.length > 2) {
        final Geolocation geolocation = new Geolocation();
        geolocalization.setGeolocation(geolocation);
        geolocation.setTimeZone(fields[2]);
        geolocation.setCountryCode(fields[3]);
      }
      return geolocalization;
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
  }
  
  /**
   * Deserializes Geolocalizations serialized by {@link #toString()},
   * one geolocalization per line.
   * <p>
   * The input stream is closed when the result stream is closed.
   * </p>
   */
  public static Stream<Geolocalization> parse(final InputStream input)
  throws IllegalArgumentException {
    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(input));
    return reader.lines().map(line -> Geolocalization.parse(line))
        .onClose(() -> {
      try {
        reader.close();
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

}
