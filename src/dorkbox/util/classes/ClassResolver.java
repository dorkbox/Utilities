/*
 * Copyright 2015 dorkbox, llc
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
package dorkbox.util.classes;

public abstract class ClassResolver {
    /**
     * A helper class to get the call context. It subclasses SecurityManager to make getClassContext() accessible. An instance of
     * CallerResolver only needs to be created, not installed as an actual security manager.
     */
    private static final class CallerResolver extends SecurityManager {
        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }

    private static final int CALL_CONTEXT_OFFSET = 3; // may need to change if this class is redesigned
    private static final CallerResolver CALLER_RESOLVER;

    static {
        try {
            // This can fail if the current SecurityManager does not allow
            // RuntimePermission ("createSecurityManager"):
            CALLER_RESOLVER = new CallerResolver();
        } catch (SecurityException se) {
            throw new RuntimeException("ClassLoaderResolver: could not create CallerResolver: " + se);
        }
    }

    /**
     * Indexes into the current method call context with a given offset.
     */
    public static Class<?> getCallerClass(final int callerOffset) {
        return CALLER_RESOLVER.getClassContext()[CALL_CONTEXT_OFFSET + callerOffset];
    }
}

