package de.aitools.aq.geolocating.collector;

import java.net.InetAddress;

/**
 * Extending blocks must override {@link #clone()}.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public abstract class IpBlock implements Comparable<IpBlock>, Cloneable {

  private static long IP_BLOCK_SIZE =
      256;
  private static long MULTIPLIER_THIRD_BLOCK =
      IP_BLOCK_SIZE;
  private static long MULTIPLIER_SECOND_BLOCK =
      MULTIPLIER_THIRD_BLOCK * IP_BLOCK_SIZE;
  private static long MULTIPLIER_FIRST_BLOCK =
      MULTIPLIER_SECOND_BLOCK * IP_BLOCK_SIZE;


  
  private long firstIp;
  
  private long lastIp;
  
  public IpBlock(final long firstIp, final long lastIp) {
    if (firstIp > lastIp) {
      throw new IllegalArgumentException("First IP of block, " + firstIp
          + ", is larger than last IP of block, " + lastIp + ".");
    }
    this.firstIp = firstIp;
    this.lastIp = lastIp;
  }
  
  public IpBlock(
      final String firstIp, final String lastIp) {
    this(IpBlock.ipToLong(firstIp), IpBlock.ipToLong(lastIp));
  }
  
  public long getFirstIp() {
    return this.firstIp;
  }
  
  public long getLastIp() {
    return this.lastIp;
  }
  
  public boolean containsIp(final long ip) {
    return this.firstIp <= ip && ip <= this.lastIp;
  }
  
  public boolean containsIp(final String ip) {
    return this.containsIp(IpBlock.ipToLong(ip));
  }
  
  public IpBlock split(final long newLastIp) {
    if (!this.containsIp(newLastIp)) {
      throw new IllegalArgumentException();
    }
    if (this.lastIp == newLastIp) {
      return null;
    }
    
    final IpBlock higherPart = this.clone();
    higherPart.firstIp = newLastIp + 1;
    
    this.lastIp = newLastIp;
    
    return higherPart;
  }
  
  @Override
  public IpBlock clone() {
    try {
      return (IpBlock) super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e); // Should never happen
    }
  }

  @Override
  public int compareTo(final IpBlock o) {
    final long difference = this.firstIp - o.firstIp;
    if (difference < 0) {
      return -1;
    } else if (difference == 0) {
      return 0;
    }  else {
      return 1;
    }
  }
  
  /**
   * Encodes an IPv4 address as long
   * <p>
   * Decode with {@link #longToIp(long)}.
   * </p>
   * @param ip An IPv4 address
   * @return The encoded IP address
   */
  public static long addressToLong(final InetAddress address)
  throws NullPointerException, IllegalArgumentException {
    return IpBlock.ipToLong(address.getHostAddress());
  }
  
  /**
   * Encodes an IPv4 address as long
   * <p>
   * Decode with {@link #longToIp(long)}.
   * </p>
   * @param ip An IPv4 address written as XXX.XXX.XXX.XXX
   * @return The encoded IP address
   */
  public static long ipToLong(final String ip)
  throws NullPointerException, IllegalArgumentException {
    final String[] blocks = ip.split("\\.");
    if (blocks.length != 4) {
      throw new IllegalArgumentException(
          "Number of blocks in IP \"" + ip + "\" is not 4");
    }

    long result = MULTIPLIER_FIRST_BLOCK  * getBlockValue(blocks[0]);
    result     += MULTIPLIER_SECOND_BLOCK * getBlockValue(blocks[1]);
    result     += MULTIPLIER_THIRD_BLOCK  * getBlockValue(blocks[2]);
    result     +=                           getBlockValue(blocks[3]);

    return result;
  }
  
  /**
   * Decodes an IPv4 address that was encoded using {@link #ipToLong(String)}.
   * @param ip The encoded IP address
   * @return The IPv4 address written as XXX.XXX.XXX.XXX
   */
  public static String longToIp(final long ip) {
    final StringBuilder ipBuilder = new StringBuilder();
    long remaining = ip;
    
    long mod = remaining % MULTIPLIER_FIRST_BLOCK;
    ipBuilder.append((remaining - mod) / MULTIPLIER_FIRST_BLOCK).append('.');
    remaining = mod;
    
    mod = remaining % MULTIPLIER_SECOND_BLOCK;
    ipBuilder.append((remaining - mod) / MULTIPLIER_SECOND_BLOCK).append('.');
    remaining = mod;
    
    mod = remaining % MULTIPLIER_THIRD_BLOCK;
    ipBuilder.append((remaining - mod) / MULTIPLIER_THIRD_BLOCK).append('.');
    remaining = mod;
    
    ipBuilder.append(remaining);
    
    return ipBuilder.toString();
  }

  private static int getBlockValue(final String block)
  throws NullPointerException, IllegalArgumentException {
    final int value = Integer.parseInt(block);
    if (value < 0 || value >= IP_BLOCK_SIZE) {
      throw new IllegalArgumentException(
          "IP block values has to be within [0,255]");
    }
    return value;
  }

}
