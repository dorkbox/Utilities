/*
 * AhoCorasickDoubleArrayTrie Project
 *      https://github.com/hankcs/AhoCorasickDoubleArrayTrie
 *
 * Copyright 2008-2018 hankcs <me@hankcs.com>
 * You may modify and redistribute as long as this attribution remains.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package dorkbox.vaadin.util.ahoCorasick

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*

/**
 * An implementation of Aho Corasick algorithm based on Double Array Trie
 *
 * Will create a DoubleArray Trie from a Map or InputStream (if previously saved)
 *
 * @author hankcs
 */
class DoubleArrayTrie<V>(map: Map<String, V>? = null,
                         inputStream: ObjectInputStream? = null) : Serializable {
    /**
     * check array of the Double Array Trie structure
     */
    private val check: IntArray

    /**
     * base array of the Double Array Trie structure
     */
    private val base: IntArray

    /**
     * fail table of the Aho Corasick automata
     */
    private val fail: IntArray

    /**
     * output table of the Aho Corasick automata
     */
    private val output: Array<IntArray?>

    /**
     * outer value array
     */
    private val v: Array<V>

    /**
     * the length of every key
     */
    private val l: IntArray

    /**
     * the size of base and check array
     */
    private val size: Int

    init {
        when {
            map != null -> {
                @Suppress("UNCHECKED_CAST")
                v = kotlin.jvm.internal.collectionToArray(map.values) as Array<V>
                l = IntArray(map.size)

                val builder = Builder()
                builder.build(map)

                fail = builder.fail
                base = builder.base
                check = builder.check

                size = builder.size
                output = builder.output
            }
            inputStream != null -> {
                @Suppress("UNCHECKED_CAST")
                v = inputStream.readObject() as Array<V>
                l = inputStream.readObject() as IntArray

                fail = inputStream.readObject() as IntArray
                base = inputStream.readObject() as IntArray
                check = inputStream.readObject() as IntArray
                size = inputStream.readObject() as Int

                @Suppress("UNCHECKED_CAST")
                output = inputStream.readObject() as Array<IntArray?>
            }
            else -> throw NullPointerException("Map or InputStream must be specified!")
        }
    }


    /**
     * Save
     */
    @Throws(IOException::class)
    fun save(out: ObjectOutputStream) {
        out.writeObject(v)
        out.writeObject(l)
        out.writeObject(fail)
        out.writeObject(base)
        out.writeObject(check)
        out.writeObject(size)
        out.writeObject(output)
    }

    /**
     * Parse text
     *
     * @return a list of outputs
     */
    fun parseText(text: CharSequence): List<Hit<V>> {
        var position = 1
        var currentState = 0
        val collectedEmits = LinkedList<Hit<V>>()  // unknown size, so

        for (i in 0 until text.length) {
            currentState = getState(currentState, text[i])
            storeEmits(position, currentState, collectedEmits)
            ++position
        }

        return collectedEmits
    }

    /**
     * Parse text
     *
     * @param text The text
     * @param processor A processor which handles the output
     */
    fun parseText(text: CharSequence,
                  processor: IHit<V>
    ) {
        var position = 1
        var currentState = 0
        for (i in 0 until text.length) {
            currentState = getState(currentState, text[i])
            val hitArray = output[currentState]
            if (hitArray != null) {
                for (hit in hitArray) {
                    processor.hit(position - l[hit], position, v[hit])
                }
            }
            ++position
        }
    }

    /**
     * Parse text
     *
     * @param text The text
     * @param processor A processor which handles the output
     */
    fun parseText(text: CharSequence,
                  processor: IHitCancellable<V>
    ) {
        var currentState = 0
        for (i in 0 until text.length) {
            val position = i + 1
            currentState = getState(currentState, text[i])
            val hitArray = output[currentState]
            if (hitArray != null) {
                for (hit in hitArray) {
                    val proceed = processor.hit(position - l[hit], position, v[hit])
                    if (!proceed) {
                        return
                    }
                }
            }
        }
    }

    /**
     * Parse text
     *
     * @param text The text
     * @param processor A processor which handles the output
     */
    fun parseText(text: CharArray,
                  processor: IHit<V>
    ) {
        var position = 1
        var currentState = 0
        for (c in text) {
            currentState = getState(currentState, c)
            val hitArray = output[currentState]
            if (hitArray != null) {
                for (hit in hitArray) {
                    processor.hit(position - l[hit], position, v[hit])
                }
            }
            ++position
        }
    }

    /**
     * Parse text
     *
     * @param text The text
     * @param processor A processor which handles the output
     */
    fun parseText(text: CharArray,
                  processor: IHitFull<V>
    ) {
        var position = 1
        var currentState = 0
        for (c in text) {
            currentState = getState(currentState, c)
            val hitArray = output[currentState]
            if (hitArray != null) {
                for (hit in hitArray) {
                    processor.hit(position - l[hit], position, v[hit], hit)
                }
            }
            ++position
        }
    }

    /**
     * Checks that string contains at least one substring
     *
     * @param text source text to check
     *
     * @return `true` if string contains at least one substring
     */
    fun matches(text: String): Boolean {
        var currentState = 0
        for (i in 0 until text.length) {
            currentState = getState(currentState, text[i])
            val hitArray = output[currentState]
            if (hitArray != null) {
                return true
            }
        }
        return false
    }

    /**
     * Search first match in string
     *
     * @param text source text to check
     *
     * @return first match or `null` if there are no matches
     */
    fun findFirst(text: String): Hit<V>? {
        var position = 1
        var currentState = 0
        for (i in 0 until text.length) {
            currentState = getState(currentState, text[i])
            val hitArray = output[currentState]
            if (hitArray != null) {
                val hitIndex = hitArray[0]
                return Hit(position - l[hitIndex], position, v[hitIndex])
            }
            ++position
        }
        return null
    }


    /**
     * Get value by a String key, just like a map.get() method
     *
     * @param key The key
     */
    operator fun get(key: String): V? {
        val index = exactMatchSearch(key)
        return if (index >= 0) {
            v[index]
        }
        else null

    }

    /**
     * Update a value corresponding to a key
     *
     * @param key the key
     * @param value the value
     *
     * @return successful or not（failure if there is no key）
     */
    operator fun set(key: String,
                     value: V): Boolean {
        val index = exactMatchSearch(key)
        if (index >= 0) {
            v[index] = value
            return true
        }

        return false
    }

    /**
     * Pick the value by index in value array <br></br>
     * Notice that to be more efficiently, this method DOES NOT check the parameter
     *
     * @param index The index
     *
     * @return The value
     */
    operator fun get(index: Int): V {
        return v[index]
    }

    /**
     * Processor handles the output when hit a keyword
     */
    interface IHit<V> {
        /**
         * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
         *
         * @param begin the beginning index, inclusive.
         * @param end the ending index, exclusive.
         * @param value the value assigned to the keyword
         */
        fun hit(begin: Int,
                end: Int,
                value: V)
    }


    /**
     * Processor handles the output when hit a keyword, with more detail
     */
    interface IHitFull<V> {
        /**
         * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
         *
         * @param begin the beginning index, inclusive.
         * @param end the ending index, exclusive.
         * @param value the value assigned to the keyword
         * @param index the index of the value assigned to the keyword, you can use the integer as a perfect hash value
         */
        fun hit(begin: Int,
                end: Int,
                value: V,
                index: Int)
    }


    /**
     * Callback that allows to cancel the search process.
     */
    interface IHitCancellable<V> {
        /**
         * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
         *
         * @param begin the beginning index, inclusive.
         * @param end the ending index, exclusive.
         * @param value the value assigned to the keyword
         *
         * @return Return true for continuing the search and false for stopping it.
         */
        fun hit(begin: Int,
                end: Int,
                value: V): Boolean
    }


    /**
     * A result output
     *
     * @param <V> the value type
    </V> */
    class Hit<V> internal constructor(
            /**
             * the beginning index, inclusive.
             */
            val begin: Int,
            /**
             * the ending index, exclusive.
             */
            val end: Int,
            /**
             * the value assigned to the keyword
             */
            val value: V) {

        override fun toString(): String {
            return String.format("[%d:%d]=%s", begin, end, value)
        }
    }

    /**
     * transmit state, supports failure function
     */
    private fun getState(currentState: Int,
                         character: Char): Int {

        @Suppress("NAME_SHADOWING")
        var currentState = currentState

        var newCurrentState = transitionWithRoot(currentState, character)  // First press success
        while (newCurrentState == -1)
        // If the jump fails, press failure to jump
        {
            currentState = fail[currentState]
            newCurrentState = transitionWithRoot(currentState, character)
        }
        return newCurrentState
    }

    /**
     * store output
     */
    private fun storeEmits(position: Int,
                           currentState: Int,
                           collectedEmits: MutableList<Hit<V>>) {
        val hitArray = output[currentState]
        if (hitArray != null) {
            for (hit in hitArray) {
                collectedEmits.add(Hit(position - l[hit], position, v[hit]))
            }
        }
    }

    /**
     * transition of a state
     */
    private fun transition(current: Int,
                           c: Char): Int {
        var b = current
        var p: Int

        p = b + c.code + 1
        if (b == check[p]) {
            b = base[p]
        }
        else {
            return -1
        }

        p = b
        return p
    }

    /**
     * transition of a state, if the state is root and it failed, then returns the root
     */
    private fun transitionWithRoot(nodePos: Int,
                                     c: Char): Int {
        val b = base[nodePos]
        val p: Int

        p = b + c.code + 1
        return if (b != check[p]) {
            if (nodePos == 0) {
                0
            }
            else -1
        }
        else p
    }

    /**
     * match exactly by a key
     *
     * @param key the key
     *
     * @return the index of the key, you can use it as a perfect hash function
     */
    fun exactMatchSearch(key: String): Int {
        return exactMatchSearch(key, 0, 0, 0)
    }

    /**
     * match exactly by a key-char array
     *
     * @param keyChars the key (as a Character array)
     *
     * @return the index of the key, you can use it as a perfect hash function
     */
    fun exactMatchSearch(keyChars: CharArray): Int {
        return exactMatchSearch(keyChars, 0, 0, 0)
    }

    /**
     * match exactly by a key
     */
    private fun exactMatchSearch(key: String,
                                 pos: Int,
                                 len: Int,
                                 nodePos: Int): Int {
        @Suppress("NAME_SHADOWING")
        var len = len

        @Suppress("NAME_SHADOWING")
        var nodePos = nodePos

        if (len <= 0) {
            len = key.length
        }
        if (nodePos <= 0) {
            nodePos = 0
        }

        var result = -1

        val keyChars = key.toCharArray()

        var b = base[nodePos]
        var p: Int

        for (i in pos until len) {
            p = b + keyChars[i].code + 1
            if (b == check[p]) {
                b = base[p]
            }
            else {
                return result
            }
        }

        p = b
        val n = base[p]
        if (b == check[p] && n < 0) {
            result = -n - 1
        }
        return result
    }

    /**
     * match exactly by a key
     *
     * @param keyChars the char array of the key
     * @param pos the begin index of char array
     * @param len the length of the key
     * @param nodePos the starting position of the node for searching
     *
     * @return the value index of the key, minus indicates null
     */
    private fun exactMatchSearch(keyChars: CharArray,
                                 pos: Int,
                                 len: Int,
                                 nodePos: Int): Int {
        var result = -1

        var b = base[nodePos]
        var p: Int

        for (i in pos until len) {
            p = b + keyChars[i].code + 1
            if (b == check[p]) {
                b = base[p]
            }
            else {
                return result
            }
        }

        p = b
        val n = base[p]
        if (b == check[p] && n < 0) {
            result = -n - 1
        }
        return result
    }

    //    /**
    //     * Just for debug when I wrote it
    //     */
    //    public void debug()
    //    {
    //        System.out.println("base:");
    //        for (int i = 0; i < base.length; i++)
    //        {
    //            if (base[i] < 0)
    //            {
    //                System.out.println(i + " : " + -base[i]);
    //            }
    //        }
    //
    //        System.out.println("output:");
    //        for (int i = 0; i < output.length; i++)
    //        {
    //            if (output[i] != null)
    //            {
    //                System.out.println(i + " : " + Arrays.toString(output[i]));
    //            }
    //        }
    //
    //        System.out.println("fail:");
    //        for (int i = 0; i < fail.length; i++)
    //        {
    //            if (fail[i] != 0)
    //            {
    //                System.out.println(i + " : " + fail[i]);
    //            }
    //        }
    //
    //        System.out.println(this);
    //    }
    //
    //    @Override
    //    public String toString()
    //    {
    //        String infoIndex = "i    = ";
    //        String infoChar = "char = ";
    //        String infoBase = "base = ";
    //        String infoCheck = "check= ";
    //        for (int i = 0; i < Math.min(base.length, 200); ++i)
    //        {
    //            if (base[i] != 0 || check[i] != 0)
    //            {
    //                infoChar += "    " + (i == check[i] ? " ×" : (char) (i - check[i] - 1));
    //                infoIndex += " " + String.format("%5d", i);
    //                infoBase += " " + String.format("%5d", base[i]);
    //                infoCheck += " " + String.format("%5d", check[i]);
    //            }
    //        }
    //        return "DoubleArrayTrie：" +
    //                "\n" + infoChar +
    //                "\n" + infoIndex +
    //                "\n" + infoBase +
    //                "\n" + infoCheck + "\n" +
    ////                "check=" + Arrays.toString(check) +
    ////                ", base=" + Arrays.toString(base) +
    ////                ", used=" + Arrays.toString(used) +
    //                "size=" + size
    ////                ", length=" + Arrays.toString(length) +
    ////                ", value=" + Arrays.toString(value) +
    //                ;
    //    }
    //
    //    /**
    //     * A debug class that sequentially outputs variable names and variable values
    //     */
    //    private static class DebugArray
    //    {
    //        Map<String, String> nameValueMap = new LinkedHashMap<String, String>();
    //
    //        public void add(String name, int value)
    //        {
    //            String valueInMap = nameValueMap.get(name);
    //            if (valueInMap == null)
    //            {
    //                valueInMap = "";
    //            }
    //
    //            valueInMap += " " + String.format("%5d", value);
    //
    //            nameValueMap.put(name, valueInMap);
    //        }
    //
    //        @Override
    //        public String toString()
    //        {
    //            String text = "";
    //            for (Map.Entry<String, String> entry : nameValueMap.entrySet())
    //            {
    //                String name = entry.getKey();
    //                String value = entry.getValue();
    //                text += String.format("%-5s", name) + "= " + value + '\n';
    //            }
    //
    //            return text;
    //        }
    //
    //        public void println()
    //        {
    //            System.out.print(this);
    //        }
    //    }

    /**
     * Get the size of the keywords
     */
    fun size(): Int {
        return v.size
    }

    /**
     * A builder to build the AhoCorasickDoubleArrayTrie
     */
    private inner class Builder {
        /**
         * the root state of trie
         */
        private var rootState: State? = State()
        /**
         * whether the position has been used
         */
        private var used: BooleanArray? = null
        /**
         * the allocSize of the dynamic array
         */
        private var allocSize: Int = 0
        /**
         * a parameter controls the memory growth speed of the dynamic array
         */
        private var progress: Int = 0
        /**
         * the next position to check unused memory
         */
        private var nextCheckPos: Int = 0
        /**
         * the size of the key-pair sets
         */
        private var keySize: Int = 0


        internal lateinit var output: Array<IntArray?>
        internal lateinit var fail: IntArray
        internal lateinit var base: IntArray
        internal lateinit var check: IntArray
        internal var size: Int = 0

        /**
         * Build from a map
         *
         * @param map a map containing key-value pairs
         */
        fun build(map: Map<String, V>) {
            val keySet = map.keys

            // Construct a two-point trie tree
            addAllKeyword(keySet)

            // Building a double array trie tree based on a two-point trie tree
            buildDoubleArrayTrie(keySet.size)
            used = null

            // Build the failure table and merge the output table
            constructFailureStates()
            rootState = null
            loseWeight()
        }

        /**
         * fetch siblings of a parent node
         *
         * @param parent parent node
         * @param siblings parent node's child nodes, i . e . the siblings
         *
         * @return the amount of the siblings
         */
        private fun fetch(parent: State,
                          siblings: MutableList<Pair<Int, State>>): Int {

            if (parent.isAcceptable) {
                // This node is a child of the parent and has the output of the parent.
                val fakeNode = State(-(parent.depth + 1))
                fakeNode.addEmit(parent.largestValueId!!)
                siblings.add(Pair(0, fakeNode))
            }

            for ((key, value) in parent.getSuccess()) {
                siblings.add(Pair(key.code + 1, value))
            }

            return siblings.size
        }

        /**
         * add a keyword
         *
         * @param keyword a keyword
         * @param index the index of the keyword
         */
        private fun addKeyword(keyword: String,
                               index: Int) {
            var currentState = this.rootState
            keyword.toCharArray().forEach { character ->
                currentState = currentState!!.addState(character)
            }

            currentState!!.addEmit(index)
            l[index] = keyword.length
        }

        /**
         * add a collection of keywords
         *
         * @param keywordSet the collection holding keywords
         */
        private fun addAllKeyword(keywordSet: Collection<String>) {
            var i = 0
            keywordSet.forEach { keyword ->
                addKeyword(keyword, i++)
            }
        }

        /**
         * construct failure table
         */
        private fun constructFailureStates() {
            fail = IntArray(Math.max(size + 1, 2))
            fail[1] = base[0]
            output = arrayOfNulls(size + 1)

            val queue = ArrayDeque<State>()

            // The first step is to set the failure of the node with depth 1 to the root node.
            this.rootState!!.states.forEach { depthOneState ->
                depthOneState.setFailure(this.rootState!!, fail)
                queue.add(depthOneState)
                constructOutput(depthOneState)
            }

            // The second step is to create a failure table for nodes with depth > 1, which is a bfs
            while (!queue.isEmpty()) {
                val currentState = queue.remove()

                for (transition in currentState.transitions) {
                    val targetState = currentState.nextState(transition)
                    queue.add(targetState)

                    var traceFailureState = currentState.failure()
                    while (traceFailureState!!.nextState(transition) == null) {
                        traceFailureState = traceFailureState.failure()
                    }

                    val newFailureState = traceFailureState.nextState(transition)
                    targetState!!.setFailure(newFailureState!!, fail)
                    targetState.addEmit(newFailureState.emit())
                    constructOutput(targetState)
                }
            }
        }

        /**
         * construct output table
         */
        private fun constructOutput(targetState: State) {
            val emit = targetState.emit()
            if (emit.isEmpty()) {
                return
            }

            val output = IntArray(emit.size)
            val it = emit.iterator()
            for (i in output.indices) {
                output[i] = it.next()
            }

            this.output[targetState.index] = output
        }

        private fun buildDoubleArrayTrie(keySize: Int) {
            progress = 0
            this.keySize = keySize
            resize(65536 * 32) // 32 double bytes

            base[0] = 1
            nextCheckPos = 0

            val rootNode = this.rootState
            val initialCapacity = rootNode!!.getSuccess().entries.size

            val siblings = ArrayList<Pair<Int, State>>(initialCapacity)
            fetch(rootNode, siblings)

            if (siblings.isNotEmpty()) {
                insert(siblings)
            }
        }

        /**
         * allocate the memory of the dynamic array
         */
        private fun resize(newSize: Int): Int {
            val base2 = IntArray(newSize)
            val check2 = IntArray(newSize)
            val used2 = BooleanArray(newSize)

            if (allocSize > 0) {
                System.arraycopy(base, 0, base2, 0, allocSize)
                System.arraycopy(check, 0, check2, 0, allocSize)
                System.arraycopy(used!!, 0, used2, 0, allocSize)
            }

            base = base2
            check = check2
            used = used2

            allocSize = newSize
            return newSize
        }

        /**
         * insert the siblings to double array trie
         *
         * @param siblings the siblings being inserted
         *
         * @return the position to insert them
         */
        private fun insert(siblings: List<Pair<Int, State>>): Int {
            var begin: Int
            var pos = Math.max(siblings[0].first + 1, nextCheckPos) - 1
            var nonzeroNum = 0
            var first = 0

            if (allocSize <= pos) {
                resize(pos + 1)
            }

            outer@
            // The goal of this loop body is to find n free spaces that satisfy base[begin + a1...an] == 0, a1...an are n nodes in siblings
            while (true) {
                pos++

                if (allocSize <= pos) {
                    resize(pos + 1)
                }

                if (check[pos] != 0) {
                    nonzeroNum++
                    continue
                }
                else if (first == 0) {
                    nextCheckPos = pos
                    first = 1
                }

                begin = pos - siblings[0].first // The distance of the current position from the first sibling node
                if (allocSize <= begin + siblings[siblings.size - 1].first) {
                    // progress can be zero
                    // Prevent progress from generating zero divide errors
                    val l = if (1.05 > 1.0 * keySize / (progress + 1)) 1.05 else 1.0 * keySize / (progress + 1)
                    resize((allocSize * l).toInt())
                }

                if (used!![begin]) {
                    continue
                }

                for (i in 1 until siblings.size) {
                    if (check[begin + siblings[i].first] != 0) {
                        continue@outer
                    }
                }

                break
            }

            // -- Simple heuristics --
            // if the percentage of non-empty contents in check between the
            // index
            // 'next_check_pos' and 'check' is greater than some constant value
            // (e.g. 0.9),
            // new 'next_check_pos' index is written by 'check'.
            if (1.0 * nonzeroNum / (pos - nextCheckPos + 1) >= 0.95) {
                // From the position next_check_pos to pos, if the occupied space is above 95%, the next
                // time you insert a node, you can start looking directly at the pos position.
                nextCheckPos = pos
            }
            used!![begin] = true  // valid because resize is called.

            val sizeLimit = begin + siblings[siblings.size - 1].first + 1
            if (size <= sizeLimit) {
                size = sizeLimit
            }


            for (sibling in siblings) {
                check[begin + sibling.first] = begin
            }

            for (sibling in siblings) {
                val newSiblings = ArrayList<Pair<Int, State>>(sibling.second.getSuccess().entries.size + 1)

                if (fetch(sibling.second, newSiblings) == 0) {
                    // The termination of a word and not the prefix of other words, in fact, is the leaf node
                    base[begin + sibling.first] = 0 - sibling.second.largestValueId!! - 1
                    progress++
                }
                else {
                    val h = insert(newSiblings)   // depth first search
                    base[begin + sibling.first] = h
                }
                sibling.second.index = begin + sibling.first
            }
            return begin
        }

        /**
         * free the unnecessary memory
         */
        private fun loseWeight() {
            base = base.copyOf(size + 65535)
            check = check.copyOf(size + 65535)
        }
    }

//    companion object {
//        @JvmStatic
//        fun main(args: Array<String>) {
//            // test outliers
//            test(hashMapOf())
//
//            test(hashMapOf("bmw" to "bmw"))
//
//
//            var map = hashMapOf<String, String>()
//            var keyArray = arrayOf("bmw.com", "cnn.com", "google.com", "reddit.com")
//            for (key in keyArray) {
//                map[key] = key
//            }
//            test(map)
//
//            map = hashMapOf()
//            keyArray = arrayOf("bmw.com", "cnn.com", "google.com", "reddit.com", "reddit.google.com")
//            for (key in keyArray) {
//                map[key] = key
//            }
//            test(map)
//        }
//
//        fun test(map: Map<String, String>) {
//            val trie = DoubleArrayTrie(map)
//
//            val text = "reddit.google.com"
//            println(trie.parseText(text).toString())
//            println(trie.exactMatchSearch(text))
//        }
//    }
}
