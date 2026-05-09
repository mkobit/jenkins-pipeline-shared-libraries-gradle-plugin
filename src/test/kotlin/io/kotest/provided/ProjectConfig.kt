package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.decoroutinator.DecoroutinatorExtension

class ProjectConfig : AbstractProjectConfig() {
  override val extensions = listOf(DecoroutinatorExtension())
}
