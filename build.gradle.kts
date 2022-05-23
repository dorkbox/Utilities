/*
 * Copyright 2021 dorkbox, llc
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

plugins {
    id("com.dorkbox.GradleUtils") version "2.16"
    id("com.dorkbox.Licensing") version "2.12"
    id("com.dorkbox.VersionUpdate") version "2.4"
    id("com.dorkbox.GradlePublish") version "1.12"

    kotlin("jvm") version "1.6.10"
}

object Extras {
    // set for the project
    const val description = "Utilities for use within Java projects"
    const val group = "com.dorkbox"
    const val version = "1.25"

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
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)

licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        author(Extras.vendor)
        url(Extras.url)


        extra("MersenneTwisterFast", License.BSD_3) {
            url(Extras.url)
            copyright(2003)
            author("Sean Luke")
            author("Michael Lecuyer (portions Copyright 1993")
        }
        extra("FileUtil (code from FilenameUtils.java for normalize + dependencies)", License.APACHE_2) {
            url(Extras.url)
            url("http://commons.apache.org/proper/commons-io/")
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
        }
        extra("FastThreadLocal", License.BSD_3) {
            url(Extras.url)
            url("https://github.com/LWJGL/lwjgl3/blob/5819c9123222f6ce51f208e022cb907091dd8023/modules/core/src/main/java/org/lwjgl/system/FastThreadLocal.java")
            url("https://github.com/riven8192/LibStruct/blob/master/src/net/indiespot/struct/runtime/FastThreadLocal.java")
            copyright(2014)
            author("Lightweight Java Game Library Project")
            author("Riven")
        }
        extra("Base64Fast", License.BSD_3) {
            url(Extras.url)
            url("http://migbase64.sourceforge.net/")
            copyright(2004)
            author("Mikael Grev, MiG InfoCom AB. (base64@miginfocom.com)")
        }
        extra("BCrypt", License.BSD_2) {
            url(Extras.url)
            url("http://www.mindrot.org/projects/jBCrypt")
            copyright(2006)
            author("Damien Miller (djm@mindrot.org)")
            note("GWT modified version")
        }
        extra("Modified hex conversion utility methods", License.APACHE_2) {
            url(Extras.url)
            url("https://netty.io")
            copyright(2014)
            author("The Netty Project")
        }
        extra("Retrofit", License.APACHE_2) {
            copyright(2020)
            description("A type-safe HTTP client for Android and Java")
            author("Square, Inc")
            url("https://github.com/square/retrofit")
        }
        extra("Resource Listing", License.APACHE_2) {
            copyright(2017)
            description("Listing the contents of a resource directory")
            author("Greg Briggs")
            url("http://www.uofr.net/~greg/java/get-resource-listing.html")
        }
        extra("CommonUtils", License.APACHE_2) {
            copyright(2017)
            description("Common utility extension functions for kotlin")
            author("Pronghorn Technology LLC")
            author("Dorkbox LLC")
            url("https://www.pronghorn.tech ")
        }
        extra("UrlRewriteFilter", License.BSD_3) {
            description("UrlRewriteFilter is a Java Web Filter for any J2EE compliant web application server")
            url("https://github.com/paultuckey/urlrewritefilter")
            copyright(2022)
            author("Paul Tuckey")
        }
    }
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

// NOTE: compileOnly is used because there are some classes/dependencies that ARE NOT necessary to be included, UNLESS the user
//  is actually using that part of the library. If this happens, they will (or should) already be using the dependency)
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    api("com.dorkbox:Collections:1.0")
    api("com.dorkbox:Executor:3.9")
    api("com.dorkbox:OS:1.0")
    api("com.dorkbox:Updates:1.1")


    val jnaVersion = "5.10.0"
    compileOnly("net.java.dev.jna:jna-jpms:$jnaVersion")
    compileOnly("net.java.dev.jna:jna-platform-jpms:$jnaVersion")

    // https://github.com/cowtowncoder/java-uuid-generator
    // Java UUID class doesn't expose time/location versions, has a flawed compareTo() on 64bit, and is slow. This one is also thread safe.
    api("com.fasterxml.uuid:java-uuid-generator:4.0.1")

    // https://github.com/MicroUtils/kotlin-logging
    api("io.github.microutils:kotlin-logging:2.1.21")
    api("org.slf4j:slf4j-api:1.8.0-beta4")

    api("org.tukaani:xz:1.9")
    compileOnly("com.fasterxml.uuid:java-uuid-generator:4.0.1")

//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

//    compileOnly("com.esotericsoftware:kryo:5.3.0")
//    compileOnly("de.javakaffee:kryo-serializers:0.45")

    compileOnly("io.netty:netty-buffer:4.1.77.Final")

    val bcVersion = "1.70"
    compileOnly("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    compileOnly("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    compileOnly("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    compileOnly("org.bouncycastle:bctls-jdk15on:$bcVersion")

    compileOnly("org.lwjgl:lwjgl-xxhash:3.3.1")

    compileOnly("net.jodah:typetools:0.6.3")


    // testing
    testImplementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bcmail-jdk15on:$bcVersion")
    testImplementation("org.bouncycastle:bctls-jdk15on:$bcVersion")

    testImplementation("com.esotericsoftware:kryo:5.1.0")
    testImplementation("de.javakaffee:kryo-serializers:0.45")

    testImplementation("com.dorkbox:Serializers:2.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.3.0-alpha4")
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
