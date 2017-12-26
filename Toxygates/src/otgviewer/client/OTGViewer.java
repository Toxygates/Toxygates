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

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.OTGSchema;
import t.common.shared.DataSchema;
import t.viewer.client.*;
import t.viewer.client.intermine.InterMineData;
import t.viewer.shared.intermine.IntermineInstance;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

public class OTGViewer extends TApplication {

  @Override
  protected void initScreens() {
    addScreenSeq(new StartScreen(this));
    addScreenSeq(new ColumnScreen(this));
    addScreenSeq(new SampleSearchScreen(this));
    addScreenSeq(new DualDataScreen(this));
    addScreenSeq(new RankingScreen(this));
    addScreenSeq(new PathologyScreen(this));
    addScreenSeq(new SampleDetailScreen(this));    

    if (factory.hasMyData()) {      
      addScreenSeq(new MyDataScreen(this));
    }
  }

  final private OTGSchema schema = new OTGSchema();

  @Override
  public DataSchema schema() {
    return schema;
  }

  private UIFactory factory;

  @Override
  protected void setupUIBase() {
    initFactory();
    super.setupUIBase();
  }

  private void initFactory() {
    UIFactory f;
    //TODO hardcoding these instance names here may be controversial
    // - think of a better way of handling this
    if (appInfo().instanceName().equals("toxygates")
        || appInfo().instanceName().equals("tg-update")) {
      f = new ClassicOTGFactory();
    } else {
      f = new OTGFactory();
    }    
    logger.info("Using factory: " + f.getClass().toString());
    factory = f;
  }
  
  @Override
  public UIFactory factory() {    
    return factory;
  }

  @Override
  protected void setupToolsMenu(MenuBar toolsMenuBar) {
    for (IntermineInstance ii: appInfo.intermineInstances()) {
      toolsMenuBar.addItem(intermineMenu(ii));
    }  
  }
  
  protected MenuItem intermineMenu(final IntermineInstance inst) {
    MenuBar mb = new MenuBar(true);
    final String title = inst.title();
    MenuItem mi = new MenuItem(title + " data", mb);

    mb.addItem(new MenuItem("Import gene sets from " + title + "...", () -> {      
        new InterMineData(currentScreen, inst).importLists(true);
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT, Analytics.ACTION_IMPORT_GENE_SETS,
            title);      
    }));

    mb.addItem(new MenuItem("Export gene sets to " + title + "...", () -> {      
        new InterMineData(currentScreen, inst).exportLists();
        Analytics.trackEvent(Analytics.CATEGORY_IMPORT_EXPORT, Analytics.ACTION_EXPORT_GENE_SETS,
            title);      
    }));

    mb.addItem(new MenuItem("Enrichment...", () -> {      
        //TODO this should be disabled if we are not on the data screen.
        //The menu item is only here in order to be logically grouped with other 
        //TargetMine items, but it is a duplicate and may be removed.
        if (currentScreen instanceof DataScreen) {
          ((DataScreen) currentScreen).runEnrichment(inst);
        } else {
          Window.alert("Please go to the data screen to use this function.");
        }      
    }));

    mb.addItem(new MenuItem("Go to " + title, () -> 
        Utils.displayURL("Go to " + title + " in a new window?", "Go", inst.webURL())
        ));
      
    return mi;
  }

  @Override
  protected List<PersistedState<?>> getPersistedItems() {
    List<PersistedState<?>> r = new ArrayList<PersistedState<?>>();
    r.addAll(super.getPersistedItems());
    return r;
  }  
}
