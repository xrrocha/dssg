package dssg

import java.io.File
import scala.util.{Failure, Success}

object Main:
  def main(args: Array[String]): Unit =

    val (builderMapperResult, inputFilename, outputFilename) =
      args.length match
        case 2 =>
          (Success(Seq.empty), args(0), args(1))
        case 3 =>
          (BuilderMapper.fromConfigFile(args(0)), args(1), args(2))
        case _ =>
          error("Usage: dssg [-n | --no-delete] [configuration-file] input-directory output-directory")

    val builderMappers = builderMapperResult match
      case Success(builderMappers) => BuilderMapper.DefaultMappers ++ builderMappers // Allow user overrides
      case Failure(exception) => error(s"Error processing configuration ${exception.getMessage}")

    val inputDirectory = File(inputFilename)
    expect(inputDirectory.isDirectory && inputDirectory.canRead, s"Can't access input directory $inputDirectory")

    val outputDirectory = File(outputFilename)
    expect(outputDirectory.exists() || outputDirectory.mkdirs(), s"Can't create output directory: $outputDirectory")
    expect(outputDirectory.isDirectory && inputDirectory.canWrite, s"Can't access output directory $inputDirectory")

    val traverser = Traverser(BuilderMapper.DefaultMappers ++ builderMappers) // Allow user overrides
    val plan = traverser.traverse(inputDirectory, outputDirectory)

    plan.foreach { action =>
      log(action)
      action.execute()
    }

    println(s"${plan.size} actions applied")

  def expect(condition: Boolean, message: String) = if !condition then error(message)

  def error(message: String, exitCode: Int = 1) =
    log(message)
    sys.exit(exitCode)
