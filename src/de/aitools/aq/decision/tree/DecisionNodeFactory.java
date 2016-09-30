package de.aitools.aq.decision.tree;

import java.util.function.Predicate;

public class DecisionNodeFactory<ELEMENT, VALUE> {
  
  public DecisionNode<ELEMENT, VALUE> internal(
      final DecisionBranch<ELEMENT, VALUE> branching)
  throws NullPointerException {
    return new DecisionInternalNode<>(branching);
  }
  
  public DecisionNode<ELEMENT, VALUE> internal(
      final String name,
      final Predicate<? super ELEMENT> predicate,
      final DecisionNode<ELEMENT, ? extends VALUE> branchTrue,
      final DecisionNode<ELEMENT, ? extends VALUE> branchFalse)
  throws NullPointerException {
    return this.internal(
        new BinaryDecisionBranch<>(name, predicate, branchTrue, branchFalse));
  }
  
  public DecisionNode<ELEMENT, VALUE> leaf(
      final VALUE value)
  throws NullPointerException {
    return new DecisionLeafNode<ELEMENT, VALUE>(value);
  }

}
