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
 *   `resources/` from the path set in the `test.library.location` system property.
 *   Additional libraries are injected via contiguous `test.library.N.{name,location,implicit}`
 *   properties. A future iteration may replace the property scheme with a single manifest
 *   file on the test classpath once external library resolution is implemented.
 * - `SharedLibraryAutoRegistrar.java` — an `@Initializer`-annotated class that auto-registers
 *   all libraries in `GlobalLibraries` at embedded Jenkins startup (generated unless
 *   `sharedLibrary.autoRegisterLibrary = false`). Libraries are scanned via contiguous
 *   zero-based indices: `test.library.0.*` is always the project's own library; additional
 *   libraries resolved from Gradle dependencies follow at `test.library.1.*`, `test.library.2.*`, …
 * - `META-INF/services/annotations/hudson.init.Initializer` — the annotation-indexer class list
 *   that Jenkins reads at startup; each listed class is scanned for `@Initializer` methods via
 *   reflection. Generated directly here instead of via the `annotation-indexer` processor.
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
    val annotationsDir =
      resourcesOutputDir
        .get()
        .asFile
        .toPath()
        .resolve("META-INF/services/annotations")
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

    // Jenkins reads META-INF/services/annotations/hudson.init.Initializer at startup, loads each
    // listed class, then scans its methods for @Initializer via reflection (@Retention RUNTIME).
    // Only the class name is needed — no ClassName#method format.
    private const val INITIALIZER_INDEX_ENTRY =
      "com.mkobit.jenkins.pipelines.testing.SharedLibraryAutoRegistrar"

    private val AUTO_REGISTRAR_SOURCE =
      """
      package com.mkobit.jenkins.pipelines.testing;

      import hudson.init.InitMilestone;
      import hudson.init.Initializer;
      import java.io.File;
      import java.util.ArrayList;
      import java.util.List;
      import javax.annotation.processing.Generated;
      import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
      import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;

      @Generated("com.mkobit.jenkins.pipelines.GenerateLocalLibraryFiles")
      public final class SharedLibraryAutoRegistrar {

          private SharedLibraryAutoRegistrar() {}

          /**
           * Runs after all Jenkins extensions are loaded and registers all shared libraries
           * injected by the shared-library Gradle plugin in GlobalLibraries so test pipelines
           * can reference them without any explicit setup code.
           *
           * <p>All libraries use contiguous zero-based indices: the project's own library is
           * always at index 0 ({@code test.library.0.name} / {@code test.library.0.location} /
           * {@code test.library.0.implicit}); additional libraries resolved from Gradle
           * dependencies follow at 1, 2, … The scan stops at the first missing name or location.
           * The plugin always injects contiguous indices, so gaps do not arise in practice.
           *
           * <p>Set system property {@code test.library.auto.register=false} at JVM startup to
           * disable auto-registration for a specific test run (e.g. to test manual registration).
           */
          @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
          public static void registerLibrary() {
              if ("false".equalsIgnoreCase(System.getProperty("test.library.auto.register", "true"))) {
                  return;
              }
              List<LibraryConfiguration> libraries = new ArrayList<>();
              int i = 0;
              while (true) {
                  String name = System.getProperty("test.library." + i + ".name");
                  String location = System.getProperty("test.library." + i + ".location");
                  if (name == null || location == null) break;
                  boolean implicit = !"false".equalsIgnoreCase(System.getProperty("test.library." + i + ".implicit", "true"));
                  libraries.add(makeLibrary(name, location, implicit));
                  i++;
              }
              if (!libraries.isEmpty()) {
                  GlobalLibraries.get().setLibraries(libraries);
              }
          }

          private static LibraryConfiguration makeLibrary(String name, String location, boolean implicit) {
              LibraryConfiguration cfg = new LibraryConfiguration(name, new LocalLibraryRetriever(new File(location)));
              cfg.setImplicit(implicit);
              cfg.setDefaultVersion("fixed");
              return cfg;
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

          private final File location;

          public LocalLibraryRetriever() {
              this(new File(Objects.requireNonNull(
                  System.getProperty("test.library.0.location"),
                  "System property test.library.0.location must be set")));
          }

          public LocalLibraryRetriever(File location) {
              this.location = location;
          }

          @Override
          public void retrieve(String name, String version, boolean changelog, FilePath target, Run<?, ?> run, TaskListener listener) throws Exception {
              new FilePath(location).copyRecursiveTo("src/**/*.groovy,vars/*.groovy,vars/*.txt,resources/**", null, target);
          }

          @Override
          public void retrieve(String name, String version, FilePath target, Run<?, ?> run, TaskListener listener) throws Exception {
              retrieve(name, version, false, target, run, listener);
          }

          public static LibraryConfiguration implicitLibrary() {
              String name = Objects.requireNonNull(
                  System.getProperty("test.library.0.name"),
                  "System property test.library.0.name must be set (injected by the shared-library plugin)");
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
