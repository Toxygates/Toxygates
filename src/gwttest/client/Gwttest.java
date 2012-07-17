package gwttest.client;

import gwttest.shared.ExpressionRow;
import gwttest.shared.ValueType;

import java.util.List;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.Range;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Gwttest implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private KCServiceAsync kcService = (KCServiceAsync) GWT
			.create(KCService.class);

	private ListBox compoundList, organList, doseLevelList, barcodeList,
			timeList;
	private DataGrid<ExpressionRow> exprGrid;
	private ListDataProvider<ExpressionRow> listDataProvider;
	private KCAsyncProvider asyncProvider = new KCAsyncProvider();

	private ValueType chosenValueType = ValueType.Folds;
	private ListSelectionHandler<String> compoundHandler, organHandler,
			doseHandler, timeHandler, pathwayHandler;
	private MultiSelectionHandler<String> barcodeHandler;

	private TextBox pathwayBox;
	private String[] chosenProbes = new String[0];
	
	enum DataSet {
		HumanVitro, RatVitro, RatVivoKidneySingle, RatVivoKidneyRepeat, RatVivoLiverSingle, RatVivoLiverRepeat
	}

	private DataSet chosenDataSet = DataSet.HumanVitro;

	class ExpressionColumn extends Column<ExpressionRow, Number> {
		int i;
		NumberCell nc;

		public ExpressionColumn(NumberCell nc, int i) {
			super(nc);
			this.i = i;
			this.nc = nc;
		}

		public Double getValue(ExpressionRow er) {
			if (!er.getValue(i).present()) {
				return Double.NaN;
			} else {
				return er.getValue(i).value();
			}
		}
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("rootPanelContainer");
		rootPanel.setSize("100%", "");
		rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);

		VerticalPanel verticalPanel_3 = new VerticalPanel();
		verticalPanel_3.setBorderWidth(0);
		rootPanel.add(verticalPanel_3);
		verticalPanel_3.setWidth("100%");

		MenuBar menuBar = new MenuBar(false);
		verticalPanel_3.add(menuBar);
		menuBar.setWidth("100%");
		MenuBar menuBar_1 = new MenuBar(true);

		MenuItem mntmNewMenu = new MenuItem("New menu", false, menuBar_1);

		MenuItem mntmNewItem = new MenuItem("New item", false, (Command) null);
		mntmNewItem.setHTML("Human, in vitro");
		menuBar_1.addItem(mntmNewItem);

		MenuItem mntmNewItem_1 = new MenuItem("New item", false, (Command) null);
		mntmNewItem_1.setHTML("Rat, in vitro");
		menuBar_1.addItem(mntmNewItem_1);

		MenuItem mntmNewItem_2 = new MenuItem("New item", false, (Command) null);
		mntmNewItem_2.setHTML("Rat, in vivo, liver, single");
		menuBar_1.addItem(mntmNewItem_2);

		MenuItem mntmNewItem_3 = new MenuItem("New item", false, (Command) null);
		mntmNewItem_3.setHTML("Rat, in vivo, liver, repeat");
		menuBar_1.addItem(mntmNewItem_3);

		MenuItem mntmNewItem_4 = new MenuItem("New item", false, new Command() {
			public void execute() {
			}
		});
		mntmNewItem_4.setHTML("Rat, in vivo, kidney, single");
		menuBar_1.addItem(mntmNewItem_4);

		MenuItem mntmNewItem_5 = new MenuItem("New item", false, (Command) null);
		mntmNewItem_5.setHTML("Rat, in vivo, kidney, repeat");
		menuBar_1.addItem(mntmNewItem_5);

		MenuItemSeparator separator = new MenuItemSeparator();
		menuBar_1.addSeparator(separator);

		MenuItem mntmFolds = new MenuItem("Fold values", false, new Command() {
			public void execute() {
				chosenValueType = ValueType.Folds;
				getCompounds();
			}
		});
		menuBar_1.addItem(mntmFolds);

		MenuItem mntmAbsoluteValues = new MenuItem("Absolute values", false,
				new Command() {
					public void execute() {
						chosenValueType = ValueType.Absolute;
						getCompounds();
					}
				});

		menuBar_1.addItem(mntmAbsoluteValues);
		mntmNewMenu.setHTML("Data set");
		menuBar.addItem(mntmNewMenu);

		MenuItem mntmSettings = new MenuItem("Settings", false, (Command) null);
		menuBar.addItem(mntmSettings);
		SimplePager.Resources pagerResources = GWT
				.create(SimplePager.Resources.class);
		
		HorizontalSplitPanel horizontalSplitPanel = new HorizontalSplitPanel();
		horizontalSplitPanel.setSplitPosition("200px");
		verticalPanel_3.add(horizontalSplitPanel);
		horizontalSplitPanel.setSize("100%", "700px");
		
				VerticalPanel verticalPanel_2 = new VerticalPanel();
				horizontalSplitPanel.setLeftWidget(verticalPanel_2);
				verticalPanel_2.setBorderWidth(1);
				verticalPanel_2.setSize("100%", "");
				
						Label lblPathwaySearch = new Label("Pathway search");
						verticalPanel_2.add(lblPathwaySearch);
						
								pathwayBox = new TextBox();
								verticalPanel_2.add(pathwayBox);
								pathwayBox.setWidth("165px");
								pathwayBox.addKeyPressHandler(new KeyPressHandler() {
									public void onKeyPress(KeyPressEvent event) {
										if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
											getPathways(pathwayBox.getText());
										}
									}
								});
								
								ListBox pathwayList = new ListBox();
								verticalPanel_2.add(pathwayList);
								pathwayList.setSize("100%", "500px");
								pathwayList.setVisibleItemCount(5);
								
								pathwayHandler = new ListSelectionHandler<String>("pathways",
										pathwayList, false) {
									protected void getUpdates(String pathway) {
										owlimService.probes(pathway, new AsyncCallback<String[]>() {
											public void onFailure(Throwable caught) {
												Window.alert("Unable to get probes.");
											}
											public void onSuccess(String[] probes) {
												chosenProbes = probes;
												getExpressions();
											}
										});
									}
								};
								
		VerticalPanel verticalPanel = new VerticalPanel();
		horizontalSplitPanel.setRightWidget(verticalPanel);
		verticalPanel.setBorderWidth(0);
		verticalPanel.setSize("100%", "100%");
		
				HorizontalPanel horizontalPanel = new HorizontalPanel();
				verticalPanel.add(horizontalPanel);
				
						compoundList = new ListBox();
						horizontalPanel.add(compoundList);
						compoundList.setSize("210px", "202px");
						compoundList.setVisibleItemCount(10);
						
								compoundHandler = new ListSelectionHandler<String>("compounds",
										compoundList, false) {
									protected void getUpdates(String compound) {
										getOrgans(compound);
									}
								};
								
										organList = new ListBox();
										horizontalPanel.add(organList);
										organList.setSize("11em", "202px");
										organList.setVisibleItemCount(10);
										
												organHandler = new ListSelectionHandler<String>("organs", organList,
														false) {
													protected void getUpdates(String organ) {
														getDoseLevels(compoundHandler.lastSelected(), organ);
														getTimes(compoundHandler.lastSelected(), organ);
													}
												};
												
														VerticalPanel verticalPanel_1 = new VerticalPanel();
														horizontalPanel.add(verticalPanel_1);
														
																doseLevelList = new ListBox();
																verticalPanel_1.add(doseLevelList);
																doseLevelList.setSize("10em", "100px");
																doseLevelList.setVisibleItemCount(10);
																
																		doseHandler = new ListSelectionHandler<String>("dose levels",
																				doseLevelList, true) {
																			protected void getUpdates(String dose) {
																				getBarcodes(compoundHandler.lastSelected(),
																						organHandler.lastSelected(),
																						doseHandler.lastSelected(), timeHandler.lastSelected());
																
																			}
																		};
																		
																				timeList = new ListBox();
																				verticalPanel_1.add(timeList);
																				timeList.setSize("10em", "100px");
																				timeList.setVisibleItemCount(5);
																				
																						timeHandler = new ListSelectionHandler<String>("times", timeList, true) {
																							protected void getUpdates(String time) {
																								getBarcodes(compoundHandler.lastSelected(),
																										organHandler.lastSelected(),
																										doseHandler.lastSelected(), timeHandler.lastSelected());
																							}
																						};
																						
																								barcodeList = new ListBox();
																								horizontalPanel.add(barcodeList);
																								barcodeList.setMultipleSelect(true);
																								barcodeList.setVisibleItemCount(10);
																								barcodeList.setSize("15em", "202px");
																								
																										barcodeHandler = new MultiSelectionHandler<String>("barcodes",
																												barcodeList) {
																											protected void getUpdates(String barcode) {
																								
																											}
																								
																											protected void getUpdates(List<String> barcodes) {
																												getExpressions();
																											}
																										};
																										
																												compoundList.addChangeHandler(new ChangeHandler() {
																													public void onChange(ChangeEvent event) {
																														String compound = compoundHandler.lastSelected();
																														getOrgans(compound);
																														getDoseLevels(compound, null);
																													}
																												});
																												
																														SimplePager exprPager = new SimplePager(TextLocation.CENTER,
																																pagerResources, true, 100, true);
																														verticalPanel.add(exprPager);
																														
																																exprGrid = new DataGrid<ExpressionRow>();
																																exprGrid.setStyleName("exprGrid");
																																exprGrid.setSize("100%", "500px");
																																exprGrid.setPageSize(20);
																																exprGrid.setEmptyTableWidget(new HTML("No Data to Display Yet"));
																																
																																		verticalPanel.add(exprGrid);
																																		asyncProvider.addDataDisplay(exprGrid);
																																		exprPager.setDisplay(exprGrid);

		listDataProvider = new ListDataProvider<ExpressionRow>();
		compoundHandler.addAfter(organHandler);
		organHandler.addAfter(doseHandler);
		organHandler.addAfter(timeHandler);
		doseHandler.addAfter(barcodeHandler);
		timeHandler.addAfter(barcodeHandler);

		getCompounds();
	}

	private void setupColumns() {
		// todo: explicitly set the width of each column
		NumberCell nc = new NumberCell();

		int count = exprGrid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			exprGrid.removeColumn(0);
		}

		TextColumn<ExpressionRow> probeCol = new TextColumn<ExpressionRow>() {
			public String getValue(ExpressionRow er) {
				return er.getProbe();
			}
		};
		exprGrid.addColumn(probeCol, "Probe");

		TextColumn<ExpressionRow> titleCol = new TextColumn<ExpressionRow>() {
			public String getValue(ExpressionRow er) {
				return er.getTitle();
			}
		};
		exprGrid.addColumn(titleCol, "Title");

		int i = 0;
		List<String> selection = barcodeHandler.lastMultiSelection();
		for (String bc : selection) {
			Column<ExpressionRow, Number> valueCol = new ExpressionColumn(nc, i);
			exprGrid.addColumn(valueCol, bc);
			i += 1;
		}
	}

	void getCompounds() {
		compoundList.clear();
		owlimService.compounds(compoundHandler.retrieveCallback());
	}

	void getOrgans(String compound) {
		organHandler.clear();
		owlimService.organs(compound, organHandler.retrieveCallback());
	}

	void getDoseLevels(String compound, String organ) {
		doseLevelList.clear();
		owlimService.doseLevels(null, null, doseHandler.retrieveCallback());
	}

	void getBarcodes(String compound, String organ, String doseLevel,
			String time) {
		barcodeList.clear();
		owlimService.barcodes(compound, organ, doseLevel, time,
				barcodeHandler.retrieveCallback());
	}

	void getTimes(String compound, String organ) {
		timeList.clear();
		owlimService.times(compound, organ, timeHandler.retrieveCallback());
	}

	void getExpressions() {
		setupColumns();

		kcService.loadDataset(barcodeHandler.lastMultiSelection(), chosenProbes,
				chosenValueType, new AsyncCallback<Integer>() {
					public void onFailure(Throwable caught) {
						Window.alert("Unable to load dataset.");
					}

					public void onSuccess(Integer result) {
						exprGrid.setRowCount(result);
						exprGrid.setVisibleRangeAndClearData(new Range(0, 20),
								true);
					}
				});
	}

	void getPathways(String pattern) {
		owlimService.pathways(pattern, pathwayHandler.retrieveCallback());
	}
	
	class KCAsyncProvider extends AsyncDataProvider<ExpressionRow> {
		private int start = 0;

		AsyncCallback<List<ExpressionRow>> rowCallback = new AsyncCallback<List<ExpressionRow>>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get expression values.");
			}

			public void onSuccess(List<ExpressionRow> result) {
				exprGrid.setRowData(start, result);
			}
		};

		protected void onRangeChanged(HasData<ExpressionRow> display) {
			Range range = display.getVisibleRange();
			start = range.getStart();
			kcService.datasetItems(range.getStart(), range.getLength(),
					rowCallback);
		}

	}
}
