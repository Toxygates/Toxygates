import { Component, ViewChild, AfterViewInit, NgZone, ChangeDetectorRef,
  TemplateRef, ElementRef, OnInit } from '@angular/core';
import Tabulator from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../../shared/services/user-data.service';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { SampleFilter } from '../../shared/models/sample-filter.model';
import { IAttribute, Sample } from '../../shared/models/backend-types.model';
import { SampleTableHelper } from './sample-table-helper'
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { FetchedDataService } from 'src/app/shared/services/fetched-data.service';

@Component({
  selector: 'app-sample-table',
  templateUrl: './sample-table.component.html',
  styleUrls: ['./sample-table.component.scss']
})
export class SampleTableComponent implements OnInit, AfterViewInit {

  constructor(public fetchedData: FetchedDataService, private ngZone: NgZone,
    private changeDetector: ChangeDetectorRef,
    private userData: UserDataService, private toastr: ToastrService,
    private modalService: BsModalService) {

    this.samples$ = this.fetchedData.samples$;
    this.selectedBatch$ = this.userData.selectedBatch$;
    this.samplesMap$ = this.fetchedData.samplesMap$;
    this.attributes$ = this.fetchedData.attributes$;
    this.attributeMap$ = this.fetchedData.attributeMap$;
    this.fetchedAttributes$ = this.fetchedData.fetchedAttributes$;

  }

  tabulator: Tabulator | undefined;
  sampleFilteringModalRef: BsModalRef | undefined;
  tabulatorReady = false;

  helper = new SampleTableHelper();

  samples$: BehaviorSubject<Sample[] | null>;
  selectedBatch$: Observable<string | null>;

  samplesMap$: Observable<Map<string, Sample>>;

  attributes$: Observable<IAttribute[] | null>;
  attributeMap$: Observable<Map<string, IAttribute>>;
  fetchedAttributes$: BehaviorSubject<Set<string>>;

  subscriptions: Subscription[] = [];

  sampleCreationIsCollapsed = true;

  controlGroupsExpanded = true;
  treatmentGroupsExpanded = true;

  selectedTreatmentGroups = new Set<string>();

  @ViewChild('tabulatorContainer') tabulatorContainer: ElementRef | undefined;

  ngOnInit(): void {
    this.subscriptions.push(this.selectedBatch$.subscribe(_batch => {
      if (this.tabulatorContainer != null) {
        (this.tabulatorContainer.nativeElement as HTMLElement).innerHTML = '';
      }
      this.helper.filters = [];
    }));

    this.subscriptions.push(this.samples$.subscribe(_samples => {
      this.tryDrawTable();
    }));

    this.subscriptions.push(this.fetchedAttributes$.subscribe(_attributes => {
      if (this.samples$.value) {
        void this.tabulator?.replaceData(this.samples$.value);
      }
    }));
  }

  ngAfterViewInit(): void {
    this.tabulatorReady = true;
    this.tryDrawTable();
  }

  initialColumns(): Tabulator.ColumnDefinition[] {
    return [
      //{formatter:"rowSelection", titleFormatter:"rowSelection", align:"center", headerSort:false},
      {title: 'Sample ID', field: 'sample_id'},
    ];
  }

  onSampleGroupSaved(sampleGroupName: string): void {
    if (this.selectedTreatmentGroups.size > 0) {
      if (this.samples$.value == null) throw new Error("samples not defined");

      const samplesInGroup = this.samples$.value.filter(s =>
        this.selectedTreatmentGroups.has(s.treatment));

      this.userData.saveSampleGroup(sampleGroupName, samplesInGroup);
      this.toastr.success('Group name: ' + sampleGroupName, 'Sample group saved');
      this.selectedTreatmentGroups.clear();

      this.tabulator?.redraw();
    }
  }

  toggleControlGroups(): void {
    this.controlGroupsExpanded = !this.controlGroupsExpanded;
    const groups = this.tabulator?.getGroups();
    if (!groups) throw new Error("groups is not defined");
    groups.forEach(function(group) {
      group.toggle();
    });
  }

