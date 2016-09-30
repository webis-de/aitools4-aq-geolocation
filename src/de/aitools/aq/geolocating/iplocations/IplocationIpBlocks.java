package de.aitools.aq.geolocating.iplocations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import de.aitools.aq.geolocating.Geolocator;
import de.aitools.aq.geolocating.collector.GeolocationCollector;
import de.aitools.aq.geolocating.collector.IpBlocks;
import de.aitools.aq.geolocating.iplocations.IplocationCsvParser.Record;
import de.aitools.aq.geolocating.timezones.CachedTimeZoneMap;
import de.aitools.aq.geolocating.timezones.TimeZoneMap;

public class IplocationIpBlocks extends IpBlocks<IplocationIpBlock>
implements Comparable<IplocationIpBlocks> {

  private final Instant time;
  
  private final String name;
  
  public IplocationIpBlocks(final Instant time, final String name) {
    if (time == null) { throw new NullPointerException(); }
    if (name == null) { throw new NullPointerException(); }
    this.time = time;
    this.name = name;
  }
  
  public static List<IplocationIpBlocks> deserializeAll(final File file)
  throws IOException {
    final List<IplocationIpBlocks> blockss = new ArrayList<>();
    if (file.isDirectory()) {
      for (final File child : file.listFiles()) {
        blockss.addAll(IplocationIpBlocks.deserializeAll(child));
      }
    } else {
      blockss.add(IplocationIpBlocks.deserialize(file));
    }
    return blockss;
  }
  
  public static IplocationIpBlocks deserialize(final File file)
  throws IOException {
    try (final BufferedReader reader =
        new BufferedReader(new FileReader(file))) {
      return IplocationIpBlocks.deserialize(reader);
    }
  }
  
  public static IplocationIpBlocks deserialize(
      final BufferedReader reader)
  throws IOException {
    final String header = reader.readLine();
    if (header == null) { throw new IllegalArgumentException(); }
    final String[] parts = header.split("\t");
    if (parts.length != 3) { throw new IllegalArgumentException(header); }
    final String className = parts[0];
    if (!IplocationIpBlocks.class.getName().equals(className)) {
      throw new IllegalArgumentException(
          IplocationIpBlocks.class.getName() + " != " + className);
    }

    final String name = parts[1];
    final Instant time = Instant.ofEpochMilli(Long.parseLong(parts[2]));
    
    final IplocationIpBlocks blocks = new IplocationIpBlocks(time, name);
    blocks.deserializeBlocks(reader);
    
    return blocks;
  }
  
  public String getName() {
    return this.name;
  }

  public Instant getTime() {
    return this.time;
  }

  @Override
  public IplocationIpBlock getBlock(final long ip) {
    return super.getBlock(ip);
  }

  public IplocationGeolocation getGeolocation(final long ip) {
    final IplocationIpBlock block = this.getBlock(ip);
    if (block == null) { return null; }
    return block.toGeolocation(this);
  }
  
  @Override
  public int compareTo(final IplocationIpBlocks o) {
    return this.getTime().compareTo(o.getTime());
  }
  
  public void serialize(final Writer writer) throws IOException {
    writer.write(this.getClass().getName());
    writer.write('\t');
    writer.write(this.name);
    writer.write('\t');
    writer.write(String.valueOf(this.getTime().toEpochMilli()));
    writer.write('\n');
    
    this.serializeBlocks(writer);
    
    writer.write('\n');
  }
  
  @Override
  protected String serializeBlockContent(final IplocationIpBlock block) {
    final StringBuilder output = new StringBuilder();
    output.append(block.getCountryCode()).append('\t');
    output.append(block.getTimeZone()).append('\t');
    output.append(block.getLatitude()).append('\t');
    output.append(block.getLongitude());
    return output.toString();
  }
  
  @Override
  protected void deserializeBlockContent(
      final IplocationIpBlock block, final String content) {
    final String[] parts = content.split("\t" , 4);
    final String countryCode = parts[0];
    final String timeZone = parts[1];
    final double latitude = Double.parseDouble(parts[2]);
    final double longitude = Double.parseDouble(parts[3]);
    
    block.setCountryCode(countryCode);
    block.setTimeZone(timeZone);
    block.setLatitude(latitude);
    block.setLongitude(longitude);
  }

  @Override
  protected IplocationIpBlock callNew(
      final long firstIp, final long lastIp) {
    return new IplocationIpBlock(
        firstIp, lastIp, null, null, Double.NaN, Double.NaN);
  }

  @Override
  protected IplocationIpBlock callSplit(
      final IplocationIpBlock block, final long newLastIp) {
    return block.split(newLastIp);
  }

  public static Stream<IplocationIpBlocks> parseAll(
      final File file, final List<IplocationCsvParser> parsers)
  throws ParseException, IOException {
    
    if (file.isDirectory()) {
      return Arrays.asList(file.listFiles()).stream().flatMap(child -> {
        try {
          return IplocationIpBlocks.parseAll(child, parsers);
        } catch (final Exception e) {
          throw new RuntimeException(e);
        }
      });
    } else {
      final List<IplocationIpBlocks> blockss = new ArrayList<>(1);
      for (final IplocationCsvParser parser : parsers) {
        if (parser.isForFile(file)) {
          System.out.println("Parsing " + file);
          final IplocationIpBlocks blocks = new IplocationIpBlocks(
              parser.getFileInstant(file), file.getName());
          for (final Record record : parser.parse(file)) {
            for (final IplocationIpBlock block
                : blocks.getExactBlocks(record.firstIp, record.lastIp)) {
              block.setCountryCode(record.countryCode);
              block.setTimeZone(record.timeZone);
              block.setLatitude(record.latitude);
              block.setLongitude(record.longitude);
            }
          }
          blockss.add(blocks);
          break;
        }
      }
      return blockss.stream();
    }
  }

  /**
   * Parses all IPlocation CSV files in the input directory, processes them for
   * usage within a {@link GeolocationCollector} (for example by
   * {@link Geolocator#main(String[])}), and writes the result to an output
   * directory. 
   * @param args Input directory and output directory.
   */
  public static void main(final String[] args)
  throws ParseException, IOException {
    if (args.length != 2) {
      System.err.println("Synopsis:");
      System.err.println("  Preprocesses all IPlocation CSV files in a directory.");
      System.err.println("Usage:");
      System.err.println("   <input> <output>");
      System.err.println("Where:");
      System.err.println("  input");
      System.err.println("    Is the directory that contains the CSV files.");
      System.err.println("    Currently supported formats:");
      System.err.println("      - IPligence");
      System.err.println("      - IP2Location DB11");
      System.err.println("  output");
      System.err.println("    Is the directory to which the processed files will");
      System.err.println("    be written (then to be used by");
      System.err.println("    " + Geolocator.class.getName() + ")");
      System.exit(1);
    }
    final File inputOriginal = new File(args[0]);
    final File outputParsed = new File(args[1]);
    final TimeZoneMap timeZoneMap = new CachedTimeZoneMap();
    final List<IplocationCsvParser> parsers = new ArrayList<>();
    parsers.add(new IpligenceParser(timeZoneMap));
    parsers.add(new Ip2locationDb11Parser(timeZoneMap));
    
    outputParsed.mkdirs();
    IplocationIpBlocks.parseAll(inputOriginal, parsers).forEach(blockss -> {
      System.out.println("Serializing: " + blockss.getName());
      try (final BufferedWriter writer = new BufferedWriter(new FileWriter(
          new File(outputParsed, blockss.getName())))) {
        blockss.serialize(writer);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
