package hydro.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

import hydro.common.GuavaReplacement.Preconditions.checkNotNull
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Optional
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Required
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.StringMap
import hydro.common.ValidatingYamlParser.ParseResult.ValidationError
import hydro.common.time.LocalDateTime
import hydro.common.time.TimeUtils
import org.yaml.snakeyaml.Yaml

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ClassTag

object ValidatingYamlParser {

  def parse[V](fileContent: String, parsableValue: ParsableValue[V]): V = {
    val result = parsableValue.parse(new Yaml().load(fileContent))

    require(
      result.validationErrors.isEmpty,
      s"""Found validation errors:
         |
         |${result.validationErrors.map(e => s"  - ${e.toErrorString}\n").mkString}
         |""".stripMargin,
    )

    result.maybeValue.get
  }

  trait ParsableValue[V] {
    def parse(yamlValue: Any): ParseResult[V]
  }
  object ParsableValue {
    private def parsePrimitiveValue[V: ClassTag](yamlValue: Any): ParseResult[V] = {
      val clazz = implicitly[ClassTag[V]].runtimeClass
      if (clazz.isInstance(yamlValue)) {
        ParseResult.success(yamlValue.asInstanceOf[V])
      } else {
        ParseResult.onlyError(s"Expected ${clazz.getSimpleName} but found $yamlValue")
      }
    }

    object IntValue extends ParsableValue[Int] {
      override def parse(yamlValue: Any) = {
        yamlValue match {
          case v: java.lang.Integer => ParseResult.success(v.toInt)
          case v                    => parsePrimitiveValue[Int](v)
        }
      }
    }
    object DoubleValue extends ParsableValue[Double] {
      override def parse(yamlValue: Any) = {
        yamlValue match {
          case v: Int               => ParseResult.success(v.toDouble)
          case v: java.lang.Integer => ParseResult.success(v.toDouble)
          case v: java.lang.Double  => ParseResult.success(v.toDouble)
          case v                    => parsePrimitiveValue[Double](v)
        }
      }
    }
    object StringValue extends ParsableValue[String] {
      override def parse(yamlValue: Any) = {
        yamlValue match {
          case v: java.lang.Integer => ParseResult.success(v.toString)
          case v: java.lang.Long    => ParseResult.success(v.toString)
          case v: java.lang.Boolean => ParseResult.success(v.toString)
          case v                    => parsePrimitiveValue[String](v)
        }
      }
    }
    object BooleanValue extends ParsableValue[Boolean] {
      override def parse(yamlValue: Any) = {
        yamlValue match {
          case "true" | "True" | "TRUE" | "yes" | "Yes"  => ParseResult.success(true)
          case "false" | "False" | "FALSE" | "no" | "No" => ParseResult.success(false)
          case v: java.lang.Boolean                      => ParseResult.success[Boolean](v)
          case v                                         => parsePrimitiveValue[Boolean](v)
        }
      }
    }
    object LocalDateTimeValue extends ParsableValue[LocalDateTime] {
      override def parse(yamlValue: Any) = {
        yamlValue match {
          case v: java.util.Date =>
            ParseResult.success(
              LocalDateTime.of(
                java.time.LocalDateTime.ofInstant(v.toInstant, ZoneId.systemDefault()).toLocalDate,
                LocalTime.MIN,
              )
            )
          case v: String =>
            try {
              ParseResult.success(TimeUtils.parseDateString(v))
            } catch {
              case e: Exception => ParseResult.onlyError(e.getMessage)
            }
          case _ =>
            ParseResult.onlyError(
              s"Expected a date in yyyy-mm-dd format, but found $yamlValue of type ${yamlValue.getClass}"
            )
        }
      }
    }

