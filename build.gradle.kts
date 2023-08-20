/*
 * Copyright 2023 dorkbox, llc
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

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!

plugins {
    id("com.dorkbox.GradleUtils") version "3.17"
    id("com.dorkbox.Licensing") version "2.24"
    id("com.dorkbox.VersionUpdate") version "2.8"
    id("com.dorkbox.GradlePublish") version "1.18"

    kotlin("jvm") version "1.8.0"
}

object Extras {
    // set for the project
    const val description = "Utilities for use within Java projects"
    const val group = "com.dorkbox"
    const val version = "1.45"

    // set as project.ext
    const val name = "Utilities"
    const val id = "Utilities"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Utilities"
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
        extra("FastThreadLocal", License.BSD_3) {
            url(Extras.url)
            url("https://github.com/LWJGL/lwjgl3/blob/5819c9123222f6ce51f208e022cb907091dd8023/modules/core/src/main/java/org/lwjgl/system/FastThreadLocal.java")
            url("https://github.com/riven8192/LibStruct/blob/master/src/net/indiespot/struct/runtime/FastThreadLocal.java")
            copyright(2014)
            author("Lightweight Java Game Library Project")
            author("Riven")
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
            url("https://www.uofr.net/~greg/java/get-resource-listing.html")
        }
        extra("CommonUtils", License.APACHE_2) {
            copyright(2017)
            description("Common utility extension functions for kotlin")
            author("Pronghorn Technology LLC")
            author("Dorkbox LLC")
            url("https://www.pronghorn.tech ")
        }
        extra("Kotlin Coroutine CountDownLatch", License.APACHE_2) {
            url("https://github.com/Kotlin/kotlinx.coroutines/issues/59")
            url("https://github.com/venkatperi/kotlin-coroutines-lib")
            copyright(2018)
            author("Venkat Peri")
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
        attributes["Implementation-Version"] = GradleUtils.now()
        attributes["Implementation-Vendor"] = Extras.vendor
    }
}

// NOTE: compileOnly is used because there are some classes/dependencies that ARE NOT necessary to be included, UNLESS the user
//  is actually using that part of the library. If this happens, they will (or should) already be using the dependency)
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    api("com.dorkbox:OS:1.8")
    api("com.dorkbox:Updates:1.1")


    // https://github.com/cowtowncoder/java-uuid-generator
    // Java UUID class doesn't expose time/location versions, has a flawed compareTo() on 64bit, and is slow. This one is also thread safe.
    api("com.fasterxml.uuid:java-uuid-generator:4.2.0")

//    // https://github.com/MicroUtils/kotlin-logging  NO JPMS SUPPORT!
//    api("io.github.microutils:kotlin-logging:3.0.4")
//    api("org.slf4j:slf4j-api:2.0.7")


    compileOnly("com.fasterxml.uuid:java-uuid-generator:4.1.0")

//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

//    compileOnly("io.netty:netty-buffer:4.1.96.Final")


    testImplementation("com.dorkbox:Executor:3.13")
    testImplementation("junit:junit:4.13.2")
//    testImplementation("ch.qos.logback:logback-classic:1.4.5")
//    implementation(kotlin("stdlib-jdk8"))
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
