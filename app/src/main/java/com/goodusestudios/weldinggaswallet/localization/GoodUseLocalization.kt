package com.goodusestudios.weldinggaswallet.localization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import org.json.JSONObject
import java.util.Locale

/**
 * Locked portfolio-common copy. Shared-core semantic keys always win over app
 * deltas so a consuming app cannot silently fork a certified translation.
 *
 * App/domain strings remain app-owned and are supplied through [appDelta].
 */
class GoodUseCommonLocalization private constructor(
    val contractVersion: String,
    val bundleVersion: String,
    val sourceSpreadsheetId: String,
    val fallbackLocale: String,
    val locales: List<String>,
    private val entries: Map<String, Map<String, String>>,
) {
    val coreKeys: Set<String> get() = entries.keys

    fun isCoreKey(key: String): Boolean = entries.containsKey(key)

    fun resolveCore(key: String, localeTag: String): String? {
        val entry = entries[key] ?: return null
        val locale = normalizeLocale(localeTag)
        return entry[locale] ?: entry[fallbackLocale]
    }

    fun resolve(
        key: String,
        localeTag: String,
        appDelta: (String) -> String? = { null },
    ): String = resolveCore(key, localeTag) ?: appDelta(key) ?: key

    fun labelResolver(
        localeTag: String,
        appDelta: (String) -> String? = { null },
    ): (String) -> String = { key -> resolve(key, localeTag, appDelta) }

    private fun normalizeLocale(localeTag: String): String {
        val raw = localeTag
            .replace('_', '-')
            .substringBefore('-')
            .lowercase(Locale.ROOT)
        val canonical = when (raw) {
            "iw" -> "he"
            "in" -> "id"
            "no" -> "nb"
            else -> raw
        }
        return if (canonical in locales) canonical else fallbackLocale
    }

    companion object {
        const val DEFAULT_ASSET = "gooduse-common-localization-v1.json"
        const val EXPECTED_LOCALE_COUNT = 31

        fun fromJson(json: String): GoodUseCommonLocalization {
            val root = JSONObject(json)
            require(root.getString("contractVersion") == "1.0.0") {
                "Unsupported GoodUse common localization contract"
            }
            val localeArray = root.getJSONArray("locales")
            val locales = List(localeArray.length()) { localeArray.getString(it) }
            require(locales.size == EXPECTED_LOCALE_COUNT && locales.distinct().size == EXPECTED_LOCALE_COUNT) {
                "GoodUse common localization must contain exactly 31 unique selectable locales"
            }

            val fallback = root.getString("fallbackLocale")
            require(fallback in locales) { "fallbackLocale must be in locales" }

            val entriesObject = root.getJSONObject("entries")
            val entries = buildMap {
                val keys = entriesObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    require(key.startsWith("common.")) {
                        "Shared localization bundle may contain only approved common.* semantic keys: $key"
                    }
                    val item = entriesObject.getJSONObject(key)
                    val translations = item.getJSONArray("translations")
                    require(translations.length() == locales.size) {
                        "$key translation count must equal locale count"
                    }
                    put(
                        key,
                        locales.indices.associate { index ->
                            locales[index] to translations.getString(index)
                        },
                    )
                }
            }
            require(entries.isNotEmpty()) { "GoodUse common localization bundle must not be empty" }

            return GoodUseCommonLocalization(
                contractVersion = root.getString("contractVersion"),
                bundleVersion = root.getString("bundleVersion"),
                sourceSpreadsheetId = root.getString("sourceSpreadsheetId"),
                fallbackLocale = fallback,
                locales = locales,
                entries = entries,
            )
        }

        fun bundled(context: Context): GoodUseCommonLocalization =
            context.assets.open(DEFAULT_ASSET).bufferedReader(Charsets.UTF_8).use {
                fromJson(it.readText())
            }
    }
}

/**
 * Compose convenience for the shell's existing semantic `label(key)` contract.
 * The current Android locale selects the locked shared-core translation; only
 * non-core keys fall through to [appDelta].
 */
@Composable
fun rememberGoodUseLabelResolver(
    appDelta: (String) -> String? = { null },
): (String) -> String {
    val context = LocalContext.current.applicationContext
    val configuration = LocalConfiguration.current
    val localeTag = ConfigurationCompat.getLocales(configuration)[0]?.toLanguageTag() ?: "en"
    val bundle = remember(context) { GoodUseCommonLocalization.bundled(context) }
    return remember(bundle, localeTag, appDelta) {
        bundle.labelResolver(localeTag = localeTag, appDelta = appDelta)
    }
}
