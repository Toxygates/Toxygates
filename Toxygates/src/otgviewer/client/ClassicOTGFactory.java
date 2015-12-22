/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

import otgviewer.client.components.GeneSetEditor;
import otgviewer.client.components.Screen;
import otgviewer.client.components.compoundsel.RankingCompoundSelector;
import otgviewer.client.components.ranking.CompoundRanker;
import otgviewer.client.components.ranking.FullCompoundRanker;

/**
 * This factory lets the UI mimic the "classic" Toxygates interface
 * as released in 2013.
 */
public class ClassicOTGFactory extends OTGFactory {
  @Override
  public CompoundRanker compoundRanker(Screen _screen, RankingCompoundSelector selector) {
    return new FullCompoundRanker(_screen, selector);
  }

  @Override
  public GeneSetEditor geneSetEditor(Screen screen) {
    return new GeneSetEditor(screen) {
      @Override
      protected boolean hasClustering() {
        return false;
      }
    };
  }

  @Override
  public boolean hasHeatMapMenu() {
    return false;
  }
  
  @Override
  public GeneSetsMenuItem geneSetsMenuItem(DataScreen screen) {
    return new GeneSetsMenuItem(screen) {
      @Override
      protected boolean hasUserClustering() {
        return false;
      }
      @Override
      protected boolean hasPredefinedClustering() {
        return false;
      }
    };
  }
}
