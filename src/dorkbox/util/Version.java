/*
 * Copyright 2017 dorkbox, llc
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
package dorkbox.util;

import java.util.Locale;

/**
 * Version a = new Version("1.1");
 * Version b = new Version("1.1.1");
 * a.compareTo(b) // return -1 (a<b)
 * a.equals(b)    // return false
 * <p>
 * Version a = new Version("2.0");
 * Version b = new Version("1.9.9");
 * a.compareTo(b) // return 1 (a>b)
 * a.equals(b)    // return false
 * <p>
 * Version a = new Version("1.0");
 * Version b = new Version("1");
 * a.compareTo(b) // return 0 (a=b)
 * a.equals(b)    // return true
 * <p>
 * Version a = new Version("1");
 * Version b = null;
 * a.compareTo(b) // return 1 (a>b)
 * a.equals(b)    // return false
 * <p>
 * List<Version> versions = new ArrayList<Version>();
 * versions.add(new Version("2"));
 * versions.add(new Version("1.0.5"));
 * versions.add(new Version("1.01.0"));
 * versions.add(new Version("1.00.1"));
 * Collections.min(versions).get() // return min version
 * Collections.max(versions).get() // return max version
 * <p>
 * // WARNING
 * Version a = new Version("2.06");
 * Version b = new Version("2.060");
 * a.equals(b)    // return false
 * <p>
 * <p>
 * If the numbers are the same, then
 * BETA+BUILD < BETA < STABLE+BUILD < STABLE.
 * <p>
 * Stable is a version that is exclusively numbers. Builds are always equal, even if a different build commit hash/etc.
 */
@SuppressWarnings({"unused", "SimplifiableIfStatement"})
public
class Version implements Comparable<Version> {
    private static final int[] PRIME = {2, 3, 5};

    private final String version;
    private final int[] internalVersion;

    private final boolean isBeta;
    private final String build;

    /**
     * Creates a comparable version based on only numbers
     *
     * @param version must consist of just numbers with a maximum of 1 decimal point
     */
    public
    Version(double version) {
        this(Double.toString(version), false, null);
    }

    /**
     * Creates a comparable version from a string
     *
     * @param version The version part must consist of just numbers with a maximum of 3 groups separated by a '.' and BETA or BUILD info
     */
    public
    Version(String version) {
        this(Version.fromString(version));
    }

    /**
     * Creates a comparable version based on an existing version
     */
    public
    Version(final Version version) {
        this.version = version.version;
        this.internalVersion = version.internalVersion;
        this.isBeta = version.isBeta;
        this.build = version.build;
    }

