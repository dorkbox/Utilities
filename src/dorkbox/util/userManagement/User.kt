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
package dorkbox.util.userManagement

import java.io.Serializable
import java.util.*

// TODO: this class needs to save itself to the database on changes
class User : Serializable {
    /**
     * @return the global, unique ID for this user. This is the only thing used to determine equality
     */
    /** Global, unique ID for this user. This is the only thing used to determine equality  */
    val uUID: UUID

    /**
     * @return a random, per-user salt that is used for secure password hashing
     */
    /** Random, per-user salt that is used for secure password hashing  */
    val salt: ByteArray = ByteArray(256)

    /**
     * @return the user name assigned. Equality is determined by the UUID, so depending on use, the name does not have to be unique.
     */
    /**
     * @param name the user name to be assigned. Equality is determined by the UUID, so depending on use, the name does not have to be unique.
     */
    /**  user name assigned. Equality is determined by the UUID, so depending on use, the name does not have to be unique.  */
    var name: String? = null

    /** This is the groups that this user is a member of  */
    val groups: MutableSet<UUID?> = HashSet<UUID?>()
        /**
         * @return an unmodifiable set of groups this user is a member of
         */
        get() = Collections.unmodifiableSet<UUID?>(field)

    constructor() {
        this.uUID = UserManagement.UUID_GENERATOR.generate()
        UserManagement.RANDOM.nextBytes(salt)
    }

    internal constructor(uuid: UUID, salt: ByteArray) {
        this.uUID = uuid

        // set the salt
        var i = 0
        val saltLength = salt.size
        while (i < saltLength) {
            this.salt[i] = salt[i]
            i++
        }
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val user = o as User

        return this.uUID == user.uUID
    }

    override fun hashCode(): Int {
        return uUID.hashCode()
    }

    override fun toString(): String {
        return "User {" + this.uUID + ", '" + name + '\'' + '}'
    }
}
