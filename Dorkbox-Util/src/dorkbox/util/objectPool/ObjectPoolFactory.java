/*
 * Copyright 2014 dorkbox, llc
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
package dorkbox.util.objectPool;

import dorkbox.util.Sys;

public class ObjectPoolFactory {

    private ObjectPoolFactory() {
    }

    /**
     * Creates a pool of the specified size
     */
    public static <T> ObjectPool<T> create(PoolableObject<T> poolableObject, int size) {
        if (Sys.isAndroid) {
            // unfortunately, unsafe is not available in android
            SlowObjectPool<T> slowObjectPool = new SlowObjectPool<T>(poolableObject, size);
            return slowObjectPool;
        } else {
            // here we use FAST (via UNSAFE) one!
            FastObjectPool<T> fastObjectPool = new FastObjectPool<T>(poolableObject, size);
            return fastObjectPool;
        }
    }
}