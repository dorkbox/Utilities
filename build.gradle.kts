/*
 * Copyright 2026 dorkbox, llc
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

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All

plugins {
    id("com.dorkbox.GradleUtils") version "4.8"
    id("com.dorkbox.Licensing") version "3.1"
    id("com.dorkbox.VersionUpdate") version "3.2"
    id("com.dorkbox.GradlePublish") version "2.2"

    kotlin("jvm") version "2.3.0"
}


GradleUtils.load {
    group = "com.dorkbox"
    id = "Utilities"

    description = "Utilities for use within Java projects"
    name = "Utilities"
    version = "1.48"

    vendor = "Dorkbox LLC"
    vendorUrl = "https://dorkbox.com"

    url = "https://git.dorkbox.com/dorkbox/Utilities"

    issueManagement {
        url = "${url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = vendor
        email = "email@dorkbox.com"
    }
}
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_25)


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


// NOTE: compileOnly is used because there are some classes/dependencies that ARE NOT necessary to be included, UNLESS the user
//  is actually using that part of the library. If this happens, they will (or should) already be using the dependency)
dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    api("com.dorkbox:OS:1.11")
    api("com.dorkbox:Updates:1.1")


    // https://github.com/cowtowncoder/java-uuid-generator
    // Java UUID class doesn't expose time/location versions, has a flawed compareTo() on 64bit, and is slow. This one is also thread safe.
    api("com.fasterxml.uuid:java-uuid-generator:5.2.0")

//    // https://github.com/MicroUtils/kotlin-logging  NO JPMS SUPPORT!
//    api("io.github.microutils:kotlin-logging:3.0.4")
//    api("org.slf4j:slf4j-api:2.0.7")


//    api "com.koloboke:koloboke-api-jdk8:1.0.0"
//    runtime "com.koloboke:koloboke-impl-jdk8:1.0.0"

//    compileOnly("io.netty:netty-buffer:4.1.96.Final")


    testImplementation("com.dorkbox:Executor:3.14")
    testImplementation("junit:junit:4.13.2")
//    testImplementation("ch.qos.logback:logback-classic:1.4.5")
//    implementation(kotlin("stdlib-jdk8"))
}
