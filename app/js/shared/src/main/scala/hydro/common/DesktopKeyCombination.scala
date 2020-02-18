package hydro.common

import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import japgolly.scalajs.react.raw.SyntheticKeyboardEvent

sealed trait DesktopKeyCombination {
  def ctrlOrMeta: Boolean
  def shift: Boolean
  def alt: Boolean
}
object DesktopKeyCombination {

  def fromEvent(event: SyntheticKeyboardEvent[_]): DesktopKeyCombination = {
    val ctrlOrMeta = event.ctrlKey || event.metaKey // TODO: Use the one or the other based on OS X / not OS X
    val key = event.key

    if (key.length == 1) {
      CharacterKey(
        character = getOnlyElement(key),
        ctrlOrMeta = ctrlOrMeta,
        shift = event.shiftKey,
        alt = event.altKey)
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
        ctrlOrMeta = ctrlOrMeta,
        shift = event.shiftKey,
        alt = event.altKey
      )
    }
  }

  case class CharacterKey(
      character: Char,
      override val ctrlOrMeta: Boolean,
      override val shift: Boolean,
      override val alt: Boolean,
  ) extends DesktopKeyCombination

  case class SpecialKey(
      specialKeyType: SpecialKeyType,
      override val ctrlOrMeta: Boolean,
      override val shift: Boolean,
      override val alt: Boolean,
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
