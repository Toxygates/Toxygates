package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import otgviewer.client.Utils;
import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.SharedUtils;
import otgviewer.shared.ValueType;
import bioweb.shared.array.DataColumn;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * A Composite that is also a DataViewListener.
 * Has default implementations for the change listener methods.
 * 
 * @author johan
 *
 */
public class DataListenerWidget extends Composite implements DataViewListener {

	private List<DataViewListener> listeners = new ArrayList<DataViewListener>();
	
	public DataFilter chosenDataFilter; //TODO
	protected String[] chosenProbes = new String[0];
	protected List<String> chosenCompounds = new ArrayList<String>();
	protected String chosenCompound;
	protected ValueType chosenValueType;
	protected List<Group> chosenColumns = new ArrayList<Group>();
	protected BarcodeColumn chosenCustomColumn;
	
	public List<Group> chosenColumns() { return this.chosenColumns; }
	
	public DataListenerWidget() {
		super();
	}
	
	public void addListener(DataViewListener l) {
		listeners.add(l);
	}
	
	//incoming signals
	public void dataFilterChanged(DataFilter filter) {		
		chosenDataFilter = filter;
		changeDataFilter(filter);
	}

	public void probesChanged(String[] probes) {
		chosenProbes = probes;
		changeProbes(probes);
	}
	
	public void availableCompoundsChanged(List<String> compounds) {
		changeAvailableCompounds(compounds);
	}
	
	public void compoundsChanged(List<String> compounds) {
		chosenCompounds = compounds;
		changeCompounds(compounds);
	}
	
	public void compoundChanged(String compound) {
		chosenCompound = compound;
		changeCompound(compound);
	}
	
	public void valueTypeChanged(ValueType type) {
		chosenValueType = type;
		changeValueType(type);
	}
	
	public void columnsChanged(List<Group> columns) {
		chosenColumns = columns;		
		changeColumns(columns);
	}
	
	public void customColumnChanged(BarcodeColumn customColumn) {
		this.chosenCustomColumn = customColumn;
		changeCustomColumn(customColumn);
	}
//	
//	public void heightChanged(int newHeight) {
//		changeHeight(newHeight);
//	}
	
	//outgoing signals	
	protected void changeDataFilter(DataFilter filter) {
		chosenDataFilter = filter;
		for (DataViewListener l : listeners) {
			l.dataFilterChanged(filter);
		}		
	}
	
	protected void changeProbes(String[] probes) {
		chosenProbes = probes;
		for (DataViewListener l: listeners) {
			l.probesChanged(probes);
		}
	}
	
	/**
	 * Change the available compounds
	 * @param compounds
	 */
	protected void changeAvailableCompounds(List<String> compounds) {
		for (DataViewListener l: listeners) {
			l.availableCompoundsChanged(compounds);
		}
	}
	
	/** 
	 * Change the selected compounds
	 * @param compounds
	 */
	protected void changeCompounds(List<String> compounds) {
		chosenCompounds = compounds;
		assert(compounds != null);
		for (DataViewListener l: listeners) {
			l.compoundsChanged(compounds);
		}
	}
	
	protected void changeCompound(String compound) {
		chosenCompound = compound;
		for (DataViewListener l : listeners) {
			l.compoundChanged(compound);
		}
	}
	
	protected void changeValueType(ValueType type) {
		chosenValueType = type;
		for (DataViewListener l : listeners) {
			l.valueTypeChanged(type);
		}
	}
	
	protected void changeColumns(List<Group> columns) {
		chosenColumns = columns;
		assert(columns != null);
		for (DataViewListener l : listeners) {
			l.columnsChanged(columns);
		}
	}
	
	protected void changeCustomColumn(BarcodeColumn customColumn) {
		this.chosenCustomColumn = customColumn;
		for (DataViewListener l: listeners) {
			l.customColumnChanged(customColumn);
		}
	}
	
	public void propagateTo(DataViewListener other) {
		other.dataFilterChanged(chosenDataFilter);
		other.probesChanged(chosenProbes);
		other.compoundsChanged(chosenCompounds);
		other.compoundChanged(chosenCompound);
		other.valueTypeChanged(chosenValueType);
		other.columnsChanged(chosenColumns);		
		other.customColumnChanged(chosenCustomColumn);
	}

	protected Storage tryGetStorage() {
		Storage r = Storage.getLocalStorageIfSupported();
		if (r == null) {
			Window.alert("Local storage must be supported in the web browser. The application cannot continue.");
		} 
		return r;
	}
	
	/**
	 * Store this widget's state into local storage.
	 */
	public void storeState() {
		Storage r = tryGetStorage();
		if (r != null) {
			storeState(r);
		}
	}
	
	public void storeState(Storage s) {
		storeDataFilter(s);
		storeColumns(s);
		storeProbes(s);
	}
		
