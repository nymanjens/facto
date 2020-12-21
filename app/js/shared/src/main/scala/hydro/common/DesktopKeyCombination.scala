package hydro.common

import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import japgolly.scalajs.react.raw.SyntheticKeyboardEvent

sealed trait DesktopKeyCombination {
  def ctrl: Boolean
  def shift: Boolean
  def alt: Boolean
  def meta: Boolean
}
object DesktopKeyCombination {

  def fromEvent(event: SyntheticKeyboardEvent[_]): DesktopKeyCombination = {
    val ctrl = if (BrowserUtils.isMacOsX) event.metaKey else event.ctrlKey
    val meta = if (BrowserUtils.isMacOsX) event.ctrlKey else event.metaKey
    val key = event.key

    if (key.length == 1) {
      CharacterKey(
        character = getOnlyElement(key),
        ctrl = ctrl,
        shift = event.shiftKey,
        alt = event.altKey,
        meta = meta,
      )
    } else {
      SpecialKey(
        specialKeyType = key match {
          case "Enter"     => Enter
          case "Backspace" => Backspace
          case "Delete"    => Delete
          case "Tab"       => Tab
          case "ArrowUp"   => ArrowUp
          case "ArrowDown" => ArrowDown
          case _           => UnknownKeyType(key)
        },
        ctrl = ctrl,
        shift = event.shiftKey,
        alt = event.altKey,
        meta = meta,
      )
    }
  }

  case class CharacterKey(
      character: Char,
      override val ctrl: Boolean,
      override val shift: Boolean,
      override val alt: Boolean,
      override val meta: Boolean,
  ) extends DesktopKeyCombination

  case class SpecialKey(
      specialKeyType: SpecialKeyType,
      override val ctrl: Boolean,
      override val shift: Boolean,
      override val alt: Boolean,
      override val meta: Boolean,
  ) extends DesktopKeyCombination

  sealed trait SpecialKeyType
  case object Enter extends SpecialKeyType
  case object Backspace extends SpecialKeyType
  case object Delete extends SpecialKeyType
  case object Tab extends SpecialKeyType
  case object ArrowUp extends SpecialKeyType
  case object ArrowDown extends SpecialKeyType
  case class UnknownKeyType(key: String) extends SpecialKeyType
}
