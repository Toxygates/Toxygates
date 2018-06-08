/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import otgviewer.client.components.*;
import otgviewer.client.components.compoundsel.CompoundSelector;
import otgviewer.client.components.groupdef.GroupInspector;
import otgviewer.client.components.groupdef.SelectionTDGrid;
import otgviewer.client.components.groupdef.SelectionTDGrid.UnitListener;
import otgviewer.client.components.ranking.CompoundRanker;
import t.common.client.ValueAcceptor;
import t.common.client.components.StringArrayTable;
import t.common.shared.DataSchema;
import t.common.shared.StringList;
import t.common.shared.sample.Group;
import t.viewer.shared.intermine.IntermineInstance;

public interface UIFactory {

  public SelectionTDGrid selectionTDGrid(Screen scr, @Nullable UnitListener listener);

  public CompoundRanker compoundRanker(Screen _screen);
  
  public GroupInspector groupInspector(CompoundSelector cs, Screen scr,
      GroupInspector.Delegate delegate);
  
  public GroupLabels groupLabels(Screen screen, DataSchema schema, List<Group> groups);
  
  public GeneSetEditor geneSetEditor(ImportingScreen screen);
  
  public boolean hasHeatMapMenu();
  
  public GeneSetsMenuItem geneSetsMenuItem(DataScreen screen);

  /**
   * Enrichment for a gene set
   * @param screen
   */
  void enrichment(ImportingScreen screen, StringList list,
      @Nullable IntermineInstance preferredInstance);

  /**
   * Enrichment for multiple gene sets
   * @param screen
   * @param lists
   */
  void multiEnrichment(ImportingScreen screen, StringList[] lists,
      @Nullable IntermineInstance preferredInstance);
  
  boolean hasMyData();
  
  /**
   * Return a summary of samples for display on the start screen, if appropriate.
   * The value will never be produced if no table should be displayed.
   * @param screen
   */
  default void sampleSummaryTable(Screen screen, 
                                  ValueAcceptor<StringArrayTable> acceptor) {    
  }
  
}
