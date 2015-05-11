package otgviewer.client.dialog;

import javax.annotation.Nullable;

import otgviewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A dialog for displaying and modifying a column filter.
 * @author johan
 */
public class FilterEditor extends Composite {

	private TextBox input = new TextBox();
	protected int editColumn;
	
	public FilterEditor(String columnTitle, int column, boolean isUpper, @Nullable Double initValue) {
		this.editColumn = column;
		VerticalPanel vp = Utils.mkVerticalPanel(true);
		initWidget(vp);
		vp.setWidth("300px");
		
		Label l = new Label("Please choose a bound for '" + columnTitle + "'. Examples: " +
				formatNumber(2.1) + ", " + formatNumber(1.2e-3));
		l.setWordWrap(true);
		vp.add(l);
		
		if (initValue != null) {
			input.setValue(formatNumber(initValue));
		}
		
		Label l1 = new Label(isUpper ? "x <=" : "|x| >=");
		HorizontalPanel hp = Utils.mkHorizontalPanel(true, l1, input);
		vp.add(hp);
		
		final Button setButton = new Button("OK");
		setButton.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				try {
					Double newVal = parseNumber(input.getText());
					onChange(newVal);
				} catch (NumberFormatException e) {
					Window.alert("Invalid number format.");
				}
			}
		});
		
		input.addValueChangeHandler(new ValueChangeHandler<String>() {			
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				setButton.click();
				
			}
		});
		
		Button clearButton = new Button("Clear filter");
		clearButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onChange(null);		
			}
			
		});
		hp = Utils.mkHorizontalPanel(true, setButton, clearButton);
		vp.add(hp);
	}

	private NumberFormat dfmt = NumberFormat.getDecimalFormat();
	private NumberFormat sfmt = NumberFormat.getScientificFormat();
	
	String formatNumber(Double val) {
		if (val < 0.01 || val > 100000) {
			return sfmt.format(val);
		} else {
			return dfmt.format(val);
		}
	}
	
	Double parseNumber(String val) throws NumberFormatException {
		try {
			return dfmt.parse(val);
		} catch (NumberFormatException e) {
			return sfmt.parse(val);
		}
	}
	
	/**
	 * Called when the filter is changed. To be overridden by subclasses.
	 */
	
	protected void onChange(@Nullable Double newFilter) { }
}