package dssg

class BuilderMapperTest extends TestSuite :
  test("Fails on malformed configuration") {
    val badConfig =
      """
        |md          html         pandoc -s -o %o %i
        |         ***notEnoughInputHere***
        |""".stripMargin

    val lines = badConfig.split("\n").toSeq
    val result = intercept[IllegalArgumentException] {
      BuilderMapper.fromLines(lines)
    }
    assert(result.getMessage.contains("Invalid config line #3"))
  }
  test("Fails on missing input reference") {
    val badConfig =
      """
        |md          html         pandoc -s -o %o %i
        |garbageIn   garbageOut   noInputFileSpec -o %o
        |ad,adoc     html         asciidoctor -o %o %i
        |""".stripMargin

    val lines = badConfig.split("\n").toSeq
    val result = intercept[IllegalArgumentException] {
      BuilderMapper.fromLines(lines)
    }
    assert(result.getMessage.contains("Missing file reference(s) in config line #3"))
  }
