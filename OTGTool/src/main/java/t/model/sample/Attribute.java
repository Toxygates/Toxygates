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

package t.model.sample;

import javax.annotation.Nullable;

/**
 * An attribute of a sample
 */
public interface Attribute {

  /**
   * Internal ID for database purposes
   */
  public String id();
  
  /**
   * Human-readable title
   */
  public String title();
  
  /**
   * Whether the attribute is numerical
   */
  public boolean isNumerical();
  
  /**
   * The section that the attribute belongs to, if any.
   */
  public @Nullable String section();

  public final String NOT_AVAILABLE = "na";

  public final String UNDEFINED_VALUE = "undef";
}
