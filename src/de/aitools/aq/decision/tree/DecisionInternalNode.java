package de.aitools.aq.decision.tree;

public class DecisionInternalNode<ELEMENT, VALUE>
extends DecisionNode<ELEMENT, VALUE> {
  
  private DecisionBranch<ELEMENT, VALUE> branching;
  
  public DecisionInternalNode(final DecisionBranch<ELEMENT, VALUE> branching)
  throws NullPointerException {
    if (branching == null) { throw new NullPointerException(); }
    this.branching = branching;
  }

  @Override
  public DecisionLeafNode<? extends ELEMENT, ? extends VALUE> decide(
      final ELEMENT element) {
    return this.branching.decide(element);
  }

  @Override
  protected StringBuilder toString(
      final int indent, final StringBuilder output) {
    return this.branching.toString(indent, output);
  }

}
