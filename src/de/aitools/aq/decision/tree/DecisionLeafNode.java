package de.aitools.aq.decision.tree;

public class DecisionLeafNode<ELEMENT, VALUE>
extends DecisionNode<ELEMENT, VALUE> {
  
  private final VALUE value;
  
  public DecisionLeafNode(final VALUE value) {
    this.value = value;
  }
  
  public VALUE getValue() {
    return this.value;
  }

  @Override
  public DecisionLeafNode<ELEMENT, VALUE> decide(final ELEMENT element) {
    return this;
  }

  @Override
  protected StringBuilder toString(
      final int indent, final StringBuilder output) {
    if (output.length() > 0) { output.append('\t'); }
    output.append(this.value);
    return output;
  }

}
