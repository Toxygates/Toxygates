package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;

/**
 * A data grid with functionality for hiding columns and displaying 
 * clickable icons in the leftmost columns.
 * It also has the concepts of data columns and extra columns.
 */
abstract public class RichTable<T> extends DataListenerWidget {
	protected DataGrid<T> grid;
	protected List<HideableColumn> hideableColumns = new ArrayList<HideableColumn>();
	protected int highlightedRow = -1;
	private int extraCols = 0;
	protected int dataColumns = 0;
 	
	public RichTable() {
		hideableColumns = initHideableColumns();
		grid = new DataGrid<T>() {
			@Override
			protected void onBrowserEvent2(Event event) {
				if ("click".equals(event.getType())) {
					String target = event.getEventTarget().toString();					
					if (interceptGridClick(target, event.getClientX(), event.getClientY())) {
						super.onBrowserEvent2(event);
					}
				}
			}
		};
		
		initWidget(grid);
		grid.setWidth("100%");
		grid.setRowStyles(new RowHighligher<T>());
		AsyncHandler colSortHandler = new AsyncHandler(grid);
		grid.addColumnSortHandler(colSortHandler);
	}
	
	/**
	 * TODO clean this mechanism up as much as possible 
	 * @param target
	 * @return true if the click event should be propagated further.
	 */
	protected boolean interceptGridClick(String target, int x, int y) {
		return true;
	}
	
	protected void setupColumns() {
		// TODO: explicitly set the width of each column

		int count = grid.getColumnCount();
		for (int i = 0; i < count; ++i) {
			grid.removeColumn(0);
		}
		grid.getColumnSortList().clear();
		
		dataColumns = 0;
		extraCols = 0;
		Column<T, String> tcl = toolColumn(toolCell());
		
		grid.addColumn(tcl, "");
		tcl.setCellStyleNames("clickCell");
		grid.setColumnWidth(tcl, "40px");		
		extraCols += 1;
		
		for (HideableColumn c: hideableColumns) {
			if (c.visible()) {
				Column<T, ?> cc = (Column<T, ?>) c;
				addExtraColumn(cc, c.name());												
			}
		}		
	}
	
	/**
	 * Obtain the index of the column at the given x-position. Only works
	 * if there is at least one row in the table.
	 * @param x
	 * @return
	 */
	protected int columnAt(int x) {
		int prev = 0;
		for (int i = 0; i < grid.getColumnCount(); ++i) {
			Column<T, ?> col = grid.getColumn(i);
			int next = grid.getRowElement(0).getCells().getItem(i).getAbsoluteLeft();
			if (prev <= x && next > x) {
				return i;
			}
			prev = next;
		}
		return grid.getColumnCount() - 1;
	}
	
	protected Cell<String> toolCell() { return new TextCell(); }
	
	abstract protected Column<T, String> toolColumn(Cell<String> cell);
	
	protected SafeHtml headerHtml(String title, String tooltip) {
		 return SafeHtmlUtils.fromSafeConstant("<span title=\"" + tooltip + "\">" + title + "</span>");
	}
	
	protected void addColWithTooltip(Column<T, ?> c, String title, String tooltip) {		
		grid.addColumn(c, getColumnHeader(headerHtml(title, tooltip)));
	}
	
	protected void insertColWithTooltip(Column<T, ?> c, int at, String title, String tooltip) {
		grid.insertColumn(at, c, getColumnHeader(headerHtml(title, tooltip)));
	}
	
	protected Header<SafeHtml> getColumnHeader(SafeHtml safeHtml) {
		return new SafeHtmlHeader(safeHtml);
	}
	
	public void addDataColumn(Column<T, ?> col, String title, String tooltip) {
		col.setSortable(true);	
		addColWithTooltip(col, title, tooltip);		
		col.setCellStyleNames("dataColumn");		
		if (dataColumns == 0 && grid.getColumnSortList().size() == 0) {
			grid.getColumnSortList().push(col); //initial sort
		}
		dataColumns += 1;
	}
	
	/**
	 * Remove a column without altering the sort order, if possible
	 * @param c
	 */
	public void removeDataColumn(Column<T, ?> c) {
		ColumnSortList csl = grid.getColumnSortList();
		
		for (int i = 0; i < csl.size(); ++i) {
			ColumnSortInfo csi = grid.getColumnSortList().get(i);
			if (csi.getColumn() == c) {
				csl.remove(csi);
				break;
			}
		}		
		grid.removeColumn(c);
		dataColumns -= 1;
	}

	private int sortCol;
	private boolean sortAsc;
	public void computeSortParams() {
		ColumnSortList csl = grid.getColumnSortList();
		sortAsc = false;
		sortCol = 0;
		if (csl.size() > 0) {
			sortCol = grid.getColumnIndex(
					(Column<T, ?>) csl.get(0).getColumn())
					- extraCols;
			sortAsc = csl.get(0).isAscending();
		}
	}
	
	/**
	 * The offset of the data column being sorted (within the data columns only)
	 */
	public int sortDataColumnIdx() { 		
		return sortCol; 
	}
		
	public boolean sortAscending() {		
		return sortAsc; 
	}
	
	/**
	 * An "extra" column is a column that is not a data column.
	 * @param col
	 * @param name
	 */
	private void addExtraColumn(Column<T, ?> col, String name) {
		col.setCellStyleNames("extraColumn");
		insertColWithTooltip(col, extraCols, name, name);		
		extraCols += 1;
	}
	
	private void removeExtraColumn(Column<T, ?> col) {
		grid.removeColumn(col);
		extraCols -= 1;
	}
	
	/**
	 * Obtain the number of leading columns before the main data columns.
	 * @return
	 */
	protected int numExtraColumns() {
		return extraCols + 1;
	}
	
	abstract protected List<HideableColumn> initHideableColumns();
	
	/**
	 * Create tick menu items corresponding to the hideable columns.
	 * @param mb
	 */
	protected void setupMenuItems(MenuBar mb) {
		for (final HideableColumn c: hideableColumns) {
			new TickMenuItem(mb, c.name(), c.visible()) {
				@Override
				public void stateChange(boolean newState) {
					c.setVisibility(newState);	
					if (newState) {
						addExtraColumn(((Column<T, ?>) c), c.name());					
					} else {
						removeExtraColumn((Column<T, ?>) c);
					}				

				}				
			};
		}
	}
	
	protected interface HideableColumn {
		String name();
		boolean visible();
		void setVisibility(boolean v);		
	}
	
	/*
	 * The default hideable column
	 */
	protected abstract static class DefHideableColumn<T> extends TextColumn<T> implements HideableColumn {
		private boolean visible;
		public DefHideableColumn(String name, boolean initState) {
			super();
			visible = initState;
			_name = name;
		}
		
		private String _name;
		public String name() { return _name; }
		public boolean visible() { return this.visible; }
		public void setVisibility(boolean v) { visible = v; }		
	}
	
	protected class RowHighligher<U> implements RowStyles<U> {		
		public RowHighligher() {}

		@Override
		public String getStyleNames(U row, int rowIndex) {
			if (highlightedRow != -1 && rowIndex == highlightedRow + grid.getVisibleRange().getStart()) {
				return "highlightedRow";
			} else {
				return "";
			}
		}		
	}
}
