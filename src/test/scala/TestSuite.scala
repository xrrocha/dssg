package dssg

import scala.language.adhocExtensions

import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should

export org.scalatest.compatible.Assertion

trait TestSuite
  extends AnyFunSuite,
    should.Matchers,
    GivenWhenThen,
    BeforeAndAfterAll,
    BeforeAndAfterEach
