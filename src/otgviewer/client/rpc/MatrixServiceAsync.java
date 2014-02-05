package otgviewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.ManagedMatrixInfo;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;
import bioweb.shared.array.ExpressionRow;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MatrixServiceAsync {

	public void identifiersToProbes(DataFilter filter, String[] identifiers,
			boolean precise, AsyncCallback<String[]> callback);

	public void loadDataset(DataFilter filter, List<Group> columns,
			String[] probes, ValueType type, List<Synthetic> synthCols, 
			AsyncCallback<ManagedMatrixInfo> callback);

	public void selectProbes(String[] probes,
			AsyncCallback<ManagedMatrixInfo> callback);

	public void setColumnThreshold(int column, @Nullable Double threshold,
			AsyncCallback<ManagedMatrixInfo> callback);
	
	public void datasetItems(int offset, int size, int sortColumn,
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);

	public void getFullData(DataFilter filter, List<String> barcodes,
			String[] probes, ValueType type, boolean sparseRead,
			boolean withSymbols,
			AsyncCallback<List<ExpressionRow>> callback);

	public void prepareCSVDownload(AsyncCallback<String> callback);

	public void getGenes(int limit, AsyncCallback<String[]> callback);
	
	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test,
			AsyncCallback<Void> callback);
	
	public void removeTwoGroupTests(AsyncCallback<Void> callback);
	
}
