package de.aitools.aq.decision.tree;

public class CountingDecisionNodeFactory<ELEMENT, VALUE>
extends DecisionNodeFactory<ELEMENT, VALUE> {
  
  public DecisionNode<ELEMENT, VALUE> internal(
      final DecisionBranch<ELEMENT, VALUE> branching)
  throws NullPointerException {
    return new CountingDecisionInternalNode<>(branching);
  }
  
  public DecisionNode<ELEMENT, VALUE> leaf(
      final VALUE value)
  throws NullPointerException {
    return new CountingDecisionLeafNode<ELEMENT, VALUE>(value);
  }

}
