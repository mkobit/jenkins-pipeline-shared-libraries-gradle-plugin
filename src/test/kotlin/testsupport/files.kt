package testsupport

import com.google.common.io.Resources
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun File.writeRelativeFile(vararg parentDirectory: String, fileName: String, content: () -> String) {
  val parentPath = Paths.get(this.absolutePath, *parentDirectory)
  Files.createDirectories(parentPath)
  Files.write(parentPath.resolve(fileName), content.invoke().toByteArray(Charsets.UTF_8))
}

fun resourceText(resourceName: String): String {
  val resource = Resources.getResource(resourceName)
  return Resources.toString(resource, Charsets.UTF_8)
}
