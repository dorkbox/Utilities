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
import kotlin.collections.set

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

plugins {
    java

    id("com.dorkbox.GradleUtils") version "1.8"
    id("com.dorkbox.CrossCompile") version "1.0.1"
    id("com.dorkbox.Licensing") version "1.4"
    id("com.dorkbox.VersionUpdate") version "1.4.1"
    id("com.dorkbox.GradlePublish") version "1.2"

    kotlin("jvm") version "1.3.72"
}

object Extras {
    // set for the project
    const val description = "Utilities for use within Java projects"
    const val group = "com.dorkbox"
    const val version = "1.5.2"

    // set as project.ext
    const val name = "Utilities"
    const val id = "Utilities"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Utilities"
    val buildDate = Instant.now().toString()

    val JAVA_VERSION = JavaVersion.VERSION_1_6.toString()
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.fixIntellijPaths()

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


fun getSwtMavenName(): String {
    val currentOS = org.gradle.internal.os.OperatingSystem.current()
    val platform = when {
        currentOS.isWindows -> "win32"
        currentOS.isMacOsX  -> "macosx"
        else                -> "linux"
    }


    var arch = System.getProperty("os.arch")
    arch = when {
        arch.matches(".*64.*".toRegex()) -> "x86_64"
        else                             -> "x86"
    }

    //  because the eclipse release of SWT is abandoned on maven, this MAVEN repo has newer version of SWT,
    //  https://github.com/maven-eclipse/maven-eclipse.github.io   for the website about it
    //  http://maven-eclipse.github.io/maven  for the maven repo
    return "org.eclipse.swt.gtk.$platform.$arch"
}


dependencies {
    api("com.dorkbox:JnaUtilities:1.1")

    val jnaVersion = "5.5.0"
    api("net.java.dev.jna:jna:$jnaVersion")
    api("net.java.dev.jna:jna-platform:$jnaVersion")


    implementation("org.slf4j:slf4j-api:1.7.30")

    implementation("com.github.jponge:lzma-java:1.3")
    implementation("com.fasterxml.uuid:java-uuid-generator:3.3.0")

//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

    implementation("com.esotericsoftware:kryo:5.0.0-RC2")
//    api("com.esotericsoftware:kryo:4.0.2")
//    api("de.javakaffee:kryo-serializers:0.45")

    implementation("io.netty:netty-all:4.1.49.Final")

    val bcVersion = "1.64"
    implementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bctls-jdk15on:$bcVersion")

    implementation("org.lwjgl:lwjgl-xxhash:3.2.3")
    implementation("org.javassist:javassist:3.26.0-GA")
    implementation("com.dorkbox:ShellExecutor:1.1")

    implementation("net.jodah:typetools:0.6.1")


    implementation("com.koloboke:koloboke-impl-jdk8:1.0.0")

    //  because the eclipse release of SWT is abandoned on maven, this repo has a newer version of SWT,
    //  http://maven-eclipse.github.io/maven
    // 4.4 is the oldest version that works with us. We use reflection to access SWT, so we can compile the project without needing SWT
//    compileOnly("org.eclipse.platform:${getSwtMavenName()}:3.113.0") {
////    compileOnly("org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64:3.113.0") {
//        isTransitive = false
//    }

//    compileOnly(group = "org.openjfx", name = "javafx", version = "12", ext = "javafx-base")
//    compileOnly(group = "org.openjfx", name = "javafx", version = "12", ext = "pom")

    // unit testing
    testImplementation("junit:junit:4.13")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
