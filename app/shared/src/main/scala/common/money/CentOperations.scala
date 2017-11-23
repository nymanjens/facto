package common.money

import common.GuavaReplacement.DoubleMath.roundToLong

/**
  * Can be mixed in a type that has cents to add some arithmetic operations.
  *
  * @tparam M The concrete type implementing this trait.
  */
trait CentOperations[M <: CentOperations[M]] {

  // **************** Methods to be overridden **************** //
  def cents: Long

  protected def withCents(newCents: Long): M

  protected def validateCentOperation(that: M): Unit

  // **************** Arithmetic operations **************** //
  final def negated: M = withCents(-cents)

  final def +(that: M): M = doCentOperationToSelfType(_ + _)(that)
  final def -(that: M): M = doCentOperationToSelfType(_ - _)(that)
  final def *(number: Long): M = withCents(cents * number)
  final def /(number: Long): M = withCents(roundToLong(cents * 1.0 / number))
  final def ==(that: M): Boolean = doCentOperation(_ == _)(that)
  final def >(that: M): Boolean = doCentOperation(_ > _)(that)
  final def <(that: M): Boolean = doCentOperation(_ < _)(that)
  final def >=(that: M): Boolean = doCentOperation(_ >= _)(that)
  final def <=(that: M): Boolean = doCentOperation(_ <= _)(that)

  // **************** Private helper methods **************** //
  private def doCentOperation[T](operation: (Long, Long) => T)(that: M): T = {
    validateCentOperation(that)
    operation(this.cents, that.cents)
  }

  private def doCentOperationToSelfType(operation: (Long, Long) => Long)(that: M): M =
    withCents(doCentOperation(operation)(that))
}

object CentOperations {

  /**
    * Can be used to easily create a Numeric for a CentOperations subclass.
    *
    * @tparam M The concrete type implementing this trait.
    */
  trait CentOperationsNumeric[M <: CentOperations[M]] extends Numeric[M] {
    override def negate(x: M): M = x.negated
    override def plus(x: M, y: M): M = x + y
    override def minus(x: M, y: M): M = x - y
    override def times(x: M, y: M): M =
      throw new UnsupportedOperationException("Multiplication of CentOperations doesn't make sense.")

    override def toDouble(x: M): Double = x.cents.toDouble
    override def toFloat(x: M): Float = x.cents.toFloat
    override def toInt(x: M): Int = x.cents.toInt
    override def toLong(x: M): Long = x.cents

    override def compare(x: M, y: M): Int = (x.cents - y.cents).signum
  }
}
