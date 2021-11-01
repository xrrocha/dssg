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

case class BuilderMapper(inputExtensions: Seq[String], outputExtensions: Seq[String], builder: Builder)

object BuilderMapper:

  private val InputPlaceholder = "%i"
  private val OutputPlaceholder = "%o"
  private val FieldRegex = """(\S+)\s+(\S+)\s+(.*)""".r

  val DefaultMappers =
    val configurationFilename = "configuration.txt"
    val is = getClass.getClassLoader.getResourceAsStream(configurationFilename)
    require(is != null, s"Can't find configuration resource file: $configurationFilename")
    fromLines(Source.fromInputStream(is).getLines().toSeq)

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
          case FieldRegex(inputExtensionList, outputExtensionList, commandLine) =>
            require(commandLine.contains(InputPlaceholder) && commandLine.contains(OutputPlaceholder),
              s"Missing file reference(s) in config line #$lineNumber: $line")
            val inputExtensions = inputExtensionList.split(",").toSeq
            val outputExtensions = outputExtensionList.split(",").toSeq
            val commandLineTemplate = (inputFilename: String, outputFilename: String) =>
              commandLine
                .replace(InputPlaceholder, inputFilename)
                .replace(OutputPlaceholder, outputFilename)
            BuilderMapper(inputExtensions, outputExtensions, OSCommandBuilder(commandLineTemplate))
          case _ =>
            throw IllegalArgumentException(s"Invalid config line #$lineNumber: $line")
      }
