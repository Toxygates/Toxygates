package otgviewer.client;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.ScreenManager;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.model.sample.AttributeSet;
import t.viewer.client.StorageParser;
import t.viewer.client.Utils;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.table.*;
import t.viewer.shared.*;
import t.viewer.shared.network.*;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * A DataScreen that can display two tables side by side.
 * The dual mode will only activate if appropriate columns have been
 * defined and saved for both tables. Otherwise, the screen will revert to
 * single-table mode.
 * 
 * The "main" table drives the side table, in the sense that what is being displayed in the
 * latter depends on the content of the former.
 */
public class DualDataScreen extends DataScreen {

  protected ExpressionTable sideExpressionTable;
  
  protected final static String sideMatrix = "SECONDARY";
  
  final static int MAX_SECONDARY_ROWS = 200;
  
  static enum DualMode {
    Forward("mRNA", "miRNA", AType.MiRNA, true), Reverse("miRNA", "mRNA", AType.MRNA, true),
    SingleMRNA("mRNA", "miRNA", null, false), SingleMiRNA("miRNA", "mRNA", null, false);
    
    final String mainType, sideType;
    final AType linkingType;
    final boolean isSplit;
    DualMode(String mainType, String sideType, AType linkingType, boolean isSplit) {
      this.mainType = mainType;
      this.sideType = sideType;
      this.linkingType = linkingType;
      this.isSplit = isSplit;
    }
    
    int sideTableWidth() { 
      if (!isSplit) {
        return 0;
      } else if (sideType.equals("miRNA")) {
        return 550;
      } else {
        return 800;
      }
    }
    TableStyle mainStyle() { return TableStyle.getStyle(mainType); }
    TableStyle sideStyle() { return TableStyle.getStyle(sideType); }
    DualMode flip() { return (this == Forward) ? Reverse : Forward; }    
  }
  
  /**
   * The preferred mode when two column types are available.
   */
  DualMode preferredDoubleMode = DualMode.Forward;
  DualMode mode = DualMode.Forward;
  
  public DualDataScreen(ScreenManager man) {
    super(man);
    
    TableFlags flags = new TableFlags(sideMatrix, true, false, 
        MAX_SECONDARY_ROWS, mode.sideType, true, true);
    sideExpressionTable = new ExpressionTable(this, flags,
        mode.sideStyle());     
    sideExpressionTable.addStyleName("sideExpressionTable");
    
    expressionTable.selectionModel().addSelectionChangeHandler(e ->
      setIndications(expressionTable, sideExpressionTable, true));      
    sideExpressionTable.selectionModel().addSelectionChangeHandler(e ->
      setIndications(sideExpressionTable, expressionTable, false));
  }
  
  protected void flipDualView() {    
    preferredDoubleMode = mode.flip();    
    List<Group> allColumns = new ArrayList<Group>(chosenColumns);
    allColumns.addAll(sideExpressionTable.chosenColumns());
    columnsChanged(allColumns);

    updateProbes();
  }
  
  protected SplitLayoutPanel splitLayout;
  
  /*
   * Note: It's probably best/easiest to rebuild the layout completely
   * each time the chosen columns change
   */
  @Override
  protected Widget mainTablePanel() {    
    splitLayout = new SplitLayoutPanel();    
    splitLayout.setWidth("100%");
    splitLayout.addEast(sideExpressionTable, 550);
    splitLayout.add(expressionTable);    
    return splitLayout;
  }
  
  @Override
  protected void addToolbars() {
    super.addToolbars();
    Button flipButton = new Button("Flip mRNA-microRNA", 
      (ClickHandler) event -> flipDualView());
    Widget tools = Utils.mkHorizontalPanel(true, flipButton);
    mainTools.add(tools);
  }
  
  @Override
  protected void setupMenuItems() {
    super.setupMenuItems();
    MenuItem mi = new MenuItem("Download interaction network (DOT)...", 
      () -> downloadNetwork(Format.DOT));             
    addAnalysisMenuItem(mi);
    
    mi = new MenuItem("Download interaction network (SIF)...", 
      () -> downloadNetwork(Format.SIF));       
    addAnalysisMenuItem(mi);
    
    mi = new MenuItem("Download interaction network (Custom)...", 
      () -> downloadNetwork(Format.Custom));       
    addAnalysisMenuItem(mi);    
  }
  
  protected void downloadNetwork(Format format) {
    if (mode.isSplit) {
      MatrixServiceAsync matrixService = manager().matrixService();
      Network network = buildNetwork("miRNA-mRNA interactions");
      matrixService.prepareNetworkDownload(network, format, new PendingAsyncCallback<String>(this) {
        public void handleSuccess(String url) {
          Utils.displayURL("Your download is ready.", "Download", url);
        }
      });
    } else {
      Window.alert("Please view mRNA and miRNA samples simultaneously to download networks.");
    }
  }
  