    class WithStringSimplification[V](delegate: ParsableValue[V], stringToDelegateInput: String => Any)
        extends ParsableValue[V] {
      override def parse(yamlValue: Any): ParseResult[V] = {
        yamlValue match {
          case v: java.lang.Integer => delegate.parse(stringToDelegateInput(v.toString))
          case v: java.lang.Long    => delegate.parse(stringToDelegateInput(v.toString))
          case v: java.lang.Boolean => delegate.parse(stringToDelegateInput(v.toString))
          case v: java.lang.String  => delegate.parse(stringToDelegateInput(v.toString))
          case _                    => delegate.parse(yamlValue)
        }
      }
    }
    object WithStringSimplification {
      def apply[V](delegate: ParsableValue[V])(stringToDelegateInput: String => Any): ParsableValue[V] = {
        new WithStringSimplification[V](delegate, stringToDelegateInput)
      }
    }

    class ListParsableValue[V](itemParsableValue: ParsableValue[V], errorPathString: V => String)
        extends ParsableValue[Seq[V]] {
      override final def parse(yamlValue: Any): ParseResult[Seq[V]] = {
        if (yamlValue.isInstanceOf[java.util.List[_]]) {
          val yamlList = yamlValue.asInstanceOf[java.util.List[_]].asScala.toVector

          val validationErrors = mutable.Buffer[ValidationError]()

          val parsedValuesSeq =
            for ((item, index) <- yamlList.zipWithIndex) yield {
              itemParsableValue.parse(item) match {
                case ParseResult(parsedValue, additionalValidationErrors) =>
                  val pathPrefix = parsedValue match {
                    case Some(v) =>
                      errorPathString(v) match {
                        case s if s.length < 20 => s
                        case s                  => s.take(17).trim + "…"
                      }
                    case None => index.toString
                  }
                  validationErrors.append(additionalValidationErrors.map(_.prependPath(pathPrefix)): _*)
                  parsedValue
              }
            }
          ParseResult(Some(parsedValuesSeq.flatten), validationErrors.toVector)
        } else {
          ParseResult.onlyError(s"Expected a list but found $yamlValue")
        }
      }
    }
    object ListParsableValue {
      def apply[V](
          itemParsableValue: ParsableValue[V]
      )(errorPathString: V => String): ListParsableValue[V] = {
        new ListParsableValue[V](itemParsableValue, errorPathString)
      }
    }
    abstract class MapParsableValue[V] extends ParsableValue[V] {
      override final def parse(yamlValue: Any): ParseResult[V] = {
        val maybeYamlMap = yamlValue match {
          case v: Map[_, _]           => Some(v.asInstanceOf[Map[String, _]])
          case v: java.util.Map[_, _] => Some(v.asInstanceOf[java.util.Map[String, _]].asScala)
          case _                      => None
        }
        maybeYamlMap match {
          case Some(yamlMap) =>
            val mapWithParsedValues = mutable.Map[String, Any]()
            val validationErrors = mutable.Buffer[ValidationError]()

            for ((mapKey, mapValue) <- yamlMap) {
              if (supportedKeyValuePairs.contains(mapKey)) {
                supportedKeyValuePairs(mapKey).parsableValue.parse(mapValue) match {
                  case ParseResult(maybeParsedValue, errors) =>
                    validationErrors.append(errors.map(_.prependPath(mapKey)): _*)
                    for (parsedValue <- maybeParsedValue) {
                      mapWithParsedValues.put(mapKey, parsedValue)
                    }
                }
              } else {
                validationErrors.append(ValidationError(s"Unknown field: $mapKey"))
              }
            }

            supportedKeyValuePairs.collect {
              case (mapKey, MaybeRequiredMapValue.Required(_, valueIfInvalid)) if !yamlMap.contains(mapKey) =>
                validationErrors.append(ValidationError(s"Required field: $mapKey"))
                mapWithParsedValues.put(mapKey, valueIfInvalid)
            }
            supportedKeyValuePairs.collect {
              case (mapKey, mapValue) if !yamlMap.contains(mapKey) =>
            }

            val resultValue =
              parseFromParsedMapValues(StringMap(mapWithParsedValues.toMap, supportedKeyValuePairs))

            validationErrors.append(additionalValidationErrors(resultValue).map(e => ValidationError(e)): _*)

            ParseResult(Some(resultValue), validationErrors.toVector)

          case None =>
            ParseResult.onlyError(s"Expected a map but found $yamlValue")
        }
      }

