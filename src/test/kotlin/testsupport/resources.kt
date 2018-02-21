package testsupport

import com.google.common.io.Resources

fun loadResource(resource: String) = Resources.getResource("jenkins-data/http/gdsl.txt").run {
  Resources.readLines(this, Charsets.UTF_8)
}.joinToString(System.lineSeparator())
