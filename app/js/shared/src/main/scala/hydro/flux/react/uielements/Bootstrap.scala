package hydro.flux.react.uielements

import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable

object Bootstrap {

  def Row: VdomTag = <.div(^.className := "row")

  def Col(
      sm: Int = -1,
      md: Int = -1,
      lg: Int = -1,
      smOffset: Int = -1,
      mdOffset: Int = -1,
      tag: VdomTag = <.div,
  ): VdomTag = {
    val classes = mutable.Buffer[String]()
    if (sm != -1) classes += s"col-sm-$sm"
    if (md != -1) classes += s"col-md-$md"
    if (lg != -1) classes += s"col-lg-$lg"
    if (smOffset != -1) classes += s"col-sm-offset-$smOffset"
    if (mdOffset != -1) classes += s"col-md-offset-$mdOffset"
    tag(^^.classes(classes))
  }

  def Button(
      variant: Variant = Variant.default,
      size: Size = null,
      block: Boolean = false,
      circle: Boolean = false,
      tag: VdomTag = <.button,
      tpe: String = "button",
  ): VdomTag = {
    val classes = mutable.Buffer[String]()
    classes += "btn"
    classes += s"btn-${variant.name}"
    if (size != null) {
      classes += s"btn-${size.name}"
    }
    if (block) {
      classes += s"btn-block"
    }
    if (circle) {
      classes += s"btn-circle"
    }
    tag(^^.classes(classes), ^.tpe := tpe)
  }

  def ButtonGroup: VdomTag = <.span(^.className := "btn-group")

  def Icon(className: String): VdomTag = <.i(^.className := className)
  def FontAwesomeIcon(
      name: String,
      otherName: String = null,
      fixedWidth: Boolean = false,
      spin: Boolean = false,
  ): VdomTag = {
    val classes = mutable.Buffer[String]()
    classes += "fa"
    classes += s"fa-$name"
    if (otherName != null) {
      classes += s"fa-$otherName"
    }
    if (fixedWidth) {
      classes += s"fa-fw"
    }
    if (spin) {
      classes += s"fa-spin"
    }
    <.i(^^.classes(classes))
  }
  def Glyphicon(name: String): VdomTag = Icon(s"glyphicon glyphicon-$name")

  def ControlLabel: VdomTag = <.label(^.className := "control-label")

  def Label(variant: Variant): VdomTag =
    <.span(^.className := s"label label-${variant.name}")

  def Panel(variant: Variant = Variant.default): VdomTag =
    <.div(^.className := s"panel panel-${variant.name}")
  def PanelHeading: VdomTag = <.div(^.className := "panel-heading")
  def PanelBody: VdomTag = <.div(^.className := "panel-body")

  def InputGroup: VdomTag = <.div(^.className := "input-group")
  def InputGroupAddon: VdomTag = <.span(^.className := "input-group-addon")
  def InputGroupButton: VdomTag = <.span(^.className := "input-group-btn")

  def NavbarBrand(tag: VdomTag = <.span): VdomTag = tag(^.className := "navbar-brand")

  def Alert(variant: Variant): VdomTag = <.div(^.className := s"alert alert-${variant.name}")

  def FormHorizontal: VdomTag = <.form(^.className := "form-horizontal")
  def FormGroup: VdomTag = <.div(^.className := "form-group")
  def FormInline(tag: VdomTag = <.form): VdomTag = tag(^.className := "form-inline")

  case class Variant private (name: String)
  object Variant {
    val default = Variant("default")
    val primary = Variant("primary")
    val secondary = Variant("secondary")
    val success = Variant("success")
    val danger = Variant("danger")
    val warning = Variant("warning")
    val info = Variant("info")
    val link = Variant("link")
  }

  case class Size private (name: String)
  object Size {
    val xs = Size("xs")
    val sm = Size("sm")
    val lg = Size("lg")
    val xl = Size("xl")
  }
}
