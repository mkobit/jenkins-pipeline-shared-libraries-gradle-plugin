package testsupport.kotest

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jvnet.hudson.test.JenkinsRule
import org.jvnet.hudson.test.fixtures.JenkinsSessionFixture

abstract class JenkinsFunSpec(
    body: JenkinsFunSpec.() -> Unit,
) : FunSpec() {
    private val fixture = JenkinsSessionFixture()

    suspend fun jenkins(block: (JenkinsRule) -> Unit) =
        withContext(Dispatchers.IO) {
            fixture.then(block)
        }

    init {
        beforeTest {
            val className = this::class.qualifiedName ?: this::class.java.name
            fixture.setUp(className, "integration")
        }
        afterTest {
            fixture.tearDown()
        }
        body()
    }
}
