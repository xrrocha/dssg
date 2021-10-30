package dssg

import java.io.{File, InputStream, OutputStream}

extension (file: File)
  def splitByExtension: (String, Option[String]) =
    val pos = file.getName.lastIndexOf('.')
    if (pos < 1) (file.getName, None)
    else (file.getName.substring(0, pos), Some(file.getName.substring(pos + 1)))

extension (inputStream: InputStream)
  def copyTo(outputStream: OutputStream, close: Boolean = true): Unit =
    val buffer = Array.ofDim[Byte](4096)
    try
      LazyList.continually(inputStream.read(buffer))
        .takeWhile(_ > 0)
        .foreach(outputStream.write(buffer, 0, _))
    finally
      if close then outputStream.close()

def log(any: Any) = System.err.println(any.toString)
