/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */
package otgviewer.client;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.GroupLabels;
import otgviewer.client.components.Screen;
import otgviewer.client.components.compoundsel.CompoundSelector;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.client.components.groupdef.GroupInspector;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import otgviewer.client.components.groupdef.TreatedControlGroupInspector;
import otgviewer.client.components.groupdef.TreatedControlSelTDGrid;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.SimpleCompoundRanker;
import t.common.shared.DataSchema;
import t.common.shared.StringList;
import t.common.shared.sample.Group;
import t.viewer.client.intermine.InterMineData;
import t.viewer.shared.intermine.IntermineInstance;


/**
 * This is the standard factory for new Toxygates/AdjuvantDB instances.
 */
public class OTGFactory implements UIFactory {

  @Override
  public SelectionTDGrid selectionTDGrid(Screen scr, @Nullable UnitListener listener) {
    return new TreatedControlSelTDGrid(scr, listener);
  }

  @Override
  public CompoundRanker compoundRanker(Screen _screen, RankingCompoundSelector selector) {
    return new SimpleCompoundRanker(_screen, selector);
  }

  @Override
  public GroupInspector groupInspector(CompoundSelector cs, Screen scr) {
    return new TreatedControlGroupInspector(cs, scr);
  }

  @Override
  public GroupLabels groupLabels(Screen screen, DataSchema schema, List<Group> groups) {
    return new GroupLabels(screen, schema, groups);
  }

  @Override
  public GeneSetEditor geneSetEditor(Screen screen) {
    return new GeneSetEditor(screen);
  }  
  
  @Override
  public boolean hasHeatMapMenu() {
    return true;
  }

  @Override
  public GeneSetsMenuItem geneSetsMenuItem(DataScreen screen) {
    return new GeneSetsMenuItem(screen);
  }

  @Override
  public void enrichment(Screen screen, StringList probes, 
      @Nullable IntermineInstance preferredInst) {
    InterMineData tm = new InterMineData(screen, preferredInst);
    tm.enrich(probes);
  }

  @Override
  public void multiEnrichment(Screen screen, StringList[] lists, 
      @Nullable IntermineInstance preferredInst) {
    InterMineData tm = new InterMineData(screen, preferredInst);
    tm.multiEnrich(lists);    
  }
  
  @Override
  public boolean hasMyData() {
    return true;
  }
}
