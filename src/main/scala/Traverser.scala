package dssg

import java.io.File
import scala.collection.mutable.ArrayBuffer

class Traverser(builderMappers: Seq[BuilderMapper]):

  private val builderMappersByInputExtension: Map[String, BuilderMapper] =
    builderMappers
      .flatMap(bm => bm.inputExtensions.map((_, bm)))
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2).last)
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
