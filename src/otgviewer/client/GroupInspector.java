package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import otgviewer.client.components.SelectionTable;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;

/**
 * This widget is intended to help visually define and modify "groups"
 * of microarrays.
 * 
 * Receives: dataFilter, compounds
 * Emits: columns
 * @author johan
 *
 */
public class GroupInspector extends DataListenerWidget implements SelectionTDGrid.BarcodeListener {


	private SelectionTDGrid timeDoseGrid;
	private Map<String, Group> groups = new HashMap<String, Group>();		
	private Screen screen;
	private Label titleLabel;
	private TextBox txtbxGroup;
	private Button saveButton, deleteButton;
	SelectionTable<Group> existingGroupsTable;
	private CompoundSelector compoundSel;

	public GroupInspector(CompoundSelector cs, Screen scr) {
		compoundSel = cs;
		this.screen = scr;
		VerticalPanel vp = new VerticalPanel();
		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(vp);
		
		titleLabel = new Label("Sample group definition");
		titleLabel.setStyleName("heading");
		vp.add(titleLabel);
		
		timeDoseGrid = new SelectionTDGrid();
		vp.add(timeDoseGrid);
		addListener(timeDoseGrid);
	
		vp.setWidth("410px");		
		
		HorizontalPanel horizontalPanel = Utils.mkHorizontalPanel(true);
		vp.add(horizontalPanel);
		
		horizontalPanel.add(new Button("New",
				new ClickHandler() {
					public void onClick(ClickEvent event) {
						newGroup();
					}
			
		}));
		
		Label lblSaveGroupAs = new Label("Save group as");
		lblSaveGroupAs.setStyleName("slightlySpaced");
		horizontalPanel.add(lblSaveGroupAs);
		
		txtbxGroup = new TextBox();
		txtbxGroup.setText(nextGroupName());
		horizontalPanel.add(txtbxGroup);
		txtbxGroup.addKeyUpHandler(new KeyUpHandler() {			
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (groups.containsKey(txtbxGroup.getValue())) {
					setDeleteable(true);
				} else {
					setDeleteable(false);
				}		
			}
		});
		
		saveButton = new Button("Save",
		new ClickHandler(){
			public void onClick(ClickEvent ce) {
				makeGroup(txtbxGroup.getValue());
			}
		});
		horizontalPanel.add(saveButton);
		setEditing(false);
		
		deleteButton = new Button("Delete", new ClickHandler() {
			public void onClick(ClickEvent ce) {
				String grp = txtbxGroup.getValue();
				if (groups.containsKey(grp)) {
					deleteGroup(grp, true);					
				}
			}
		});
		horizontalPanel.add(deleteButton);
		setDeleteable(false);
		
