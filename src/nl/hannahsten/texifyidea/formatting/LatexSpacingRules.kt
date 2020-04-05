package nl.hannahsten.texifyidea.formatting

import com.intellij.formatting.Spacing
import com.intellij.psi.codeStyle.CodeStyleSettings
import nl.hannahsten.texifyidea.LatexLanguage
import nl.hannahsten.texifyidea.psi.LatexEnvironment
import nl.hannahsten.texifyidea.psi.LatexEnvironmentContent
import nl.hannahsten.texifyidea.psi.LatexTypes.*
import nl.hannahsten.texifyidea.settings.codestyle.LatexCodeStyleSettings
import nl.hannahsten.texifyidea.util.Magic
import nl.hannahsten.texifyidea.util.firstParentOfType
import nl.hannahsten.texifyidea.util.getIndent
import nl.hannahsten.texifyidea.util.inDirectEnvironment
import kotlin.math.max

/**
 *
 * @author Sten Wessel, Abby Berkers
 */
fun createSpacingBuilder(settings: CodeStyleSettings): TexSpacingBuilder {
    fun createSpacing(minSpaces: Int, maxSpaces: Int, minLineFeeds: Int, keepLineBreaks: Boolean, keepBlankLines: Int): Spacing =
            Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)

    val latexSettings = settings.getCustomSettings(LatexCodeStyleSettings::class.java)
    val latexCommonSettings = settings.getCommonSettings(LatexLanguage.INSTANCE)

    return rules(latexCommonSettings) {

        custom {
            customRule { parent, _, right ->
                // Don't insert or remove spaces inside the text in a verbatim environment.
                if (parent.node?.elementType === NORMAL_TEXT) {
                    if (parent.node?.psi?.inDirectEnvironment(Magic.Environment.verbatim) == true) {
                        return@customRule Spacing.getReadOnlySpacing()
                    }
                }
                // Don't insert or remove spaces in front of the first word in a verbatim environment.
                else if (right.node?.elementType === ENVIRONMENT_CONTENT) {
                    if (right.node?.psi?.inDirectEnvironment(Magic.Environment.verbatim) == true) {
                        return@customRule Spacing.getReadOnlySpacing()
                    }
                }
                return@customRule null
            }
        }

        simple {
            between(NORMAL_TEXT_WORD, NORMAL_TEXT_WORD).spaces(1)
            before(ENVIRONMENT_CONTENT).lineBreakInCode()
        }

        custom {
            // Insert a new line between the end of environment content and the end command.
            inPosition(parent = ENVIRONMENT, left = ENVIRONMENT_CONTENT, right = END_COMMAND).spacing(
                    Spacing.createSpacing(0, Int.MAX_VALUE, 1, latexCommonSettings.KEEP_LINE_BREAKS, latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            )
        }

        custom {
            fun commentSpacing(minSpaces: Int): Spacing {
                if (latexCommonSettings.KEEP_FIRST_COLUMN_COMMENT) {
                    return Spacing.createKeepingFirstColumnSpacing(minSpaces, Int.MAX_VALUE, latexCommonSettings.KEEP_LINE_BREAKS, latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
                }
                return createSpacing(minSpaces, Int.MAX_VALUE, 0, latexCommonSettings.KEEP_LINE_BREAKS, latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            }

            inPosition(right = COMMENT_TOKEN).spacing(commentSpacing(0))
        }

        custom {
            // Make sure the number of new lines before a sectioning command is
            // as much as the user specified in the settings.
            // BUG? Does not work for a command that immediately follows
            // \begin{document}. But no one should start their document like
            // that anyway.
            customRule { _, _, right ->
                LatexCodeStyleSettings.blankLinesOptions.forEach {
                    if (right.node?.text?.matches(Regex("\\" + "${it.value}\\{.*\\}")) == true) {
                        return@customRule createSpacing(
                                minSpaces = 0,
                                maxSpaces = Int.MAX_VALUE,
                                minLineFeeds = it.key.get(latexSettings) + 1,
                                keepLineBreaks = false,
                                keepBlankLines = 0)
                    }
                }
                return@customRule null
            }
        }

        // Align & in tables
        // Unfortunately we have to do this manually because Alignment only aligns characters if they are the first non-whitespace in a line of code
        custom {
            customRule { parent, left, right ->
                // Check if parent is in environment content of a table environment
                if (parent.node?.psi?.firstParentOfType(LatexEnvironmentContent::class)?.firstParentOfType(LatexEnvironment::class)?.environmentName !in Magic.Environment.tableEnvironments) return@customRule null

                if (right.node?.text != "&") return@customRule null

                val contentElement = parent.node?.psi?.firstParentOfType(LatexEnvironmentContent::class)
                val content = contentElement?.text ?: return@customRule null
                val contentTextOffset = contentElement.textOffset
                val tableLineSeparator = "\\\\"
                val contentLines = content.split(tableLineSeparator).toMutableList()
                if (contentLines.size < 2) return@customRule null
                val indent = contentLines[1].getIndent()

                // Fix environment content not starting with indent
                contentLines[0] = indent + contentLines.first()
                // Remove all duplicate white spaces around ampersands
//                val contentLines = originalContentLines.map {
//                    indent + it.replace("""\s+""".toRegex(), " ").drop(1)
//                }
                // Find indices of &, relative to line start and text offset

                // We added fake content, but it doesn't count for the textoffset
                var currentOffset = contentTextOffset - indent.length
                val ampersandIndicesPerLine = contentLines.map { line ->
                    val indices = mutableListOf<Triple<Int, Int, Int>>()
                    for (indexValue in line.withIndex()) {
                        if (indexValue.value == '&') {
                            // Get the endoffset of the last non-whitespace character before this ampersand.
                            // This is the size of the indent (the start of the line) if the ampersand is the first non whitespace character.
                            val lastNonWhiteSpace = (indexValue.index - 1 downTo 0).firstOrNull { !line[it].isWhitespace() }
                                    ?.plus(1)
                                    ?: indent.length - 1

                            indices.add(Triple(lastNonWhiteSpace, indexValue.index, currentOffset))
                        }
                        currentOffset++
                    }
                    currentOffset += tableLineSeparator.length
                    indices.toList()
                }

                // Get the desired index of the first &, second &, etc.
                // Relative to the line start
                // Because the placement of the second & depends on the placement of the first one,
                // we have to take them one by one
                val levelIndices = mutableListOf<Int>()
                val numberOfAmpersands = contentLines.first().count { it == '&' }
                for (level in (0 until numberOfAmpersands)) {
                    for (initialRelativeAndAbsoluteIndices in ampersandIndicesPerLine) {
                        if (level >= initialRelativeAndAbsoluteIndices.size) continue

                        // Get new index of this level based on movements of lower levels of this line

                        // Total added extra spaces on this line so far
                        val totalAddedSpaces = if (level == 0) 0 else listOf(0, levelIndices[level - 1] - initialRelativeAndAbsoluteIndices[level - 1].first, levelIndices[level - 1] - initialRelativeAndAbsoluteIndices[level - 1].second).min() ?: 0

                        val newIndex = totalAddedSpaces + initialRelativeAndAbsoluteIndices[level].first

                        // If this level does not exist yet
                        if (level >= levelIndices.size) {
                            levelIndices.add(newIndex)
                        }
                        // If the index is larger than the largest one so far
                        else if (newIndex > levelIndices[level]) {
                            levelIndices[level] = newIndex
                        }
                    }
                }

                // Find level of 'right' block (which is a &)
                val rightElementOffset = right.node?.psi?.textOffset ?: return@customRule null
                // Current and desired index relative to line start, e.g. in case 'asdf__&` is desired, index 6
                var spaces: Int? = null
                for (initialRelativeAndAbsoluteIndices in ampersandIndicesPerLine) {
                    for (level in initialRelativeAndAbsoluteIndices.indices) {
                        // Try to find the offset of the right & in the list of all & offsets
                        if (initialRelativeAndAbsoluteIndices[level].third == rightElementOffset) {
                            val totalAddedSpaces = if (level == 0) 0 else levelIndices[level - 1] - initialRelativeAndAbsoluteIndices[level - 1].first

                            val currentLevelIndex = totalAddedSpaces + initialRelativeAndAbsoluteIndices[level].first
                            val desiredLevelIndex = if (level < levelIndices.size) {
                                levelIndices[level]
                            }
                            else {
                                break
                            }

                            // +1 for the space which always has to be there
                            spaces = max(1, desiredLevelIndex - currentLevelIndex + 1)
                            break
                        }
                    }
                }

                if (spaces == null) return@customRule null

                return@customRule createSpacing(
                        minSpaces = spaces,
                        maxSpaces = spaces,
                        minLineFeeds = 0,
                        keepLineBreaks = latexCommonSettings.KEEP_LINE_BREAKS,
                        keepBlankLines = latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            }
        }
    }
}