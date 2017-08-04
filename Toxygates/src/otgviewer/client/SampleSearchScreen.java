package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import otgviewer.client.components.FilterTools;
import otgviewer.client.components.PendingAsyncCallback;
import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.components.TickMenuItem;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.Group;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.viewer.client.Utils;
import t.viewer.client.components.search.ConditionEditor;
import t.viewer.client.components.search.ResultTable;
import t.viewer.client.components.search.SampleSearch;
import t.viewer.client.components.search.SampleTable;
import t.viewer.client.components.search.Search;
import t.viewer.client.components.search.UnitSearch;
import t.viewer.client.components.search.UnitTable;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

public class SampleSearchScreen extends Screen implements Search.Delegate, ResultTable.Delegate {
  public static final String key = "search";

  private AppInfo appInfo;
  private SampleServiceAsync sampleService;

  private FilterTools filterTools;

  private Widget tools;
  private ConditionEditor conditionEditor;
  private MenuItem saveCVSMenuItem;
  private Button saveGroupButton;
  private Label resultCountLabel;

  private DialogBox waitDialog;

  private ResultTable<Sample> sampleTableHelper = new SampleTable(this);
  private ResultTable<Unit> unitTableHelper = new UnitTable(this);

  private SampleSearch sampleSearch;
  private UnitSearch unitSearch;
  private Search<?, ?> currentSearch = null;

  private Collection<BioParamValue> searchParameters;
  private Collection<BioParamValue> nonSearchParameters;

  private Map<String, String> humanReadableParamNames;

  private Collection<ParameterTickItem> parameterMenuItems;

  private void getParameterInfo() {
    BioParamValue[] allParams = appInfo.bioParameters();
    List<BioParamValue> searchParams = new ArrayList<BioParamValue>();
    List<BioParamValue> nonSearchParams = new ArrayList<BioParamValue>();
    humanReadableParamNames = new HashMap<String, String>();
    for (BioParamValue bp : allParams) {
      if (bp instanceof NumericalBioParamValue) {
        searchParams.add(bp);
      } else {
        nonSearchParams.add(bp);
      }
      humanReadableParamNames.put(bp.id(), bp.label());
    }
    java.util.Collections.sort(searchParams);
    java.util.Collections.sort(nonSearchParams);

    searchParameters = searchParams;
    nonSearchParameters = nonSearchParams;
  }

  public SampleSearchScreen(ScreenManager man) {
    super("Sample search", key, true, man);
    appInfo = man.appInfo();
    filterTools = new FilterTools(this);
    this.addListener(filterTools);

    sampleService = man.sampleService();

    sampleSearch = new SampleSearch(this, sampleTableHelper, sampleService);
    unitSearch = new UnitSearch(this, unitTableHelper, sampleService);

    getParameterInfo();

    makeTools();
  }

