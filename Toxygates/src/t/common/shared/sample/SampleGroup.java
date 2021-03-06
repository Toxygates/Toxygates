/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

package t.common.shared.sample;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.model.sample.Attribute;

/**
 * A way of grouping microarray samples. Unique colors for each group can be generated.
 */
@SuppressWarnings("serial")
abstract public class SampleGroup<S extends Sample> implements DataColumn<S>, Serializable,
    Comparable<SampleGroup<?>> {

  /**
   * The default color is currently used by tests only. Other users should currently call
   * SampleGroup.setColors before creating instances of this class.
   */
  public static String[] groupColors = new String[] {"#FF7300"};

  public static void setColors(String[] colors) {
    groupColors = colors;
  }

  private static int nextColor = 0;

  public SampleGroup() {}

  protected String name;
  protected String color;
  protected S[] samples;

  public SampleGroup(String name, S[] samples, String color) {
    this.name = name;
    this.samples = samples;
    this.color = color;
  }

  public SampleGroup(String name, S[] samples) {
    this(name, samples, pickColor());
  }

  public static synchronized String pickColor() {
    nextColor += 1;
    if (nextColor > groupColors.length) {
      nextColor = 1;
    }
    return groupColors[nextColor - 1];
  }

  public S[] samples() {
    return samples;
  }

  @Override
  public S[] getSamples() {
    return samples;
  }
  
  public boolean containsSample(String sampleId) {
    return Arrays.stream(samples).anyMatch(s -> s.id().equals(sampleId));
  }

  public String getName() {
    return name;
  }

  @Override
  public String getShortTitle() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getColor() {
    return color;
  }

  public String getStyleName() {
    return "Group" + getColorIndex();
  }

  public static int colorIndexOf(String color) {
    return SharedUtils.indexOf(groupColors, color);
  }

  private int getColorIndex() {
    return colorIndexOf(color);
  }

  @Override
  public int compareTo(SampleGroup<?> other) {
    return name.compareTo(other.getName());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(samples) * 41 + name.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SampleGroup) {
      SampleGroup<?> that = (SampleGroup<?>) other;
      return that.canEqual(this) && Arrays.deepEquals(this.samples, that.samples())
          && name.equals(that.getName()) && color.equals(that.getColor());
    }
    return false;
  }

  protected boolean canEqual(Object other) {
    return other instanceof SampleGroup;
  }

  public Stream<String> collect(Attribute parameter) {
    return SampleClassUtils.collectInner(Arrays.asList(samples), parameter);
  }

  public static <S extends Sample, G extends SampleGroup<S>> 
  Stream<String> collectAll(Collection<G> from, Attribute parameter) {
    return from.stream().flatMap(g -> g.collect(parameter)).distinct();    
  }

}
