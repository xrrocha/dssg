package dssg

import java.io.{File, FileInputStream, FileOutputStream}
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Main:
  def main(args: Array[String]): Unit =
    val (builderMappers, inputFilename, outputFilename) =
      args.length match
        case 2 =>
          (BuilderMapper.DefaultMappers, args(0), args(1))
        case 3 =>
          (BuilderMapper.fromConfigFile(args(0)), args(1), args(2))
        case _ =>
          error("Usage: scala dssg.Main [<configFile>] <inputDirectoryName> <outputDirectoryName>")

    val inputDirectory = File(inputFilename)
    if !(inputDirectory.isDirectory && inputDirectory.canRead) then
      error(s"Can't access input directory ${inputDirectory.getAbsolutePath}")

    val outputDirectory = File(outputFilename)
    if !(outputDirectory.exists() || outputDirectory.mkdirs()) then
      error(s"Can't create output directory: ${outputDirectory.getAbsolutePath}")
    if !(outputDirectory.isDirectory && inputDirectory.canWrite) then
      error(s"Can't access output directory ${inputDirectory.getAbsolutePath}")

    val traverser = Traverser(builderMappers)
    val plan = traverser.traverse(inputDirectory, outputDirectory)
    plan.foreach { action =>
      log(action)
      action.execute()
    }
    println(s"${plan.size} actions applied")

trait Builder:
  def build(inputFile: File, outputFile: File): Unit

class OSCommandBuilder(commandTemplate: (String, String) => String) extends Builder :

  override val toString = s"OSCommandBuilder(${commandTemplate("inputFile", "outputFile")})"

  override def build(inputFile: File, outputFile: File): Unit =
    import scala.sys.process.*
    val commandLine = commandTemplate(inputFile.getAbsolutePath, outputFile.getAbsolutePath)
    val process = commandLine run ProcessIO(_.close(), _.close(), _.close())
    val exitValue = process.exitValue()
    if exitValue != 0 then
      log(s"Warning: exit value $exitValue for ${inputFile.getAbsolutePath}")

case class BuilderMapper(inputExtensions: Seq[String], outputExtension: String, builder: Builder)

object BuilderMapper:

  private val inputRegex = "([^%])%i".r
  private val outputRegex = "([^%])%o".r

  def DefaultMappers = Seq(
    BuilderMapper(Seq("scss"), "css", OSCommandBuilder((in, out) => s"sass $in $out")),
    BuilderMapper(Seq("md"), "html", OSCommandBuilder((in, out) => s"bash -c 'markdown $in > $out'")),
    BuilderMapper(Seq("ad", "adoc"), "html", OSCommandBuilder((in, out) => s"asciidoctor -o $out $in")))

  def fromConfigFile(filename: String): Seq[BuilderMapper] =
    val file = File(filename)
    if !(file.isFile && file.canRead) then
      error(s"Can't access configuration file: ${file.getAbsolutePath}")
    Source.fromFile(file)
      .getLines()
      .zipWithIndex
      .map((line, index) => (line.trim, index + 1))
      .filterNot((line, _) => line.isEmpty || line.startsWith("#"))
      .map { (line, lineNumber) =>
        val fields = line.split("\\s+")
        if fields.length < 3 then
          error(s"Invalid config line: $line")
        val inputExtensions = fields(0).split(",").toSeq
        val outputExtension = fields(1)
        val commandLine = fields.drop(2).mkString(" ")
        if inputRegex.findFirstMatchIn(commandLine).isEmpty then
          error(s"No input file reference in config line #$lineNumber: $line")
        val commandLineTemplate = (inputFilename: String, outputFilename: String) =>
          val inputReplacement = inputRegex.replaceAllIn(commandLine, s"$$1$inputFilename")
          outputRegex.replaceAllIn(inputReplacement, s"$$1$outputFilename")
        BuilderMapper(inputExtensions, outputExtension, OSCommandBuilder(commandLineTemplate))
      }
      .toSeq

