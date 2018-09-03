package t.viewer.client.table;

import javax.annotation.Nullable;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.user.cellview.client.Column;

public abstract class HideableColumn<T, C> extends Column<T, C> {
  public HideableColumn(Cell<C> cell, boolean initState,
      @Nullable StandardColumns standard) {
    super(cell);      
    _visible = initState;
    this.standard = standard;
  }

  protected boolean _visible;
  protected ColumnInfo _columnInfo;
  final @Nullable StandardColumns standard;

  public ColumnInfo columnInfo() {
    return _columnInfo;
  }

  public boolean visible() {
    return _visible;
  }

  void setVisibility(boolean v) {
    _visible = v;
  }
}
