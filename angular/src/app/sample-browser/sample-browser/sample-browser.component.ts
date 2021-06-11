import { Component, OnInit } from '@angular/core';
import { BackendService } from '../../backend.service';
import { UserDataService } from '../../user-data.service';

@Component({
  selector: 'app-sample-browser',
  templateUrl: './sample-browser.component.html',
  styleUrls: ['./sample-browser.component.scss']
})
export class SampleBrowserComponent implements OnInit {

  constructor(private backend: BackendService, 
    private userData: UserDataService) {}

  datasetId: string;
  batchId: string;
  samples;

  ngOnInit(): void {
    this.datasetId = this.userData.getSelectedDataset();
  }

  onSelectedDatasetChange(datasetId: string): void {
    this.datasetId = datasetId;
    this.userData.setSelectedDataset(datasetId);
  }

  batchChanged(batchId: string): void {
    this.batchId = batchId;
    delete this.samples;
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
        }
      )
  }
}