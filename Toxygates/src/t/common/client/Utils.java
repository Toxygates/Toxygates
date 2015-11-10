/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.common.client;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class Utils {

  public static Widget makeButtons(List<Command> commands) {
    HorizontalPanel buttons = new HorizontalPanel();
    buttons.setSpacing(4);
    for (final Command c : commands) {
      Button b = makeButton(c);
      buttons.add(b);
    }
    return buttons;
  }

  public static Button makeButton(final Command c) {
    Button b = new Button(c.getTitle());
    b.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        c.run();
      }
    });
    return b;
  }

  public static TextColumn<String[]> makeColumn(CellTable<String[]> table, final int idx,
      String title, String width) {
    TextColumn<String[]> col = new TextColumn<String[]>() {
      public String getValue(String[] x) {
        if (x.length > idx) {
          return x[idx];
        } else {
          return "";
        }
      }
    };

    SafeHtml hhtml =
        SafeHtmlUtils.fromSafeConstant("<span title=\"" + title + "\">" + title + "</span>");
    SafeHtmlHeader header = new SafeHtmlHeader(hhtml);
    table.addColumn(col, header);
    table.setColumnWidth(col, width);
    return col;
  }

  public static ScrollPanel makeScrolled(Widget w) {
    ScrollPanel sp = new ScrollPanel(w);
    sp.setWidth("auto");
    return sp;
  }
}