	public void storeDataFilter(Storage s) {		
		if (chosenDataFilter != null) {
			s.setItem("OTG.dataFilter", chosenDataFilter.pack());
		} else {
			s.setItem("OTG.dataFilter", "");
		}			
	}
	
	protected void storeColumns(Storage s, String key, Collection<BarcodeColumn> columns) {				
		if (chosenDataFilter != null) {
			if (!columns.isEmpty()) {
				s.setItem("OTG." + key + "." + chosenDataFilter.pack(),
						packColumns(columns));
			} else {
				s.setItem("OTG." + key + "." + chosenDataFilter.pack(), "");
			}
		}		
	}
	
	public void storeColumns(Storage s) {
		storeColumns(s, "columns", SharedUtils.asColumns(chosenColumns));
	}	
	
	protected void storeCustomColumn(Storage s, DataColumn column) {		
		if (column != null) {
			s.setItem("OTG.customColumn", column.pack());
		} else {
			s.removeItem("OTG.customColumn");
		}		
	}
	
	private String packColumns(Collection<BarcodeColumn> columns) {
		StringBuilder sb = new StringBuilder();
		for (DataColumn<?> c : columns) {
			sb.append(c.pack());
			sb.append("###");
		}
		return sb.toString();
	}

	private BarcodeColumn unpackColumn(String s) {
		String[] spl = s.split("\\$\\$\\$");
		if (spl[0].equals("Barcode")) {
			return Barcode.unpack(s);
		} else {
			return Group.unpack(s);
		}
	}
	
	protected List<Group> loadColumns(String key,
			Collection<BarcodeColumn> expectedColumns) throws Exception {
		Storage s = Storage.getLocalStorageIfSupported();
		if (s == null) {
			Window.alert("Local storage must be supported in the web browser. The application cannot continue.");
		} else {
			String v = s.getItem("OTG." + key + "." + chosenDataFilter.pack());
			List<Group> r = new ArrayList<Group>();		
			if (v != null && !v.equals(packColumns(expectedColumns))) {
				String[] spl = v.split("###");
				for (String cl : spl) {
					Group c = (Group) unpackColumn(cl);
					r.add(c);
				}			
				return r;
			}
		}

		return null;		
	}
	
	public void storeProbes(Storage s) {				
		s.setItem("OTG.probes", packProbes());		
	}
	
	private String packProbes() {
		StringBuilder sb = new StringBuilder();
		for (String p: chosenProbes) {			
			sb.append(p);
			sb.append("###");
		}		
		return sb.toString();
	}	

	/**
	 * Load saved state from the local storage.
	 * If the loaded state is different from what was previously remembered in this widget, the appropriate 
	 * signals will fire.
	 */
	public void loadState() {
		Storage s = Storage.getLocalStorageIfSupported();
		if (s == null) {
			Window.alert("Local storage must be supported in the web browser. The application cannot continue.");
		} else {
			loadState(s);			
		}
	}
	
	public void loadState(Storage s) {
		String v = s.getItem("OTG.dataFilter");
		if (v != null && (chosenDataFilter == null || !v.equals(chosenDataFilter.pack()))) {				
			dataFilterChanged(DataFilter.unpack(v));
		}
		if (chosenDataFilter != null) {				
			try {
				List<Group> cs = loadColumns("columns", SharedUtils.asColumns(chosenColumns));					
				if (cs != null) {						
					columnsChanged(cs);
				}						
				v = s.getItem("OTG.customColumn");
				if (v != null) {												
					BarcodeColumn cc = unpackColumn(v);						
					customColumnChanged(cc);						
				}
			} catch (Exception e) {										
				//one possible failure source is if data is stored in an incorrect format
				columnsChanged(new ArrayList<Group>());
				storeColumns(s); //overwrite the old data
				storeCustomColumn(s, null); //ditto
			}

		}
		v = s.getItem("OTG.probes");			
		if (v != null && !v.equals("") && !v.equals(packProbes())) {
			chosenProbes = v.split("###");				
			probesChanged(chosenProbes);				
		} else if (v == null || v.equals("")) {
			probesChanged(new String[0]);
		}
	}
	
	public void clearState() {
		
	}
	
	private int numPendingRequests = 0;
	
	private DialogBox waitDialog;
	
	// Load indicator handling
	protected void addPendingRequest() {
		numPendingRequests += 1;
		if (numPendingRequests == 1) {
			if (waitDialog == null) {
				waitDialog = new DialogBox(false, true);
				waitDialog.setWidget(Utils.mkEmphLabel("Please wait..."));
			}
			waitDialog.setPopupPositionAndShow(Utils.displayInCenter(waitDialog));
		}
	}
	
	protected void removePendingRequest() {
		numPendingRequests -= 1;
		if (numPendingRequests == 0) {
			waitDialog.hide();
		}
	}
	
}	
