package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.SharedUtils;
import otgviewer.shared.ValueType;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;

/**
 * A Composite that is also a DataViewListener.
 * Has default implementations for the change listener methods.
 * @author johan
 *
 */
class DataListenerWidget extends Composite implements DataViewListener {

	private List<DataViewListener> listeners = new ArrayList<DataViewListener>();
	
	protected DataFilter chosenDataFilter;
	protected String chosenProbe;
	protected String[] chosenProbes;
	protected List<String> chosenCompounds = new ArrayList<String>();
	protected String chosenCompound;
	protected ValueType chosenValueType;
	protected List<DataColumn> chosenColumns = new ArrayList<DataColumn>();
	
	protected boolean active = false;
	
	public void addListener(DataViewListener l) {
		listeners.add(l);
	}
	
	//incoming signals
	public void dataFilterChanged(DataFilter filter) {
		chosenDataFilter = filter;
		changeDataFilter(filter);
	}
	
	public void probeChanged(String probe) {
		chosenProbe = probe;
		changeProbe(probe);
	}
	
	public void probesChanged(String[] probes) {
		chosenProbes = probes;
		changeProbes(probes);
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
	
	public void columnsChanged(List<DataColumn> columns) {
		chosenColumns = columns;
		changeColumns(columns);
	}
	
	public void heightChanged(int newHeight) {
		changeHeight(newHeight);
	}
	
	//outgoing signals	
	protected void changeDataFilter(DataFilter filter) {
		chosenDataFilter = filter;
		for (DataViewListener l : listeners) {
			l.dataFilterChanged(filter);
		}		
	}
	
	protected void changeProbe(String probe) {
		chosenProbe = probe;
		for (DataViewListener l: listeners) {
			l.probeChanged(probe);
		}
	}
	
	protected void changeProbes(String[] probes) {
		chosenProbes = probes;
		for (DataViewListener l: listeners) {
			l.probesChanged(probes);
		}
	}
	
	protected void changeCompounds(List<String> compounds) {
		chosenCompounds = compounds;
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
	
	
	protected void changeColumns(List<DataColumn> columns) {
		chosenColumns = columns;
		for (DataViewListener l : listeners) {
			l.columnsChanged(columns);
		}
	}
	protected void changeHeight(int newHeight) {
		for (DataViewListener l : listeners) {
			l.heightChanged(newHeight);
		}
	}
	
	public void propagateTo(DataListenerWidget other) {
		other.dataFilterChanged(chosenDataFilter);
		other.probeChanged(chosenProbe);
		other.probesChanged(chosenProbes);
		other.compoundsChanged(chosenCompounds);
		other.compoundChanged(chosenCompound);
		other.valueTypeChanged(chosenValueType);
		other.columnsChanged(chosenColumns);		
	}
	
	// other
	
	public void activate() {
		active = true;
	}
	
	public void deactivate() {
		active = false;
	}
	
	public void storeState() {
		Storage s = Storage.getLocalStorageIfSupported();
		if (s == null) {
			Window.alert("Local storage must be supported in the web browser. The application cannot continue.");
		} else {
			if (chosenDataFilter != null) {
				s.setItem("OTG.dataFilter", chosenDataFilter.pack());
			}
			if (chosenValueType != null) {
				s.setItem("OTG.valueType", chosenValueType.toString());
			}
			if (chosenColumns != null) {
				StringBuilder sb = new StringBuilder();
				for (DataColumn c : chosenColumns) {
					sb.append(c.pack());
					sb.append("###");
				}
				s.setItem("OTG.columns", sb.toString());
			}
		}
	}
	
	public void loadState() {
		Storage s = Storage.getLocalStorageIfSupported();
		if (s == null) {
			Window.alert("Local storage must be supported in the web browser. The application cannot continue.");
		} else {
			String v = s.getItem("OTG.dataFilter");
			if (v != null) {				
				dataFilterChanged(DataFilter.unpack(v));
			}
			v = s.getItem("OTG.valueType");
			if (v != null) {
				valueTypeChanged(ValueType.valueOf(v));
			}
			v = s.getItem("OTG.columns");
			if (v != null && !v.equals("")) {				
				String[] spl = v.split("###");
				chosenColumns.clear();
				for (String cl: spl) {
					DataColumn c = SharedUtils.unpackColumn(cl);
					chosenColumns.add(c);
				}
				columnsChanged(chosenColumns);
			}
		}
	}
	
	
}	
