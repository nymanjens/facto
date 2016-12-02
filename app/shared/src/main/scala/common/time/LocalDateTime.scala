//package common.time
//
//import java.time.{LocalDate, LocalTime}
//import common.Require.requireNonNull
//
///**
//  * Drop-in replacement for java.time.LocalDateTime, which isn't supported by scala.js yet.
//  */
//trait LocalDateTime {
//
//  def toLocalDate: LocalDate
//  def toLocalTime: LocalTime
//  def getYear: Int
//  def getMonth: Month
//}
//
//object LocalDateTime {

//public static final LocalDateTime MIN = LocalDateTime.of(LocalDate.MIN, LocalTime.MIN);
//public static final LocalDateTime MAX = LocalDateTime.of(LocalDate.MAX, LocalTime.MAX);
//
//  def of(localDate: LocalDate, localTime: LocalTime): LocalDateTime = LocalDateTimeImpl(localDate, localTime)
//
//  private case class LocalDateTimeImpl(private val localDate: LocalDate,
//                                       private val localTime: LocalTime) extends LocalDateTime {
//    requireNonNull(localDate, localTime)
//
//    override def toLocalDate: LocalDate = localDate
//    override def toLocalTime: LocalTime = localTime
//  }
//}