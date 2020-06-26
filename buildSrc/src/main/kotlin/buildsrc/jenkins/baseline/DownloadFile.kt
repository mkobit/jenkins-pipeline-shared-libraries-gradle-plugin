package buildsrc.jenkins.baseline

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import retrofit2.Retrofit
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

open class DownloadFile @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:Input
  val baseUrl: Property<String> = objectFactory.property()

  @get:Input
  val downloadPath: Property<String> = objectFactory.property()

  @get:Internal
  val upToDateDuration: Property<Duration> = objectFactory.property()

  @get:OutputFile
  val destination: RegularFileProperty = objectFactory.fileProperty()

  init {
    outputs.upToDateWhen {
      val destinationFile = destination.get().asFile
      destinationFile.exists() && upToDateDuration.map {
        val between = Duration.between(
          Instant.ofEpochMilli(destinationFile.lastModified()),
          Instant.now()
        )
        logger.debug("Time between now and update is $between, allowable duration is $it")
        it > between
      }.getOrElse(false)
    }
  }

  @TaskAction
  fun retrieveFile() {
    val retrofit = Retrofit.Builder()
      .baseUrl(baseUrl.get())
      .build()
    val download = retrofit.create(Download::class.java)

    val call = download.downloadFile(downloadPath.get())
    logger.info("Making request to download file at ${call.request().url()}")
    val response = call.execute()
    if (!response.isSuccessful) {
      throw GradleException("Error downloaded file at ${call.request().url()} with code ${response.code()}")
    } else {
      val body = response.body()!!
      logger.info("Storing file from ${call.request().url()} (status ${response.code()}) to ${destination.asFile.get().toPath()}")
      Files.write(destination.asFile.get().toPath(), body.bytes())
    }
  }
}
