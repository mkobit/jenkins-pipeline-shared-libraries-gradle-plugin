// CI-only init script: forwards -P project properties to the sharedLibrary extension.
// Keeps the example's build.gradle.kts free of findProperty clutter.
//
// SharedLibraryExtension is not on the init-script classpath, so we navigate
// ext → jenkins via reflection; Property<String> is part of the Gradle API and
// is available here directly.
import org.gradle.api.provider.Property

allprojects {
    pluginManager.withPlugin("com.mkobit.jenkins.pipelines.shared-library") {
        val ext = extensions.getByName("sharedLibrary")
        val jenkins = ext.javaClass.getMethod("getJenkins").invoke(ext)!!

        fun configure(propName: String, getter: String) {
            providers.gradleProperty(propName).orNull?.let { value ->
                @Suppress("UNCHECKED_CAST")
                val prop = jenkins.javaClass.getMethod(getter).invoke(jenkins) as Property<String>
                prop.set(value)
            }
        }

        configure("jenkinsVersion", "getVersion")
        configure("jenkinsBomVersion", "getBomVersion")
        configure("jenkinsTestHarnessVersion", "getTestHarnessVersion")
    }
}
