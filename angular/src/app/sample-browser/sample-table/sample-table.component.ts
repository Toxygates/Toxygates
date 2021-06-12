import { Component, ViewChild, OnChanges, SimpleChanges, Input, 
         AfterViewInit, NgZone, ChangeDetectorRef, TemplateRef, ElementRef } from '@angular/core';
import Tabulator, { GroupComponent } from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { BackendService } from '../../backend.service';
import { UserDataService } from '../../user-data.service';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { SampleFilter } from '../../models/sample-filter.model';
import { IAttribute, Sample } from 'src/app/models/backend-types.model';

@Component({
  selector: 'app-sample-table',
  templateUrl: './sample-table.component.html',
  styleUrls: ['./sample-table.component.scss']
})
export class SampleTableComponent implements OnChanges, AfterViewInit {

  constructor(private backend: BackendService, private ngZone: NgZone,
    private changeDetector: ChangeDetectorRef,
    private userData: UserDataService, private toastr: ToastrService,
    private modalService: BsModalService) {
    this.requiredAttributes.add("sample_id");
  }

  tabulator: Tabulator;
  sampleFilteringModalRef: BsModalRef;
  tabulatorReady = false;

  @Input() samples: Sample[];
  @Input() batchId: string;

  samplesMap: Map<string, Sample>;

  attributes: IAttribute[];
  requiredAttributes = new Set<string>();
  fetchedAttributes: Set<string>;

  sampleFilters: SampleFilter[] = [];

  sampleGroupName: string;
  sampleCreationIsCollapsed = true;
  readyToCreateGroup = true;

  controlGroupsExpanded = true;
  treatmentGroupsExpanded = true;

  selectedGroups = new Set<string>();

  @ViewChild('tabulatorContainer') tabulatorContainer: ElementRef;

  ngOnChanges(changes: SimpleChanges):void {
    if (changes.samples != null) {
      if (changes.samples.currentValue == null) {
        if (this.tabulatorContainer != null) {
          (this.tabulatorContainer.nativeElement as HTMLElement).innerHTML = '';
        }
      } else {
        this.fetchedAttributes = new Set<string>();
        this.samplesMap = new Map<string, Sample>();
        this.sampleFilters = [];
        this.samples.forEach((sample) => {
          this.samplesMap.set(sample.sample_id, sample);
          Object.keys(sample).forEach((attribute) => {
            this.fetchedAttributes.add(attribute);
          })
        });
        this.tryDrawTable();
      }
    }
    if (changes.batchId != null && 
        changes.batchId.currentValue != changes.batchId.previousValue) {
      this.backend.getAttributesForBatch(this.batchId)
        .subscribe(
          result => {
            this.attributes = result;
          }
        )
    }
  }

  ngAfterViewInit(): void {
    this.tabulatorReady = true;
    this.tryDrawTable();
  }

  columns = [
    //{formatter:"rowSelection", titleFormatter:"rowSelection", align:"center", headerSort:false},
    {title: 'Sample ID', field: 'sample_id'},
  ]

  tab = document.createElement('div');

  saveSampleGroup(): void {
    if (this.sampleGroupName && this.selectedGroups.size > 0) {
      const samplesInGroup = [];
      this.samples.forEach((sample) => {
        if (this.selectedGroups.has(sample.treatment)) {
          samplesInGroup.push(sample.sample_id);
        }
      });

      this.userData.saveSampleGroup(this.sampleGroupName, samplesInGroup);
      this.toastr.success('Group name: ' + this.sampleGroupName, 'Sample group saved');
      this.sampleCreationIsCollapsed = true;
      this.sampleGroupName = undefined;
      this.selectedGroups.clear();

      this.tabulator.redraw();
    }
  }

  toggleControlGroups(): void {
    this.controlGroupsExpanded = !this.controlGroupsExpanded;
    const groups = this.tabulator.getGroups();
    groups.forEach(function(group) {
      group.toggle();
    });
  }

  toggleTreatmentGroups(): void {
    this.treatmentGroupsExpanded = !this.treatmentGroupsExpanded;
    const groups = this.tabulator.getGroups();
    groups.forEach(function(group) {
      group.getSubGroups().forEach(function(subGroup) {
        subGroup.toggle();
      });
    });
  }

