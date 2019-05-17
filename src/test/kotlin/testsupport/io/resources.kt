package testsupport.io

import com.google.common.io.Resources

fun loadResource(resource: String) = Resources.getResource(resource).run {
  Resources.readLines(this, Charsets.UTF_8)
}.joinToString(System.lineSeparator())
