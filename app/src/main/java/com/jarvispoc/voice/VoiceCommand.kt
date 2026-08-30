package com.jarvispoc.voice

/**
 * What the user asked for, extracted from a spoken phrase.
 *
 * Keyword matching, not an LLM. Same reasoning as the scripted flows: the open
 * question in this POC is whether we can drive real apps reliably, and a parser
 * whose behaviour you can predict from reading it keeps that question isolated.
 * Gemma is expensive enough already without spending a second inference pass on
 * a sentence we can classify with `contains`.
 */
data class VoiceCommand(
    val raw: String,
    val target: Target,
    val useMostRecentPhoto: Boolean,
    val autoCaption: Boolean,
    val tone: String,
    /** Product to search for, when the phrase named one. Null otherwise. */
    val searchQuery: String?,
) {
    enum class Target { INSTAGRAM, AMAZON, UNKNOWN }

    /** One-line description of what we understood, for the trace. */
    val summary: String
        get() = "target=$target, mostRecentPhoto=$useMostRecentPhoto, " +
            "autoCaption=$autoCaption, tone='$tone', product=${searchQuery?.let { "'$it'" } ?: "none"}"

    companion object {

        private const val DEFAULT_TONE = "warm, understated, a little witty"

        // No bare "gram": it is already covered by "instagram" and would also
        // fire on program / telegram / grammar.
        private val INSTAGRAM_WORDS = listOf("instagram", "insta", " ig ")
        private val AMAZON_WORDS = listOf("amazon", "order", "buy", "purchase", "cart")
        private val RECENT_WORDS = listOf(
            "most recent", "latest", "last photo", "last picture", "newest",
            "recent photo", "recent picture", "just took", "just clicked",
        )
        private val CAPTION_WORDS = listOf("caption", "description", "write something")

        /** Spoken tone adjectives mapped to a prompt fragment. */
        private val TONES = listOf(
            "funny" to "funny and playful",
            "witty" to "dry and witty",
            "professional" to "professional and restrained",
            "formal" to "professional and restrained",
            "casual" to "casual and conversational",
            "poetic" to "lyrical and image-led",
            "short" to "very short, under ten words",
            "minimal" to "very short, under ten words",
            "excited" to "upbeat and enthusiastic",
        )

        /** Longest first, so "order me a" wins over "order". */
        private val PRODUCT_TRIGGERS = listOf(
            "search for", "search",
            "order me a", "order me an", "order me", "order a", "order an", "order the", "order",
            "buy me a", "buy me an", "buy me", "buy a", "buy an", "buy the", "buy",
            "purchase a", "purchase an", "purchase the", "purchase",
            "get me a", "get me an", "get me the", "get me",
            "add a", "add an", "add",
        ).sortedByDescending { it.length }

        /** Trailing noise to strip off an extracted product. */
        private val TRAILING_NOISE = listOf(
            "on amazon india", "from amazon india", "on the amazon app",
            "to my amazon cart", "to my cart", "to the cart", "to cart",
            "on amazon", "from amazon", "in amazon", "on amazon in",
            "for me", "please", "now",
        )

        private val LEADING_ARTICLES = listOf("a ", "an ", "the ", "some ", "me ")

        /**
         * Placeholders that parse as a product but name nothing. Searching
         * Amazon for "something" and walking it to checkout is worse than
         * asking again.
         */
        private val VAGUE_PRODUCTS = setOf(
            "something", "anything", "stuff", "things", "it", "that", "this", "one",
        )

        /** Shorter than this is almost certainly a mis-transcription, not a product. */
        private const val MIN_PRODUCT_LENGTH = 3

        private val PRICE_PATTERN = Regex(
            """(?i)\b(under|below)\s*(?:₹|rs\.?|inr)?\s*(\d[\d,]*(?:\.\d+)?)\s*(?:rupees|rs\.?|inr)?"""
        )

        private fun cleanProductString(raw: String): String {
            var rest = raw.trim()
            var changed = true
            while (changed) {
                changed = false
                for (noise in TRAILING_NOISE) {
                    if (rest.endsWith(noise)) {
                        rest = rest.removeSuffix(noise).trim()
                        changed = true
                    }
                }
                for (article in LEADING_ARTICLES) {
                    if (rest.startsWith(article)) {
                        rest = rest.removePrefix(article).trim()
                        changed = true
                    }
                }
            }
            return rest.trim(' ', ',', '.', '!', '?')
        }

        /**
         * Pulls the product out of e.g. "order a usb c cable on amazon".
         * If a price limit is specified (e.g. "under ₹1000", "below 1500"),
         * extracts the product and price limit into "<product> under <price>".
         *
         * Returns null when nothing survives — "place an order on amazon" names
         * no product, and guessing one when money is involved is not a service.
         */
        private fun extractProduct(text: String): String? {
            val trigger = PRODUCT_TRIGGERS.firstOrNull { text.contains(" $it ") } ?: return null
            var rest = text.substringAfter(" $trigger ").trim()

            rest = cleanProductString(rest)

            val priceMatch = PRICE_PATTERN.find(rest)
            if (priceMatch != null) {
                val rawProduct = rest.substring(0, priceMatch.range.first)
                val product = cleanProductString(rawProduct)
                val price = priceMatch.groupValues[2].replace(",", "").trim()
                return if (product.length >= MIN_PRODUCT_LENGTH && product !in VAGUE_PRODUCTS && price.isNotBlank()) {
                    "$product under $price"
                } else {
                    null
                }
            }

            val product = cleanProductString(rest)
            return product.takeIf { it.length >= MIN_PRODUCT_LENGTH && it !in VAGUE_PRODUCTS }
        }

        fun parse(spoken: String): VoiceCommand {
            // Pad so " ig " can match at either end without a word-boundary regex.
            val text = " ${spoken.lowercase().trim()} "

            val target = when {
                INSTAGRAM_WORDS.any { text.contains(it) } -> Target.INSTAGRAM
                AMAZON_WORDS.any { text.contains(it) } -> Target.AMAZON
                else -> Target.UNKNOWN
            }

            val tone = TONES.firstOrNull { text.contains(it.first) }?.second ?: DEFAULT_TONE

            return VoiceCommand(
                raw = spoken.trim(),
                target = target,
                useMostRecentPhoto = RECENT_WORDS.any { text.contains(it) },
                // "post it on instagram" implies a caption is wanted even when
                // the word itself is never spoken.
                autoCaption = CAPTION_WORDS.any { text.contains(it) } ||
                    target == Target.INSTAGRAM,
                tone = tone,
                searchQuery = if (target == Target.AMAZON) extractProduct(text) else null,
            )
        }
    }
}
