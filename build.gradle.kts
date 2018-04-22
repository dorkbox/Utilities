import org.gradle.api.internal.HasConvention
import org.gradle.internal.impldep.org.apache.http.client.methods.RequestBuilder.options
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper.srcDir
import java.io.File

plugins {
    java
    maven
     kotlin("jvm")

    // the version is defined in the parent project. Uncomment this if you are working DIRECTLY with Utilities
    // kotlin("jvm") version "1.2.40"
}

apply {
    plugin("java")
}

kotlin.experimental.coroutines = Coroutines.ENABLE

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// make working with sourcesets easier
val sourceSets = java.sourceSets
fun sourceSets(block: SourceSetContainer.() -> Unit) = sourceSets.apply(block)

val SourceSetContainer.main: SourceSet get() = getByName("main")
fun SourceSetContainer.main(block: SourceSet.() -> Unit) = main.apply(block)

val SourceSetContainer.test: SourceSet get() = getByName("test")
fun SourceSetContainer.test(block: SourceSet.() -> Unit) = test.apply(block)

var SourceDirectorySet.sourceDirs: Iterable<File>
    get() = srcDirs
    set(value) {
        setSrcDirs(value)
    }

sourceSets {
    main {
        java {
            sourceDirs = files("src")
        }
    }
    test {
        java {
            sourceDirs = files("test")
        }
    }
}


repositories {
    mavenLocal()
    maven { setUrl("http://repo.maven.apache.org/maven2") }
}

dependencies {
    val bcVersion = "1.59"

    compile(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")
    compile(group = "ch.qos.logback", name = "logback-classic", version = "1.1.6")

    compile(group = "com.github.jponge", name = "lzma-java", version = "1.3")
    compile(group = "com.fasterxml.uuid", name = "java-uuid-generator", version = "3.1.5")

    compile(group = "com.esotericsoftware", name = "kryo", version = "4.0.2")
    compile(group = "io.netty", name = "netty-all", version = "4.1.24.Final")

    compile(group = "org.bouncycastle", name = "bcprov-jdk15on", version = bcVersion)
    compile(group = "org.bouncycastle", name = "bcpg-jdk15on", version = bcVersion)
    compile(group = "org.bouncycastle", name = "bcmail-jdk15on", version = bcVersion)
//    compile(group = "org.bouncycastle", name = "bctls-jdk15on", version = bcVersion)

    compile(group = "org.lwjgl", name = "lwjgl-xxhash", version = "3.1.6")
    compile(group = "org.javassist", name = "javassist", version = "3.21.0-GA")
    compile(group = "com.dorkbox", name = "ShellExecutor", version = "1.1+")

    compile(group = "net.java.dev.jna", name = "jna", version = "4.5.1")
    compile(group = "net.java.dev.jna", name = "jna-platform", version = "4.5.1")

    // unit testing
    testCompile(group = "junit", name = "junit", version = "4.12")
}

tasks.withType<JavaCompile> {
    println("Configuring $name in project ${project.name}...")
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.isFork = true
    options.forkOptions.executable = "javac"

    // setup compile options. we specifically want to suppress usage of "Unsafe"
    options.compilerArgs = listOf("-XDignore.symbol.file", "-Xlint:deprecation")
}


