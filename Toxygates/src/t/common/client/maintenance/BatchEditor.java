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

package t.common.client.maintenance;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

import t.common.client.rpc.BatchOperationsAsync;
import t.common.shared.Dataset;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;

abstract public class BatchEditor extends ManagedItemEditor {
  @Nullable protected BatchUploader uploader;
  
  final protected Collection<Dataset> datasets;
  final protected Collection<Instance> instances;
  final protected BatchOperationsAsync batchOps;
  
  public BatchEditor(Batch b, boolean addNew, Collection<Dataset> datasets,
      Collection<Instance> instances, BatchOperationsAsync batchOps) {
    super(b, addNew);
    this.datasets = datasets;
    this.instances = instances;
    this.batchOps = batchOps;
    guiBeforeUploader(vp, b, addNew);

    if (addNew) { 
      uploader = new BatchUploader();
      vp.add(uploader);
    }
    guiAfterUploader(vp, b, addNew);

    addCommands();
  }

  protected void guiBeforeUploader(VerticalPanel vp, Batch b, boolean addNew) {    
  }
  
  protected void guiAfterUploader(VerticalPanel vp, Batch b, boolean addNew) {    
  }
  
  abstract protected Set<String> instancesForBatch();
  
  abstract protected String datasetForBatch();
  
  @Override
  protected void triggerEdit() {
    if (idText.getValue().equals("")) {
      Window.alert("ID cannot be empty");
      return;
    }
    
    Batch b =
        new Batch(idText.getValue(), 0, commentArea.getValue(), new Date(), 
            instancesForBatch(), datasetForBatch());

    if (addNew && uploader.canProceed()) {
      batchOps.addBatchAsync(b, new TaskCallback(
          "Upload batch", batchOps) {

        @Override
        protected void onCompletion() {          
          onFinish();
          onFinishOrAbort();
        }
        
        @Override
        protected void onFailure() {
          onError();
        }
      });
    } else {
      batchOps.update(b, editCallback());
    }
  }
  
  protected void onError() {
    super.onError();
    if (uploader != null) {
      uploader.resetAll();
    }
  }
}