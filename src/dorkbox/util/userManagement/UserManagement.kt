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

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.RandomBasedGenerator
import com.fasterxml.uuid.impl.UUIDUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.*

/**
 * todo: this class should load/save itself to the database
 */
class UserManagement {

    companion object {
        private val logger: Logger? = LoggerFactory.getLogger(UserManagement::class.java.getSimpleName())

        val UUID_GENERATOR: RandomBasedGenerator = Generators.randomBasedGenerator()
        val RANDOM: SecureRandom = SecureRandom()
    }

    val ADMIN: Group

    private val users = mutableMapOf<UUID, User>()
    private val groups = mutableMapOf<UUID, Group>()

    init {
        // the "system/admin/root" group MUST always be all "0"
        val buffer = ByteArray(16)
        var i = 0
        val bufferLength = buffer.size
        while (i < bufferLength) {
            buffer[i] = 0
            i++
        }

        val uuid = UUIDUtil.uuid(buffer)
        ADMIN = Group("System", uuid)

        // the "system/root" group MUST always be all "0"
        groups[uuid] = ADMIN
    }

    fun authenticate(user: String?): User? {
        return null
    }


    /**
    * user/group create/add/remove/get actions
    */
    fun createUser(): User {
        val user = User()
        addUser(user)
        return user
    }

    private fun addUser(user: User) {
        // users.add(user);
    }

    private fun removeUser(user: User) {
        users.remove(user.uUID)
    }

    fun getUsers(): Map<UUID, User> {
        return users.toMap()
    }

    fun getUser(uuid: UUID): User? {
        return users[uuid]
    }

    fun createGroup(groupName: String): Group {
        val group = Group(groupName)
        addGroup(group)
        return group
    }

    fun addGroup(group: Group?) {
        // groups.add(group);
    }

    fun removeGroup(group: Group) {
        if (group !== ADMIN) {
            groups.remove(group.uuid)
        }
    }

