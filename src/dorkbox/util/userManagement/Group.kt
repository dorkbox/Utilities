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
package dorkbox.util.userManagement

import java.io.Serializable
import java.util.*

// TODO: this class needs to save itself to the database on changes
/**
 * Pluggable group object for user management
 */
class Group internal constructor(val name: String, val uuid: UUID?) : Serializable {
    private val users: MutableSet<UUID?> = HashSet<UUID?>()

    constructor(name: String) : this(name, UserManagement.UUID_GENERATOR.generate())

    @Synchronized
    fun addUser(user: UUID?) {
        users.add(user)
    }

    @Synchronized
    fun removeUser(user: UUID?) {
        users.remove(user)
    }

    @Synchronized
    fun getUsers(): MutableCollection<UUID?> {
        return Collections.unmodifiableCollection<UUID?>(users)
    }

    @get:Synchronized
    val isEmpty: Boolean
        get() = users.isEmpty()

    @Synchronized
    fun remove() {
        // UserManagement.Groups.remove(this);

        // for (User user : users) {
        //     user.removeGroup(this);
        // }

        users.clear()
    }

    override fun toString(): String {
        return "Group '$name'"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val group = other as Group

        return if (uuid != null) (uuid == group.uuid) else group.uuid == null
    }

    override fun hashCode(): Int {
        return uuid?.hashCode() ?: 0
    }
}
