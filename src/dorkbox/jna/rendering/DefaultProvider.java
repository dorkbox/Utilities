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

package dorkbox.jna.rendering;

class DefaultProvider implements Renderer {
    @Override
    public
    boolean isSupported() {
        return true;
    }

    @Override
    public
    ProviderType getType() {
        return ProviderType.NONE;
    }

    @Override
    public
    boolean alreadyRunning() {
        return false;
    }

    @Override
    public
    boolean isEventThread() {
        return false;
    }

    @Override
    public
    int getGtkVersion() {
        return 0;
    }

    @Override
    public
    boolean dispatch(final Runnable runnable) {
        return false;
    }
}
