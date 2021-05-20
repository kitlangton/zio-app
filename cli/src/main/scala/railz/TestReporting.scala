package railz

import view.View
import view.View._

object TestReporting {
//  - AssertionSpec
//  - nested access
//    Vigoo did not satisfy startsWithString(Z)
//  Person(Vigoo) did not satisfy hasField("name", _.name, startsWithString(Z))
//  List(Person(Vigoo)) did not satisfy hasFirst(hasField("name", _.name, startsWithString(Z)))
//  `company` = Ziverge(List(Person(Vigoo))) did not satisfy hasField("people", _.people, hasFirst(hasField("name", _.name, startsWithString(Z))))

  val line1 =
    horizontal(
      "Vigoo".blue,
      " did not satisfy ",
      "startsWithString(Z)".cyan
    )
  val line2 =
    horizontal(
      "Person(Vigoo)".blue,
      " != ",
      "hasField(\"name\", _.name, startsWithString(Z))".cyan
    )
  val line3 =
    horizontal(
      "`company` = Ziverge(List(Person(Vigoo))".blue,
      " did not satisfy ",
      "hasField(\"people\", _.people, hasFirst(hasField(\"name\", _.name, startsWithString(Z))))".cyan
    )

  val view =
    vertical(
      "- AssertionSpec".red,
      vertical(
        "- nested access".red,
        vertical(
          line1,
          line2,
          line3
        ).padding(left = 2)
      ).padding(left = 2)
    )

  val newline1 =
    horizontal(
      "name = ".dim,
      "Vigoo".blue
    )

  val newline2 =
    horizontal(
      "head = ".dim,
      "Person(Vigoo)".blue
//      " does not ",
//      "start with ".cyan,
//      "\"Z\"".cyan
    )

  val newline4 =
    horizontal(
      "company = ".dim,
      "Ziverge(List(Person(Vigoo))".blue
//      " did not satisfy ",
//      "hasField(\"people\", _.people, hasFirst(hasField(\"name\", _.name, startsWithString(Z))))".cyan
    )

  val newline3 =
    horizontal(
      "people = ".dim,
      "List(Person(Vigoo)".blue
//      " did not satisfy ",
//      "hasField(\"people\", _.people, hasFirst(hasField(\"name\", _.name, startsWithString(Z))))".cyan
    )

  val next =
    vertical(
      "- AssertionSpec".red,
      vertical(
        "- nested access".red,
        vertical(
          horizontal("company.people.head.name", ".startsWith(\"Z\")".yellow).bold,
          horizontal(
            "Vigoo".blue,
            " does not ",
            "start with ".cyan,
            "\"Z\"".cyan
          ),
          horizontal(
            "at".dim,
            " /Users/kit/code/open-source/zio/test-tests/shared/src/test/scala/zio/test/SmartAssertionSpec.scala:17".blue
          ),
          vertical(
            newline1,
            newline2,
            newline3,
            newline4
          )
        ).padding(left = 2)
      ).padding(left = 2)
    )

  def main(args: Array[String]): Unit = {
    println("")
    println(next.renderNow)
    println("")
  }

}
