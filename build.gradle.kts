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

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All

plugins {
    java

    id("com.dorkbox.GradleUtils") version "1.12"
    id("com.dorkbox.Licensing") version "2.5"
    id("com.dorkbox.VersionUpdate") version "2.0"
    id("com.dorkbox.GradlePublish") version "1.7"
}

object Extras {
    // set for the project
    const val description = "Utilities for use within Java projects"
    const val group = "com.dorkbox"
    const val version = "1.8.3"

    // set as project.ext
    const val name = "Utilities"
    const val id = "Utilities"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Utilities"

    val buildDate = Instant.now().toString()
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.fixIntellijPaths()
GradleUtils.defaultResolutionStrategy()
GradleUtils.compileConfiguration(JavaVersion.VERSION_11)


licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        author(Extras.vendor)
        url(Extras.url)

        extra("MersenneTwisterFast", License.BSD_3) {
            it.url(Extras.url)
            it.copyright(2003)
            it.author("Sean Luke")
            it.author("Michael Lecuyer (portions Copyright 1993")
        }
        extra("FileUtil (code from FilenameUtils.java for normalize + dependencies)", License.APACHE_2) {
            it.url(Extras.url)
            it.url("http://commons.apache.org/proper/commons-io/")
            it.copyright(2013)
            it.author("The Apache Software Foundation")
            it.author("Kevin A. Burton")
            it.author("Scott Sanders")
            it.author("Daniel Rall")
            it.author("Christoph.Reck")
            it.author("Peter Donald")
            it.author("Jeff Turner")
            it.author("Matthew Hawthorne")
            it.author("Martin Cooper")
            it.author("Jeremias Maerki")
            it.author("Stephen Colebourne")
        }
        extra("FastThreadLocal", License.BSD_3) {
            it.url(Extras.url)
            it.url("https://github.com/LWJGL/lwjgl3/blob/5819c9123222f6ce51f208e022cb907091dd8023/modules/core/src/main/java/org/lwjgl/system/FastThreadLocal.java")
            it.url("https://github.com/riven8192/LibStruct/blob/master/src/net/indiespot/struct/runtime/FastThreadLocal.java")
            it.copyright(2014)
            it.author("Lightweight Java Game Library Project")
            it.author("Riven")
        }
        extra("Base64Fast", License.BSD_3) {
            it.url(Extras.url)
            it.url("http://migbase64.sourceforge.net/")
            it.copyright(2004)
            it.author("Mikael Grev, MiG InfoCom AB. (base64@miginfocom.com)")
        }
        extra("BCrypt", License.BSD_2) {
            it.url(Extras.url)
            it.url("http://www.mindrot.org/projects/jBCrypt")
            it.copyright(2006)
            it.author("Damien Miller (djm@mindrot.org)")
            it.note("GWT modified version")
        }
        extra("Bias, BinarySearch", License.MIT) {
            it.url(Extras.url)
            it.url("https://github.com/timboudreau/util")
            it.copyright(2013)
            it.author("Tim Boudreau")
        }
        extra("ConcurrentEntry", License.APACHE_2) {
            it.url(Extras.url)
            it.copyright(2016)
            it.author("bennidi")
            it.author("dorkbox")
        }
        extra("Collection Utilities (Array, ArrayMap, BooleanArray, ByteArray, CharArray, FloatArray, IdentityMap, IntArray, IntFloatMap, IntIntMap, IntMap, IntSet, LongArray, LongMap, ObjectFloatMap, ObjectIntMap, ObjectMap, ObjectSet, OrderedMap, OrderedSet)", License.APACHE_2) {
            it.url(Extras.url)
            it.url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
            it.copyright(2011)
            it.author("LibGDX")
            it.author("Mario Zechner (badlogicgames@gmail.com)")
            it.author("Nathan Sweet (nathan.sweet@gmail.com)")
        }
        extra("Predicate", License.APACHE_2) {
            it.url(Extras.url)
            it.url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
            it.copyright(2011)
            it.author("LibGDX")
            it.author("Mario Zechner (badlogicgames@gmail.com)")
            it.author("Nathan Sweet (nathan.sweet@gmail.com)")
            it.author("xoppa")
        }
        extra("Select, QuickSelect", License.APACHE_2) {
            it.url(Extras.url)
            it.url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
            it.copyright(2011)
            it.author("LibGDX")
            it.author("Mario Zechner (badlogicgames@gmail.com)")
            it.author("Nathan Sweet (nathan.sweet@gmail.com)")
            it.author("Jon Renner")
        }
        extra("TimSort, ComparableTimSort", License.APACHE_2) {
            it.url(Extras.url)
            it.url("https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils")
            it.copyright(2008)
            it.author("The Android Open Source Project")
        }
        extra("Modified hex conversion utility methods", License.APACHE_2) {
            it.url(Extras.url)
            it.url("https://netty.io")
            it.copyright(2014)
            it.author("The Netty Project")
        }
        extra("Retrofit", License.APACHE_2) {
            it.copyright(2020)
            it.description("A type-safe HTTP client for Android and Java")
            it.author("Square, Inc")
            it.url("https://github.com/square/retrofit")
        }
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

// NOTE: compileOnly is used because there are some classes/dependencies that ARE NOT necessary to be included, UNLESS the user
//  is actually using that part of the library. If this happens, they will (or should) already be using the dependency)
dependencies {
    compileOnly("com.dorkbox:Executor:1.1")

    val jnaVersion = "5.6.0"
    compileOnly("net.java.dev.jna:jna:$jnaVersion")
    compileOnly("net.java.dev.jna:jna-platform:$jnaVersion")

    implementation("org.slf4j:slf4j-api:1.7.30")

    implementation("org.tukaani:xz:1.8")
    compileOnly("com.fasterxml.uuid:java-uuid-generator:4.0.1")

//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

//    compileOnly("com.esotericsoftware:kryo:5.0.0-RC8")
//    compileOnly("de.javakaffee:kryo-serializers:0.45")

    compileOnly("io.netty:netty-buffer:4.1.51.Final")

    val bcVersion = "1.66"
    compileOnly("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    compileOnly("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    compileOnly("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    compileOnly("org.bouncycastle:bctls-jdk15on:$bcVersion")

    compileOnly("org.lwjgl:lwjgl-xxhash:3.2.3")

    compileOnly("net.jodah:typetools:0.6.2")

    //  because the eclipse release of SWT is sPecIaL!
    compileOnly(GradleUtils.getSwtMavenId("3.114.100")) {
        isTransitive = false
    }

    // testing
    testImplementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bctls-jdk15on:$bcVersion")

    testImplementation("com.esotericsoftware:kryo:5.0.0-RC8")
    testImplementation("de.javakaffee:kryo-serializers:0.45")

    testImplementation("com.dorkbox:Serializers:1.0")

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