  toggleColumn(attribute: IAttribute): void {
    const columnDefinition = this.columnForAttribute(attribute);
    if (columnDefinition != null) {
      void this.tabulator.deleteColumn(columnDefinition.field);
    } else {
      void this.tabulator.addColumn({
        title: attribute.title,
        field: attribute.id,
      });
      if (!this.fetchedAttributes.has(attribute.id)) {
        this.samples.forEach(sample => sample[attribute.id] = "Loading...");
        void this.tabulator.replaceData(this.samples);
        this.backend.getAttributeValues(this.samples.map(sample => sample.sample_id),
          [this.batchId], [attribute.id]).subscribe(
            result => {
                this.fetchedAttributes.add(attribute.id);
                result.forEach((element) => {
                  this.samplesMap.get(element.sample_id)[attribute.id] = element[attribute.id]
                });
                this.samples.forEach(function(sample) {
                  if (sample[attribute.id] == "Loading...") {
                    sample[attribute.id] = "n/a";
                  }
                })
                void this.tabulator.replaceData(this.samples);
            }
          );
      }
    }
  }

  openSampleFilteringModal(template: TemplateRef<unknown>): void {
    this.sampleFilteringModalRef = this.modalService.show(template,
      { class: 'modal-dialog-centered modal-lg',
        ignoreBackdropClick: true });
  }

  filtersSubmitted(): void {
    this.sampleFilteringModalRef.hide();
  }

  columnForAttribute(attribute: IAttribute): Tabulator.ColumnDefinition {
    const columnDefinitions = this.tabulator.getColumnDefinitions();
    const column = columnDefinitions.find(function(column) {
      return column.field == attribute.id;
    })
    return column;
  }

  private tryDrawTable(): void {
    if (this.tabulatorReady && this.samples != null) {
      const tabulatorElement = document.createElement('div');
      tabulatorElement.style.width = "auto";
      (this.tabulatorContainer.nativeElement as HTMLElement).appendChild(tabulatorElement);

      const groupHeader = (value: string, count: number, data, group: GroupComponent) => {
        //value - the value all members of this group share
        //count - the number of rows in this group
        //data - an array of all the row data objects in this group
        //group - the group component for the group

        let prefix: string, itemCount: number, itemWord: string, button: string;

        if (group.getParentGroup()) {
          itemCount = count;
          itemWord = " sample";
          if (value != (group.getParentGroup() as GroupComponent).getKey()) {
            prefix = "Treatment group - ";
            if (this.selectedGroups.has(value)) {
              button = "<button type='button' class='btn btn-success'>"
                + "Group selected <i class='bi bi-check'></i></button>"
            } else {
              button = "<button type='button' class='btn btn-secondary'>"
              + "Select group</button>"
            }
          } else {
            prefix = "Control group - ";
            button = "";
          }
        } else {
          prefix = "Control group - ";
          itemCount = group.getSubGroups().length;
          itemWord = " group";
          button = "";
        }

        itemWord += itemCount != 1 ? "s" : "";

        return `${prefix}${value}<span>(${itemCount}${itemWord})</span> ${button}`;
      }

      this.ngZone.runOutsideAngular(() => {
        this.tabulator = new Tabulator(tabulatorElement, {
          data: this.samples,
          selectable: true,
          columns: this.columns,
          layout:"fitDataFill",
          height: "calc(100vh - 18.3rem)",
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
          groupHeader: groupHeader,
          groupClick: (e, group)=> {
            if (e.target instanceof Element &&
                (e.target.tagName=="BUTTON" ||
                 (e.target.parentNode instanceof Element) &&
                  ((e.target.parentNode.tagName=="BUTTON")))) {
              // click is on the button
              if (this.selectedGroups.has(group.getKey())) {
                this.selectedGroups.delete(group.getKey());
              } else {
                this.selectedGroups.add(group.getKey());
              }
              this.changeDetector.detectChanges();
              this.tabulator.redraw();
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
          // groupStartOpen: function(value, count, data, group){
          //   return true;
          //   return value.substring(0, 7) == "Control";
          // },
        });
      });
    }
  }
}