    // public
    // Collection<Group> getGroups() {
    //     return Collections.unmodifiableCollection(groups);
    // }
    //
    // /**
    //  * @param user the user to check
    //  * @return true if this user is in the admin group
    //  */
    // public
    // boolean isAdminGroup(final User user) {
    //     return ADMIN.getUsers()
    //                 .contains(user.getUUID());
    // }
    //
    //
    // /**
    //  * check if a user is the admin user
    //  *
    //  * @return false if not admin, or NOT called from the console
    //  */
    // public synchronized
    // boolean isAdminUser(final byte[] userNameHash) {
    //     if (userNameHash == null || userNameHash.length == 0) {
    //         return false;
    //     }
    //
    //     if (!checkAccessNoExit(SystemLoginImpl.class, AdminActions.class, ConsoleServer.class)) {
    //         return false;
    //     }
    //
    //
    //     if (adminUserHash == null) {
    //         Exit.FailedConfiguration("Unable to read admin user from the database. FORCED SHUTDOWN.");
    //         return false;
    //     }
    //     else {
    //         if (Arrays.equals(adminUserHash, userNameHash)) {
    //             return true;
    //         }
    //         else {
    //             AdminActions.this.logger.info("User is not the admin user");
    //             return false; // user not the same
    //         }
    //     }
    // }
    //
    // /**
    //  * Set's the admin user for this server. This user should already exist!
    //  *
    //  * @return errormessage, if there are errors, null otherwise
    //  */
    // public synchronized
    // String setAdminUser(final byte[] userNameHash, final byte[] currentAdminPasswordHash) {
    //     try {
    //         checkAccess(AdminActions.class, ConsoleServer.class);
    //     } catch (SecurityException e) {
    //         Exit.FailedSecurity(e);
    //     }
    //
    //     // only check the password if we have an admin user!
    //     if (adminUserHash != null) {
    //         final ByteArrayWrapper adminUserHashWrap = ByteArrayWrapper.wrap(adminUserHash);
    //         DB_User user = users.get(adminUserHashWrap);
    //         if (user != null) {
    //             if (!Arrays.equals(user.getPasswordHash(), currentAdminPasswordHash)) {
    //                 String mesg = "Incorrect admin password.";
    //                 AdminActions.this.logger.info(mesg);
    //                 return mesg;
    //             }
    //         }
    //         else {
    //             String mesg = "Invalid user!";
    //             AdminActions.this.logger.info(mesg);
    //             return mesg;
    //         }
    //     }
    //
    //     final ByteArrayWrapper userHashWrap = ByteArrayWrapper.wrap(userNameHash);
    //     DB_User user = users.get(userHashWrap);
    //     if (user == null) {
    //         String mesg = "User doesn't exist.";
    //         AdminActions.this.logger.info(mesg);
    //         return mesg;
    //     }
    //
    //     adminUserHash = userNameHash;
    //
    //     // have to always specify what we are saving
    //     storage.put(DatabaseStorage.ADMIN_HASH, adminUserHash);
    //
    //     return null;
    // }
    //
    // /**
    //  * Adds a user to the system. beware userName != usernamehash
    //  *
    //  * @return the error message, null if successful.
    //  */
    // public synchronized
    // String addUser(final String userName, final byte[] userNameHash, final byte[] passwordHash) {
    //     try {
    //         checkAccess(AdminActions.class, ConsoleServer.class);
    //     } catch (SecurityException e) {
    //         Exit.FailedSecurity(e);
    //     }
    //
    //     String validateLogin = Validation.userName(userName);
    //     Logger logger2 = this.logger;
    //     if (validateLogin != null) {
    //         String mesg = "Unable to create user account  ( " + userName + " ).  Reason: " + validateLogin;
    //         if (logger2.isInfoEnabled()) {
    //             logger2.info("{}  Reason: {}", mesg, validateLogin);
    //         }
    //         return mesg;
    //     }
    //
    //     final ByteArrayWrapper userNameHashWrap = ByteArrayWrapper.wrap(userNameHash);
    //
    //     // only once, to save memory
    //     DB_User user = users.get(userNameHashWrap);
    //
    //     if (user != null) {
    //         String mesg = "Unable to create user account ( " + userName + " ).  Reason: User already exists";
    //         if (logger2.isInfoEnabled()) {
    //             logger2.info(mesg);
    //         }
    //         return mesg;
    //     }
    //     else {
    //         user = new DB_User();
    //         user.setName(userName);
    //         user.setNameHash(userNameHash);
    //         user.setPasswordHash(passwordHash);
    //
    //         users.put(userNameHashWrap, user);
    //         // have to always specify what we are saving
    //         storage.put(DatabaseStorage.USERS, users);
    //
    //         if (logger2.isInfoEnabled()) {
    //             logger2.info("Added user ({})", userName);
    //         }
    //         return null; // success!
    //     }
    // }
    //
    // /**
    //  * remove a user + userName hash.
    //  *
    //  * @return true if removed, false if the user could not be removed.
    //  */
    // public synchronized
    // boolean removeUser(final byte[] userNameHash) {
    //     final ByteArrayWrapper userNameHashWrap = ByteArrayWrapper.wrap(userNameHash);
    //     DB_User user = users.get(userNameHashWrap);
    //
    //     Logger logger2 = this.logger;
    //     if (user != null) {
    //         byte[] userNameCheck = user.getNameHash();
    //         if (Arrays.equals(userNameHash, userNameCheck) && !isAdminUser(userNameHash)) {
    //
    //             DB_User removedUser = users.remove(userNameHashWrap);
    //             String name = user.getName();
    //
    //             if (removedUser != null) {
    //                 // have to always specify what we are saving
    //                 storage.put(DatabaseStorage.USERS, users);
    //                 return true;
    //             }
    //             else {
    //                 if (logger2.isInfoEnabled()) {
    //                     logger2.info("Problem removing the user ({}). Does not exist.", name);
    //                 }
    //                 return false; // problem removing the user
    //             }
    //         }
    //     }
    //
    //     if (logger2.isInfoEnabled()) {
    //         logger2.info("Problem removing unknown user");
    //     }
    //     return false; // problem removing the user
    // }
    //
    // /**
    //  * Force set the password for a user in the system.
    //  *
    //  * @return the error message, null if successful.
    //  */
    // public synchronized
    // String setPasswordUser(final byte[] userNameHash, final byte[] passwordHash, final byte[] adminPasswordHash) {
    //     try {
    //         checkAccess(ConsoleServer.class);
    //     } catch (SecurityException e) {
    //         Exit.FailedSecurity(e);
    //     }
    //
    //     final ByteArrayWrapper adminUserHashWrap = ByteArrayWrapper.wrap(adminPasswordHash);
    //     DB_User adminUser = users.get(adminUserHashWrap);
    //
    //     Logger logger2 = this.logger;
    //     if (adminUser == null || !Arrays.equals(adminUser.getPasswordHash(), adminPasswordHash)) {
    //         String mesg = "Unable to authenticate admin password.";
    //         if (logger2.isInfoEnabled()) {
    //             logger2.info(mesg);
    //         }
    //         return mesg;
    //     }
    //
    //     final ByteArrayWrapper userNameHashWrap = ByteArrayWrapper.wrap(userNameHash);
    //     DB_User user = users.get(userNameHashWrap);
    //     if (user != null) {
    //         user.setPasswordHash(passwordHash);
    //         // have to always specify what we are saving
    //         storage.put(DatabaseStorage.USERS, users);
    //
    //         String name = user.getName();
    //         if (logger2.isInfoEnabled()) {
    //             logger2.info("Reset password for user ({})", name);
    //         }
    //         return null; // success!
    //     }
    //     else {
    //         String mesg = "Unable to change the password for non-existant user.";
    //         if (logger2.isInfoEnabled()) {
    //             logger2.info(mesg);
    //         }
    //         return mesg;
    //     }
    // }
    //
    // /**
    //  * faster check that userExists(), as it only check the hash
    //  */
    // public synchronized
    // User getUser(final byte[] usernameHash) {
    //     if (usernameHash == null) {
    //         return null;
    //     }
    //
    //     try {
    //         checkAccess(SystemLoginImpl.class, AdminActions.class, ConsoleServer.class);
    //     } catch (SecurityException e) {
    //         Exit.FailedSecurity(e);
    //     }
    //
    //     return users.get(ByteArrayWrapper.wrap(usernameHash));
    // }
    //
    // /**
    //  * gets the user password given the userName hash
    //  *
    //  * @return NON NULL VALUE
    //  */
    // public synchronized
    // byte[] getPasswordHash(final byte[] userNameHash) {
    //     if (userNameHash == null || userNameHash.length == 0) {
    //         return new byte[0];
    //     }
    //
    //     try {
    //         checkAccess(AdminActions.class);
    //     } catch (SecurityException e) {
    //         Exit.FailedSecurity(e);
    //     }
    //
    //
    //     DB_User user = users.get(ByteArrayWrapper.wrap(userNameHash));
    //     if (user != null) {
    //         return user.getPasswordHash();
    //     }
    //
    //     return new byte[0];
    // }
    //
    // public
    // Group getGroup(final UUID uuid) {
    //     return groups.();
    // }
    //    public synchronized final boolean isValidUser(String userName, char[] password) {
    //        String storedToken = properties.get(AdminActions.USER_PREFIX + userName, String.class);
    //        String storedPasswd = properties.get(AdminActions.USER_PWD_PREFIX + userName, String.class);
    //
    //        if (storedToken == null || storedToken.isEmpty()) {
    //            return false;
    //        } else {
    //            ifObject (!token.equals(storedToken)) {
    //                return false;
    //            }
    //        }
    //
    //        return false;
    //        // check to see if the password matches
    /**/       // return SCrypt.check(password, storedPasswd); */ //    }



}
