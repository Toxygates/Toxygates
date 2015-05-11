package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.client.components.ImageClickCell;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import otgviewer.shared.OTGSample;
import otgviewer.shared.GroupUtils;
import otgviewer.shared.Pathology;
import t.common.shared.SampleClass;
import t.viewer.client.rpc.SparqlService;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This screen displays information about pathological findings in a given set of 
 * sample groups.
 */
public class PathologyScreen extends Screen {
	public static final String key = "path";

	private CellTable<Pathology> pathologyTable = new CellTable<Pathology>();
	private ScrollPanel sp = new ScrollPanel();
	private Set<Pathology> pathologies = new HashSet<Pathology>();
	private static Resources resources = GWT.create(Resources.class); 
	
	private SampleClass lastClass;
	private List<Group> lastColumns;
	
	@Override
	public boolean enabled() {
		//TODO check the groups for vivo samples instead
		//CellType ct = CellType.valueOf(chosenSampleClass.get("test_type"));
		return manager.isConfigured(ColumnScreen.key); // && ct == CellType.Vivo;
	}

	private SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);
	
	public PathologyScreen(ScreenManager man) {
		super("Pathologies", key, true, man);
		mkTools();
	}

	private HorizontalPanel tools = Utils.mkWidePanel();
	private void mkTools() {				
		HTML h = new HTML();
		h.setHTML("<a href=\"" +
				appInfo().pathologyTermsURL() + "\" target=_new>" +
				"Pathology terms reference</a>");
		tools.add(h);
	}
	
	@Override
	protected void addToolbars() {	
		super.addToolbars();
		addToolbar(tools, 30);
	}

	public Widget content() {
		
		sp.setWidget(pathologyTable);
		pathologyTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
		pathologyTable.setWidth("100%");
		
		TextColumn<Pathology> col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				List<Group> gs = GroupUtils.groupsFor(chosenColumns, p.barcode());
				StringBuilder sb = new StringBuilder();
				for (Group g: gs) {
					sb.append(g.getName());
					sb.append(" ");
				}
				if (gs.size() > 0) {
					return sb.toString();
				} else {
					return "None";
				}										
			}
		}; 
		pathologyTable.addColumn(col, "Group");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				OTGSample b = GroupUtils.barcodeFor(chosenColumns, p.barcode());
				return b.get("compound_name") + "/" + b.getShortTitle(schema()) + "/" + b.get("individual_id") ; 				
			}
		};
		pathologyTable.addColumn(col, "Sample");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return p.finding();
			}
		};
		
		ToolColumn tcl = new ToolColumn(new InspectCell());
		pathologyTable.addColumn(tcl, "");
		pathologyTable.setColumnWidth(tcl, "40px");
		tcl.setCellStyleNames("clickCell");
		
		pathologyTable.addColumn(col, "Finding");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return p.topography();
			}
		};
		pathologyTable.addColumn(col, "Topography");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return p.grade();
			}
		};
		pathologyTable.addColumn(col, "Grade");
		
		col = new TextColumn<Pathology>() {
			public String getValue(Pathology p) {
				return "" + p.spontaneous();
			}
		};
		pathologyTable.addColumn(col, "Spontaneous");		

		Column<Pathology, SafeHtml> lcol = new Column<Pathology, SafeHtml>(new SafeHtmlCell()) {
			public SafeHtml getValue(Pathology p) {
				SafeHtmlBuilder b = new SafeHtmlBuilder();
				b.appendHtmlConstant("<a target=_new href=\"" + p.viewerLink() +"\">Viewer</a>");
				return b.toSafeHtml();				
			}
		};
		pathologyTable.addColumn(lcol, "Digital viewer");
		
		return sp;		
	}	
	
	@Override
	public void show() {
		super.show();
		if (visible && (lastClass == null || !lastClass.equals(chosenSampleClass)
				|| lastColumns == null || !chosenColumns.equals(lastColumns))) {
			pathologies.clear();
			for (OTGColumn c : chosenColumns) {
				owlimService.pathologies(c, new AsyncCallback<Pathology[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to get pathologies.");
					}

					public void onSuccess(Pathology[] values) {
						pathologies.addAll(Arrays.asList(values));
						pathologyTable.setRowData(new ArrayList<Pathology>(
								pathologies));
					}
				});
			}
			lastClass = chosenSampleClass;
			lastColumns = chosenColumns;
		}
	}

	
	class InspectCell extends ImageClickCell.StringImageClickCell {
		InspectCell() {
			super(resources.magnify(), false);
		}
		
		public void onClick(String value) {
			displaySampleDetail(GroupUtils.barcodeFor(chosenColumns, value));
		}
	}
	
	class ToolColumn extends Column<Pathology, String> {		
		public ToolColumn(InspectCell tc) {
			super(tc);			
		}
		
		public String getValue(Pathology p) {
			return p.barcode();			
		}
	}
	
	@Override
	public String getGuideText() {
		return "This is the list of pathologies in the sample groups you have defined. Click on an icon to see detailed sample information.";
	}
}