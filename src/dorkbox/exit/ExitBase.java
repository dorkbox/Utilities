/*
 * Copyright 2010 dorkbox, llc
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
package dorkbox.exit;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

//package scope, as we don't want to accidentally let someone "catch" this error.
class ExitBase extends Error {

    private static final long serialVersionUID = 546657685093303326L;

    private final String message;
    private final int exitCode;
    private final String title;

    ExitBase(int exitCode) {
        this(exitCode, null, null);
    }

    ExitBase(int exitCode, String message) {
        this(exitCode, null, message);
    }

    ExitBase(int exitCode, String title, String message) {
        this.exitCode = exitCode;
        this.title = title;
        this.message = message;
    }

    public final String getTitle() {
        return this.title;
    }

    @Override
    public final String getMessage() {
        return this.message;
    }

    public final int getExitCode() {
        return this.exitCode;
    }

    @Override
    public final Object clone() throws java.lang.CloneNotSupportedException {
        throw new java.lang.CloneNotSupportedException();
    }

    public final void writeObject(ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }

    public final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }
}
