package de.fayard.refreshVersions.core.internal

import de.fayard.refreshVersions.core.extensions.gradle.isBuildSrc
import de.fayard.refreshVersions.core.extensions.gradle.isRootProject
import org.gradle.api.Project
import java.io.File

internal object SettingsPluginsUpdater {

    fun updateGradleSettingsWithAvailablePluginsUpdates(
        rootProject: Project,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>,
        buildSrcSettingsPluginsUpdates: List<PluginWithVersionCandidates>
    ) {
        require(rootProject.isRootProject)
        require(rootProject.isBuildSrc.not())

        val rootProjectSettingsFile = rootProject.file("settings.gradle.kts").let { kotlinDslSettings ->
            if (kotlinDslSettings.exists()) kotlinDslSettings else {
                rootProject.file("settings.gradle").also {
                    check(it.exists())
                }
            }
        }
        val buildSrcSettingsFile = rootProject.file("buildSrc/settings.gradle.kts").let { kotlinDslSettings ->
            if (kotlinDslSettings.exists()) kotlinDslSettings else {
                rootProject.file("buildSrc/settings.gradle").takeIf {
                    it.exists()
                }
            }
        }
        updateGradleSettingsWithAvailablePluginsUpdates(
            settingsFile = rootProjectSettingsFile,
            settingsPluginsUpdates = settingsPluginsUpdates
        )
        buildSrcSettingsFile?.let {
            updateGradleSettingsWithAvailablePluginsUpdates(
                settingsFile = it,
                settingsPluginsUpdates = buildSrcSettingsPluginsUpdates
            )
        }
    }

    private fun updateGradleSettingsWithAvailablePluginsUpdates(
        settingsFile: File,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>
    ) {
        val newContent = updatedGradleSettingsFileContentWithAvailablePluginsUpdates(
            fileContent = settingsFile.readText(),
            isKotlinDsl = settingsFile.name.endsWith(".kts"),
            settingsPluginsUpdates = settingsPluginsUpdates
        )
        settingsFile.writeText(newContent)
    }

    internal fun updatedGradleSettingsFileContentWithAvailablePluginsUpdates(
        fileContent: String,
        isKotlinDsl: Boolean,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>
    ): String {
        val fileContentWithoutOurComments = buildString {
            append(fileContent)
            removeCommentsAddedByUs()
        }

        val ranges = fileContentWithoutOurComments.findRanges(isKotlinDsl = isKotlinDsl)

        //TODO: Based on the ranges, build a list of indexes and data where to insert the available comments,
        // so that we can do the inseration in reverse.

        settingsPluginsUpdates.forEach {

            it.pluginId
            it.currentVersion
            fileContentWithoutOurComments
        }

        //TODO: Let's try with regex

        //TODO: Keep track of lambda/DSL nesting level, and look for

        //TODO: Locate the pluginManagement block, if any,
        // then, keep track of lambda/DSL nesting level, and look for the plugins block boundaries.

        //TODO: Should we have external data structure hold the indexes and what they represent?

        //TODO: Regardless, once either of the above is done,
        // iterate in reverse to safely append the comments for available updates.

        TODO("Implement, using code in LegacyBootstrapUpdater as an inspiration source.")
    }

    private fun StringBuilder.insertAvailableVersionComments(
        indexAfterPluginsBlockOpeningBrace: Int,
        indexOfPluginsBlockClosingBrace: Int,
        isKotlinDsl: Boolean,
        nonCodeRanges: List<Range>,
        settingsPluginsUpdates: List<PluginWithVersionCandidates>
    ) {
        val commentRanges = nonCodeRanges.filterIsInstance<Range.Comment>()
        val stringLiteralRanges = nonCodeRanges.filterIsInstance<Range.StringLiteral>()
        //start
        //lastIndexOf()
        substring(indexAfterPluginsBlockOpeningBrace, indexOfPluginsBlockClosingBrace).lineSequence()
        TODO()
    }

    /** Removes comments previously added by refreshVersions. */
    internal fun StringBuilder.removeCommentsAddedByUs() {
        val startOfRefreshVersionsCommentLines = "\n////"
        var startIndex = 0
        while (true) {
            val indexOfComment = indexOf(startOfRefreshVersionsCommentLines, startIndex = startIndex)
            if (indexOfComment == -1) return
            //TODO: Also check for `# available:` and spaces in between, and skip safely if it's not there.
            startIndex = indexOfComment
            val indexOfEndOfLine = indexOf(
                "\n",
                startIndex = indexOfComment + startOfRefreshVersionsCommentLines.length
            ).takeIf { it >= 0 }
            replace(
                /* start = */ indexOfComment,
                /* end = */ indexOfEndOfLine ?: length,
                /* str = */""
            )
        }
    }

