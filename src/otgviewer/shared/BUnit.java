package otgviewer.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import t.common.shared.SampleClass;
import t.common.shared.sample.Unit;

/**
 * A BUnit is a Unit of Barcodes.
 */
public class BUnit extends Unit<OTGSample> {

	protected BUnit() { super(); }
	private String _time, _dose, _compound;
	
	private String _organ = null;
	private String _organism = null;
	private String _cellType = null;
	private String _repeatType = null;
	
	public BUnit(String compound, String dose, String time) {
		super(compound + "/" + dose + "/" + time);
		_time = time;
		_dose = dose;
		_compound = compound;
	}
	
	public BUnit(OTGSample b, @Nullable SampleClass sc) {
		this(b.getCompound(), b.getDose(), b.getTime());
		if (sc != null) {
			setSampleClass(sc);
		}
	}
	
	public void setSampleClass(SampleClass sc) {	
		_organ = sc.get("organ_id");
		_organism = sc.get("organism");
		_cellType = sc.get("test_type");
		_repeatType = sc.get("sin_rep_type");

		_name = _organ + "/" + _organism + "/" + _cellType + "/" + _repeatType
				+ "/" + _name;
	}
	
	public String getTime() {
		return _time;
	}
	
	public String getDose() {
		return _dose;
	}
	
	public String getCompound() {
		return _compound;
	}
	
	@Nullable
	public String getOrgan() {
		return _organ.toString();
	}
	
	@Nullable
	public String getOrganism() {
		return _organism.toString();
	}
	
	@Nullable
	public String getCellType() {
		return _cellType.toString();
	}
	
	@Nullable
	public String getRepeatType() {
		return _repeatType.toString();
	}
	
	@Override 
	public int hashCode() {
		int r = _time.hashCode() + _dose.hashCode() + _compound.hashCode();
		if (_organ != null) {
			r = r + _organ.hashCode() + _organism.hashCode() + _repeatType.hashCode() +
					_cellType.hashCode();
		}
		return r;
	}
	
	@Override
	public boolean equals(Object other) {
		if ((other instanceof BUnit)) {
			BUnit that = (BUnit) other;
			
			//TODO in the future, organism, organ etc should never be null
			
			return _time.equals(that.getTime()) && _dose.equals(that.getDose())
					&& _compound.equals(that.getCompound())
					&& safeCompare(_organ, that.getOrgan())
					&& safeCompare(_organism, that.getOrganism())
					&& safeCompare(_repeatType, that.getRepeatType())
					&& safeCompare(_cellType, that.getCellType());
							
		} else {
			return false;
		}
	}
	
	private static boolean safeCompare(Object v1, Object v2) {
		if (v1 == null && v2 == null) {
			return true;
		} else if (v1 != null && v2 != null) {
			return v1.equals(v2);
		} else {
			return false;
		}
	}
	
	public static String[] compounds(BUnit[] units) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getCompound());
		}
		return r.toArray(new String[0]);
	}
	
	public static String[] times(BUnit[] units) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getTime());
		}
		return r.toArray(new String[0]);
	}
	
	public static String[] doses(BUnit[] units) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getDose());
		}
		return r.toArray(new String[0]);
	}
	
	public static BUnit[] formUnits(OTGSample[] barcodes, SampleClass sc) {
		Map<String, List<OTGSample>> units = new HashMap<String, List<OTGSample>>();
		for (OTGSample b: barcodes) {
			String cdt = b.getParamString();
			if (units.containsKey(cdt)) {
				units.get(cdt).add(b);
			} else {
				List<OTGSample> n = new ArrayList<OTGSample>();
				n.add(b);
				units.put(cdt, n);
			}
		}
		ArrayList<BUnit> r = new ArrayList<BUnit>();
		for (List<OTGSample> bcs: units.values()) {
			OTGSample first = bcs.get(0);
			BUnit b = (first.getUnit().getOrgan() == null) ? 
					new BUnit(bcs.get(0), sc) : first.getUnit(); 
			b.setSamples(bcs.toArray(new OTGSample[0]));
			r.add(b);
		}
		return r.toArray(new BUnit[0]);
	}
	
	public static OTGSample[] collectBarcodes(BUnit[] units) {
		List<OTGSample> r = new ArrayList<OTGSample>();
		for (BUnit b: units) {
			Collections.addAll(r, b.getSamples());		
		}
		return r.toArray(new OTGSample[0]);
	}
	
	public static boolean containsTime(BUnit[] units, String time) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getTime());
		}
		return r.contains(time);
	}
	
	public static boolean containsDose(BUnit[] units, String dose) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getDose());
		}
		return r.contains(dose);
	}
}
