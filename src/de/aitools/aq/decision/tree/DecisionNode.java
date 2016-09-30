package de.aitools.aq.decision.tree;

import java.util.Arrays;

public abstract class DecisionNode<ELEMENT, VALUE> {
  
  private static final int INDENT_SIZE = 2;
  
  public DecisionNode() {
  }

  public abstract DecisionLeafNode<? extends ELEMENT, ? extends VALUE> decide(
      final ELEMENT element);
  
  @Override
  public String toString() {
    return this.toString(0, new StringBuilder()).append('\n').toString();
  }
  
  protected abstract StringBuilder toString(
      final int indent, final StringBuilder output);
  
  
  public static StringBuilder newLine(
      final StringBuilder output, final int indent) {
    if (output.length() > 0) { output.append('\n'); }
    if (indent > 0) {
      final char[] characters = new char[indent * INDENT_SIZE];
      Arrays.fill(characters, ' ');
      output.append(characters);
    }
    return output;
  }

}
