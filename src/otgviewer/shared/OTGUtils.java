package otgviewer.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Data manipulation utility methods.
 * @author johan
 *
 */
public class OTGUtils {

	public static List<OTGColumn> asColumns(List<Group> groups) {		
		List<OTGColumn> r = new ArrayList<OTGColumn>(groups.size());	
		for (Group g: groups) {
			r.add(g);
		}		
		return r;
	}
	
	public static String[] allCompounds(List<OTGColumn> columns) {
		List<String> r = new ArrayList<String>();
		for (OTGColumn dc : columns) {
			for (String c : dc.getCompounds()) {
				r.add(c);
			}
		}
		return r.toArray(new String[0]);
	}

	/**
	 * In the list of groups, find the one that has the given title.
	 * @param groups
	 * @param title
	 * @return
	 */
	public static Group findGroup(List<Group> groups, String title) {
		for (Group d : groups) {
			if (d.getName().equals(title)) {
				return d;
			}
		}
		return null;
	}

	/**
	 * In the list of groups, find the first one that contains the given sample.
	 * @param columns
	 * @param barcode
	 * @return
	 */
	public static Group groupFor(List<Group> columns, String barcode) {
		for (Group c : columns) {
			for (OTGSample b : c.getSamples()) {
				if (b.getCode().equals(barcode)) {
					return c;						
				}
			}
		}
		return null;
	}
	
	/**
	 * In the list of groups, find those that contain the given sample.
	 * @param columns
	 * @param barcode
	 * @return
	 */
	public static List<Group> groupsFor(List<Group> columns, String barcode) {
		List<Group> r = new ArrayList<Group>();
		for (Group c : columns) {
			for (OTGSample b : c.getSamples()) {
				if (b.getCode().equals(barcode)) {
					r.add(c);
					break;
				}
			}
		}
		return r;
	}

	public static String[] compoundsFor(List<Group> columns) {
		List<String> compounds = new ArrayList<String>();
		for (Group g : columns) {
			for (String c : g.getCompounds()) {
				if (!compounds.contains(c)) {
					compounds.add(c);
				}
			}
		}
		return compounds.toArray(new String[0]);
	}
	
	/**
	 * Extract the Barcode object that has the given barcode (as a String)
	 * from the list of groups.
	 * @param columns
	 * @param barcode
	 * @return
	 */
	public static OTGSample barcodeFor(List<Group> columns, String barcode) {
		for (Group c : columns) {
			for (OTGSample b : c.getSamples()) {
				if (b.getCode().equals(barcode)) {
					return b;
				}
			}
		}
		return null;
	}
	
	
}
