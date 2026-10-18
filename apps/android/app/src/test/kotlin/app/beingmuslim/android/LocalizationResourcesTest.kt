package app.beingmuslim.android

import app.beingmuslim.android.ui.Language
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Integrity of the committed translations (issue #14). Every user-facing string
 * has to exist in both supported languages and keep its format arguments — the
 * part of "every string exists in both locales" a machine can check. Whether
 * the Bengali and Arabic wording is *correct* is a human review, not this test's
 * business (`docs/PRODUCT_REQUIREMENTS.md` §8, §13).
 *
 * These cases read the real committed files, so a missing `values-bn/` string,
 * a dropped `%1$s`, or a language that was declared but never translated fails
 * the build.
 */
class LocalizationResourcesTest {

    private val english: Strings by lazy { parse(stringsFile("values")) }
    private val bengali: Strings by lazy { parse(stringsFile("values-bn")) }

    @Test
    fun everyTranslatableStringIsInBothLanguages() {
        assertEquals(
            "translated in values-bn/ but missing from values/",
            emptySet<String>(),
            bengali.translatable.keys - english.translatable.keys,
        )
        assertEquals(
            "in values/ but not translated in values-bn/",
            emptySet<String>(),
            english.translatable.keys - bengali.translatable.keys,
        )
    }

    @Test
    fun everyFormatArgumentSurvivesTranslation() {
        english.translatable.forEach { (name, text) ->
            // Sorted because a translation may reorder them: the arguments are
            // positional, so what has to survive is which ones, not their order.
            assertEquals(
                "$name keeps the same format arguments in values-bn/",
                arguments(text).sorted(),
                arguments(bengali.translatable.getValue(name)).sorted(),
            )
        }
    }

    @Test
    fun stringsThatAreTheSameInEveryLanguageAreNotDuplicated() {
        assertEquals(
            "values-bn/ redeclares strings marked translatable=\"false\"; they are the same in every language",
            emptySet<String>(),
            bengali.notTranslated,
        )
    }

    @Test
    fun everySupportedLanguageIsDeclaredAndTranslated() {
        assertEquals(
            "res/xml/locale_config.xml lists languages other than the supported set",
            Language.entries.map { it.tag },
            declaredLocales(),
        )
        Language.entries.forEach { language ->
            // The app's fallback locale is values/, so only the others need a
            // directory of their own. A language with a script or region
            // qualifier (e.g. b+zh+Hans) would need this expectation widened.
            if (language != Language.ENGLISH) {
                val file = stringsFile("values-${language.tag}")
                assertTrue("${language.tag} is declared but ${file.path} does not exist", file.isFile)
            }
        }
    }

    private class Strings(
        val translatable: Map<String, String>,
        val notTranslated: Set<String>,
    )

    private fun stringsFile(qualifier: String): File = resFile("$qualifier/strings.xml")

    private fun resFile(relative: String): File {
        val path = "src/main/res/$relative"
        return listOf(File(path), File("app/$path"))
            .firstOrNull(File::isFile)
            ?: error("$path not found relative to '${File("").absolutePath}'")
    }

    private fun parse(file: File): Strings {
        val translatable = mutableMapOf<String, String>()
        val notTranslated = mutableSetOf<String>()
        elementsOf(file, "string").forEach { element ->
            val name = element.getAttribute("name")
            if (element.getAttribute("translatable") == "false") {
                notTranslated += name
            } else {
                translatable[name] = element.textContent
            }
        }
        return Strings(translatable, notTranslated)
    }

    private fun declaredLocales(): List<String> =
        elementsOf(resFile("xml/locale_config.xml"), "locale")
            .map { it.getAttributeNS(ANDROID_NAMESPACE, "name") }

    private fun elementsOf(file: File, tag: String): List<Element> {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // These are committed files with no doctype; refuse one if it appears.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }.newDocumentBuilder().parse(file)
        val found = document.getElementsByTagName(tag)
        return (0 until found.length).map { found.item(it) as Element }
    }

    /** The `%1$s`-style arguments in [text], in the order they appear. */
    private fun arguments(text: String): List<String> =
        FORMAT_ARGUMENTS.findAll(text).map { it.value }.toList()

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val FORMAT_ARGUMENTS = Regex("""%\d+\$[a-zA-Z]""")
    }
}
