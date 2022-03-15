package dorkbox.util.ahoCorasick

import java.util.*

/**
 * Creates a Finite State Machine for very fast string matching.
 *
 * This is a wrapper for DoubleArrayTrie, since that class is awkward to use
 */
class FiniteStateMachine<V>(private val trie: DoubleArrayTrie<V>) {
    companion object {
        fun <V> build(map: Map<String, V>): FiniteStateMachine<V> {
            return FiniteStateMachine(DoubleArrayTrie(map))
        }

        fun build(strings: List<String>): FiniteStateMachine<Boolean> {
            if (strings.isEmpty()) {
                throw IllegalArgumentException("strings cannot be empty")
            }

            val map = TreeMap<String, Boolean>()
            for (key in strings) {
                map[key] = java.lang.Boolean.TRUE
            }

            return build(map)
        }

        fun build(vararg strings: String): FiniteStateMachine<Boolean> {
            if (strings.isEmpty()) {
                throw IllegalArgumentException("strings cannot be empty")
            }

            val map = TreeMap<String, Boolean>()
            for (key in strings) {
                map[key] = java.lang.Boolean.TRUE
            }

            return build(map)
        }

//        @JvmStatic
//        fun main(args: Array<String>) {
//            val strings = arrayOf("khanacademy.com", "cnn.com", "google.com", "fun.reddit.com", "reddit.com")
//            val keys = Arrays.asList(*strings)
//            var text: String
//            run {
//                val map = TreeMap<String, String>()
//                for (key in keys) {
//                    map[key] = key
//                }
//                val fsm: FiniteStateMachine<*> = build(map)
//                text = "reddit.google.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//                println()
//                text = "reddit.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//                println()
//                text = "fun.reddit.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//            }
//            println("\n\nTrying with new type\n\n")
//            run {
//                val fsm: FiniteStateMachine<*> = build(keys)
//                text = "reddit.google.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//                println()
//                text = "reddit.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//                println()
//                text = "fun.reddit.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//            }
//            println("\n\nTrying with new type\n\n")
//            run {
//                val fsm: FiniteStateMachine<*> = build(*strings)
//                text = "reddit.google.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//                println()
//                text = "reddit.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//                println()
//                text = "fun.reddit.com"
//                println("Searching : $text")
//                println(fsm.partialMatch(text))
//                println("Found: " + fsm.matches(text))
//            }
//            val fsm: FiniteStateMachine<*> = build(*strings)
//            run {
//                println("Keywords Orig: " + Arrays.toString(strings))
//                println("Keywords FSM : " + Arrays.toString(fsm.getKeywords()))
//            }
//        }
    }

    /**
     * @return true if this string is exactly contained. False otherwise
     */
    fun matches(text: String): Boolean {
        return (trie.exactMatchSearch(text) > -1)
    }

    /**
     * Parses text and finds PARTIALLY matching results. For exact matches only it is better to use `matches`
     *
     * @return a list of outputs that contain matches or partial matches. The returned list will specify HOW MUCH of the text matches (A full match would be from 0 (the start), to N (the length of the text).
     */
    fun partialMatch(text: String): List<DoubleArrayTrie.Hit<V>> {
        return trie.parseText(text)
    }

    /**
     * Parses text and returns true if there are PARTIALLY matching results. For exact matches only it is better to use `matches`
     *
     * @return true if there is a match or partial match. "fun.reddit.com" will partially match to "reddit.com"
     */
    fun hasPartialMatch(text: String): Boolean {
        return trie.parseText(text).isNotEmpty()
    }

    /**
     * Returns the backing keywords IN THEIR NATURAL ORDER, in the case that you need access to the original FSM data.
     *
     * @return for example, if the FSM was populated with [reddit.com, cnn.com], this will return [cnn.com, reddit.com]
     */
    fun getKeywords(): Array<V> {
        return trie.v
    }
}
