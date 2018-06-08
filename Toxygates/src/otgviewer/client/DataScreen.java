/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import javax.annotation.Nullable;

import otgviewer.client.components.*;
import t.common.shared.ItemList;
import t.common.shared.StringList;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.model.sample.AttributeSet;
import t.viewer.client.*;
import t.viewer.client.components.DataView;
import t.viewer.client.table.TableView;
import t.viewer.shared.intermine.IntermineInstance;

import com.google.gwt.user.client.ui.*;

/**
 * The main data display screen. It displays data in a single DataView instance.
 */
public class DataScreen extends MinimalScreen implements ImportingScreen {

  public static final String key = "data";
  protected GeneSetToolbar geneSetToolbar;
  protected DataView dataView;

  protected String[] lastProbes;
  protected List<Group> lastColumns;

  // TODO: factor out heat map management logic + state
  // together with UIFactory.hasHeatMapMenu
  @Nullable
  private MenuItem heatMapMenu;

  private List<MenuItem> intermineMenuItems;

  GeneSetsMenuItem geneSetsMenu;

  protected String[] chosenProbes = new String[0];
  protected List<Group> chosenColumns = new ArrayList<Group>();
  public List<ItemList> chosenItemLists = new ArrayList<ItemList>();
  public ItemList chosenGeneSet = null;
  protected List<ItemList> chosenClusteringList = new ArrayList<ItemList>();

  private String[] urlProbes = null;
  private List<String[]> urlGroups;
  private List<String> groupNames;

  @Override
  public void loadState(AttributeSet attributes) {
    StorageParser parser = getParser();
    chosenProbes = parser.getProbes();
    chosenColumns = parser.getChosenColumns(schema(), attributes());
    chosenItemLists = parser.getItemLists();
    chosenGeneSet = parser.getGeneSet();
    chosenClusteringList = parser.getClusteringLists();
    dataView.columnsChanged(chosenColumns);
    dataView.sampleClassChanged(parser.getSampleClass(attributes()));
    dataView.probesChanged(chosenProbes);  

    geneSetToolbar.geneSetChanged(chosenGeneSet);

    geneSetsMenu.itemListsChanged(chosenItemLists);
  }

  DataScreen(ScreenManager man, List<MenuItem> intermineItems) {
    super("View data", key, man, man.resources().dataDisplayHTML(),
        man.resources().dataDisplayHelp());
    geneSetToolbar = makeGeneSetSelector();    
    dataView = makeDataView();
    setupMenuItems();
  }

  @Override
  public List<ItemList> clusteringList() {
    return chosenClusteringList;
  }

  @Override
  public List<ItemList> itemLists() {
    return chosenItemLists;
  }

  public ItemList geneSet() {
    return chosenGeneSet;
  }

  @Override
  public List<Group> chosenColumns() {
    return chosenColumns;
  }

  @Override
  public String[] chosenProbes() {
    return chosenProbes;
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
    Widget dvTools = dataView.tools();
    if (dvTools != null) {
      mainTools.add(dvTools);
    }
    mainTools.add(geneSetToolbar.selector());    
    mainTools.add(makeInfoPanel());
    addToolbar(mainTools, STANDARD_TOOL_HEIGHT);    
    for (Widget w: dataView.toolbars()) {
      addToolbar(w, STANDARD_TOOL_HEIGHT);
    }    
  }

  protected void displayInfo(String message) {
    logger.info("User info: " + message);
    infoLabel.setText(message);
  }
  
  @Override
  public Widget content() {    
    return dataView;
  }

  protected TableView makeDataView() {
    return new TableView(this, mainTableTitle(),
      mainTableSelectable()) {
        @Override
        protected void beforeGetAssociations() {
          super.beforeGetAssociations();
          DataScreen.this.beforeGetAssociations();
        }
    };
  }

  protected MenuBar analysisMenu;
  protected void setupMenuItems() {
    analysisMenu = new MenuBar(true);
    MenuItem analysisItem = new MenuItem("Tools", false, analysisMenu);
    addMenu(analysisItem);
        
    for (MenuItem mi: dataView.topLevelMenus()) {
      addMenu(mi);
    }
    
    addAnalysisMenuItem(new MenuItem("Enrichment...", () -> runEnrichment(null)));   
    for (MenuItem mi: dataView.analysisMenuItems()) {
      addAnalysisMenuItem(mi);
    }

    geneSetsMenu = factory().geneSetsMenuItem(this);
    addMenu(geneSetsMenu.menuItem());       
  }
  