class Traverser(builderMappers: Seq[BuilderMapper]):
  private val builderMappersByInputExtension: Map[String, BuilderMapper] =
    builderMappers
      .flatMap(bm => bm.inputExtensions.map((_, bm)))
      .toMap
  private val builderMappersByOutputExtension: Map[String, Seq[BuilderMapper]] =
    builderMappers
      .map(bm => (bm.outputExtension, bm))
      .groupBy(_._1).view.mapValues(_.map(_._2))
      .toMap

  def traverse(inputDirectory: File, outputDirectory: File): Seq[Action] =
    val plan = ArrayBuffer[Action]()
    traverse(inputDirectory, outputDirectory, plan)
    plan.toSeq

  private def traverse(inputDirectory: File, outputDirectory: File, actions: ArrayBuffer[Action]): Unit =

    val inputFiles = inputDirectory.listFiles()
    val outputFiles = inputFiles.map(inputFile => File(outputDirectory, inputFile.getName))
    inputFiles.zip(outputFiles).foreach { (inputFile, outputFile) =>

      (inputFile, outputFile) match

        case (inputDir, outputDir) if inputDir.isDirectory && outputDir.isDirectory =>
          traverse(inputDir, outputDir, actions)

        case (inputDir, outputFile) if inputDir.isDirectory && outputFile.isFile =>
          actions ++= Seq(Delete(outputFile), Mkdir(outputFile))
          traverse(inputDir, outputFile, actions)

        case (inputDir, outputFile) if inputDir.isDirectory && !outputFile.exists() =>
          actions += Mkdir(outputFile)
          traverse(inputDir, outputFile, actions)

        case (inputFile, outputFile) if inputFile.isFile =>

          if outputFile.isDirectory then actions += Delete(outputFile)

          def addActionIfNeed(file: File, action: Action) =
            if !file.exists() || file.isDirectory || inputFile.lastModified() > file.lastModified() then
              actions += action

          inputFile.splitByExtension match

            case (_, None) =>
              addActionIfNeed(outputFile, Copy(inputFile, outputFile))

            case (baseName, Some(extension)) =>
              builderMappersByInputExtension.get(extension) match

                case None =>
                  addActionIfNeed(outputFile, Copy(inputFile, outputFile))

                case Some(builderMapper) =>
                  val targetFile = File(outputDirectory, s"$baseName.${builderMapper.outputExtension}")
                  addActionIfNeed(targetFile, Build(inputFile, targetFile, builderMapper.builder))
    }

    if outputDirectory.exists() then
      outputDirectory.listFiles()
        .map(outputFile => (outputFile, File(inputDirectory, outputFile.getName)))
        .filterNot((outputFile, inputFile) =>
          (outputFile.isFile && inputFile.isFile) || (outputFile.isDirectory && inputFile.isDirectory))
        .map { (outputFile, inputFile) =>
          if outputFile.isDirectory then (outputFile, inputFile)
          else
            outputFile.splitByExtension match
              case (_, None) => (outputFile, inputFile)
              case (baseName, Some(extension)) =>
                builderMappersByOutputExtension.get(extension) match
                  case None => (outputFile, inputFile)
                  case Some(builderMappers) =>
                    val inputFile =
                      builderMappers
                        .flatMap(bm => bm.inputExtensions.map(ie => File(inputDirectory, s"$baseName.$ie")))
                        .find(_.exists())
                        .getOrElse(File(inputDirectory, s"$baseName.${builderMappers.head.inputExtensions.head}"))
                    (outputFile, inputFile)
        }
        .filterNot((_, inputFile) => inputFile.exists())
        .foreach((outputFile, _) => actions += Delete(outputFile))

extension (file: File)
  def splitByExtension: (String, Option[String]) =
    val pos = file.getName.lastIndexOf('.')
    if (pos < 1) (file.getName, None)
    else (file.getName.substring(0, pos), Some(file.getName.substring(pos + 1)))

trait Action:
  def execute(): Unit

case class Copy(from: File, to: File) extends Action :
  override def execute(): Unit =
    val buffer = Array.ofDim[Byte](4096)
    val in = FileInputStream(from)
    val out = FileOutputStream(to)
    LazyList.continually(in.read(buffer))
      .takeWhile(_ > 0)
      .foreach(out.write(buffer, 0, _))

case class Mkdir(dir: File) extends Action :
  override def execute(): Unit = dir.mkdirs()

case class Build(from: File, to: File, builder: Builder) extends Action :
  override def execute(): Unit = builder.build(from, to)

case class Delete(file: File) extends Action :
  override def execute(): Unit = delete(file)

  private def delete(file: File): Unit =
    if file.isDirectory then file.listFiles().foreach(delete)
    if !file.delete() then log(s"Can't delete ${file.getAbsolutePath}")

def error(message: String, exitCode: Int = 1) =
  log(message)
  sys.exit(exitCode)

def log(any: Any) = System.err.println(any.toString)
