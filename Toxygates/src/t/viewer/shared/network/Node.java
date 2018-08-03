package t.viewer.shared.network;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import t.common.shared.SharedUtils;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.ExpressionValue;
import t.viewer.shared.ColumnSet;

@SuppressWarnings("serial")
public class Node implements Serializable {
  String id, type;
  List<String> symbols;
  HashMap<String, Double> weights;
  
  //GWT constructor
  Node() {}

  public static Node fromRow(ExpressionRow row, String type, ColumnSet columnNames) {
    String[] geneSymbols = row.getGeneSyms();
    ExpressionValue[] values = row.getValues();
    Map<String, Double> weights = IntStream.range(0, values.length).boxed()
        .collect(Collectors.toMap(i -> columnNames.columnName(i), i -> values[i].getValue()));

    return new Node(row.getProbe(), 
        Arrays.asList(geneSymbols), type, new HashMap<String, Double>(weights));
  }
  
  public Node(String id, List<String> symbols, String type, HashMap<String, Double> weights) {
    this.id = id;
    this.symbols = symbols;
    this.type = type;
    this.weights = weights;
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof Node) {
      return id.equals(((Node)other).id);
    }
    return false;
  }
  
  public String id() { return id; }
  public String type() { return type; }

  public Map<String, Double> weights() {
    return weights;
  }
  public List<String> symbols() {
    return symbols;
  }
  
  /**
   * Convenience method to get all symbols as a slash-separated string.
   */
  public String symbolString() {
    if (symbols != null) {
      return SharedUtils.mkString(symbols, "/");
    } else {
      return "";
    }
  }

}