  public void addAnalysisMenuItem(MenuItem mi) {
    analysisMenu.addItem(mi);
  }
  
  @Override
  public void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists) {
    itemListsChanged(itemLists);
    clusteringListsChanged(clusteringLists);
  }

  @Override
  public void runEnrichment(@Nullable IntermineInstance preferredInstance) {
    logger.info("Enrich " + DataScreen.this.displayedAtomicProbes().length + " ps");
    StringList genes = 
        new StringList(StringList.PROBES_LIST_TYPE, 
            "temp", DataScreen.this.displayedAtomicProbes());
    DataScreen.this.factory().enrichment(DataScreen.this, genes, preferredInstance);
  }

  @Override
  public boolean enabled() {
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
    getProbes();
    getColumns();
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

  public void probesChanged(String[] probes) {
    logger.info("received " + probes.length + " probes");

    chosenProbes = probes;

    getParser().storeProbes(chosenProbes);

    lastProbes = null;
    lastColumns = null;
    dataView.probesChanged(probes);
  }

  public void geneSetChanged(ItemList geneSet) {
    chosenGeneSet = geneSet;
    getParser().storeGeneSet(geneSet);
    geneSetToolbar.geneSetChanged(geneSet);
  }

  public void columnsChanged(List<Group> columns) {
    chosenColumns = columns;
    dataView.columnsChanged(columns);
  }

  @Override
  public void itemListsChanged(List<ItemList> lists) {
    chosenItemLists = lists;
    getParser().storeItemLists(lists);
    geneSetsMenu.itemListsChanged(lists);
  }

  @Override
  public void clusteringListsChanged(List<ItemList> lists) {
    chosenClusteringList = lists;
    getParser().storeClusteringLists(lists);
    geneSetsMenu.clusteringListsChanged(lists);
  }

  public String[] displayedAtomicProbes() {
    return dataView.displayedAtomicProbes();    
  }

  @Override
  public List<PersistedState<?>> getPersistedItems() {
    return dataView.getPersistedItems();    
  }

  @Override
  public void setUrlProbes(String[] probes) {
    urlProbes = probes;
  }

  /**
   * Fetch probes if applicable. Used to load probes that were read from URL string.
   */
  public void getProbes() {
    if (urlProbes != null) {
      manager().probeService().identifiersToProbes(urlProbes, true, true, false, null,
          new PendingAsyncCallback<String[]>(this,
              "Failed to resolve gene identifiers") {
            @Override
            public void handleSuccess(String[] probes) {
              importProbes(probes);
            }
          });
    }
    urlProbes = null;
  }

  public boolean importProbes(String[] probes) {
    if (Arrays.equals(probes, chosenProbes)) {
      return false;
    } else {
      probesChanged(probes);
      storeState();
      reloadDataIfNeeded();     
      return true;
    }
  }

  @Override
  public void setUrlColumns(List<String[]> groups, List<String> names) {
    urlGroups = groups;
    groupNames = names;
  }

  /**
   * Fetch columns if applicable. Used to load columns that were read from URL string.
   */
  public void getColumns() {
    if (urlGroups != null) {
      manager().sampleService().samplesById(urlGroups,
          new PendingAsyncCallback<List<Sample[]>>(this,
              "Failed to look up samples") {
            @Override
            public void handleSuccess(List<Sample[]> samples) {
              int i = 0;
              List<Group> finalGroups = new ArrayList<Group>();
              for (Sample[] ss : samples) {
                Group g = new Group(schema(), groupNames.get(i), ss);
                i += 1;
                finalGroups.add(g);
              }
              groupNames = null;
              importColumns(finalGroups);
            }
          });
    }
    urlGroups = null;
  }

  public boolean importColumns(List<Group> groups) {
    if (groups.size() > 0 && !groups.equals(chosenColumns)) {
      columnsChanged(groups);
      storeState();
      reloadDataIfNeeded();
      return true;
    } else {
      return false;
    }
  }
}
