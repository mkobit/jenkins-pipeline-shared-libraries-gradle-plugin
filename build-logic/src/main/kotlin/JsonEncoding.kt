internal fun Any?.toJson(): String =
  when (this) {
    is String -> {
      val escaped = replace("""\""", """\\""").replace("\"", """\"""")
      """"$escaped""""
    }
    is Number, is Boolean -> toString()
    is Map<*, *> -> entries.joinToString(",", "{", "}") { (k, v) -> """"$k":${v.toJson()}""" }
    is List<*> -> joinToString(",", "[", "]") { it.toJson() }
    null -> "null"
    else -> """"$this""""
  }
