package de.aitools.aq.geolocating.collector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.trove.list.array.TLongArrayList;

public abstract class IpBlocks<BLOCK extends IpBlock> {
  
  private TLongArrayList firstIps;
  
  private List<BLOCK> blocks;
  
  public IpBlocks() {
    this.firstIps = new TLongArrayList();
    this.blocks = new ArrayList<>();
  }

  @Override
  public String toString() {
    final StringBuilder output = new StringBuilder();
    for (final BLOCK block : this.blocks) {
      output.append(block.toString()).append('\n');
    }
    return output.toString();
  }

  protected BLOCK getBlock(final long ip) {
    return this.getBlockByIndex(ip, this.getBlockIndex(ip));
  }
  
  protected List<BLOCK> getExactBlocks(final long firstIp, final long lastIp) {
    final int firstBlockIndex =
        this.getExactBlocksFirstBlockIndex(firstIp, lastIp);
    final int lastBlockIndex =
        this.getExactBlocksLastBlockIndex(firstBlockIndex, lastIp);
    
    return Collections.unmodifiableList(
        this.blocks.subList(firstBlockIndex, lastBlockIndex + 1));
  }
  
  private int getExactBlocksFirstBlockIndex(
      final long firstIp, final long lastIp) {
    final int firstIndex = this.getBlockIndex(firstIp);
    final BLOCK firstBlock = this.getBlockByIndex(firstIp, firstIndex);
    if (firstBlock != null) {
      if (firstBlock.getFirstIp() == firstIp) {
        // IP is at start of existing block
        return firstIndex;
      } else {
        // IP is within an existing block
        this.split(firstBlock, firstIndex, firstIp - 1);
        return firstIndex + 1;
      }
    } else {
      // IP is not in an existing block
      final int newFirstIndex = firstIndex + 1;
      long newBlockLastIp = lastIp;
      if (newFirstIndex < this.blocks.size()) {
        final BLOCK nextBlock = this.blocks.get(newFirstIndex);
        if (nextBlock.getFirstIp() < newBlockLastIp) {
          newBlockLastIp = nextBlock.getFirstIp() - 1;
        }
      }
      this.firstIps.insert(newFirstIndex, firstIp);
      this.blocks.add(newFirstIndex, this.callNew(firstIp, newBlockLastIp));
      return newFirstIndex;
    }
  }
  
  private int getExactBlocksLastBlockIndex(
      final int firstBlockIndex, final long lastIp) {
    int lastBlockIndex = firstBlockIndex;
    long previousBlockLastIp =
        this.blocks.get(firstBlockIndex).getFirstIp() - 1;
    while (lastBlockIndex < this.blocks.size()) {
      final BLOCK lastBlock = this.blocks.get(lastBlockIndex);
      if (lastIp < lastBlock.getFirstIp()) {
        // IP is not within an existing block
        break;
      }
      
      if (previousBlockLastIp + 1 < lastBlock.getFirstIp()) {
        // Some IPs between blocks did not have a block yet
        final long newBlockFirstIp = previousBlockLastIp + 1;
        final long newBlockLastIp = lastBlock.getFirstIp() - 1;
        this.firstIps.insert(lastBlockIndex, newBlockFirstIp);
        this.blocks.add(
            lastBlockIndex, this.callNew(newBlockFirstIp, newBlockLastIp));
        ++lastBlockIndex;
      }
      
      if (lastIp == lastBlock.getLastIp()) {
        // IP is at end of existing block
        return lastBlockIndex;
      } else if (lastBlock.containsIp(lastIp)) {
        // IP is within an existing block
        this.split(lastBlock, lastBlockIndex, lastIp);
        return lastBlockIndex;
      }
      previousBlockLastIp = lastBlock.getLastIp();
      ++lastBlockIndex;
    }
    
    // IP is not within an existing block, lastBlockIndex gives index for new block
    final int previousBlockIndex = lastBlockIndex - 1;
    final BLOCK previousBlock = this.blocks.get(previousBlockIndex);
    final long newBlockFirstIp = previousBlock.getLastIp() + 1;
    
    this.firstIps.insert(lastBlockIndex, newBlockFirstIp);
    this.blocks.add(lastBlockIndex, this.callNew(newBlockFirstIp, lastIp));
    return lastBlockIndex;
  }
  
  private BLOCK split(
      final BLOCK block, final int blockIndex, final long newLastIp) {
    final BLOCK higherPart = this.callSplit(block, newLastIp);
    this.firstIps.insert(blockIndex + 1, newLastIp + 1);
    this.blocks.add(blockIndex + 1, higherPart);
    return higherPart;
  }
  
  /**
   * Returns the block that contains the IP (or null if no such block exists).
   * The index has to be the return value of {@link #getBlockIndex(long)}.
   */
  private BLOCK getBlockByIndex(final long ip, final int index) {
    if (index < 0) {
      return null;
    }

    final BLOCK block = this.blocks.get(index);
    if (ip <= block.getLastIp()) {
      return block;
    } else {
      return null;
    }
  }
  
  /**
   * Returns the index of the block that contains the IP. If no such block
   * exists, it returns the index of the last block before the IP. Note that it
   * thus can also return -1!
   */
  private int getBlockIndex(final long ip) {
    int index = this.firstIps.binarySearch(ip);
    if (index < 0) {
      index = -1 * (index + 2); // see Arrays#binarySearch
    }
    if (index == this.blocks.size()) {
      --index;
    }

    if (index < 0) {
      return -1;
    }

    return index;
  }
  
  protected void serializeBlocks(final Writer writer) throws IOException {
    for (final BLOCK block : this.blocks) {
      this.serializeBlock(block, writer);
    }
  }
  
  protected void deserializeBlocks(final BufferedReader reader)
  throws IOException {
    BLOCK block;
    while ((block = deserializeBlock(reader)) != null) {
      this.firstIps.add(block.getFirstIp());
      this.blocks.add(block);
    }
  }
  
  private void serializeBlock(final BLOCK block, final Writer writer)
  throws IOException {
    writer.write(String.valueOf(block.getFirstIp()));
    writer.write('\t');
    writer.write(String.valueOf(block.getLastIp()));
    writer.write('\t');
    writer.write(this.serializeBlockContent(block));
    writer.write('\n');
  }
  
  private BLOCK deserializeBlock(final BufferedReader reader)
  throws IOException {
    final String line = reader.readLine();
    if (line == null) { return null; }
    if (line.isEmpty()) { return null; }
    
    final String[] parts = line.split("\t", 3);
    final long firstIp = Long.parseLong(parts[0]);
    final long lastIp = Long.parseLong(parts[1]);
    final String content = parts[2];

    final BLOCK block = this.callNew(firstIp, lastIp);
    this.deserializeBlockContent(block, content);

    return block;
  }
  
  protected abstract String serializeBlockContent(final BLOCK block);
  
  protected abstract void deserializeBlockContent(
      final BLOCK block, final String content);
  
  protected abstract BLOCK callNew(final long firstIp, final long lastIp);
  
  protected abstract BLOCK callSplit(final BLOCK block, final long newLastIp);

}
