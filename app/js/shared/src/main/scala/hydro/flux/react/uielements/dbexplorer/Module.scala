package hydro.flux.react.uielements.dbexplorer

import hydro.common.I18n
import hydro.flux.react.uielements.PageHeader
import hydro.flux.stores.DatabaseExplorerStoreFactory
import hydro.models.access.JsEntityAccess

final class Module(implicit
    i18n: I18n,
    pageHeader: PageHeader,
    jsEntityAccess: JsEntityAccess,
    databaseExplorerStoreFactory: DatabaseExplorerStoreFactory,
) {

  private implicit lazy val databaseTableView: DatabaseTableView = new DatabaseTableView

  lazy val databaseExplorer: DatabaseExplorer = new DatabaseExplorer
}