  private void makeTools() {
    conditionEditor = new ConditionEditor(searchParameters);

    Button sampleSearchButton = new Button("Sample Search");
    sampleSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        sampleSearch.attemptSearch(chosenSampleClass, conditionEditor.getCondition());
      }
    });

    Button unitSearchButton = new Button("Unit Search");
    unitSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        unitSearch.attemptSearch(chosenSampleClass, conditionEditor.getCondition());
      }
    });

    resultCountLabel = new Label();

    saveGroupButton = new Button("Save sample group", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        try {
          Unit[] units =
              unitSearch.sampleGroupFromUnits(unitTableHelper.selectionTable().getSelection());
          String name = findAvailableGroupName("Sample search group ");
          Group pendingGroup = new Group(schema(), name, units);

          chosenColumns.add(pendingGroup);
          columnsChanged(chosenColumns);
          storeColumns(manager().getParser());

          Window.alert("Saved group: " + name);
        } catch (Exception e) {
          Window.alert("Saving group failed: " + e);
        }
      }
    });
    saveGroupButton.setVisible(false);

    tools = Utils.mkVerticalPanel(true, conditionEditor, Utils.mkHorizontalPanel(true,
        unitSearchButton, sampleSearchButton, resultCountLabel, saveGroupButton));
  }

  private String findAvailableGroupName(String prefix) throws Exception {
    List<Group> inactiveGroups =
        loadColumns(manager().getParser(), schema(), "inactiveColumns", new ArrayList<Group>());

    Set<String> groupNames = new HashSet<String>();
    for (Group group : chosenColumns) {
      groupNames.add(group.getName());
    }
    if (inactiveGroups != null) {
      for (Group group : inactiveGroups) {
        groupNames.add(group.getName());
      }
    }

    int n = 1;
    while (groupNames.contains(prefix + n)) {
      n++;
    }

    return prefix + n;
  }

  @Override
  public Widget content() {
    setupMenuItems();

    ScrollPanel searchPanel = new ScrollPanel();
    unitTableHelper.selectionTable().setVisible(false);
    sampleTableHelper.selectionTable().setVisible(false);
    VerticalPanel vp =
        Utils.mkVerticalPanel(true, tools, unitTableHelper.selectionTable(),
            sampleTableHelper.selectionTable());
    searchPanel.add(vp);
    return searchPanel;
  }

  private class ParameterTickItem extends TickMenuItem {
    private String parameterId;
    private boolean isNumeric;

    public ParameterTickItem(MenuBar mb, String title, String id, boolean numeric,
        boolean initState, boolean enabled) {
      super(mb, title, initState);
      parameterId = id;
      isNumeric = numeric;
      setEnabled(enabled);
    }

    @Override
    public void setState(boolean state) {
      super.setState(state);
    }

    @Override
    public void stateChange(boolean newState) {
      if (newState) {
        boolean waitForData = false;
        if (!currentSearch.hasParameter(parameterId)) {
          currentSearch.fetchParameter(parameterId);
          waitForData = true;
        }
        currentSearch.helper().addExtraColumn(parameterId, isNumeric, waitForData);
      } else {
        currentSearch.helper().removeKeyColumn(parameterId);
      }
    }
  }

  private void setupMenuItems() {
    parameterMenuItems = new ArrayList<ParameterTickItem>();

    MenuBar fileBar = new MenuBar(true);
    MenuItem fileItem = new MenuItem("File", false, fileBar);

    saveCVSMenuItem = new MenuItem("Save results to CSV", false, new Command() {
      @Override
      public void execute() {
        SampleSearchScreen.this.sampleService.prepareUnitCSVDownload(unitSearch.searchResult(),
            unitTableHelper.allKeys(), new PendingAsyncCallback<String>(SampleSearchScreen.this,
                "Unable to prepare the data for download,") {
              @Override
              public void handleSuccess(String url) {
                Utils.displayURL("Your download is ready.", "Download", url);
              }
            });
      }
    });
    saveCVSMenuItem.setEnabled(false);
    fileBar.addItem(saveCVSMenuItem);

    addMenu(fileItem);

    MenuBar parametersBar = new MenuBar(true);
    MenuItem parameterItem = new MenuItem("View", false, parametersBar);

    MenuBar numericalParametersBar = new MenuBar(true);
    MenuItem numericalParametersItem =
        new MenuItem("Numerical parameters", false, numericalParametersBar);
    for (BioParamValue parameter : searchParameters) {
      parameterMenuItems.add(new ParameterTickItem(numericalParametersBar,
          parameter.label(), parameter.id(), true, false, false));
    }

    MenuBar stringParametersBar = new MenuBar(true);
    MenuItem stringParametersItem =
        new MenuItem("Non-numerical parameters", false, stringParametersBar);
    for (BioParamValue parameter : nonSearchParameters) {
      parameterMenuItems.add(new ParameterTickItem(stringParametersBar,
          parameter.label(), parameter.id(), false, false, false));
    }

    parametersBar.addItem(numericalParametersItem);
    parametersBar.addItem(stringParametersItem);

    addMenu(parameterItem);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    addToolbar(filterTools, 30);
  }

  @Override
  public String getGuideText() {
    return "Here you can search for samples and units based on values of biological parameters.";
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public void changeSampleClass(SampleClass sc) {
    super.changeSampleClass(sc);
    storeSampleClass(getParser());
  }

  protected void hideTables() {
    sampleTableHelper.clear();
    sampleTableHelper.selectionTable().setVisible(false);
    unitTableHelper.clear();
    unitTableHelper.selectionTable().setVisible(false);
  }

  /*
   * Search.Delegate methods
   */
  @Override
  public void searchStarted(Search<?, ?> search) {
    if (waitDialog == null) {
      waitDialog = Utils.waitDialog();
    } else {
      waitDialog.show();
    }
  }

  @Override
  public void searchEnded(Search<?, ?> search, int numResults) {
    waitDialog.hide();
    hideTables();
    resultCountLabel.setText("Found " + numResults + " results");
    currentSearch = search;
    saveCVSMenuItem.setEnabled(currentSearch == unitSearch);
    saveGroupButton.setVisible((currentSearch == unitSearch));
  }

  /*
   * ResultTable.Delegate methods
   */
  @Override
  public ImageResource inspectCellImage() {
    return manager.resources().magnify();
  }

  @Override
  public String humanReadableTitleForColumn(String id) {
    String title = humanReadableParamNames.get(id);
    if (title == null) {
      return id;
    } else {
      return humanReadableParamNames.get(id);
    }
  }

  @Override
  public void finishedSettingUpTable() {
    currentSearch.helper().selectionTable().setVisible(true);
    currentSearch.helper().selectionTable().clearSelection();

    HashSet<String> requiredParameterIds =
        new HashSet<String>(Arrays.asList(currentSearch.helper().requiredKeys()));
    HashSet<String> nonRequiredParameterIds =
        new HashSet<String>(Arrays.asList(currentSearch.helper().nonRequiredKeys()));

    for (ParameterTickItem item : parameterMenuItems) {
      if (requiredParameterIds.contains(item.parameterId)) {
        item.setState(true);
        item.setEnabled(false);
      } else if (nonRequiredParameterIds.contains(item.parameterId)) {
        item.setState(true);
        item.setEnabled(true);
      } else {
        item.setState(false);
        item.setEnabled(true);
      }
    }
  }

  @Override
  public void displayDetailsForEntry(Unit unit) {
    SampleDetailTable table = new SampleDetailTable(this, "Unit details", false);
    
    List<Unit> singleUnitList = new ArrayList<Unit>();
    singleUnitList.add(unit);
    Unit[] unitsWithControl = unitSearch.sampleGroupFromUnits(singleUnitList);
    Group g = new Group(schema(), "data", unitsWithControl);

    table.loadFrom(g, false);
    Utils.displayInPopup("Unit details", table, DialogPosition.Center);
  }
}