  /**
   * When the selection in one of the tables has changed, highlight the associated rows
   * in the other table.
   * @param fromTable
   * @param toTable
   * @param fromType
   */
  protected void setIndications(ExpressionTable fromTable, ExpressionTable toTable,
                                boolean fromMain) {
    if (mode.isSplit) {
      ExpressionRow r =
          ((SingleSelectionModel<ExpressionRow>) fromTable.selectionModel()).getSelectedObject();
      toTable.setIndicatedProbes(getIndicatedRows(r != null ? r.getProbe() : null, fromMain), true);
    }
  }
  
  protected Set<String> getIndicatedRows(@Nullable String selected, boolean fromMain) {    
    if (fromMain && selected != null) {   
      Map<String, Collection<AssociationValue>> lookup = linkingMap();
      Collection<AssociationValue> assocs = lookup.get(selected);
      if (assocs != null) {          
        return lookup.get(selected).stream().map(av -> 
            av.formalIdentifier()).collect(Collectors.toSet());        
      } else {
        logger.warning("No association indications for " + selected);
      }
    } else if (selected != null) {
      Map<String, Collection<String>> lookup = mappingSummary.getReverseMap();
      Collection<String> assocs = lookup.get(selected);
      if (assocs != null) {
        return new HashSet<String>(assocs);
      } else {
        logger.warning("No reverse association indications for " + selected);
      }           
    }    
    return new HashSet<String>();
  }
  
  //Initial title only - need the constant here since the field won't be initialised
  @Override
  protected String mainTableTitle() { return "mRNA"; }     
  
  protected boolean mainTableSelectable() { return true; }  
  
  protected List<Group> columnsOfType(List<Group> from, String type) {
    return from.stream().filter(g -> type.equals(GroupUtils.groupType(g))).
        collect(Collectors.toList());    
  }
  
  protected List<Group> columnsForMainTable(List<Group> from) {    
    List<Group> r = columnsOfType(from, mode.mainType);
    
    //If mRNA and miRNA columns are not mixed, we simply display them as they are
    if (r.isEmpty()) {
      r.addAll(from);
    }
    return r;
  }
  
  protected List<Group> columnsForSideTable(List<Group> from) {
    return columnsOfType(from, mode.sideType);        
  }
  
  protected TableStyle mainTableStyle() {
    return super.styleForColumns(columnsForMainTable(chosenColumns));
  }

  protected TableStyle sideTableStyle() {
    return super.styleForColumns(columnsForSideTable(chosenColumns));    
  }
  
  /**
   * Based on the available columns, pick the correct display mode.
   * The mode may be split or non-split.
   */
  protected DualMode pickMode(List<Group> columns) {
    String[] types = columns.stream().map(g -> GroupUtils.groupType(g)).distinct().toArray(String[]::new);
    if (types.length >= 2) {
      return preferredDoubleMode;
    } else if (types.length == 1 && types[0].equals("mRNA")) {
      return DualMode.SingleMRNA;
    } else if (types.length == 1 && types[0].equals("miRNA")) {
      return DualMode.SingleMiRNA;
    } else {
      logger.warning("No valid dual mode found.");
      return DualMode.SingleMRNA;
    }
  }
  
  @Override
  public void loadState(StorageParser p, DataSchema schema, AttributeSet attributes) {
    //TODO this is a state management hack to force the columns to be fully re-initialised
    //every time we show the screen.
    chosenColumns = new ArrayList<Group>();
    super.loadState(p, schema, attributes);
  }
  
  @Override
  protected void changeColumns(List<Group> columns) {
    logger.info("Dual mode pick for " + columns.size() + " columns");
    mode = pickMode(columns);    
    
    expressionTable.setTitleHeader(mode.mainType);    
    expressionTable.setStyleAndApply(mode.mainStyle());
    if (mode.isSplit) {
      sideExpressionTable.setTitleHeader(mode.sideType);      
      sideExpressionTable.setStyleAndApply(mode.sideStyle());
    }

    super.changeColumns(columnsForMainTable(columns));
    
    List<Group> sideColumns = columnsForSideTable(columns);
    if (sideExpressionTable != null && !sideColumns.isEmpty()) {    
      sideExpressionTable.columnsChanged(sideColumns);    
    }
    logger.info("Dual table mode: " + mode);

    splitLayout.setWidgetSize(sideExpressionTable, mode.sideTableWidth());
  }  
  
