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

package t.common.shared.maintenance;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OperationResults implements Serializable {

	public OperationResults() { }

	public OperationResults(String taskName, boolean successful, String[] infoStrings) {
		_taskName = taskName;
		_infoStrings = infoStrings;
		_successful = successful;
	}
	
	private String _taskName;
	private boolean _successful;
	private String[] _infoStrings;
	
	public String taskName() { return _taskName; }
	public boolean successful() { return _successful; }
	public String[] infoStrings() { return _infoStrings; }	
}