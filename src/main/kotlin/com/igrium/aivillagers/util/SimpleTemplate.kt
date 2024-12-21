package com.igrium.aivillagers.util

import java.util.function.Supplier

/**
 * A simple string template implementation that replaces values surrounded by `{...}`
 */
class SimpleTemplate() {

    private val regex = "(?<!\\\\)\\{(.*?)}".toRegex()

    private val replacements = HashMap<String, () -> String?>();

    fun add(key: String, value: () -> String?) {
        replacements[key] = value
    }

    fun add(key: String, value: Supplier<String?>) {
        replacements[key] = value::get
    }

    fun add(key: String, value: String?) {
        if (value == null) return;
        replacements[key] = { value }
    }

    /**
     * Replace all templated values in a template string with the values in this template object.
     * @param template Template string
     * @param fallback A fallback string to use for any unset values. If `null`, literal string from template is used.
     * @return Literal string
     */
    @JvmOverloads
    fun render(template: String, fallback: String? = null): String {
        val matches = regex.findAll(template).toList()
        if (matches.isEmpty()) return template

        val builder = StringBuilder()

        var head = 0 // The next index to add to the builder.
        for (match in matches) {
            val replacement = replacements[match.groupValues[1]] ?: { null }
            var literal = replacement()
            if (literal == null) {
                if (fallback != null) literal = fallback else continue;
            }

            // Append everything since previous match
            builder.append(template.substring(head, match.range.first))
            builder.append(literal)
            head = match.range.last + 1
        }

        // There's still some characters left
        if (head < template.length - 1) {
            builder.append(template.substring(head, template.length - 1))
        }

        return builder.toString()
    }
}