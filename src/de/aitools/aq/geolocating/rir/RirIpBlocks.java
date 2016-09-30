package de.aitools.aq.geolocating.rir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import de.aitools.aq.geolocating.Geolocator;
import de.aitools.aq.geolocating.collector.IpBlock;
import de.aitools.aq.geolocating.collector.IpBlocks;

public class RirIpBlocks extends IpBlocks<RirIpBlock> {
  
  public static final String RIR_FILENAME = "rir.txt";
  
  private static final String RIR_DATE_FORMAT = "yyyyMMdd";
  
  public static RirIpBlocks deserialize(final File file)
  throws IOException {
    try (final BufferedReader reader =
        new BufferedReader(new FileReader(file))) {
      return RirIpBlocks.deserialize(reader);
    }
  }
  
  public static RirIpBlocks deserialize(final BufferedReader reader)
  throws IOException {
    final String header = reader.readLine();
    if (header == null) { throw new IllegalArgumentException(); }
    if (!RirIpBlocks.class.getName().equals(header)) {
      throw new IllegalArgumentException(header);
    }
    
    final RirIpBlocks blocks = new RirIpBlocks();
    blocks.deserializeBlocks(reader);
    
    return blocks;
  }

  @Override
  public RirIpBlock getBlock(final long ip) {
    return super.getBlock(ip);
  }
  
  public void serialize(final Writer writer) throws IOException {
    writer.write(this.getClass().getName());
    writer.write('\n');
    
    this.serializeBlocks(writer);
    
    writer.write('\n');
  }
  
  @Override
  protected String serializeBlockContent(final RirIpBlock block) {
    if (block.hasFirst()) {
      return block.getFirst().serialize();
    } else {
      return "";
    }
  }
  
  @Override
  protected void deserializeBlockContent(
      final RirIpBlock block, final String content) {
    if (!content.isEmpty()) {
      block.setFirst(RirIpBlockEntry.deserialize(content));
    }
  }

  @Override
  protected RirIpBlock callNew(
      final long firstIp, final long lastIp) {
    return new RirIpBlock(firstIp, lastIp);
  }

  @Override
  protected RirIpBlock callSplit(
      final RirIpBlock block, final long newLastIp) {
    return block.split(newLastIp);
  }
  
  public void parseDirectory(final File ripeDirectory)
  throws IOException, ParseException {
    for (final File file : ripeDirectory.listFiles()) {
      if (file.isDirectory()) {
        this.parseDirectory(file);
      } else if (file.getName().contains("delegated")) {
        System.out.println(new Date() + " Parsing " + file.getAbsolutePath());
        this.parse(file);
      }
    }
  }
  
  public void parse(final File ripeFile) throws IOException {
    try (final BufferedReader reader =
        new BufferedReader(new FileReader(ripeFile))) {
      String line = reader.readLine();
      if (line == null) {
        System.err.println("EMPTY: " + ripeFile.getAbsolutePath());
        return;
      }
      while (line.startsWith("#")) { line = reader.readLine(); }
      final String[] parts = line.split("\\|");
      final String rirOffsetToUtc = parts.length > 6 ? parts[6] : "";

      while (line != null) {
        if (line.startsWith("#")) { line = reader.readLine(); continue; }
        try {
          this.parse(line, rirOffsetToUtc);
        } catch (final Exception e) {
          System.err.println(
              "IGNORING invalid line: \"" + line + "\": " + e.getMessage());
        }
        line = reader.readLine();
      }
    }
  }
  
  private void parse(final String line, final String rirOffsetToUtc)
  throws ParseException {
    final String[] parts = line.split("\\|");

    final String protocol = parts[2];
    if (!protocol.equals("ipv4")) { return; }
    
    final String countryCode = parts[1];
    if (countryCode.isEmpty() || countryCode.equals("*")) { return; }
    
    final String status = parts[6];
    if (!status.equals("assigned") && !status.equals("allocated")) { return; }
    
    final long firstIp = IpBlock.ipToLong(parts[3]);
    final long blockSize = Long.parseLong(parts[4]);
    final long lastIp = firstIp + blockSize - 1;
    
    final String dateString = parts[5];
    if (dateString.equals("00000000")) { return; }

    final Instant start = RirIpBlocks.getInstant(dateString, rirOffsetToUtc);

    this.parse(firstIp, lastIp, countryCode, start);
  }
  
  private void parse(final long firstIp, final long lastIp,
      final String countryCode, final Instant start) {
    for (final RirIpBlock block
        : this.getExactBlocks(firstIp, lastIp)) {
      block.insert(start, countryCode);
    }
  }
  
  private static Instant getInstant(
      final String dateString, final String rirOffsetToUtc)
  throws ParseException {
    final DateFormat dateFormat = new SimpleDateFormat(RIR_DATE_FORMAT);
    dateFormat.setCalendar(Calendar.getInstance(
        TimeZone.getTimeZone("UTC" + rirOffsetToUtc)));
    final Date date = dateFormat.parse(dateString);
    return date.toInstant();
  }
  
  public static void main(final String[] args)
  throws ParseException, IOException {
    if (args.length != 2) {
      System.err.println("Synopsis:");
      System.err.println("  Preprocesses all RIR files in a directory recursively.");
      System.err.println("Usage:");
      System.err.println("   <input> <output>");
      System.err.println("Where:");
      System.err.println("  input");
      System.err.println("    Is the directory that contains the RIR registry files.");
      System.err.println("    (see the README.txt).");
      System.err.println("  output");
      System.err.println("    Is the directory to which the processed file will");
      System.err.println("    be written (then to be used by");
      System.err.println("    " + Geolocator.class.getName() + ")");
      System.exit(1);
    }
    final File inputOriginal = new File(args[0]);
    final File outputParsed = new File(args[1]);

    outputParsed.mkdirs();
    
    final RirIpBlocks blocks = new RirIpBlocks();
    blocks.parseDirectory(inputOriginal);
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(
        new File(outputParsed, RIR_FILENAME)))) {
      blocks.serialize(writer);
    }
  }

}