  @Override
  public void updateProbes() {   
    if (mode.isSplit) {
      expressionTable.setAssociationAutoRefresh(false);
      if (mode == DualMode.Forward) {
        expressionTable.setVisible(AType.MiRNA, true);
        expressionTable.setVisible(AType.MRNA, false);
      } else {
        expressionTable.setVisible(AType.MiRNA, false);
        expressionTable.setVisible(AType.MRNA, true);
      }
      expressionTable.setAssociationAutoRefresh(true);
    }    
    
    super.updateProbes();
    sideExpressionTable.clearMatrix();
    sideExpressionTable.setIndicatedProbes(new HashSet<String>(), false);
    expressionTable.setIndicatedProbes(new HashSet<String>(), false);    
//    extractSideTableProbes();
  }
  
  @Override
  protected void associationsUpdated(Association[] result) {
    super.associationsUpdated(result);    
    if (mode.isSplit) {
      extractSideTableProbes();
    }
  }
  
  @Override
  protected void beforeGetAssociations() {
    super.beforeGetAssociations();
    if (mode.isSplit) {
      sideExpressionTable.clearMatrix();
    }
  }
  
  //Maps main table to side table via a column.
  protected AssociationSummary<ExpressionRow> mappingSummary;
  
  protected void extractSideTableProbes() {
    mappingSummary = expressionTable.associationSummary(mode.linkingType);  
    if (sideExpressionTable.chosenColumns().isEmpty()) {
      return;
    }
    
    if (mappingSummary == null) {
      logger.info("Unable to get miRNA-mRNA summary - not updating side table probes");
      return;
    }
    String[][] rawData = mappingSummary.getTable();
    if (rawData.length < 2) {
      logger.info("No secondary probes found in summary - not updating side table probes");
      return;
    }
    String[] ids = new String[rawData.length - 1];
    Map<String, Double> counts = new HashMap<String, Double>();
    
    //The first row is headers
    for (int i = 1; i < rawData.length && i < MAX_SECONDARY_ROWS; i++) {    
      ids[i - 1] = rawData[i][1];
      counts.put(rawData[i][1], Double.parseDouble(rawData[i][2]));
    }
    
    logger.info("Extracted " + ids.length + " " + mode.sideType);    
    
    Synthetic.Precomputed countColumn = new Synthetic.Precomputed("Count", 
      "Number of times each " + mode.sideType + " appeared", counts,
      null);

    List<Synthetic> synths = new ArrayList<Synthetic>();
    synths.add(countColumn);
    
    changeSideTableProbes(ids, synths);
  }
  
  protected void changeSideTableProbes(String[] probes, List<Synthetic> synths) {
    sideExpressionTable.probesChanged(probes);
    if (probes.length > 0) {
      sideExpressionTable.getExpressions(synths, true);
    }
  }
  
  /**
   * Build Nodes by using expression values from the first column in the rows.
   * @param type
   * @param rows
   * @return
   */
  static List<Node> buildNodes(String kind, List<ExpressionRow> rows) {
    return rows.stream().map(r -> 
      Node.fromRow(r, kind)).collect(Collectors.toList());    
  }
  
  /**
   * Maps mRNA-miRNA in forward mode, miRNA-mRNA in reverse mode
   * @return
   */
  protected Map<String, Collection<AssociationValue>> linkingMap() {    
    return mappingSummary.getFullMap();
  }
  
  /**
   * Build the interaction network represented by the current view in the two tables.
   * @return
   */
  public Network buildNetwork(String title) {
    Map<String, ExpressionRow> lookup = new HashMap<String, ExpressionRow>();
    List<Node> nodes = new ArrayList<Node>();
    nodes.addAll(buildNodes(mode.mainType, expressionTable.getDisplayedRows()));    
    nodes.addAll(buildNodes(mode.sideType, sideExpressionTable.getDisplayedRows()));
        
    expressionTable.getDisplayedRows().stream().forEach(r -> lookup.put(r.getProbe(), r));
    sideExpressionTable.getDisplayedRows().stream().forEach(r -> lookup.put(r.getProbe(), r));
    
    List<Interaction> interactions = new ArrayList<Interaction>();
    Map<String, Collection<AssociationValue>> fullMap = linkingMap();
    for (String mainProbe: fullMap.keySet()) {
      for (AssociationValue av: fullMap.get(mainProbe)) {
        Node side = Node.fromAssociation(av, mode.sideType);
        Node main = Node.fromRow(lookup.get(mainProbe), mode.mainType);
        
        //Directed interaction normally from miRNA to mRNA
        Node from = (mode == DualMode.Forward) ? main : side;
        Node to = (mode == DualMode.Forward)  ? side: main;
        Interaction i = new Interaction(from, to, null, null);
        interactions.add(i);
      }
    }
    return new Network(title, nodes, interactions);
  }
}

