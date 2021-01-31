/*
 * Copyright 2021 dorkbox, llc
 * Copyright (C) 2014 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
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

package dorkbox

import dorkbox.executor.Executor
import dorkbox.executor.stream.slf4j.Slf4jStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class ProcessExecutorMainTest {
    @Test
    fun testJavaVersionLogInfoAndOutput() {
        // Just expect no errors - don't check the log file itself
        val output = Slf4jStream.asInfo()

        val result = runBlocking {
            Executor()
                .command("java", "-version")
                .redirectOutput(output)
                .enableRead()
                .start()
        }

        val str: String = result.output.utf8()
        println(str)
        Assert.assertFalse(str.isEmpty())
    }
    @Test
    fun testJavaVersionPid() {
        println("PID: " + Executor().command("java", "-version").startBlocking().pid)
    }
}
