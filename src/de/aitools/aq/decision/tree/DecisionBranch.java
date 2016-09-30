package de.aitools.aq.decision.tree;

public abstract class DecisionBranch<ELEMENT, VALUE> {

  private final String name;
  
  public DecisionBranch(final String name) {
    if (name == null) { throw new NullPointerException(); }
    this.name = name;
  }
  
  public String getName() {
    return this.name;
  }
  
  public abstract DecisionLeafNode<? extends ELEMENT, ? extends VALUE> decide(
      final ELEMENT element);
  
  @Override 
  public String toString() {
    return this.toString(0, new StringBuilder()).append('\n').toString();
  }
  
  public abstract StringBuilder toString(
      final int indent, final StringBuilder output);
  
  protected void branchToString(
      final String branchValue,
      final DecisionNode<? super ELEMENT, ? extends VALUE> branch,
      final int indent, final StringBuilder output) {
    DecisionNode.newLine(output, indent).append(this.getName()).append(" = ")
      .append(branchValue);
    branch.toString(indent + 1, output);
  }

}