		existingGroupsTable = new SelectionTable<Group>("Active") {
			protected void initTable(CellTable<Group> table) {
				TextColumn<Group> textColumn = new TextColumn<Group>() {
					@Override
					public String getValue(Group object) {
						return object.getName();
					}
				};
				table.addColumn(textColumn, "Group");
				
				textColumn = new TextColumn<Group>() {
					@Override
					public String getValue(Group object) {
						return "" + object.getBarcodes().length;
					}
				};
				table.addColumn(textColumn, "Samples");
			}
			
			protected void selectionChanged(Set<Group> selected) {
				chosenColumns = new ArrayList<DataColumn>(selected);
				storeColumns();
			}
		};
		vp.add(existingGroupsTable);
		existingGroupsTable.setSize("100%", "100px");
		
		
		existingGroupsTable.table().getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Group g = existingGroupsTable.highlightedRow();
				if (g != null) {
					displayGroup(g.getName());
				}
			}
		});		
	}
	
	private void deleteGroup(String name, boolean createNew) {
		groups.remove(name);									
		reflectGroupChanges(); //stores columns
		if (createNew) {
			newGroup();
		}
	}
	
	/**
	 * Toggle edit mode
	 * @param editing
	 */
	private void setEditing(boolean editing) {
		boolean val = editing && (chosenCompounds.size() > 0); 
		saveButton.setEnabled(val);
		txtbxGroup.setEnabled(val);
	}
	
	/**
	 * Toggle deleteable mode
	 * @param deleteable
	 */
	private void setDeleteable(boolean deleteable) {
		deleteButton.setEnabled(deleteable);
	}
	
	private void setHeading(String title) {
		titleLabel.setText("Sample group definition - " + title);
	}

	private void newGroup() {
		txtbxGroup.setText(nextGroupName());
		timeDoseGrid.setAll(false);
		compoundSel.setSelection(new ArrayList<String>());
		setHeading("new group");
		setEditing(true);
		setDeleteable(false);
	}
	
	private List<Group> sortedGroupList(Collection<Group> groups) {
		ArrayList<Group> r = new ArrayList<Group>(groups);
		Collections.sort(r);
		return r;
	}
	
	private void reflectGroupChanges() {
		existingGroupsTable.reloadWith(sortedGroupList(groups.values()), false);
		chosenColumns = new ArrayList<DataColumn>(existingGroupsTable.selection());
		storeColumns();
		txtbxGroup.setText(nextGroupName());
		updateConfigureStatus();
	}
	
	private void updateConfigureStatus() {		
		if (chosenColumns.size() == 0) {
			screen.setConfigured(false);
			screen.manager().deconfigureAll(screen);
		} else if (chosenColumns.size() > 0) {
			screen.setConfigured(true);
			screen.manager().deconfigureAll(screen);
		}
	}
	
	private String nextGroupName() {
		int i = 1;
		String name = "Group " + i;
		while (groups.containsKey(name)) {
			i += 1;
			name = "Group " + i;
		}
		return name;
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {		
		if (!filter.equals(chosenDataFilter)) {			
			super.dataFilterChanged(filter); //this call changes chosenDataFilter						
			groups.clear();
			existingGroupsTable.reloadWith(new ArrayList<Group>(), true);						
			compoundsChanged(new ArrayList<String>());
		} else {
			super.dataFilterChanged(filter);
		}
	}
	
	@Override 
	public void columnsChanged(List<DataColumn> columns) {
		super.columnsChanged(columns);
		groups.clear();
			
		for (DataColumn c: columns) {
			Group g = (Group) c;
			groups.put(g.getName(), g);			
		}
		updateConfigureStatus();
				
		existingGroupsTable.reloadWith(sortedGroupList(groups.values()), true);
		existingGroupsTable.setSelection(asGroupList(chosenColumns));
		newGroup();
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
		super.compoundsChanged(compounds);
		if (compounds.size() == 0) {
			setEditing(false);
		} else {
			setEditing(true);
		}
	}
	
	public void inactiveColumnsChanged(List<DataColumn> columns) {
		Collection<Group> igs = sortedGroupList(asGroupList(columns));
		for (Group g : igs) {
			groups.put(g.getName(), g);
		}
		
		List<Group> all = new ArrayList<Group>();
		all.addAll(sortedGroupList(existingGroupsTable.selection()));
		all.addAll(igs);
		existingGroupsTable.reloadWith(all, false);		
		existingGroupsTable.unselectAll(igs);
		existingGroupsTable.table().redraw();
		newGroup();
	}
	
	private List<Group> asGroupList(Collection<DataColumn> dcs) {
		List<Group> r = new ArrayList<Group>();
		for (DataColumn dc : dcs) {
			r.add((Group) dc);
		}
		return r;
	}
	
	@Override 
	public void storeColumns() {
		super.storeColumns();			
		storeColumns("inactiveColumns", 
				new ArrayList<DataColumn>(existingGroupsTable.inverseSelection()));
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
	private Group pendingGroup;
	
	/**
	 * Get here if save button is clicked
	 * @param name
	 */
	private void makeGroup(final String name) {		
		pendingGroup = new Group(name, new Barcode[0]);		
		groups.put(name, pendingGroup);
		existingGroupsTable.addItem(pendingGroup);
		existingGroupsTable.setSelected(pendingGroup);

		timeDoseGrid.getSelection(this);		
	}
	
	/**
	 * callback from selectionTDgrid
	 */
	public void barcodesObtained(List<Barcode> barcodes) {
		if (barcodes.size() == 0) {
			 Window.alert("No samples found.");
			 cullEmptyGroups();
		} else {
			setGroup(pendingGroup.getName(), barcodes);
			newGroup();
		}
	}
	
	private void cullEmptyGroups() {
		// look for empty groups, undo the saving
		// this is needed if we found no barcodes or if the user didn't select
		// any combination
		for (String name : groups.keySet()) {
			Group g = groups.get(name);
			if (g.getBarcodes().length == 0) {
				deleteGroup(name, false);
			}
		}
	}
		
	private void setGroup(String pendingGroupName, List<Barcode> barcodes) {
		Group pendingGroup = groups.get(pendingGroupName);
		pendingGroup = new Group(pendingGroupName, barcodes.toArray(new Barcode[0]));

		existingGroupsTable.removeItem(groups.get(pendingGroupName));
		groups.put(pendingGroupName, pendingGroup);
		existingGroupsTable.addItem(pendingGroup);
		existingGroupsTable.setSelected(pendingGroup);
		reflectGroupChanges();
	}
	
	private void displayGroup(String name) {
		setHeading("editing " + name);
		List<String> compounds = new ArrayList<String>(Arrays.asList(groups.get(name).getCompounds()));
		
		compoundSel.setSelection(compounds);		
		txtbxGroup.setValue(name);
		
		Group g = groups.get(name);
		timeDoseGrid.setSelection(g.getBarcodes());
		
		setDeleteable(true);
		setEditing(true);
	}
	
}
