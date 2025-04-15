package testsupport.testkit.runner

import java.io.File

/**
 * A singleton to be used with the DSL methods for a file that means "use the existing content".
 * This object implements CharSequence but all methods throw UnsupportedOperationException.
 * It's meant to be used as a marker rather than an actual string.
 */
object Original : CharSequence {
  // These methods throw exceptions as Original is a marker object, not meant to be used as a string
  override val length: Int
    get() = throw UnsupportedOperationException("Cannot access length from ${this::class.java.canonicalName}")

  override fun get(index: Int): Char {
    throw UnsupportedOperationException("Cannot call get from ${this::class.java.canonicalName}")
  }

  override fun subSequence(
    startIndex: Int,
    endIndex: Int
  ): CharSequence {
    throw UnsupportedOperationException("Cannot call subSequence from ${this::class.java.canonicalName}")
  }

  override fun toString(): String {
    return "Original"
  }
}

/**
 * Allows using string paths as functions to create files in test setups.
 * Example: "build.gradle"("plugins { id 'com.mkobit.jenkins.pipelines' }")
 */
operator fun String.invoke(content: CharSequence): File {
  val file = File(this)
  file.parentFile?.mkdirs()
  file.writeText(content.toString())
  return file
}

// Overload that allows empty content
operator fun String.invoke(): File {
  return this("")
}

/**
 * Operator that allows using a string as a file path with content and a configuration block.
 * Used in test DSLs for creating and manipulating files.
 */
operator fun String.invoke(
  content: CharSequence = "",
  block: File.() -> Unit = {}
): File {
  val file = File(this)
  file.parentFile?.mkdirs()

  if (content !== Original) {
    file.writeText(content.toString())
  }

  file.block()
  return file
}

/**
 * Appends a newline character to the file.
 */
fun File.appendNewline() {
  appendText("\n")
}

/**
 * Appends the given text to the file.
 */
fun File.append(text: String) {
  appendText(text)
}
