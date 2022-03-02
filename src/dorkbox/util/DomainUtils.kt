/*
 * Copyright 2013 dorkbox, llc
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 * Derivative code has been released as Apache 2.0, used with permission.
 *
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
package dorkbox.util

import java.io.File
import java.util.*

/**
 * And the effective_tld_names.dat is from mozilla (the following are all the same data)
 *
 *
 * https://mxr.mozilla.org/mozilla-central/source/netwerk/dns/effective_tld_names.dat?raw=1
 * which is...
 * https://publicsuffix.org/list/effective_tld_names.dat
 *
 *
 * also
 *
 *
 * https://publicsuffix.org/list/public_suffix_list.dat
 */
object DomainUtils {
    private val exceptions = HashSet<String>()
    private val suffixes = HashSet<String>()

    fun init() {
        // just here to load the class.
    }

    init {
        val tldFileName = "effective_tld_names.dat.txt"

        /*
         * Parses the list from publicsuffix.org
         * Copied from
         * http://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient/src/main/java/org/apache/http/impl/cookie/PublicSuffixListParser.java
         *
         * new one at:
         * http://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient5/src/main/java/org/apache/hc/client5/http/impl/cookie/PublicSuffixDomainFilter.java
         * and
         * http://svn.apache.org/repos/asf/httpcomponents/httpclient/trunk/httpclient5/src/main/java/org/apache/hc/client5/http/psl/
         */

        // now load this file into memory, so it's faster to process.
        var file = File("blacklist", tldFileName)
        val paths = LinkedList(Arrays.asList("NetRefDependencies", ".."))

        while (!file.canRead() && !paths.isEmpty()) {
            // for work in an IDE. Path can vary, so we work our way up
            file = File(paths.removeFirst(), file.toString())
        }

        file = file.absoluteFile
        if (!file.canRead()) {
            throw RuntimeException("Unable to load the TLD list: $tldFileName")
        }

        FileUtil.read(file, object : FileUtil.Action {
            override fun onLineRead(line: String) {
                var line = line

                // entire lines can also be commented using //
                if (!line.isEmpty() && !line.startsWith("//")) {

                    if (line.startsWith(".")) {
                        line = line.substring(1) // A leading dot is optional
                    }

                    // An exclamation mark (!) at the start of a rule marks an exception
                    // to a previous wildcard rule
                    val isException = line.startsWith("!")
                    if (isException) {
                        line = line.substring(1)
                    }

                    if (isException) {
                        exceptions.add(line)
                    } else {
                        suffixes.add(line)
                    }
                }
            }

            override fun finished() {}
        })

    }

    /**
     * Extracts the second level domain, from a fully qualified domain (ie: www.aa.com, or www.amazon.co.uk).
     *
     *
     * This algorithm works from left to right parsing the domain string parameter
     *
     * @param domain a fully qualified domain (ie: www.aa.com, or www.amazon.co.uk)
     *
     * @return null (if there is no second level domain) or the SLD www.aa.com -> aa.com , or www.amazon.co.uk -> amazon.co.uk
     */
    fun extractSLD(domain: String): String? {
        var domain = domain
        var last = domain
        var anySLD = false

        do {
            if (isTLD(domain)) {
                return if (anySLD) {
                    last
                }
                else {
                    null
                }
            }

            anySLD = true
            last = domain

            val nextDot = domain.indexOf(".")
            if (nextDot == -1) {
                return null
            }

            domain = domain.substring(nextDot + 1)
        } while (domain.isNotEmpty())

        return null
    }

    /**
     * Returns a domain that is without it's TLD at the end.
     *
     * @param domain  domain a fully qualified domain or not, (ie: www.aa.com, or amazon.co.uk).
     *
     * @return a domain that is without it's TLD, ie: www.aa.com -> www.aa, or google.com -> google
     */
    fun withoutTLD(domain: String): String? {

        var index = 0
        while (index != -1) {
            index = domain.indexOf('.', index)

            if (index != -1) {
                if (isTLD(domain.substring(index))) {
                    return domain.substring(0, index)
                }
                index++
            }
            else {
                return null
            }
        }

        return null
    }

    /**
     * Checks if the domain is a TLD.
     */
    fun isTLD(domain: String): Boolean {
        var domain = domain
        if (domain.startsWith(".")) {
            domain = domain.substring(1)
        }

        // An exception rule takes priority over any other matching rule.
        // Exceptions are ones that are not a TLD, but would match a pattern rule
        // e.g. bl.uk is not a TLD, but the rule *.uk means it is. Hence there is an exception rule
        // stating that bl.uk is not a TLD.
        if (exceptions.contains(domain)) {
            return false
        }

        if (suffixes.contains(domain)) {
            return true
        }

        // Try patterns. ie *.jp means that boo.jp is a TLD
        val nextdot = domain.indexOf('.')
        if (nextdot == -1) {
            return false
        }
        domain = "*" + domain.substring(nextdot)

        return suffixes.contains(domain)

    }
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//        System.err.println("isTLD(espn.com) = " + isTLD("espn.com"))
//        System.err.println("withoutTLD(com) = " + withoutTLD("com"))
//        System.err.println("withoutTLD(chrome:extension) = " + withoutTLD(""))
//    }
}
