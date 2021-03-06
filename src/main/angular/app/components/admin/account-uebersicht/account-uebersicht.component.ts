import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {MatDialog, MatPaginator, MatSort} from "@angular/material";
import {EMPTY, fromEvent, merge, of} from "rxjs";
import {debounceTime, distinctUntilChanged, flatMap, switchMap, tap} from "rxjs/operators";
import {AccountsDataSource} from "../../../datasources/accounts.dataSource";
import {AdminService} from "../../../services/admin.service";
import {AccountUebersicht} from "../../../model/account-uebersicht";
import {YesNoDialogComponent} from "../../allgemein/yes-no-dialog/yes-no-dialog.component";


@Component({
  selector: 'app-account-uebersicht',
  templateUrl: './account-uebersicht.component.html',
  styleUrls: ['./account-uebersicht.component.scss']
})

export class AccountUebersichtComponent implements OnInit, AfterViewInit {
    dataSource: AccountsDataSource;
    initialPageSize = 10;
    pageSizes = [10, 20, 50];
    displayedColumns = ["id", "username", "rolle", "veroeffentlichungen", "actions"];

    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;
    @ViewChild('searchField') searchField: ElementRef;

    constructor(private adminService: AdminService,
                public dialog: MatDialog) {
    }

    ngOnInit() {
        this.dataSource = new AccountsDataSource(this.adminService);

        this.dataSource.loadAccounts('', 'username', 'asc', 0, this.initialPageSize);
    }

    ngAfterViewInit() {

        this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

        fromEvent(this.searchField.nativeElement,'keyup')
            .pipe(
                debounceTime(150),
                distinctUntilChanged(),
                tap(() => {
                    this.paginator.pageIndex = 0;

                    this.loadAccountsPage();
                })
            )
            .subscribe();

        merge(this.sort.sortChange, this.paginator.page)
            .pipe(
                tap(() => this.loadAccountsPage())
            )
            .subscribe();

    }

    loadAccountsPage() {
        this.dataSource.loadAccounts(
            this.searchField.nativeElement.value,
            'username',
            this.sort.direction,
            this.paginator.pageIndex,
            this.paginator.pageSize);
    }

    deleteAccount(account: AccountUebersicht) {
        const dialogRef = this.dialog.open(YesNoDialogComponent, {
            disableClose: true,
            autoFocus: true,
            data: 'Möchten Sie den User wirklich löschen?'
        });

        dialogRef.afterClosed().pipe(
            flatMap(val => val ? of(val) : EMPTY),
            switchMap(_ => this.adminService.delete(account.id))
        ).subscribe(_ => this.loadAccountsPage());
    }
}
