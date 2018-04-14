package org.javacs.kt.position

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.javacs.kt.util.toPath
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import kotlin.math.max

/**
 * Convert from 0-based line and column to 0-based offset
 */
fun offset(content: String, line: Int, char: Int): Int {
    assert(!content.contains('\r'))

    val reader = content.reader()
    var offset = 0

    var lineOffset = 0
    while (lineOffset < line) {
        val nextChar = reader.read()

        if (nextChar == -1)
            throw RuntimeException("Reached end of file before reaching line $line")

        if (nextChar.toChar() == '\n')
            lineOffset++

        offset++
    }

    var charOffset = 0
    while (charOffset < char) {
        val nextChar = reader.read()

        if (nextChar == -1)
            throw RuntimeException("Reached end of file before reaching char $char")

        charOffset++
        offset++
    }

    return offset
}

fun position(content: String, offset: Int): Position {
    val reader = content.reader()
    var line = 0
    var char = 0

    var find = 0
    while (find < offset) {
        val nextChar = reader.read()

        if (nextChar == -1)
            throw RuntimeException("Reached end of file before reaching offset $offset")

        find++
        char++

        if (nextChar.toChar() == '\n') {
            line++
            char = 0
        }
    }

    return Position(line, char)
}

fun range(content: String, range: TextRange) =
        Range(position(content, range.startOffset), position(content, range.endOffset))

fun location(declaration: DeclarationDescriptor): Location? {
    val target = declaration.findPsi() ?: return null

    return location(target)
}

fun location(expr: PsiElement): Location {
    val content = expr.containingFile.text
    val file = expr.containingFile.toPath().toUri().toString()

    return Location(file, range(content, expr.textRange))
}

/**
 * Region that has been changed
 */
fun changedRegion(oldContent: String, newContent: String): Pair<TextRange, TextRange>? {
    if (oldContent == newContent) return null

    val prefix = oldContent.commonPrefixWith(newContent).length
    val suffix = oldContent.commonSuffixWith(newContent).length
    val oldEnd = max(oldContent.length - suffix, prefix)
    val newEnd = max(newContent.length - suffix, prefix)

    return Pair(TextRange(prefix, oldEnd), TextRange(prefix, newEnd))
}