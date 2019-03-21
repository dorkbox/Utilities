/*
 * Copyright 2018 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.time.Instant
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

println("\tGradle ${project.gradle.gradleVersion} on Java ${JavaVersion.current()}")

plugins {
    java

    // close and release on sonatype
    id("io.codearte.nexus-staging") version "0.20.0"

    id("com.dorkbox.CrossCompile") version "1.0.1"
    id("com.dorkbox.Licensing") version "1.4"
    id("com.dorkbox.VersionUpdate") version "1.4.1"

    // setup checking for the latest version of a plugin or dependency
    id("com.github.ben-manes.versions") version "0.20.0"

    kotlin("jvm") version "1.3.11"
}

object Extras {
    // set for the project
    const val description = "Linux, MacOS, or Windows (notification/growl/toast/) popups for the desktop for Java 6+"
    const val group = "com.dorkbox"
    const val version = "3.7"

    // set as project.ext
    const val name = "Utilities"
    const val id = "Utilities"
    const val vendor = "Dorkbox LLC"
    const val url = "https://git.dorkbox.com/dorkbox/Utilities"
    val buildDate = Instant.now().toString()

    val JAVA_VERSION = JavaVersion.VERSION_1_6.toString()
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////z
description = Extras.description
group = Extras.group
version = Extras.version

val propsFile = File("$projectDir/../../gradle.properties").normalize()
if (propsFile.canRead()) {
    println("\tLoading custom property data from: [$propsFile]")

    val props = Properties()
    propsFile.inputStream().use {
        props.load(it)
    }

    val extraProperties = Extras::class.declaredMemberProperties.filterIsInstance<KMutableProperty<String>>()
    props.forEach { (k, v) -> run {
        val key = k as String
        val value = v as String

        val member = extraProperties.find { it.name == key }
        if (member != null) {
            member.setter.call(Extras::class.objectInstance, value)
        }
        else {
            project.extra.set(k, v)
        }
    }}
}

licensing {
    license(License.APACHE_2) {
        author(Extras.vendor)
        url(Extras.url)
        note(Extras.description)
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")
        }
    }

    test {
        java {
            setSrcDirs(listOf("test"))

            // want to include java files for the source. 'setSrcDirs' resets includes...
            include("**/*.java")
        }
    }
}

repositories {
    mavenLocal() // this must be first!
    jcenter()
}

///////////////////////////////
//////    Task defaults
///////////////////////////////
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"

    sourceCompatibility = Extras.JAVA_VERSION
    targetCompatibility = Extras.JAVA_VERSION
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = Extras.buildDate
        attributes["Implementation-Vendor"] = Extras.vendor
    }
}

tasks.compileJava.get().apply {
    println("\tCompiling classes to Java $sourceCompatibility")
}


dependencies {
    val bcVersion = "1.60"
    val jnaVersion = "4.5.2"
    api("org.slf4j:slf4j-api:1.7.25")

    api("com.github.jponge:lzma-java:1.3")
    api("com.fasterxml.uuid:java-uuid-generator:3.1.5")

//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

    api("com.esotericsoftware:kryo:4.0.2")
    api("io.netty:netty-all:4.1.24.Final")

    api("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    api("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    api("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    api("org.bouncycastle:bctls-jdk15on:$bcVersion")

    api("org.lwjgl:lwjgl-xxhash:3.2.0")
    api("org.javassist:javassist:3.23.0-GA")
    api("com.dorkbox:ShellExecutor:1.1+")


    api("net.java.dev.jna:jna:$jnaVersion")
    api("net.java.dev.jna:jna-platform:$jnaVersion")
}

///////////////////////////////
/////   Prevent anything other than a release from showing version updates
////  https://github.com/ben-manes/gradle-versions-plugin/blob/master/README.md
///////////////////////////////
tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview")
                        .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                        .any { it.matches(candidate.version) }
                if (rejected) {
                    reject("Release candidate")
                }
            }
        }
    }

    // optional parameters
    checkForGradleUpdate = true
}


///////////////////////////////
//////    Gradle Wrapper Configuration.
/////  Run this task, then refresh the gradle project
///////////////////////////////
val wrapperUpdate by tasks.creating(Wrapper::class) {
    gradleVersion = "5.3"
    distributionUrl = distributionUrl.replace("bin", "all")
}


//plugins {
//    id 'java'
//    id 'java-library' // give us access to api/implementation differences for building java libraries
//    id 'maven'
//
////    // setup checking for the latest version of a plugin or dependency (and updating the gradle build)
////    id "se.patrikerdes.use-latest-versions" version "0.2.3"
////    id 'com.github.ben-manes.versions' version '0.16.0'
//}
//
//// common dependencies configuration
//apply from: 'scripts/gradle/utilities.gradle'
//
//sourceCompatibility = JavaVersion.VERSION_1_8
//targetCompatibility = JavaVersion.VERSION_1_8
//
//sourceSets {
//    main {
//        java {
//            setSrcDirs Collections.singletonList('src')
//        }
//    }
//    test {
//        java {
//            setSrcDirs Collections.singletonList('test')
//        }
//    }
//}
//
//
//repositories {
//    mavenLocal()
//    jcenter()
//}
//
//dependencies {
//    // utilities dependencies compile only (this is so the IDE can compile the util source)
//    compileOnly utilDependencies
//
//    // ALSO so tests can run
//    testImplementation utilDependencies
//
//    // unit testing
//    testCompile 'junit:junit:4.12'
//    testRuntime group: 'ch.qos.logback', name: 'logback-classic', version: '1.1.6'
//}
//
//tasks.withType(JavaCompile) {
//    options.encoding = 'UTF-8'
//    options.incremental = true
//    options.fork = true
//    options.forkOptions.executable = 'javac'
//
//    // setup compile options. we specifically want to suppress usage of "Unsafe"
//    options.compilerArgs += ['-XDignore.symbol.file', '-Xlint:deprecation']
//}
//
//
///////////////////////////////
//////    Gradle Wrapper Configuration.
/////  Run this task, then refresh the gradle project
///////////////////////////////
//task updateWrapper(type: Wrapper) {
//    gradleVersion = '4.10.2'
//    distributionUrl = distributionUrl.replace("bin", "all")
//    setDistributionType(Wrapper.DistributionType.ALL)
//}
