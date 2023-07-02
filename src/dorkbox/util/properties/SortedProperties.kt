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
package dorkbox.util.properties

import java.util.*

class SortedProperties : Properties() {
    private val compare = Comparator<Any> { o1, o2 ->
        o1.toString().compareTo(o2.toString())
    }

    @Synchronized
    override fun keys(): Enumeration<Any> {
        val keysEnum = super.keys()
        val vector: Vector<Any> = Vector<Any>(this.size)

        while (keysEnum.hasMoreElements()) {
            vector.add(keysEnum.nextElement())
        }

        Collections.sort(vector, compare)
        return vector.elements()
    }

    companion object {
        private const val serialVersionUID = 3988064683926999433L

        /**
         * Gets the version number.
         */
        val version = "1.42"
    }
}
