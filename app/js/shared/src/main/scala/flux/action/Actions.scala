package flux.action

import flux.action.Actions.AddSongsToPlaylist.Placement
import hydro.flux.action.Action

import scala.collection.immutable.Seq

object Actions {

  // **************** Media-related actions **************** //
  case class AddSongsToPlaylist(songIds: Seq[Long], placement: Placement) extends Action
  object AddSongsToPlaylist {
    sealed trait Placement
    object Placement {
      object AfterCurrentSong extends Placement
      object AtEnd extends Placement
    }
  }
  case class RemoveEntriesFromPlaylist(playlistEntryIds: Seq[Long]) extends Action
}
