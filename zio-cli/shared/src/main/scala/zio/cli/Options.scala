package zio.cli

import java.nio.file.{ Path => JPath }
import java.time.{
  Instant => JInstant,
  LocalDate => JLocalDate,
  LocalDateTime => JLocalDateTime,
  LocalTime => JLocalTime,
  MonthDay => JMonthDay,
  OffsetDateTime => JOffsetDateTime,
  OffsetTime => JOffsetTime,
  Period => JPeriod,
  Year => JYear,
  YearMonth => JYearMonth,
  ZonedDateTime => JZonedDateTime,
  ZoneOffset => JZoneOffset,
  ZoneId => JZoneId
}

/**
 * A `Flag[A]` models a command-line flag that produces a value of type `A`.
 */
sealed trait Options[+A] { self =>

  def ::[That, A1 >: A](that: Options[That]): Options.Cons[That, A1] =
    Options.Cons(that, self)

  final def helpDoc: List[HelpDoc.Block] = ???

  final def requires[B](that: Options[B], suchThat: B => Boolean = (_: B) => true): Options[A] =
    Options.Requires(self, that, suchThat)

  final def requiresNot[B](that: Options[B], suchThat: B => Boolean = (_: B) => true): Options[A] =
    Options.RequiresNot(self, that, suchThat)
}

object Options {
  // --verbose 3
  final case object Empty extends Options[Unit]

  final case class Single[+A](name: String, aliases: Vector[String], flagType: Options.Type[A]) extends Options[A] {
    import Options.Type._

    def ? : Options[Option[A]] = optional

    def alias(name: String): Options[A] = copy(aliases = aliases :+ name)

    def aliases(names: String*): Options[A] = copy(aliases = aliases ++ names)

    def collect[B](message: String)(f: PartialFunction[A, B]): Options[B] =
      copy(flagType = Map(flagType, (a: A) => f.lift(a).fold[Either[String, B]](Left(message))(Right(_))))

    def optional: Options[Option[A]] = copy(flagType = Optional(flagType))

    def map[B](f: A => B): Options[B] = copy(flagType = Map(flagType, (a: A) => Right(f(a))))

    def mapTry[B](f: A => B): Options[B] =
      copy(flagType = Map(flagType, (a: A) => scala.util.Try(f(a)).toEither.left.map(_.getMessage)))
  }

  final case class Cons[A, B](left: Options[A], right: Options[B]) extends Options[(A, B)]

  final case class Requires[A, B](options: Options[A], target: Options[B], predicate: B => Boolean) extends Options[A]

  final case class RequiresNot[A, B](options: Options[A], target: Options[B], predicate: B => Boolean)
      extends Options[A]

  sealed trait Type[+A]
  object Type {

    final case class Toggle(negationName: Option[String], ifPresent: Boolean) extends Type[Boolean]
    final case class Map[A, B](value: Type[A], f: A => Either[String, B])     extends Type[B]
    final case class Optional[A](value: Type[A])                              extends Type[Option[A]]
    final case class Primitive[A](primType: PrimType[A])                      extends Type[A]
  }

  import Type._

  /**
   * Creates a boolean flag with the specified name, which, if present, will
   * produce the specified constant boolean value.
   */
  def bool(name: String, ifPresent: Boolean, negationName: Option[String] = None): Single[Boolean] =
    Single(name, Vector.empty, Type.Toggle(negationName, ifPresent))

  def file(name: String, exists: Boolean): Single[JPath] =
    Single(name, Vector.empty, Primitive(PrimType.Path(PrimType.PathType.File, exists)))

  def directory(name: String, exists: Boolean): Single[JPath] =
    Single(name, Vector.empty, Primitive(PrimType.Path(PrimType.PathType.Directory, exists)))

  def text(name: String): Single[String] =
    Single(name, Vector.empty, Primitive(PrimType.Text))

  def decimal(name: String): Single[BigDecimal] =
    Single(name, Vector.empty, Primitive(PrimType.Decimal))

  def integer(name: String): Single[BigInt] =
    Single(name, Vector.empty, Primitive(PrimType.Integer))

  def instant(name: String): Single[JInstant] =
    Single(name, Vector.empty, Primitive(PrimType.Instant))

  def localDate(name: String): Single[JLocalDate] =
    Single(name, Vector.empty, Primitive(PrimType.LocalDate))

  def localDateTime(name: String): Single[JLocalDateTime] =
    Single(name, Vector.empty, Primitive(PrimType.LocalDateTime))

  def localTime(name: String): Single[JLocalTime] =
    Single(name, Vector.empty, Primitive(PrimType.LocalTime))

  def monthDay(name: String): Single[JMonthDay] =
    Single(name, Vector.empty, Primitive(PrimType.MonthDay))

  def offsetDateTime(name: String): Single[JOffsetDateTime] =
    Single(name, Vector.empty, Primitive(PrimType.OffsetDateTime))

  def offsetTime(name: String): Single[JOffsetTime] =
    Single(name, Vector.empty, Primitive(PrimType.OffsetTime))

  def period(name: String): Single[JPeriod] =
    Single(name, Vector.empty, Primitive(PrimType.Period))

  def year(name: String): Single[JYear] =
    Single(name, Vector.empty, Primitive(PrimType.Year))

  def yearMonth(name: String): Single[JYearMonth] =
    Single(name, Vector.empty, Primitive(PrimType.YearMonth))

  def zonedDateTime(name: String): Single[JZonedDateTime] =
    Single(name, Vector.empty, Primitive(PrimType.ZonedDateTime))

  def zoneId(name: String): Single[JZoneId] =
    Single(name, Vector.empty, Primitive(PrimType.ZoneId))

  def zoneOffset(name: String): Single[JZoneOffset] =
    Single(name, Vector.empty, Primitive(PrimType.ZoneOffset))

  val empty: Options[Unit] = Empty
}
