import {Component} from '@angular/core';
import {InseratUebersicht} from "../../../model/inserat-uebersicht";
import {InserateService} from "../../../services/inserate.service";
import {ColumnSortedEvent} from "../../../lib/sortable-table/sort.service";
import {Router} from "@angular/router";


@Component({
    selector: 'app-inserate',
    templateUrl: './inserate-uebersicht.component.html',
    styleUrls: ['./inserate-uebersicht.component.scss']
})
export class InserateUebersichtComponent {
    inserate: InseratUebersicht[];

    constructor(private inserateService: InserateService,
                private router: Router) {
        this.loadInserate();
    }

    onSorted(evt: ColumnSortedEvent){
        this.loadInserate(evt);
    }

    private loadInserate(sortEvent?: ColumnSortedEvent) {
        const sortColumn = (sortEvent || ({} as ColumnSortedEvent)).sortColumn;
        const sortDirection = (sortEvent || ({} as ColumnSortedEvent)).sortDirection;
        this.inserateService.loadAll(sortColumn, sortDirection)
            .subscribe(result => this.inserate = result);
    }

    addInserat() {
        this.router.navigate(['/inserate/create']);
    }
}