    /**
     * Creates a comparable version based on numbers, BETA status, and BUILD
     *
     * @param version must consist of only numbers with a maximum of 3 groups separated by a .
     * @param isBeta true if this is a beta build
     * @param build custom build info, such as the commit sha hash
     */
    public
    Version(String version, boolean isBeta, String build) {
        if (version == null) {
            throw new IllegalArgumentException("Version can not be null");
        }
        if (!version.matches("[0-9]+(\\.[0-9]+){0,3}")) {
            throw new IllegalArgumentException("Invalid version format");
        }

        if (build != null) {
            this.build = build.toLowerCase(Locale.US);
        }
        else {
            this.build = null;
        }

        this.isBeta = isBeta;
        this.version = version;

        String[] parts = this.version.split("\\.");
        internalVersion = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            final String s = parts[i];
            internalVersion[i] = Integer.parseInt(s);
        }
    }

    /**
     * Creates a comparable version based on only numbers
     */
    public
    Version(String... version) {
        if (version == null) {
            throw new IllegalArgumentException("Version can not be null");
        }

        int length = version.length;
        if (length > 3) {
            throw new IllegalArgumentException("Invalid version format");
        }

        this.build = null;
        this.isBeta = false;

        StringBuilder builder = new StringBuilder(length + 3);
        internalVersion = new int[length];
        for (int i = 0; i < length; i++) {
            final String s = version[i];
            if (!MathUtil.isInteger(s)) {
                throw new IllegalArgumentException("Version must be a number");
            }
            internalVersion[i] = Integer.parseInt(s);
            builder.append(s)
                   .append('.');
        }

        this.version = builder.toString();
    }

    /**
     * Creates a comparable version based on only numbers
     */
    public
    Version(int... version) {
        if (version == null) {
            throw new IllegalArgumentException("Version can not be null");
        }

        if (version.length > 3) {
            throw new IllegalArgumentException("Invalid version format");
        }

        this.build = null;
        this.isBeta = false;

        StringBuilder builder = new StringBuilder(version.length + 3);
        internalVersion = new int[version.length];
        for (int i = 0; i < version.length; i++) {
            internalVersion[i] = version[i];
            builder.append(i)
                   .append('.');
        }

        this.version = builder.toString();
    }

    /**
     * Converts a version into a "beta" version, without any additional build information
     * <p>
     * BETA+BUILD < BETA < STABLE+BUILD < STABLE.
     * Stable is a version that is exclusively numbers. Builds are always equal, even if a different build commit hash/etc.
     */
    public
    Version beta() {
        return new Version(version, true, build);
    }

    /**
     * Creates a version with specific build information (such as sha commit hash, etc)
     * <p>
     * BETA+BUILD < BETA < STABLE+BUILD < STABLE.
     * Stable is a version that is exclusively numbers. Builds are always equal, even if a different build commit hash/etc.
     */
    public
    Version build(String build) {
        return new Version(version, isBeta, build);
    }

    public
    boolean isGreater(Object that) {
        if (this == that) {
            return false;
        }
        if (that == null) {
            return true;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return this.compareTo((Version) that) > 0;
    }

    public
    boolean isGreaterOrEquals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return true;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return this.compareTo((Version) that) >= 0;
    }

    public
    boolean isLess(Object that) {
        if (this == that) {
            return false;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return this.compareTo((Version) that) < 0;
    }

    public
    boolean isLessOrEquals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return this.compareTo((Version) that) <= 0;
    }

    public
    boolean isEquals(Object that) {
        return equals(that);
    }

    @Override
    public
    boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return this.compareTo((Version) that) == 0;
    }

    @Override
    public
    int compareTo(Version that) {
        if (that == null) {
            return 1;
        }

        int[] thisParts = this.internalVersion;
        int[] thatParts = that.internalVersion;

        int maxLength = Math.max(thisParts.length, thatParts.length);

        for (int i = 0; i < maxLength; i++) {
            int thisPart;
            if (i < thisParts.length) {
                thisPart = thisParts[i];
            }
            else {
                thisPart = 0;
            }

            int thatPart;
            if (i < thatParts.length) {
                thatPart = thatParts[i];
            }
            else {
                thatPart = 0;
            }

            if (thisPart < thatPart) {
                return -1;
            }
            if (thisPart > thatPart) {
                return 1;
            }
        }

        // our numbers are all equal, now determine equality based on BETA/BUILD info

        // BETA+BUILD < BETA < STABLE+BUILD < STABLE.
        // Stable is a version that is exclusively numbers. Builds are always equal, even if a different build commit hash/etc.

        if (this.isBeta) {
            if (this.build != null) {
                if (that.isBeta) {
                    if (that.build != null) {
                        // BETA+BUILD == BETA+BUILD
                        return 0;
                    }
                    // BETA+BUILD < BETA
                    return -1;
                }
                // BETA+BUILD < STABLE+BUILD < STABLE
                return -1;
            }
            else {
                if (that.isBeta) {
                    if (that.build != null) {
                        // BETA > BETA+BUILD
                        return 1;
                    }
                    // BETA == BETA
                    return 0;
                }
                // BETA < STABLE+BUILD < STABLE
                return -1;
            }
        }

        // else this is STABLE or STABLE+BUILD

        if (this.build != null) {
            if (that.isBeta) {
                // STABLE+BUILD > BETA > BETA+BUILD
                return 1;
            }
            if (that.build != null) {
                // STABLE+BUILD == STABLE+BUILD
                return 0;
            }
            // STABLE+BUILD < STABLE
            return -1;
        }
        else {
            if (that.isBeta) {
                // STABLE > BETA > BETA+BUILD
                return 1;
            }
            if (that.build != null) {
                // STABLE > STABLE+BUILD
                return 1;
            }
            // STABLE == STABLE
            return 0;
        }
    }


    @Override
    public final
    int hashCode() {
        // better hashing than just using .toString().hashCode()
        int hashCode = 0;
        for (int i = 0; i < internalVersion.length; i++) {
            final int part = internalVersion[i];
            if (part > 0) {
                hashCode += PRIME[i] ^ part;
            }
        }

        if (build != null) {
            hashCode += build.hashCode();
        }

        return hashCode;
    }

    /**
     * Converts version information from a string
     *
     * @param string The version string, as received by {@link Version#toString()}
     */
    public static
    Version fromString(String string) {
        int betaIndex = string.indexOf("-BETA");
        int buildIndex = string.indexOf("+");
        int lastIndex = string.length();


        boolean isBeta = betaIndex > 0;
        String build = null;
        if (buildIndex > 0) {
            build = string.substring(buildIndex + 1, lastIndex);
            lastIndex = buildIndex;
        }

        if (isBeta) {
            lastIndex = betaIndex;
        }

        String version = string.substring(0, lastIndex);

        return new Version(version, isBeta, build);
    }

    @Override
    public
    String toString() {
        if (isBeta) {
            if (build != null) {
                return version + "-BETA+" + build;
            }
            return version + "-BETA";
        }
        if (build != null) {
            return version + "+" + build;
        }
        return version;
    }
}
