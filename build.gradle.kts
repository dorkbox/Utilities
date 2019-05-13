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

import java.time.Instant
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
//////
////// TESTING : local maven repo <PUBLISHING - publishToMavenLocal>
//////
////// RELEASE : sonatype / maven central, <PUBLISHING - publish> then <RELEASE - closeAndReleaseRepository>
///////////////////////////////

println("\tGradle ${project.gradle.gradleVersion} on Java ${JavaVersion.current()}")

plugins {
    java
    signing
    `maven-publish`

    // close and release on sonatype
    id("de.marcphilipp.nexus-publish") version "0.2.0"
    id("io.codearte.nexus-staging") version "0.20.0"

    id("com.dorkbox.CrossCompile") version "1.0.1"
    id("com.dorkbox.Licensing") version "1.4"
    id("com.dorkbox.VersionUpdate") version "1.4.1"
    id("com.dorkbox.GradleUtils") version "1.0"

    kotlin("jvm") version "1.3.11"
}

object Extras {
    // set for the project
    const val description = "Utilities for use within Java projects"
    const val group = "com.dorkbox"
    const val version = "1.1"

    // set as project.ext
    const val name = "Utilities"
    const val id = "Utilities"
    const val vendor = "Dorkbox LLC"
    const val url = "https://git.dorkbox.com/dorkbox/Utilities"
    val buildDate = Instant.now().toString()

    val JAVA_VERSION = JavaVersion.VERSION_1_6.toString()

    var sonatypeUserName = ""
    var sonatypePassword = ""
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

    license("MersenneTwisterFast", License.BSD_3) {
        copyright(2003)
        author("Sean Luke")
        author("Michael Lecuyer (portions Copyright 1993")
        url(Extras.url)
    }

    license("FileUtil (code from FilenameUtils.java for normalize + dependencies)", License.APACHE_2) {
        copyright(2013)
        author("The Apache Software Foundation")
        author("Kevin A. Burton")
        author("Scott Sanders")
        author("Daniel Rall")
        author("Christoph.Reck")
        author("Peter Donald")
        author("Jeff Turner")
        author("Matthew Hawthorne")
        author("Martin Cooper")
        author("Jeremias Maerki")
        author("Stephen Colebourne")
        url(Extras.url)
        url("http://commons.apache.org/proper/commons-io/")
    }

    license("FastThreadLocal", License.BSD_3) {
        copyright(2014)
        author("Lightweight Java Game Library Project")
        author("Riven")
        url(Extras.url)
        url("https://github.com/LWJGL/lwjgl3/blob/5819c9123222f6ce51f208e022cb907091dd8023/modules/core/src/main/java/org/lwjgl/system/FastThreadLocal.java")
        url("https://github.com/riven8192/LibStruct/blob/master/src/net/indiespot/struct/runtime/FastThreadLocal.java")
    }

    license("Base64Fast", License.BSD_3) {
        copyright(2004)
        author("Mikael Grev, MiG InfoCom AB. (base64@miginfocom.com)")
        url(Extras.url)
        url("http://migbase64.sourceforge.net/")
    }

    license("BCrypt", License.BSD_2) {
        copyright(2006)
        author("Damien Miller (djm@mindrot.org)")
        note("GWT modified version")
        url(Extras.url)
        url("http://www.mindrot.org/projects/jBCrypt")
    }

    license("Bias, BinarySearch", License.MIT) {
        copyright(2013)
        author("Tim Boudreau")
        url(Extras.url)
        url("https://github.com/timboudreau/util")
    }

    license("ConcurrentEntry", License.APACHE_2) {
        copyright(2016)
        author("bennidi")
        author("dorkbox")
        url(Extras.url)
    }

    license("Byte Utils (UByte, UInteger, ULong, Unsigned, UNumber, UShort)", License.APACHE_2) {
        copyright(2017)
        author("Data Geekery GmbH (http://www.datageekery.com)")
        author("Lukas Eder")
        author("Ed Schaller")
        author("Jens Nerche")
        author("Ivan Sokolov")
        url(Extras.url)
        url("https://github.com/jOOQ/jOOQ/tree/master/jOOQ/src/main/java/org/jooq/types")
    }

    license("Collection Utilities (Array, ArrayMap, BooleanArray, ByteArray, CharArray, FloatArray, IdentityMap, IntArray, IntFloatMap, IntIntMap, IntMap, IntSet, LongArray, LongMap, ObjectFloatMap, ObjectIntMap, ObjectMap, ObjectSet, OrderedMap, OrderedSet)", License.APACHE_2) {
        copyright(2011)
        author("LibGDX")
        author("Mario Zechner (badlogicgames@gmail.com)")
        author("Nathan Sweet (nathan.sweet@gmail.com)")
        url(Extras.url)
        url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
    }

