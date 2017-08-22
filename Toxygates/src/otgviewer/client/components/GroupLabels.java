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

package otgviewer.client.components;

import java.util.List;

import t.common.shared.DataSchema;
import t.common.shared.sample.Group;
import t.common.shared.sample.SampleClassUtils;
import t.model.SampleClass;
import t.viewer.client.Utils;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

public class GroupLabels extends Composite {

  protected List<Group> groups;
  protected DataSchema schema;
  private FlowPanel fpo;
  protected Screen screen;

  final static int LABEL_MAX_LEN = 40;

  public GroupLabels(Screen screen, DataSchema schema, List<Group> groups) {
    fpo = new FlowPanel();
    this.groups = groups;
    this.schema = schema;
    this.screen = screen;
    initWidget(fpo);
    showSmall();
  }

  protected String groupDetailString(Group g) {
    return ":" + g.getTriples(schema, 2, ", ");
  }

  //TODO move some of the style code to CSS
  private void show(List<Group> groups) {
    fpo.clear();
    for (Group g : groups) {
      FlowPanel fp = new FlowPanel();
      fp.setStylePrimaryName("statusBorder");
      SampleClass sc = g.getSamples()[0].sampleClass();
      String tip =
          SampleClassUtils.label(sc, schema) + ":\n" + g.getTriples(schema, -1, ", ");
      Label l = Utils.mkEmphLabel(g.getName());
      l.setWordWrap(false);
      l.getElement().getStyle().setMargin(2, Unit.PX);
      l.setStylePrimaryName(g.getStyleName());
      l.addStyleName("grouplabel");
      Utils.floatLeft(fp, l);
      l.setTitle(tip);
      l = new Label(groupDetailString(g));
      l.getElement().getStyle().setMargin(2, Unit.PX);
      l.setStylePrimaryName(g.getStyleName());
      UIObject.setStyleName(l.getElement(), "grouplabel", true);
      Utils.floatLeft(fp, l);
      l.setTitle(tip);
      l.setWordWrap(false);
      Utils.floatLeft(fpo, fp);
    }
  }

  private void showAll() {
    show(groups);
    if (groups.size() > 5) {
      Button b = new Button("Hide", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showSmall();
        }
      });
      Utils.floatLeft(fpo, b);
    }
    screen.resizeInterface();
  }

  private void showSmall() {
    if (groups.size() > 5) {
      List<Group> gs = groups.subList(0, 5);
      show(gs);
      Button b = new Button("Show all", new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAll();
        }
      });
      Utils.floatLeft(fpo, b);
    } else {
      show(groups);
    }
    screen.resizeInterface();
  }

}
