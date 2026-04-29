package com.mkobit.jenkins.pipelines

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Generates source files that allow `JenkinsRule` integration tests to load the project's shared library.
 *
 * The plugin registers this task automatically; consumers do not invoke it directly.
 * The generated files are wired into the `integrationTest` source set:
 *
 * - `LocalLibraryRetriever.java` — a `LibraryRetriever` that copies `src/`, `vars/`, and
 *   `resources/` from the path set in the `test.library.root` system property.
 * - `META-INF/hudson.remoting.ClassFilter` — registers `LocalLibraryRetriever` so XStream
 *   can deserialise `LibraryConfiguration` during test setup.
 *
 * Use `LocalLibraryRetriever.implicitLibrary(name)` to load the library in a test:
 *
 * ```java
 * GlobalLibraries.get().setLibraries(
 *     List.of(LocalLibraryRetriever.implicitLibrary("my-lib"))
 * );
 * ```
 */
@CacheableTask
abstract class GenerateLocalLibraryFiles : DefaultTask() {
  @get:OutputDirectory
  abstract val javaOutputDir: DirectoryProperty

  @get:OutputDirectory
  abstract val resourcesOutputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val javaFile =
      javaOutputDir
        .get()
        .file("com/mkobit/jenkins/pipelines/testing/LocalLibraryRetriever.java")
        .asFile
    javaFile.parentFile.mkdirs()
    javaFile.writeText(JAVA_SOURCE)

    val classFilterFile =
      resourcesOutputDir
        .get()
        .file("META-INF/hudson.remoting.ClassFilter")
        .asFile
    classFilterFile.parentFile.mkdirs()
    classFilterFile.writeText(CLASS_NAME)
  }

  companion object {
    private const val CLASS_NAME = "com.mkobit.jenkins.pipelines.testing.LocalLibraryRetriever"

    private val JAVA_SOURCE =
      """
      package com.mkobit.jenkins.pipelines.testing;

      import hudson.FilePath;
      import hudson.model.Run;
      import hudson.model.TaskListener;
      import java.io.File;
      import java.util.Objects;
      import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
      import org.jenkinsci.plugins.workflow.libs.LibraryRetriever;

      public final class LocalLibraryRetriever extends LibraryRetriever {

          private final File root;

          public LocalLibraryRetriever() {
              this(new File(Objects.requireNonNull(
                  System.getProperty("test.library.root"),
                  "System property test.library.root must be set")));
          }

          public LocalLibraryRetriever(File root) {
              this.root = root;
          }

          @Override
          public void retrieve(String name, String version, boolean changelog, FilePath target, Run<?, ?> run, TaskListener listener) throws Exception {
              new FilePath(root).copyRecursiveTo("src/**/*.groovy,vars/*.groovy,vars/*.txt,resources/", null, target);
          }

          @Override
          public void retrieve(String name, String version, FilePath target, Run<?, ?> run, TaskListener listener) throws Exception {
              retrieve(name, version, false, target, run, listener);
          }

          public static LibraryConfiguration implicitLibrary(String name) {
              LibraryConfiguration cfg = new LibraryConfiguration(name, new LocalLibraryRetriever());
              cfg.setImplicit(true);
              cfg.setDefaultVersion("fixed");
              return cfg;
          }
      }
      """.trimIndent()
  }
}
