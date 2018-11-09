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
plugins {
    id 'java'
    id 'java-library' // give us access to api/implementation differences for building java libraries
    id 'maven'

//    // setup checking for the latest version of a plugin or dependency (and updating the gradle build)
//    id "se.patrikerdes.use-latest-versions" version "0.2.3"
//    id 'com.github.ben-manes.versions' version '0.16.0'
}

// common dependencies configuration
apply from: 'scripts/gradle/utilities.gradle'

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

sourceSets {
    main {
        java {
            setSrcDirs Collections.singletonList('src')
        }
    }
    test {
        java {
            setSrcDirs Collections.singletonList('test')
        }
    }
}


repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    // utilities dependencies compile only (this is so the IDE can compile the util source)
    compileOnly utilDependencies

    // ALSO so tests can run
    testImplementation utilDependencies

    // unit testing
    testCompile 'junit:junit:4.12'
    testRuntime group: 'ch.qos.logback', name: 'logback-classic', version: '1.1.6'
}

tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
    options.incremental = true
    options.fork = true
    options.forkOptions.executable = 'javac'

    // setup compile options. we specifically want to suppress usage of "Unsafe"
    options.compilerArgs += ['-XDignore.symbol.file', '-Xlint:deprecation']
}


/////////////////////////////
////    Gradle Wrapper Configuration.
///  Run this task, then refresh the gradle project
/////////////////////////////
task updateWrapper(type: Wrapper) {
    gradleVersion = '4.10.2'
    distributionUrl = distributionUrl.replace("bin", "all")
    setDistributionType(Wrapper.DistributionType.ALL)
}
