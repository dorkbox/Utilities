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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// TODO: this class needs to save itself to the database on changes


public
class User implements Serializable {
    /** Global, unique ID for this user. This is the only thing used to determine equality */
    private final UUID uuid;

    /** Random, per-user salt that is used for secure password hashing */
    private final byte[] salt = new byte[256];

    /**  user name assigned. Equality is determined by the UUID, so depending on use, the name does not have to be unique. */
    protected String name;

    /** This is the groups that this user is a member of */
    private final Set<UUID> groups = new HashSet<UUID>();

    public
    User() {
        uuid = UserManagement.UUID_GENERATOR.generate();
        UserManagement.RANDOM.nextBytes(salt);
    }

    User(final UUID uuid, final byte[] salt) {
        this.uuid = uuid;

        // set the salt
        for (int i = 0, saltLength = salt.length; i < saltLength; i++) {
            this.salt[i] = salt[i];
        }
    }

    /**
     * @return the global, unique ID for this user. This is the only thing used to determine equality
     */
    public final
    UUID getUUID() {
        return uuid;
    }

    /**
     * @return a random, per-user salt that is used for secure password hashing
     */
    public final
    byte[] getSalt() {
        return salt;
    }

    /**
     * @return an unmodifiable set of groups this user is a member of
     */
    public final
    Set<UUID> getGroups() {
        return Collections.unmodifiableSet(groups);
    }


    /**
     * @return the user name assigned. Equality is determined by the UUID, so depending on use, the name does not have to be unique.
     */
    public final
    String getName() {
        return name;
    }

    /**
     * @param name the user name to be assigned. Equality is determined by the UUID, so depending on use, the name does not have to be unique.
     */
    public final
    void setName(final String name) {
        this.name = name;
    }

    @Override
    public final
    boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final User user = (User) o;

        return uuid.equals(user.uuid);
    }

    @Override
    public final
    int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public final
    String toString() {
        return "User {" + uuid + ", '" + name + '\'' + '}';
    }
}
