package dssg

import java.io.{File, FileInputStream, FileOutputStream}

trait Action:
  def execute(): Unit

case class Copy(from: File, to: File) extends Action :
  override def execute(): Unit = FileInputStream(from).copyTo(FileOutputStream(to))

case class Mkdir(dir: File) extends Action :
  override def execute(): Unit = dir.mkdirs()

case class Build(from: File, to: File, builder: Builder) extends Action :
  override def execute(): Unit = builder.build(from, to)

case class Delete(file: File) extends Action :
  override def execute(): Unit = delete(file)

  private def delete(file: File): Unit =
    if file.isDirectory then file.listFiles().foreach(delete)
    if !file.delete() then log(s"Can't delete $file")
