package com.mkobit.jenkins.pipelines

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

/**
 * Generates source files that allow `JenkinsRule` integration tests to load the project's shared library.
 *
 * The plugin registers this task automatically; consumers do not invoke it directly.
 * The generated files are wired into the `integrationTest` source set:
 *
 * - `LocalLibraryRetriever.java` — a `LibraryRetriever` that copies `src/`, `vars/`, and
 *   `resources/` from the path set in the `test.library.root` system property.
 * - `SharedLibraryAutoRegistrar.java` — an `@Initializer`-annotated class that auto-registers
 *   the library in `GlobalLibraries` at embedded Jenkins startup (generated unless
 *   `sharedLibrary.autoRegisterLibrary = false`).
 * - `META-INF/annotations/hudson.init.Initializer` — the SezPoz index that tells Jenkins which
 *   method carries `@Initializer`; generated directly here instead of via `annotation-indexer`.
 * - `META-INF/hudson.remoting.ClassFilter` — registers `LocalLibraryRetriever` so XStream
 *   can deserialise `LibraryConfiguration` during test setup.
 *
 * When `generateAutoRegistrar = true` (the default), no explicit `GlobalLibraries.get().setLibraries(...)`
 * call is needed in test code — the auto-registrar handles it at Jenkins startup time.
 * Pass an explicit name if you need a different library name than `project.name`:
 *
 * ```java
 * // Explicit registration (only needed when autoRegisterLibrary = false):
 * GlobalLibraries.get().setLibraries(List.of(LocalLibraryRetriever.implicitLibrary()));
 * GlobalLibraries.get().setLibraries(List.of(LocalLibraryRetriever.implicitLibrary("my-lib")));
 * ```
 */
@CacheableTask
abstract class GenerateLocalLibraryFiles : DefaultTask() {
  @get:OutputDirectory
  abstract val javaOutputDir: DirectoryProperty

  @get:OutputDirectory
  abstract val resourcesOutputDir: DirectoryProperty

  /** Content written to `LocalLibraryRetriever.java`; tracked as a task input so a plugin upgrade that changes the template forces regeneration. */
  @get:Input
  abstract val retrieverSource: Property<String>

  /** When `true`, generates `SharedLibraryAutoRegistrar.java` so Jenkins auto-registers the library via `@Initializer`; otherwise deletes it. */
  @get:Input
  abstract val generateAutoRegistrar: Property<Boolean>

  /** Content written to `SharedLibraryAutoRegistrar.java`; tracked as a task input for the same reason as [retrieverSource]. */
  @get:Input
  abstract val autoRegistrarSource: Property<String>

  /** Content written to `META-INF/hudson.remoting.ClassFilter`; tracked as a task input for the same reason. */
  @get:Input
  abstract val classFilterEntry: Property<String>

  init {
    retrieverSource.convention(JAVA_SOURCE)
    generateAutoRegistrar.convention(true)
    autoRegistrarSource.convention(AUTO_REGISTRAR_SOURCE)
    classFilterEntry.convention(CLASS_NAME)
  }

  @TaskAction
  fun generate() {
    val testingPkg =
      javaOutputDir
        .get()
        .asFile
        .toPath()
        .resolve("com/mkobit/jenkins/pipelines/testing")
    testingPkg.createDirectories()
    testingPkg.resolve("LocalLibraryRetriever.java").writeText(retrieverSource.get())

    val autoRegistrar = testingPkg.resolve("SharedLibraryAutoRegistrar.java")
    val annotationsDir = resourcesOutputDir.get().asFile.toPath().resolve("META-INF/annotations")
    val initializerIndex = annotationsDir.resolve("hudson.init.Initializer")
    if (generateAutoRegistrar.get()) {
      autoRegistrar.writeText(autoRegistrarSource.get())
      annotationsDir.createDirectories()
      initializerIndex.writeText("$INITIALIZER_INDEX_ENTRY\n")
    } else {
      autoRegistrar.deleteIfExists()
      initializerIndex.deleteIfExists()
    }

    val classFilter =
      resourcesOutputDir
        .get()
        .asFile
        .toPath()
        .resolve("META-INF/hudson.remoting.ClassFilter")
    classFilter.parent.createDirectories()
    classFilter.writeText(classFilterEntry.get())
  }

  companion object {
    private const val CLASS_NAME = "com.mkobit.jenkins.pipelines.testing.LocalLibraryRetriever"

    // SezPoz format: ClassName#methodName — Jenkins reads @Initializer attributes from the class
    // file at runtime; the index only needs to name the annotated method.
    private const val INITIALIZER_INDEX_ENTRY =
      "com.mkobit.jenkins.pipelines.testing.SharedLibraryAutoRegistrar#registerLibrary"

    private val AUTO_REGISTRAR_SOURCE =
      """
      package com.mkobit.jenkins.pipelines.testing;

      import hudson.init.InitMilestone;
      import hudson.init.Initializer;
      import java.util.List;
      import javax.annotation.processing.Generated;
      import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;

      @Generated("com.mkobit.jenkins.pipelines.GenerateLocalLibraryFiles")
      public final class SharedLibraryAutoRegistrar {

          private SharedLibraryAutoRegistrar() {}

          /**
           * Runs after all Jenkins extensions are loaded. Reads {@code test.library.name} (injected
           * by the shared-library Gradle plugin) and registers the local library in GlobalLibraries
           * so test pipelines can reference it without any explicit setup code.
           *
           * <p>Set system property {@code test.library.auto.register=false} at JVM startup to
           * disable auto-registration for a specific test run (e.g. to test manual registration).
           */
          @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
          public static void registerLibrary() {
              if ("false".equalsIgnoreCase(System.getProperty("test.library.auto.register", "true"))) {
                  return;
              }
              String name = System.getProperty("test.library.name");
              if (name == null) {
                  return;
              }
              GlobalLibraries.get().setLibraries(List.of(LocalLibraryRetriever.implicitLibrary(name)));
          }
      }
      """.trimIndent()

    private val JAVA_SOURCE =
      """
      package com.mkobit.jenkins.pipelines.testing;

      import hudson.FilePath;
      import hudson.model.Run;
      import hudson.model.TaskListener;
      import java.io.File;
      import java.util.Objects;
      import javax.annotation.processing.Generated;
      import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
      import org.jenkinsci.plugins.workflow.libs.LibraryRetriever;

      @Generated("com.mkobit.jenkins.pipelines.GenerateLocalLibraryFiles")
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
              new FilePath(root).copyRecursiveTo("src/**/*.groovy,vars/*.groovy,vars/*.txt,resources/**", null, target);
          }

          @Override
          public void retrieve(String name, String version, FilePath target, Run<?, ?> run, TaskListener listener) throws Exception {
              retrieve(name, version, false, target, run, listener);
          }

          public static LibraryConfiguration implicitLibrary() {
              String name = Objects.requireNonNull(
                  System.getProperty("test.library.name"),
                  "System property test.library.name must be set (injected by the shared-library plugin)");
              return implicitLibrary(name);
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
