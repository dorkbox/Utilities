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
package dorkbox.util.userManagement;

import java.io.Serializable;
import java.util.*;

// TODO: this class needs to save itself to the database on changes
/**
 * Plugable group object for user management
 */
public final
class Group implements Serializable {

    private final UUID uuid;
    private String name;

    private Set<UUID> users = new HashSet<UUID>();

    public
    Group(final String name) {
        this(name, UserManagement.UUID_GENERATOR.generate());
    }

    Group(final String name, final UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public
    UUID getUuid() {
        return uuid;
    }

    public
    String getName() {
        return name;
    }

    public synchronized
    void addUser(UUID user) {
        users.add(user);
    }

    public synchronized
    void removeUser(UUID user) {
        users.remove(user);
    }

    public synchronized
    Collection<UUID> getUsers() {
        return Collections.unmodifiableCollection(users);
    }

    public synchronized
    boolean isEmpty() {
        return users.isEmpty();
    }

    public synchronized
    void remove() {
        // UserManagement.Groups.remove(this);

        // for (User user : users) {
        //     user.removeGroup(this);
        // }

        users.clear();
    }

    @Override
    public
    String toString() {
        return "Group '" + name + '\'';
    }

    @Override
    public
    boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Group group = (Group) o;

        return uuid != null ? uuid.equals(group.uuid) : group.uuid == null;
    }

    @Override
    public
    int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
