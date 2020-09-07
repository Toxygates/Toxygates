import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(private http: HttpClient) { }

  serviceUrl = 'toxygates/json/';
  datasetsPath = 'datasets';
  batchesPath = 'batches';

  getDatasets() {
    return this.http.get(this.serviceUrl + this.datasetsPath)
      .pipe(
        tap(_ => console.log('fetched datasets')),
        catchError(error => {
          console.error('Error fetching datasets: ' + error);
          throw error;
        })
      );
  }

  getBatchesForDataset(datasetId: string) {
    return this.http.get(this.serviceUrl + this.batchesPath 
      + '?id=' +  datasetId)
      .pipe(
        tap(_ => console.log('fetched batches')),
        catchError(error => {
          console.error('Error fetching datasets: ' + error);
          throw error;
        })
      )
  }
}