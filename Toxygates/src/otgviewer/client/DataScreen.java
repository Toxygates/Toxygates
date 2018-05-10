/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import otgviewer.client.dialog.MirnaSourceDialog;
import t.common.shared.*;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.client.*;
import t.viewer.client.components.DataView;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.table.*;
import t.viewer.client.table.RichTable.HideableColumn;
import t.viewer.shared.intermine.IntermineInstance;
import t.viewer.shared.mirna.MirnaSource;

/**
 * The main data display screen. Data is displayed in the ExpressionTable widget.
 */
public class DataScreen extends DLWScreen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected DataView dataView;
  
  protected ExpressionTable expressionTable;

  protected String[] lastProbes;
  protected List<Group> lastColumns;

  // TODO: factor out heat map management logic + state
  // together with UIFactory.hasHeatMapMenu
  @Nullable
  private MenuItem heatMapMenu;
  Map<String, TickMenuItem> hideableMenuItems = new HashMap<String, TickMenuItem>();

  public DataScreen(ScreenManager man) {
    super("View data", key, true, man, man.resources().dataDisplayHTML(),
        man.resources().dataDisplayHelp());
    geneSetToolbar = makeGeneSetSelector();
    dataView = makeDataView();
    this.addListener(dataView);    
    expressionTable = dataView.expressionTable();
    expressionTable.setDisplayPColumns(false);
    // To ensure that GeneSetToolbar has chosenColumns
    addListener(geneSetToolbar);
  }

  protected GeneSetToolbar makeGeneSetSelector() {
    return new GeneSetToolbar(this) {
      @Override
      public void itemsChanged(List<String> items) {
        reloadDataIfNeeded();
      }
    };
  }
  
  protected static final String defaultMatrix = "DEFAULT";

  protected DataView makeDataView() {
    return new TableView(this, mainTableTitle(),
      mainTableSelectable()) {
        @Override
        protected void beforeGetAssociations() {
          super.beforeGetAssociations();
          DataScreen.this.beforeGetAssociations();
        }
    };
  }
  
  protected @Nullable String mainTableTitle() { return null; }
  
  protected boolean mainTableSelectable() { return false; }  

  //TODO remove
  protected void beforeGetAssociations() {}

  static final public int STANDARD_TOOL_HEIGHT = 43;

  protected Label infoLabel;
  
  protected Widget makeInfoPanel() {
    infoLabel = Utils.mkEmphLabel("");
    infoLabel.addStyleName("infoLabel");
    HorizontalPanel p = Utils.mkHorizontalPanel(true, infoLabel);
    p.setHeight(STANDARD_TOOL_HEIGHT + "px");
    return p;       
  }
  
  protected HorizontalPanel mainTools;
  
  @Override
  protected void addToolbars() {
    super.addToolbars();
    mainTools = new HorizontalPanel();
    mainTools.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    mainTools.add(expressionTable.tools());
    mainTools.add(geneSetToolbar.selector());    
    mainTools.add(makeInfoPanel());
    addToolbar(mainTools, STANDARD_TOOL_HEIGHT);
    addToolbar(expressionTable.analysisTools(), STANDARD_TOOL_HEIGHT);
  }

  protected void displayInfo(String message) {
    logger.info("User info: " + message);
    infoLabel.setText(message);
  }
  
  @Override
  public Widget content() {
    setupMenuItems();
    return mainTablePanel();
  }
  
  protected Widget mainTablePanel() {
    ResizeLayoutPanel rlp = new ResizeLayoutPanel();
    rlp.setWidth("100%");
//    rlp.add(dataView);
    rlp.add(expressionTable);
    return rlp;
  }

  protected void setupMenuItems() {
    MenuBar menuBar = new MenuBar(true);
    MenuItem mActions = new MenuItem("File", false, menuBar);
    MenuItem mntmDownloadCsv =
        new MenuItem("Download CSV (grouped samples)...", false, () -> {          
            expressionTable.downloadCSV(false);
            Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
                Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_GROUPED_SAMPLES);
        });
    menuBar.addItem(mntmDownloadCsv);
    mntmDownloadCsv = new MenuItem("Download CSV (individual samples)...", false, () -> {      
        expressionTable.downloadCSV(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT,
            Analytics.ACTION_DOWNLOAD_EXPRESSION_DATA, Analytics.LABEL_INDIVIDUAL_SAMPLES);      
    });
    menuBar.addItem(mntmDownloadCsv);

    addMenu(mActions);

    menuBar = new MenuBar(true);
    //TODO store the TickMenuItem in HideableColumn so that the state can be synchronised
    for (final HideableColumn<ExpressionRow, ?> c : expressionTable.getHideableColumns()) {
    	final String title = c.columnInfo().title();
   
      hideableMenuItems.put(title, 
        //Automatically added to the menuBar
      new TickMenuItem(menuBar, title, c.visible()) {
        @Override
        public void stateChange(boolean newState) {
          expressionTable.setVisible(c, newState);
          expressionTable.getAssociations();
          if (newState) {
        	  Analytics.trackEvent(Analytics.CATEGORY_TABLE, 
        			  Analytics.ACTION_DISPLAY_OPTIONAL_COLUMN, title);
          }
        }
      });
    }

    GeneSetsMenuItem geneSetsMenu = factory().geneSetsMenuItem(this);
    addListener(geneSetsMenu);
    addMenu(geneSetsMenu.menuItem());

    MenuItem mColumns = new MenuItem("View", false, menuBar);
    addMenu(mColumns);

    // TODO: this is effectively a tick menu item without the tick.
    // It would be nice to display the tick graphic, but then the textual alignment
    // of the other items on the menu becomes odd.
    addAnalysisMenuItem(new TickMenuItem("Compare two sample groups", false, false) {
      @Override
      public void stateChange(boolean newState) {
        if (!visible) {
          // Trigger screen
          manager.attemptProceed(DataScreen.key);
          setState(true);
          showToolbar(expressionTable.analysisTools());
        } else {
          // Just toggle
          if (newState) {
            showToolbar(expressionTable.analysisTools());
          } else {
            hideToolbar(expressionTable.analysisTools());
          }
        }
      }
    }.menuItem());

    addAnalysisMenuItem(new MenuItem("Enrichment...", () -> runEnrichment(null)));
    
    MenuItem mi = new MenuItem("Select MiRNA sources...", () -> {      
        MirnaSource[] sources = appInfo().mirnaSources();        
        new MirnaSourceDialog(DataScreen.this, manager().probeService(), sources, 
          mirnaState).
          display("Choose miRNA sources", DialogPosition.Center);
    });
    
    addAnalysisMenuItem(mi);
    
    if (factory().hasHeatMapMenu()) {
      heatMapMenu = new MenuItem("Show heat map", () -> makeHeatMap());        
      addAnalysisMenuItem(heatMapMenu);
    }
  }
  
  public void runEnrichment(@Nullable IntermineInstance preferredInstance) {
    logger.info("Enrich " + DataScreen.this.displayedAtomicProbes().length + " ps");
    StringList genes = 
        new StringList(StringList.PROBES_LIST_TYPE, 
            "temp", DataScreen.this.displayedAtomicProbes());
    DataScreen.this.factory().enrichment(DataScreen.this, genes, preferredInstance);
  }
  
  protected void makeHeatMap() {
    HeatmapViewer.show(DataScreen.this, expressionTable.getValueType());
  }

  @Override
  public boolean enabled() {
    // return manager.isConfigured(ProbeScreen.key)
    // && manager.isConfigured(ColumnScreen.key);
    return manager.isConfigured(ColumnScreen.key);
  }

  /**
   * Trigger a data reload, if necessary.
   */
  public void reloadDataIfNeeded() {
    dataView.reloadDataIfNeeded();    
  }

  @Override
  public void show() {
    super.show();
    reloadDataIfNeeded();
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public String getGuideText() {
    return "Here you can inspect expression values for the sample groups you have defined. "
        + "Click on column headers to sort data.";
  }

  @Override
  public void probesChanged(String[] probes) {
    super.probesChanged(probes);
    logger.info("received " + probes.length + " probes");

    StorageParser p = getParser(this);
    storeProbes(p);

    lastProbes = null;
    lastColumns = null;
  }

  @Override
  public void geneSetChanged(ItemList geneSet) {
    super.geneSetChanged(geneSet);

    StorageParser p = getParser(this);
    storeGeneSet(p);
  }

  public String[] displayedAtomicProbes() {
    String[] r = expressionTable.currentMatrixInfo().getAtomicProbes();
    if (r.length < expressionTable.currentMatrixInfo().numRows()) {
      Window.alert("Too many genes. Only the first " + r.length + " genes will be used.");
    }
    return r;
  }
  
  protected PersistedState<MirnaSource[]> mirnaState = new PersistedState<MirnaSource[]>(
      "miRNASources", "mirnaSources") {
    @Override
    protected String doPack(MirnaSource[] state) {
      return Arrays.stream(state).map(ms -> ms.pack()).collect(Collectors.joining(":::"));
    }

    @Override
    protected MirnaSource[] doUnpack(String state) {
      String[] spl = state.split(":::");
      return Arrays.stream(spl).map(ms -> MirnaSource.unpack(ms)).
          filter(ms -> ms != null).toArray(MirnaSource[]::new);
    }

    @Override
    public void onValueChange(MirnaSource[] state) {
      if (state != null) {        
        manager().probeService().setMirnaSources(state, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Unable to set miRNA sources.");
          }

          @Override
          public void onSuccess(Void result) {
            expressionTable.getAssociations();
          }
        });
      }
    }
  };

  
  @Override
  public List<PersistedState<?>> getPersistedItems() {
    List<PersistedState<?>> r = new ArrayList<PersistedState<?>>();
    r.addAll(expressionTable.getPersistedItems());
    r.add(mirnaState);
    return r;
  }

  @Override
  public void loadPersistedState() {
    super.loadPersistedState();
    for (String title: hideableMenuItems.keySet()) {
      TickMenuItem mi = hideableMenuItems.get(title);
      boolean state = expressionTable.persistedVisibility(title, mi.getState());
      mi.setState(state);
    }
  }

  @Override
  public boolean importProbes(String[] probes) {
    boolean changed = super.importProbes(probes);
    if (changed) {
      reloadDataIfNeeded();
    }
    return changed;
  }

  @Override
  public boolean importColumns(List<Group> groups) {
    boolean changed = super.importColumns(groups);
    if (changed) {
      reloadDataIfNeeded();
    }
    return changed;
  }
}