    license("Predicate", License.APACHE_2) {
        copyright(2011)
        author("LibGDX")
        author("Mario Zechner (badlogicgames@gmail.com)")
        author("Nathan Sweet (nathan.sweet@gmail.com)")
        author("xoppa")
        url(Extras.url)
        url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
    }

    license("Select, QuickSelect", License.APACHE_2) {
        copyright(2011)
        author("LibGDX")
        author("Mario Zechner (badlogicgames@gmail.com)")
        author("Nathan Sweet (nathan.sweet@gmail.com)")
        author("Jon Renner")
        url(Extras.url)
        url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
    }

    license("TimSort, ComparableTimSort", License.APACHE_2) {
        copyright(2008)
        author("The Android Open Source Project")
        url(Extras.url)
        url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
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

        attributes["Automatic-Module-Name"] = Extras.id
    }
}

tasks.compileJava.get().apply {
    println("\tCompiling classes to Java $sourceCompatibility")
}


dependencies {
    val bcVersion = "1.60"
    val jnaVersion = "4.5.2"
    implementation("org.slf4j:slf4j-api:1.7.25")

    implementation("com.github.jponge:lzma-java:1.3")
    implementation("com.fasterxml.uuid:java-uuid-generator:3.1.5")

//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

    implementation("com.esotericsoftware:kryo:5.0.0-RC2")
//    api("com.esotericsoftware:kryo:4.0.2")
//    api("de.javakaffee:kryo-serializers:0.45")

    implementation("io.netty:netty-all:4.1.24.Final")

    implementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bctls-jdk15on:$bcVersion")

    implementation("org.lwjgl:lwjgl-xxhash:3.2.0")
    implementation("org.javassist:javassist:3.23.0-GA")
    implementation("com.dorkbox:ShellExecutor:1.1")

    implementation("net.jodah:typetools:0.6.1")


    implementation("net.java.dev.jna:jna:$jnaVersion")
    implementation("net.java.dev.jna:jna-platform:$jnaVersion")

    runtime("com.koloboke:koloboke-impl-jdk8:1.0.0")

    // unit testing
    testCompile("junit:junit:4.12")
    testRuntime("ch.qos.logback:logback-classic:1.1.6")
}


///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
//////
////// TESTING : local maven repo <PUBLISHING - publishToMavenLocal>
//////
////// RELEASE : sonatype / maven central, <PUBLISHING - publish> then <RELEASE - closeAndReleaseRepository>
///////////////////////////////
val sourceJar = task<Jar>("sourceJar") {
    description = "Creates a JAR that contains the source code."

    from(sourceSets["main"].java)

    archiveClassifier.set("sources")
}

val javaDocJar = task<Jar>("javaDocJar") {
    description = "Creates a JAR that contains the javadocs."

    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Extras.group
            artifactId = Extras.id
            version = Extras.version

            from(components["java"])

            artifact(sourceJar)
            artifact(javaDocJar)

            pom {
                name.set(Extras.name)
                description.set(Extras.description)
                url.set(Extras.url)

                issueManagement {
                    url.set("${Extras.url}/issues")
                    system.set("Gitea Issues")
                }
                organization {
                    name.set(Extras.vendor)
                    url.set("https://dorkbox.com")
                }
                developers {
                    developer {
                        id.set("dorkbox")
                        name.set(Extras.vendor)
                        email.set("email@dorkbox.com")
                    }
                }
                scm {
                    url.set(Extras.url)
                    connection.set("scm:${Extras.url}.git")
                }
            }

        }
    }


    repositories {
        maven {
            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = Extras.sonatypeUserName
                password = Extras.sonatypePassword
            }
        }
    }


    tasks.withType<PublishToMavenRepository> {
        onlyIf {
            publication == publishing.publications["maven"] && repository == publishing.repositories["maven"]
        }
    }

    tasks.withType<PublishToMavenLocal> {
        onlyIf {
            publication == publishing.publications["maven"]
        }
    }

    // output the release URL in the console
    tasks["releaseRepository"].doLast {
        val url = "https://oss.sonatype.org/content/repositories/releases/"
        val projectName = Extras.group.replace('.', '/')
        val name = Extras.name
        val version = Extras.version

        println("Maven URL: $url$projectName/$name/$version/")
    }
}

nexusPublishing {
    packageGroup.set("com.dorkbox")
    username.set(Extras.sonatypeUserName)
    password.set(Extras.sonatypePassword)
}

nexusStaging {
    username = Extras.sonatypeUserName
    password = Extras.sonatypePassword
}

signing {
    sign(publishing.publications["maven"])
}

task<Task>("publishAndRelease") {
    group = "publish and release"
    dependsOn(tasks["publishToNexus"], tasks["closeAndReleaseRepository"])
}
