package com.mkobit.jenkins.pipelines

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Objects
import javax.annotation.Generated
import javax.lang.model.element.Modifier

private const val codegenPackage = "com.mkobit.jenkins.pipelines.codegen"

private val generatedAnnotationSpec: AnnotationSpec = AnnotationSpec.builder(Generated::class.java).addMember("value", "{ \$S }", "Shared Library Plugin").build()

internal fun localLibraryAdder(): JavaFile {
  val javaxNonNull = AnnotationSpec.builder(
    ClassName.get("javax.annotation", "Nonnull")
  ).build()

  val nameParam = ParameterSpec.builder(ClassName.get(String::class.java).annotated(javaxNonNull), "name")
    .addModifiers(Modifier.FINAL)
    .build()
  val versionParam = ParameterSpec.builder(ClassName.get(String::class.java).annotated(javaxNonNull), "version")
    .addModifiers(Modifier.FINAL)
    .build()
  val changelogParam = ParameterSpec.builder(TypeName.BOOLEAN, "changelog")
    .addModifiers(Modifier.FINAL)
    .build()
  val targetParam = ParameterSpec.builder(ClassName.get("hudson", "FilePath")
    .annotated(javaxNonNull), "target")
    .addModifiers(Modifier.FINAL)
    .build()
  val runClass = ClassName.get("hudson.model", "Run")
//  val jobClass = ClassName.get("hudson.model", "Job")
//  val runParam = ParameterSpec.builder(ParameterizedTypeName.get(runClass, WildcardTypeName.subtypeOf(jobClass), WildcardTypeName.subtypeOf(runClass)), "run", Modifier.FINAL)
  val runParam = ParameterSpec.builder(ParameterizedTypeName.get(runClass, WildcardTypeName.subtypeOf(Object::class.java), WildcardTypeName.subtypeOf(Object::class.java)), "run", Modifier.FINAL)
    .addAnnotation(javaxNonNull)
    .build()
  val listenerParam = ParameterSpec.builder(ClassName.get("hudson.model", "TaskListener").annotated(javaxNonNull), "listener")
    .addModifiers(Modifier.FINAL)
    .build()

  val typeSpec = TypeSpec.classBuilder("LocalLibraryRetriever")
    .addAnnotation(generatedAnnotationSpec)
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addField(
      FieldSpec.builder(Path::class.java, "localPath", Modifier.FINAL, Modifier.PRIVATE)
        .build()
    ).superclass(ClassName.get("org.jenkinsci.plugins.workflow.libs", "LibraryRetriever"))
    .addMethod(
      MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addStatement("this(\$T.get(\$T.getProperty(\"user.dir\")))", Paths::class.java, System::class.java)
        .build()
    ).addMethod(
      MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Path::class.java, "path", Modifier.FINAL)
        .addStatement("localPath = \$T.requireNonNull(path)", Objects::class.java)
        .build()
  ).addMethod(
    MethodSpec.methodBuilder("retrieve")
      .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
      .addAnnotation(Override::class.java)
      .returns(TypeName.VOID)
      .addParameter(nameParam)
      .addParameter(versionParam)
      .addParameter(changelogParam)
      .addParameter(targetParam)
      .addParameter(runParam)
      .addParameter(listenerParam)
      .addException(Exception::class.java)
      .addStatement("doRetrieve(target, listener)")
      .build()
    ).addMethod(
      MethodSpec.methodBuilder("retrieve")
        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
        .returns(TypeName.VOID)
        .addAnnotation(Override::class.java)
        .addParameter(nameParam)
        .addParameter(versionParam)
        .addParameter(targetParam)
        .addParameter(runParam)
        .addParameter(listenerParam)
        .addException(Exception::class.java)
        .addStatement("doRetrieve(target, listener)")
        .build()
    ).addMethod(
      MethodSpec.methodBuilder("doRetrieve")
        .returns(TypeName.VOID)
        .addModifiers(Modifier.PRIVATE)
        .addParameter(targetParam)
        .addParameter(listenerParam)
        .addException(IOException::class.java)
        .addException(InterruptedException::class.java)
        .addStatement("final FilePath localFilePath = new FilePath(localPath.toFile())")
        .addStatement("listener.getLogger().format(\$S, localPath, target, \$T.lineSeparator())", "Copying from local path %s to workspace path %s%s", ClassName.get(System::class.java))
        .addComment("Exclusion filter copied from SCMSourceRetriever")
        .addStatement("localFilePath.copyRecursiveTo(${'$'}S, null, target)", "src/**/*.groovy,vars/*.groovy,vars/*.txt,resources/")
        .build()
    ).build()

  return JavaFile.builder(codegenPackage, typeSpec)
    .build()
}