    internal fun CharSequence.findRanges(
        isKotlinDsl: Boolean
    ): List<Range> = mutableListOf<Range>().also {
        val text: CharSequence = this
        val canBlockCommentsBeNested = isKotlinDsl
        val isGroovyDsl = isKotlinDsl.not()

        var isNextCharacterEscaped = false
        var currentRangeStartIndex = 0

        var stringLiteralState: StringLiteralState = StringLiteralState.None

        var isInEndOfLineComment = false
        var blockCommentDepth = 0

        fun addCodeChunk(endIndex: Int) {
            val range = Range.CodeChunk(
                startIndex = currentRangeStartIndex,
                endIndex = endIndex
            )
            it.add(range)
            currentRangeStartIndex = endIndex
        }

        fun addCommentRange(endIndex: Int) {
            val commentRange = Range.Comment(
                startIndex = currentRangeStartIndex,
                endIndex = endIndex
            )

            it.add(commentRange)
            currentRangeStartIndex = endIndex
        }

        fun addStringLiteralRange(endIndex: Int) {
            val stringLiteralRange = Range.StringLiteral(
                startIndex = currentRangeStartIndex,
                endIndex = endIndex
            )

            it.add(stringLiteralRange)
            stringLiteralState = StringLiteralState.None
            currentRangeStartIndex = endIndex
        }

        forEachIndexedSkippable { index, c ->
            if (isInEndOfLineComment) check(blockCommentDepth == 0)
            if (blockCommentDepth > 0) check(isInEndOfLineComment.not())
            if (isNextCharacterEscaped) {
                isNextCharacterEscaped = false
                return@forEachIndexedSkippable
            }
            if (c == '\\') {
                isNextCharacterEscaped = true
                return@forEachIndexedSkippable
            }
            when {
                stringLiteralState != StringLiteralState.None -> when (stringLiteralState) {
                    StringLiteralState.Ordinary.GroovySingleQuotes -> if (c == '\'') {
                        addStringLiteralRange(endIndex = index + 1)
                    }
                    StringLiteralState.Ordinary.DoubleQuotes -> if (c == '"') {
                        addStringLiteralRange(endIndex = index + 1)
                    }
                    StringLiteralState.Raw -> if (text.startsWith(tripleQuotes, startIndex = index)) {
                        skipIteration(offset = 2)
                        addStringLiteralRange(endIndex = index + 3)
                    }
                    else -> error("sorry dollar slashy. BTW, how did you even get there??")
                }
                isInEndOfLineComment -> if (c == '\n') {
                    addCommentRange(endIndex = index + 1)
                    isInEndOfLineComment = false
                }
                blockCommentDepth > 0 -> when {
                    canBlockCommentsBeNested && text.startsWith("/*", startIndex = index) -> {
                        blockCommentDepth++
                        skipIteration(offset = 1)
                    }
                    text.startsWith("*/", startIndex = index) -> {
                        blockCommentDepth--
                        skipIteration(offset = 1)
                        if (blockCommentDepth == 0) {
                            addCommentRange(endIndex = index + 2)
                        }
                    }
                }
                else -> {
                    when {
                        text.startsWith("//", startIndex = index) -> {
                            isInEndOfLineComment = true
                            skipIteration(offset = 1)
                        }
                        text.startsWith("/*", startIndex = index) -> {
                            blockCommentDepth = 1
                            skipIteration(offset = 1)
                        }
                        text.startsWith(tripleQuotes, startIndex = index) -> {
                            stringLiteralState = StringLiteralState.Raw
                            skipIteration(offset = 2)
                        }
                        text.startsWith(singleQuote, startIndex = index) -> {
                            stringLiteralState = StringLiteralState.Ordinary.DoubleQuotes
                        }
                        isGroovyDsl && text.startsWith("'", startIndex = index) -> {
                            stringLiteralState = StringLiteralState.Ordinary.GroovySingleQuotes
                        }
                        else -> return@forEachIndexedSkippable
                    }
                    if (currentRangeStartIndex != index) {
                        addCodeChunk(endIndex = index)
                    }
                    currentRangeStartIndex = index
                }
            }
        }
        if (blockCommentDepth > 0 || isInEndOfLineComment) {
            addCommentRange(endIndex = length)
        } else {
            addCodeChunk(endIndex = length)
        }
    }

    private interface SkippableIterationScope {
        fun skipIteration(offset: Int)
    }

    private inline fun CharSequence.forEachIndexedSkippable(
        action: SkippableIterationScope.(index: Int, c: Char) -> Unit
    ) {
        var index = 0
        val scope = object : SkippableIterationScope {
            override fun skipIteration(offset: Int) {
                index += offset
            }
        }
        while (index < length) {
            val currentIndex = index++
            val c = this[currentIndex]
            scope.action(currentIndex, c)
        }
    }

    private const val tripleQuotes = "\"\"\""
    private const val singleQuote = "\""

    /**
     * @property startIndex inclusive
     * @property endIndex exclusive
     */
    internal sealed class Range {
        abstract val startIndex: Int
        abstract val endIndex: Int

        /**
         * The code chunk will be cut-off on any string literal or comment start.
         *
         * TODO: Maybe add block depth level?
         */
        data class CodeChunk(
            override val startIndex: Int,
            override val endIndex: Int
        ) : Range()

        data class Comment(
            override val startIndex: Int,
            override val endIndex: Int
        ) : Range()

        data class StringLiteral(
            override val startIndex: Int,
            override val endIndex: Int
        ) : Range()
    }

    private sealed class StringLiteralState {
        object None : StringLiteralState()
        sealed class Ordinary : StringLiteralState() {
            object GroovySingleQuotes : Ordinary()
            object DoubleQuotes : Ordinary()

            @Deprecated("Are we sure we want to deal with that?", level = DeprecationLevel.ERROR)
            object GroovyDollarSlashy : Ordinary()
        }

        object Raw : StringLiteralState()
    }
}
