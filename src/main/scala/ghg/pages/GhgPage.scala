package ghg.pages

import diode.react.ModelProxy
import ghg.components.LeftNav
import ghg.routes.{AppRoutes, AppRoute}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import model.GhgData
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object GhgPage {
  object Style extends StyleSheet.Inline { import dsl._
    val container = style(
      display.flex,
      minHeight(600.px))
    val nav = style(width(190.px))
    val content = style(
      padding(10.px),
      borderLeft :=! "1px solid rgb(223, 220, 220)",
      flex := "1")
  }

  case class Backend($: BackendScope[Props, _]) {
    def render(P: Props) = {
      <.div(Style.container,
        <.div(Style.nav, LeftNav(AppRoutes.all, P.selectedPage, P.ctrl)),
        <.div(Style.content, P.selectedPage.render())
      )
    }
  }

  val component = ReactComponentB[Props]("GhgPage")
    .renderBackend[Backend]
//    .configureSpec(ThemeInstaller.installMuiContext())
    .build

  case class Props(proxy: ModelProxy[GhgData], selectedPage: AppRoute, ctrl: RouterCtl[AppRoute])

  def apply(proxy: ModelProxy[GhgData], selectedPage: AppRoute, ctrl: RouterCtl[AppRoute]) = component(Props(proxy, selectedPage, ctrl))
}