      val supportedKeyValuePairs: Map[String, MaybeRequiredMapValue]
      def parseFromParsedMapValues(map: StringMap): V
      def additionalValidationErrors(parsedValue: V): Seq[String] = Seq()
    }
    object MapParsableValue {
      case class StringMap(
          private val delegate: Map[String, Any],
          private val supportedKeyValuePairs: Map[String, MaybeRequiredMapValue],
      ) {
        def required[V](mapKey: String): V = {
          require(supportedKeyValuePairs(mapKey).isInstanceOf[Required[_]])
          get[V](mapKey) getOrElse supportedKeyValuePairs(mapKey).asInstanceOf[Required[V]].valueIfInvalid
        }
        def optional[V](mapKey: String): Option[V] = {
          require(supportedKeyValuePairs(mapKey).isInstanceOf[Optional])
          get(mapKey)
        }
        def optional[V](mapKey: String, defaultValue: V): V = {
          require(supportedKeyValuePairs(mapKey).isInstanceOf[Optional])
          get[V](mapKey) getOrElse checkNotNull(defaultValue)
        }

        private def get[V](mapKey: String): Option[V] = {
          delegate.get(mapKey).asInstanceOf[Option[V]].map(checkNotNull)
        }
      }

      sealed trait MaybeRequiredMapValue {
        def parsableValue: ParsableValue[_]
      }
      object MaybeRequiredMapValue {
        case class Required[V](override val parsableValue: ParsableValue[V], valueIfInvalid: V)
            extends MaybeRequiredMapValue
        object Required {
          def apply[V](v: ListParsableValue[V]): Required[Seq[V]] = Required(v, valueIfInvalid = Seq())
          def apply(v: IntValue.type): Required[Int] = Required(v, valueIfInvalid = 0)
          def apply(v: StringValue.type): Required[String] = Required(v, valueIfInvalid = "")
          def apply(v: BooleanValue.type): Required[Boolean] = Required(v, valueIfInvalid = false)
          def apply(v: DoubleValue.type): Required[Double] = Required(v, valueIfInvalid = 0.0)
          def apply(v: LocalDateTimeValue.type): Required[LocalDateTime] =
            Required(v, valueIfInvalid = LocalDateTime.MIN)
        }
        case class Optional(override val parsableValue: ParsableValue[_]) extends MaybeRequiredMapValue
      }
    }
  }

  case class ParseResult[+V](
      maybeValue: Option[V],
      validationErrors: Seq[ValidationError] = Seq(),
  ) {
    require(maybeValue.isDefined || validationErrors.nonEmpty, this.toString)
    require(maybeValue.isEmpty || validationErrors.isEmpty, this.toString)

    def map[V2](function: V => V2): ParseResult[V2] = {
      ParseResult(maybeValue.map(function), validationErrors)
    }
    def flatMap[V2](function: V => ParseResult[V2]): ParseResult[V2] = {
      maybeValue match {
        case None        => this.asInstanceOf[ParseResult[V2]]
        case Some(value) => function(value)
      }
    }
  }
  object ParseResult {
    def onlyError[V](validationError: String): ParseResult[V] = {
      ParseResult[V](maybeValue = None, validationErrors = Seq(ValidationError(validationError)))
    }
    def success[V](value: V): ParseResult[V] = {
      ParseResult(Some(value))
    }

    case class ValidationError(error: String, path: String = "") {
      def prependPath(pathPrefix: String): ValidationError = {
        ValidationError(error, path = if (path.nonEmpty) s"$pathPrefix > $path" else pathPrefix)
      }

      def toErrorString: String = {
        if (path.nonEmpty) s"[$path] $error" else error
      }
    }
  }
}
