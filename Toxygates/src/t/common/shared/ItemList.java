/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package t.common.shared;

import java.io.Serializable;
import java.util.Collection;

/**
 * A typed, named list of items.
 * 
 * Current supported types are "probes" and "compounds".
 * However, lists of type probes may actually be gene identifiers (entrez).
 */
@SuppressWarnings("serial")
abstract public class ItemList implements Packable, Serializable {

	protected String type;
	protected String name;
	
	protected ItemList() { }
	
	public ItemList(String type, String name) {
		this.name = name;
		this.type = type;
	}
	
	public String name() { return name; }
	public String type() { return type; }
	
	public String pack() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append(":::");
		sb.append(name);
		sb.append(":::");
		sb.append(SharedUtils.packList(packedItems(), "^^^"));
		return sb.toString();
	}
	
	abstract protected Collection<String> packedItems();

	abstract public int size();
	
	public static ItemList unpack(String input) {
		if (input == null) {
			return null;
		}
		
		String[] spl = input.split(":::");
		if (spl.length < 3) {
			return null;
		}
		
		String type = spl[0];
		String name = spl[1];
		String[] items = spl[2].split("\\^\\^\\^");
	
		// TODO would be good to avoid having this kind of central registry
		// of list types here. Use enum?
		if (type.equals("probes")) {
			return new StringList(type, name, items);
		} else if (type.equals("compounds")) {
			return new StringList(type, name, items);
		} else {
			// Unexpected type, ignore
			return null;
		} 
	}	

}
