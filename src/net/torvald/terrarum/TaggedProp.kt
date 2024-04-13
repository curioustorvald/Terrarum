package net.torvald.terrarum

/**
 * Created by minjaesong on 2024-04-13.
 */
interface TaggedProp {

    fun hasTag(s: String): Boolean
    fun hasAnyTagsOf(vararg s: String) = s.any { hasTag(it) }
    fun hasAnyTags(s: Collection<String>) = s.any { hasTag(it) }
    fun hasAnyTags(s: Array<String>) = s.any { hasTag(it) }
    fun hasAllTagsOf(vararg s: String) = s.all { hasTag(it) }
    fun hasAllTags(s: Collection<String>) = s.all { hasTag(it) }
    fun hasAllTags(s: Array<String>) = s.all { hasTag(it) }
    fun hasNoTagsOf(vararg s: String) = s.none { hasTag(it) }
    fun hasNoTags(s: Collection<String>) = s.none { hasTag(it) }
    fun hasNoTags(s: Array<String>) = s.none { hasTag(it) }


}