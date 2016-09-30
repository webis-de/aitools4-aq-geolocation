package de.aitools.aq.geolocating;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Stream;

import de.aitools.aq.decision.tree.CountingDecisionNodeFactory;
import de.aitools.aq.decision.tree.DecisionNode;
import de.aitools.aq.decision.tree.DecisionNodeFactory;
import de.aitools.aq.geolocating.collector.GeolocationCollector;
import de.aitools.aq.geolocating.collector.GeolocationConsistency;
import de.aitools.aq.geolocating.collector.Geolocations;
import de.aitools.aq.geolocating.collector.IpBlock;
import de.aitools.aq.geolocating.iplocations.IplocationIpBlocks;
import de.aitools.aq.geolocating.rir.RirIpBlocks;

/**
 * A class that geolocates IP addresses at specific time instants.
 * <p>
 * It uses a {@link GeolocationCollector} to collect geolocations from the
 * different sources and decides on a final geolocalization in a tree-like
 * decision process (implemented by a {@link DecisionNode}).
 * </p><p>
 * This class provides a main that uses a rather strict decision tree for
 * deciding on whether or not to take a geolocalization (see
 * {@link #createDefaultDecisionTree()}). For usage, see
 * {@link #main(String[], Function)} or start the program without arguments. You
 * can easily create classes with main methods that use the same interface but
 * different decision trees using the following main in your class:
 * <pre>
 * public static void main(final String[] args) throws IOException {
 *   final DecisionNode<Geolocations, Boolean> tree = ...
 *   Geolocator.main(args, collector -> new Geolocator(collector, tree));
 * }
 * </pre>
 * Where <tt>...</tt> is replaced by the creation of the decision tree.
 * </p>
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class Geolocator {
  
  protected final GeolocationCollector<?> collector;
  
  protected final DecisionNode<Geolocations, Boolean> decisionTree;

  /**
   * Creates a geolocator that geolocates using the information collected by
   * the given collector and a standard decision tree
   * (see {@link #createDefaultDecisionTree()}).
   * @param collector The collector to use
   * @throws NullPointerException If the collector is null 
   */
  public Geolocator(
      final GeolocationCollector<?> collector)
  throws NullPointerException {
    this(collector, Geolocator.createDefaultDecisionTree(true));
  }

  /**
   * Creates a geolocator that geolocates using the information collected by
   * the given collector.
   * @param collector The collector to use
   * @param decisionTree The tree to use to decide on whether or not to take
   * a geolocation
   * @throws NullPointerException If the collector or decisionTree is null 
   */
  public Geolocator(
      final GeolocationCollector<?> collector,
      final DecisionNode<Geolocations, Boolean> decisionTree)
  throws NullPointerException {
    if (collector == null) { throw new NullPointerException(); }
    if (decisionTree == null) { throw new NullPointerException(); }
    this.collector = collector;
    this.decisionTree = decisionTree;
  }
  
  /**
   * Geolocate given IP address at given time.
   * @param ip An IPv4 IP as encoded by {@link IpBlock#ipToLong(String)}
   * @param time The instant at which the IP should be geolocated
   * @return The geolocalization, but without geolocation if the IP can not be
   * geolocated for that time with the existing data
   */
  public Geolocalization geolocate(
      final InetAddress address, final Instant time) {
    final Geolocalization geolocalization =  new Geolocalization(address, time);
    final Geolocations geolocalisations = this.collector.collect(address, time);
    if (geolocalisations != null) {
      final Geolocation geolocation = this.geolocate(geolocalisations);
      if (geolocation != null) {
        geolocalization.setGeolocation(geolocation);
      }
    }
    return geolocalization;
  }
  
  /**
   * Geolocate given IP address at given time.
   * <p>
   * The format of the line has to be
   * <pre>
   * &lt;address&gt;[TAB]&lt;time&gt;
   * </pre>
   * where the time is formatted according to given dateFormat.
   * </p>
   * @param line The line containing the address and time
   * @param dateFormat Format of the time
   * @return The geolocalization, but without geolocation if the IP can not be
   * geolocated for that time with the existing data or null if the line is
   * empty or starts with a #.
   */
  public Geolocalization geolocate(
      final String line, final DateFormat dateFormat) {
    if (line.isEmpty()) { return null; }
    if (line.charAt(0) == '#') { return null; }
    final String[] fields = line.split("\t");
    if (fields.length != 2) {
      throw new IllegalArgumentException("Invalid line: " + line);
    }
    try {
      final InetAddress address = InetAddress.getByName(fields[0]);
      final Instant time = dateFormat.parse(fields[1]).toInstant();
      return Geolocator.this.geolocate(address, time);
    } catch (final ParseException | UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
  }
  
  /**
   * Geolocates IP addresses from an input stream.
   * <p>
   * Each line has to be formatted according to 
   * {@link #geolocate(String, DateFormat)}.
   * </p><p>
   * The input stream is closed when the result stream is closed.
   * </p>
   * @param input
   * @param dateFormat
   * @return
   */
  public Stream<Geolocalization> geolocate(
      final InputStream input, final DateFormat dateFormat) {
    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(input));
    return reader.lines().map(line -> this.geolocate(line, dateFormat))
        .onClose(() -> {
      try {
        reader.close();
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }
  
  /**
   * Create a geolocalisation based on the found {@link Geolocations}
   * (asserted to be non-null).
   * @param geolocations The geolocations to use a data
   * @return The geolocation or null if this geolocator does not create
   * a geolocation from given geolocations (e.g., since the geolocation are not
   * consistent)
   */
  protected Geolocation geolocate(final Geolocations geolocalisations) {
    if (this.decisionTree.decide(geolocalisations).getValue()) {
      return new Geolocation(geolocalisations);
    } else {
      return null;
    }
  }
  
  @Override
  public String toString() {
    return this.decisionTree.toString();
  }

  /**
   * Creates the default decision tree.
   * <p>
   * The parameter count controls on whether every node in the created tree
   * should count how often it is reached or not. Counts are accessible by using
   * {@link DecisionNode#toString()}.
   * </p><p>
   * This tree takes all geolocations for which one of these conditions hold,
   * and none other:
   * <ol>
   * <li>RIR and IPlocation geolocations are available and agree on country
   * and time zone.</li>
   * <li>RIR and IPlocation geolocations are available and agree on country,
   * there is one IPlocation before and one IPlocation after the target time,
   * and the two IPlocations directly before and after the target time agree
   * on the time zone.</li>
   * <li>There is a RIR geolocation to a country with only one time zone
   * (and no IPlocation geolocation, as it would otherwise fall in the above
   * categories).</li>
   * </ol>
   * </p>
   * @param count Whether to count how often each decision node is reached
   * @return The root of the decision tree
   */
  public static DecisionNode<Geolocations, Boolean> createDefaultDecisionTree(
      final boolean count) {
    DecisionNodeFactory<Geolocations, Boolean> node = null;
    if (count) {
      node = new CountingDecisionNodeFactory<>(); 
    } else {
      node = new DecisionNodeFactory<>();
    }
    return Geolocator.createDefaultDecisionTree(node);
  }
  
  /**
   * Creates the default decision tree.
   * <p>
   * The factory node is used to create the single decision nodes of the
   * resulting tree.
   * </p><p>
   * This tree takes all geolocations for which one of these conditions hold,
   * and none other:
   * <ol>
   * <li>RIR and IPlocation geolocations are available and agree on country
   * and time zone.</li>
   * <li>RIR and IPlocation geolocations are available and agree on country,
   * there is one IPlocation before and one IPlocation after the target time,
   * and the two IPlocations directly before and after the target time agree
   * on the time zone.</li>
   * <li>There is a RIR geolocation to a country with only one time zone
   * (and no IPlocation geolocation, as it would otherwise fall in the above
   * categories).</li>
   * </ol>
   * </p>
   * @param node The factory that creates the decision nodes
   * @return The root of the decision tree
   */
  public static <G extends Geolocations> DecisionNode<G, Boolean>
      createDefaultDecisionTree(final DecisionNodeFactory<G, Boolean> node) {
    return node.internal(
        "RIR",
        geos -> geos.getRirStart() != null,
        node.internal(
            "IPlocation",
            geos -> !geos.getIplocationGeolocations().isEmpty(),
            node.internal(
                "inconsistent",
                geos -> geos.getConsistency() == GeolocationConsistency.INCONSISTENT,
                node.leaf(false),
                node.internal(
                    "time zone consistent",
                    geos -> geos.getConsistency() == GeolocationConsistency.TIME_ZONE_CONSISTENT,
                    node.leaf(true),
                    node.internal(
                        "locally time zone consistent",
                        geos -> {
                          final boolean iplocationBefore =
                              geos.getIplocationGeolocationBefore() != null;
                          final boolean iplocationAfter =
                              geos.getIplocationGeolocationAfter() != null;
                          final String timeZone = geos.getTimeZone();
                          return iplocationBefore && iplocationAfter
                              && timeZone != null;
                        },
                        node.leaf(true),
                        node.leaf(false)))),
            node.internal(
                "1 time zone",
                geos -> geos.getRirTimeZoneCandidates().size() == 1,
                node.leaf(true),
                node.leaf(false))),
        node.leaf(false));
  }

  /**
   * Prints the usage of the program of {@link #main(String[], Function)}.
   * @param out The stream to print the usage to
   */
  public static void printHelp(final PrintStream out) {
    out.println("Synopsis:");
    out.println("  Reads IP address/time pairs and geolocates them.");
    out.println("Usage:");
    out.println("  <iplocations> <rir> <input> <time-format> <output>");
    out.println("Where:");
    out.println("  iplocations");
    out.println("    Directory containing the parsed IPlocation databases.");
    out.println("    Parser: " + IplocationIpBlocks.class.getName());
    out.println("  rir");
    out.println("    Directory containing the parsed RIR database "
        + RirIpBlocks.RIR_FILENAME + ".");
    out.println("    Parser: " + RirIpBlocks.class.getName());
    out.println("    You should have received a parsed database with this");
    out.println("    program located at data/rir-parsed.");
    out.println("  input");
    out.println("    A file containing the IPv4 addresses and times for");
    out.println("    historical geolocation. One address per line:");
    out.println("      <address>[TAB]<time>");
    out.println("  time-format");
    out.println("    Format of the <time> field in the input file. The format");
    out.println("    needs to be specified for Java SimpleDateFormat:");
    out.println("    http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html");
    out.println("  output");
    out.println("    File where the geolocalizations are written to.");
    out.println("    Output format is either (on successful geolocalization)");
    out.println("      <address>[TAB]<time>[TAB]<time-zone>[TAB]<country-code>");
    out.println("    or (on failed geolocalization)");
    out.println("      <address>[TAB]<time>");
    out.println("    You can parse the geolocalizations again using");
    out.println("    " + Geolocalization.class.getName() + "#parse(InputStream)");
  }
  
  /**
   * Main method reusable for different {@link Geolocator}s.
   * <p>
   * The program will create a {@link GeolocationCollector} using RIR and
   * IPlocation directories, use them to geolocate IP address/time pairs read
   * from a file via a {@link Geolocator} by given factory, and write the
   * geolocalizations to a file. The decision tree employed by the geolocator
   * is printed to standard out at the end. 
   * </p>
   * @param args Command line arguments (start the program without arguments
   * to see its usage)
   * @param factory Method used to create the geolocator
   * @throws IOException If an error occurred on reading or writing
   */
  public static void main(
      final String[] args,
      final Function<GeolocationCollector<?>, Geolocator> factory)
  throws IOException {
    if (args.length != 5) {
      Geolocator.printHelp(System.err);
      System.exit(1);
    }

    final File iplocationsDirectory = new File(args[0]);
    final File rirDirectory = new File(args[1]);
    final File inputFile = new File(args[2]);
    final InputStream input = new FileInputStream(inputFile);
    final DateFormat dateFormat = new SimpleDateFormat(args[3]);
    final File outputFile = new File(args[4]);
    final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
    
    try {
      System.out.println(new Date() + "  LOADING");
      final GeolocationCollector<?> collector =
          GeolocationCollector.create(iplocationsDirectory, rirDirectory);
      final Geolocator geolocator = factory.apply(collector);
      System.out.println(new Date() + "  GEOLOCATING");
      geolocator.geolocate(input, dateFormat)
        .forEach(geo -> {
            try {
              if (geo != null) {
                writer.write(geo.toString());
                writer.write('\n');
              }
            } catch (final IOException e) {
              throw new UncheckedIOException(e);
            }
          });
      System.out.println(new Date() + "  DONE");
      System.out.println();
      System.out.println("Decisions:");
      System.out.println(geolocator);
    } finally {
      writer.close();
    }
  }
  
  /**
   * Uses {@link #main(String[], Function)} with a default decision tree
   * (see {@link #createDefaultDecisionTree(boolean)}).
   * @param args Command line arguments (start the program without arguments
   * to see its usage)
   * @throws IOException If an error occurred on reading or writing
   */
  public static void main(final String[] args) throws IOException {
    Geolocator.main(args, collector -> new Geolocator(collector));
  }

}
