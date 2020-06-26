package testsupport.junit

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

@ExtendWith(MockWebServerExtension::class)
annotation class UseMockServer

private class MockWebServerExtension : BeforeEachCallback, AfterEachCallback, ParameterResolver {
  override fun beforeEach(context: ExtensionContext) {
    getOrCreateMockWebServer(context)
  }

  override fun afterEach(context: ExtensionContext) {
    remoteMockWebServer(context)
  }

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
    parameterContext.parameter.type == MockWebServer::class.java

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
    getOrCreateMockWebServer(extensionContext)

  private fun storeKeyFor(context: ExtensionContext) = ExtensionContext.Namespace.create(
    context.requiredTestClass, context.testInstance, context.testMethod
  )

  private fun getOrCreateMockWebServer(context: ExtensionContext) =
    getStoreFor(context).getOrComputeIfAbsent(storeKeyFor(context), { MockWebServer() }, MockWebServer::class.java)

  private fun remoteMockWebServer(context: ExtensionContext) =
    getStoreFor(context).remove(storeKeyFor(context), MockWebServer::class.java).shutdown()

  private fun getStoreFor(context: ExtensionContext) = context.getStore(namespaceFor(context))

  private fun namespaceFor(context: ExtensionContext): ExtensionContext.Namespace =
    ExtensionContext.Namespace.create(MockWebServerExtension::class, storeKeyFor(context))
}
