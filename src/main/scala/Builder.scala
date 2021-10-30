package dssg

import java.io.{ByteArrayOutputStream, File, InputStream, OutputStream}
import scala.io.Source
import scala.util.Try

trait Builder:
  def build(inputFile: File, outputFile: File): Unit

class OSCommandBuilder(commandTemplate: (String, String) => String) extends Builder :

  override val toString = s"OSCommandBuilder(${commandTemplate("inputFile", "outputFile")})"

  override def build(inputFile: File, outputFile: File): Unit =
    import scala.sys.process.*
    val commandLine = commandTemplate(inputFile.getPath, outputFile.getPath)
    val errorOutputStream = ByteArrayOutputStream()
    val process = commandLine run ProcessIO(_.close(), _.close(), IOStreamCollector(errorOutputStream))
    val exitValue = process.exitValue()
    if exitValue != 0 then
      log(s"Warning: exit value $exitValue for $inputFile")
      log(errorOutputStream.toString)

  class IOStreamCollector(outputStream: OutputStream) extends ((InputStream) => Unit) :
    override def apply(inputStream: InputStream): Unit = inputStream.copyTo(outputStream)

case class BuilderMapper(inputExtensions: Seq[String], outputExtension: String, builder: Builder)

object BuilderMapper:

  val DefaultMappers = Seq(
    BuilderMapper(Seq("scss"), "css", OSCommandBuilder((in, out) => s"sass --no-source-map $in $out")),
    BuilderMapper(Seq("ts"), "js", OSCommandBuilder((in, out) => s"npx swc --out-file $out $in")),
    BuilderMapper(Seq("md"), "html", OSCommandBuilder((in, out) => s"pandoc --standalone --output $out $in")),
    BuilderMapper(Seq("ad", "adoc"), "html", OSCommandBuilder((in, out) => s"asciidoctor --out-file $out $in")))

  private val InputRegex = "([^%])%i".r
  private val OutputRegex = "([^%])%o".r
  private val FieldRegex = """^(\S+)\s+(\S+)\s+([^%].*)$""".r

  def fromConfigFile(filename: String): Try[Seq[BuilderMapper]] = Try {
    val file = File(filename)
    require(file.isFile && file.canRead, s"Can't access configuration file: $file")

    fromLines(Source.fromFile(file).getLines().toSeq)
  }

  def fromLines(lines: Seq[String]) =
    lines
      .zipWithIndex
      .map((line, index) => (line.trim, index + 1))
      .filterNot((line, _) => line.isEmpty || line.startsWith("#"))
      .map { (line, lineNumber) =>
        line match
          case FieldRegex(inputExtensionList, outputExtension, commandLine) =>
            require(InputRegex.findFirstMatchIn(commandLine).isDefined,
              s"No input file reference in config line #$lineNumber: $line")
            val inputExtensions = inputExtensionList.split(",").toSeq
            // TODO Collect offsets here for performance
            val commandLineTemplate = (inputFilename: String, outputFilename: String) =>
              val inputReplacement = InputRegex.replaceAllIn(commandLine, s"$$1$inputFilename")
              OutputRegex.replaceAllIn(inputReplacement, s"$$1$outputFilename")
            BuilderMapper(inputExtensions, outputExtension, OSCommandBuilder(commandLineTemplate))
          case _ =>
            throw IllegalArgumentException(s"Invalid config line #$lineNumber: $line")
      }


