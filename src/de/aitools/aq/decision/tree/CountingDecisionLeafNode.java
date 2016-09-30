package de.aitools.aq.decision.tree;

public class CountingDecisionLeafNode<ELEMENT, VALUE>
extends DecisionLeafNode<ELEMENT, VALUE> {
  
  private long count;

  public CountingDecisionLeafNode(final VALUE value) {
    super(value);
    this.count = 0;
  }
  
  public long getCount() {
    return this.count;
  }
  
  @Override
  public DecisionLeafNode<ELEMENT, VALUE> decide(final ELEMENT element) {
    this.count(element);
    return super.decide(element);
  }
  
  protected void count(final ELEMENT element) {
    synchronized (this) {
      ++this.count;
    }
  }
  
  protected void countsToString(final StringBuilder output) {
    output.append(this.getCount());
  }
  
  @Override
  protected StringBuilder toString(
      final int indent, final StringBuilder output) {
    if (output.length() > 0) { output.append('\t'); }
    this.countsToString(output);
    return super.toString(indent, output);
  }

}
