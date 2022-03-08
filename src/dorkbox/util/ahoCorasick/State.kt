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

package dorkbox.vaadin.util.ahoCorasick

import java.util.*

/**
 *
 *
 * A state has the following functions
 *
 *
 *
 *
 *  * success; successfully transferred to another state
 *  * failure; if you cannot jump along the string, jump to a shallow node
 *  * emits; hit a pattern string
 *
 *
 *
 *
 *
 * The root node is slightly different. The root node has no failure function. Its "failure" refers to moving to the next state according to the string path. Other nodes have a failure state.
 *
 *
 * @author Robert Bor
 */
class State
/**
 * Construct a node with a depth of depth
 */
@JvmOverloads constructor(
    /**
     * The length of the pattern string is also the depth of this state
     */
        /**
         * Get node depth
         */
        val depth: Int = 0) {

    /**
     * The fail function, if there is no match, jumps to this state.
     */
    private var failure: State? = null

    /**
     * Record mode string as long as this state is reachable
     */
    private var emits: MutableSet<Int>? = null
    /**
     * The goto table, also known as the transfer function. Move to the next state based on the next character of the string
     */
    private val success = TreeMap<Char, State>()

    /**
     * Corresponding subscript in double array
     */
    var index: Int = 0

    /**
     * Get the largest value
     */
    val largestValueId: Int?
        get() = if (emits == null || emits!!.size == 0) {
            null
        }
        else emits!!.iterator().next()

    /**
     * Whether it is the termination status
     */
    val isAcceptable: Boolean
        get() = this.depth > 0 && this.emits != null

    val states: Collection<State>
        get() = this.success.values

    val transitions: Collection<Char>
        get() = this.success.keys

    /**
     * Add a matching pattern string (this state corresponds to this pattern string)
     */
    fun addEmit(keyword: Int) {
        if (this.emits == null) {
            this.emits = TreeSet(Collections.reverseOrder())
        }
        this.emits!!.add(keyword)
    }

    /**
     * Add some matching pattern strings
     */
    fun addEmit(emits: Collection<Int>) {
        for (emit in emits) {
            addEmit(emit)
        }
    }

    /**
     * Get the pattern string represented by this node (we)
     */
    fun emit(): Collection<Int> {
        return this.emits ?: emptyList()
    }

    /**
     * Get the failure status
     */
    fun failure(): State? {
        return this.failure
    }

    /**
     * Set the failure status
     */
    fun setFailure(failState: State,
                   fail: IntArray) {
        this.failure = failState
        fail[index] = failState.index
    }

    /**
     * Move to the next state
     *
     * @param character wants to transfer by this character
     * @param ignoreRootState Whether to ignore the root node, it should be true if the root node calls itself, otherwise it is false
     *
     * @return transfer result
     */
    private fun nextState(character: Char,
                          ignoreRootState: Boolean): State? {
        var nextState: State? = this.success[character]
        if (!ignoreRootState && nextState == null && this.depth == 0) {
            nextState = this
        }
        return nextState
    }

    /**
     * According to the character transfer, the root node transfer failure will return itself (never return null)
     */
    fun nextState(character: Char): State? {
        return nextState(character, false)
    }

    /**
     * According to character transfer, any node transfer failure will return null
     */
    fun nextStateIgnoreRootState(character: Char): State? {
        return nextState(character, true)
    }

    fun addState(character: Char): State {
        var nextState = nextStateIgnoreRootState(character)
        if (nextState == null) {
            nextState = State(this.depth + 1)
            this.success[character] = nextState
        }
        return nextState
    }

    override fun toString(): String {
        val sb = StringBuilder("State{")
        sb.append("depth=").append(depth)
        sb.append(", ID=").append(index)
        sb.append(", emits=").append(emits)
        sb.append(", success=").append(success.keys)
        sb.append(", failureID=").append(if (failure == null) "-1" else failure!!.index)
        sb.append(", failure=").append(failure)
        sb.append('}')
        return sb.toString()
    }

    /**
     * Get goto table
     */
    fun getSuccess(): Map<Char, State> {
        return success
    }
}
/**
 * Construct a node with a depth of 0
 */
