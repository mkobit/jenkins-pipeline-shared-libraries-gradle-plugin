package testsupport.io

import com.google.common.io.Resources

fun loadResource(resource: String) =
  Resources.readLines(Resources.getResource(resource), Charsets.UTF_8)
    .joinToString(System.lineSeparator())
