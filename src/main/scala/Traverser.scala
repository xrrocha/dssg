package dssg

import java.io.File

class Traverser(builderMappers: Seq[BuilderMapper]):

  private val builderMappersByInputExtension: Map[String, BuilderMapper] =
    builderMappers
      .flatMap(bm => bm.inputExtensions.map((_, bm)))
      .groupBy(_._1).view.mapValues(_.map(_._2).last) // Last allows for override
      .toMap

  private val builderMappersByOutputExtension: Map[String, Seq[BuilderMapper]] =
    builderMappers
      .flatMap(bm => bm.outputExtensions.map((_, bm)))
      .groupBy(_._1).view.mapValues(_.map(_._2))
      .toMap

  def traverse(inputDirectory: File, outputDirectory: File): Seq[Action] =
    val inputFiles = inputDirectory.listFiles()
    val outputFiles = inputFiles.map(inputFile => File(outputDirectory, inputFile.getName))
    inputFiles.zip(outputFiles).foldLeft(Seq[Action]()) { (accumActions, files) =>

      val createActions = files match
        case (inputDir, outputDir) if inputDir.isDirectory && outputDir.isDirectory =>
          traverse(inputDir, outputDir)
        case (inputDir, outputFile) if inputDir.isDirectory && outputFile.isFile =>
          Seq(Delete(outputFile), Mkdir(outputFile)) ++ traverse(inputDir, outputFile)
        case (inputDir, outputFile) if inputDir.isDirectory && !outputFile.exists() =>
          Mkdir(outputFile) +: traverse(inputDir, outputFile)
        case (inputFile, outputFile) if inputFile.isFile =>
          val deleteAction = if outputFile.isDirectory then Some(Delete(outputFile)) else None
          def addActionIfNeed(file: File, action: Action) =
            if !file.exists() || file.isDirectory || inputFile.lastModified() > file.lastModified() then
              Some(action)
            else
              None
          val createAction =
            inputFile.splitByExtension match
              case (_, None) =>
                addActionIfNeed(outputFile, Copy(inputFile, outputFile))
              case (baseName, Some(extension)) =>
                builderMappersByInputExtension.get(extension) match
                  case None =>
                    addActionIfNeed(outputFile, Copy(inputFile, outputFile))
                  case Some(builderMapper) =>
                    val targetFile = File(outputDirectory, s"$baseName.${builderMapper.outputExtensions.head}")
                    addActionIfNeed(targetFile, Build(inputFile, targetFile, builderMapper.builder))
          end createAction
          Seq(deleteAction, createAction).filter(_.isDefined).map(_.get)
      end createActions

      val deleteActions =
        if !outputDirectory.exists() then
          Seq.empty[Action]
        else
          outputDirectory.listFiles()
            .map(outputFile => (outputFile, File(inputDirectory, outputFile.getName)))
            .filterNot((outputFile, inputFile) =>
              (outputFile.isFile && inputFile.isFile) || (outputFile.isDirectory && inputFile.isDirectory))
            .map { (outputFile, inputFile) =>
              if outputFile.isDirectory then 
                (outputFile, inputFile)
              else
                outputFile.splitByExtension match
                  case (_, None) => (outputFile, inputFile)
                  case (baseName, Some(extension)) =>
                    builderMappersByOutputExtension.get(extension) match
                      case None => (outputFile, inputFile)
                      case Some(builderMappers) =>
                        val sourceFile =
                          builderMappers
                            .flatMap(bm => bm.inputExtensions.map(ie => File(inputDirectory, s"$baseName.$ie")))
                            .find(_.exists())
                            .getOrElse(File(inputDirectory, s"$baseName.${builderMappers.head.inputExtensions.head}"))
                        (outputFile, sourceFile)
            }
            .filterNot((_, inputFile) => inputFile.exists())
            .map((outputFile, _) => Delete(outputFile))
            .toSeq
      end deleteActions

      accumActions ++ createActions ++ deleteActions
    }
  end traverse
