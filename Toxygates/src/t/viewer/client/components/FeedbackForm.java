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

package t.viewer.client.components;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import t.viewer.client.Utils;
import t.viewer.client.dialog.InteractionDialog;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.screen.Screen;

public class FeedbackForm extends InteractionDialog {

  TextArea commentArea;
  InputGrid ig;
  private final MatrixServiceAsync matrixService;
  private final String emailAddresses;

  public FeedbackForm(final Screen parent, String emailAddresses) {
    super(parent);
    this.emailAddresses = emailAddresses;
    matrixService = parent.manager().matrixService();
  }

  @Override
  protected Widget content() {
    VerticalPanel vpanel = Utils.mkVerticalPanel();

    Label l =
        new Label("We welcome comments, bug reports, questions or any kind of inquiries. "
            + "If you report a problem, we will try to respond to you as soon as possible. "
            + "Alternatively, you may e-mail us directly (" + emailAddresses + ").");
    l.setWordWrap(true);
    vpanel.add(l);
    l.setWidth("40em");

    ig = new InputGrid("Your name:", "Your e-mail address (optional):");
    vpanel.add(ig);

    l = new Label("Comments:");
    vpanel.add(l);
    commentArea = new TextArea();
    commentArea.setCharacterWidth(60);
    commentArea.setVisibleLines(10);
    vpanel.add(commentArea);

    Button b1 = new Button("Send", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String name = ig.getValue(0);
        String email = ig.getValue(1);
        String fb = commentArea.getText();
        matrixService.sendFeedback(name, email, fb, new PendingAsyncCallback<Void>(parent.manager(),
            "There was a problem sending your feedback.") {
          @Override
          public void handleSuccess(Void t) {
            Window.alert("Your feedback was sent.");
            userProceed();
          }
        });
      }
    });
    Button b2 = new Button("Cancel", cancelHandler());
    HorizontalPanel hp = Utils.mkHorizontalPanel(true, b1, b2);
    vpanel.add(hp);
    return vpanel;
  }
}