  toggleTreatmentGroups(): void {
    this.treatmentGroupsExpanded = !this.treatmentGroupsExpanded;
    const groups = this.tabulator?.getGroups();
    if (!groups) throw new Error("groups is not defined");
    groups.forEach(function(group) {
      group.getSubGroups().forEach(function(subGroup) {
        subGroup.toggle();
      });
    });
  }

  toggleColumn(attribute: IAttribute): void {
    const columnDefinition = this.findColumnForAttribute(attribute);
    if (columnDefinition?.field) {
      void this.tabulator?.deleteColumn(columnDefinition.field);
    } else {
      void this.tabulator?.addColumn(this.helper.createColumnForAttribute(attribute));
      if (!this.fetchedAttributes$.value.has(attribute.id)) {
        this.fetchedData.fetchAttribute(attribute);
      }
      if (this.samples$.value) {
        void this.tabulator?.replaceData(this.samples$.value);
      }
    }
  }

  openSampleFilteringModal(template: TemplateRef<unknown>): void {
    this.sampleFilteringModalRef = this.modalService.show(template,
      { class: 'modal-dialog-centered modal-lg',
        ignoreBackdropClick: true,
        keyboard: false
      });
  }

  onSubmitFilters(filters: SampleFilter[]): void {
    this.sampleFilteringModalRef?.hide();
    this.helper.filters = filters;
    this.filterSamples(true);
    this.helper.updateColumnFormatters(this.tabulator);
  }

  onCancelEditFilters(): void {
    this.sampleFilteringModalRef?.hide();
  }

  clearFilters(): void {
    this.helper.clearFilters();
    this.tabulator?.setData(this.samples$.value);
    this.helper.updateColumnFormatters(this.tabulator);
  }

  private filterSamples(grouped: boolean): void {
    if (this.samples$.value == undefined) throw new Error("samples not defined");
    this.tabulator?.setData(this.helper.filterSamples(this.samples$.value, grouped));
  }

  findColumnForAttribute(attribute: IAttribute):
      Tabulator.ColumnDefinition | undefined {
    const columnDefinitions = this.tabulator?.getColumnDefinitions();
    if (!columnDefinitions) throw new Error("columnDefinitions not defiend");
    return columnDefinitions.find(function(column) {
      return column.field == attribute.id;
    })
  }

  private tryDrawTable(): void {
    if (this.tabulatorReady && this.samples$.value != null) {
      const tabulatorElement = document.createElement('div');
      tabulatorElement.style.width = "auto";
      (this.tabulatorContainer?.nativeElement as HTMLElement).appendChild(tabulatorElement);

      this.ngZone.runOutsideAngular(() => {
        this.tabulator = new Tabulator(tabulatorElement, {
          data: this.samples$.value || undefined,
          selectable: true,
          columns: this.initialColumns(),
          layout:"fitDataFill",
          height: "calc(100vh - 18.8rem)",
          /* eslint-disable @typescript-eslint/no-unsafe-assignment,
                            @typescript-eslint/no-explicit-any */
          groupBy: ([function(data: { control_treatment: string }): string {
              return data.control_treatment;
            },
            function(data: { treatment: string }): string {
              return data.treatment;
            }
          // Workaround for GroupArg union type not including ((data: any) => any)[]
          ]) as any,
          /* eslint-enable @typescript-eslint/no-unsafe-assignment,
                           @typescript-eslint/no-explicit-any */
          groupHeader: SampleTableHelper.groupHeader(this.selectedTreatmentGroups),
          groupClick: (e, group)=> {
            if (e.target instanceof Element &&
                (e.target.tagName=="BUTTON" ||
                 (e.target.parentNode instanceof Element) &&
                  ((e.target.parentNode.tagName=="BUTTON")))) {
              // click is on the button
              if (this.selectedTreatmentGroups.has(group.getKey())) {
                this.selectedTreatmentGroups.delete(group.getKey());
              } else {
                this.selectedTreatmentGroups.add(group.getKey());
              }
              this.changeDetector.detectChanges();
              this.tabulator?.redraw();
            } else {
              // click is elsewhere on the header
              if (group.isVisible()) {
                group.hide();
              } else {
                group.show();
              }
            }
          },
          groupToggleElement: false,
        });
      });
    }
  }
}
