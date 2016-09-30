package de.aitools.aq.decision.tree;

import java.util.function.Predicate;

public class BinaryDecisionBranch<ELEMENT, VALUE>
extends DecisionBranch<ELEMENT, VALUE> {
  
  private final Predicate<? super ELEMENT> predicate;
  
  private final DecisionNode<ELEMENT, ? extends VALUE> branchTrue;
  
  private final DecisionNode<ELEMENT, ? extends VALUE> branchFalse;
  
  public BinaryDecisionBranch(
      final String name,
      final Predicate<? super ELEMENT> predicate,
      final DecisionNode<ELEMENT, ? extends VALUE> branchTrue,
      final DecisionNode<ELEMENT, ? extends VALUE> branchFalse)
  throws NullPointerException {
    super(name);
    if (predicate == null) { throw new NullPointerException(); }
    if (branchTrue == null) { throw new NullPointerException(); }
    if (branchFalse == null) { throw new NullPointerException(); }
    
    this.predicate = predicate;
    this.branchTrue = branchTrue;
    this.branchFalse = branchFalse;
  }
 
  public DecisionLeafNode<? extends ELEMENT, ? extends VALUE> decide(
      final ELEMENT element) {
    if (this.predicate.test(element)) {
      return this.branchTrue.decide(element);
    } else {
      return this.branchFalse.decide(element);
    }
  }

  @Override
  public StringBuilder toString(
      final int indent, final StringBuilder output) {
    this.branchToString("true", this.branchTrue, indent, output);
    this.branchToString("false", this.branchFalse, indent, output);
    return output;
  }
}
