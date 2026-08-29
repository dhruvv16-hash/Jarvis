package com.jarvispoc.flows

import com.jarvispoc.core.AgentLog
import com.jarvispoc.core.FlowResult
import com.jarvispoc.core.Selector
import com.jarvispoc.core.UiNode
import com.jarvispoc.core.query
import com.jarvispoc.service.ActionExecutor
import kotlinx.coroutines.delay
import java.net.URLEncoder

/**
 * Search Amazon for the requested product, inspect visible search results,
 * pick the FIRST product whose title is a reasonable match (skipping non-matching
 * sponsored results), verify the title on the product detail page, add to cart,
 * open the cart, and verify the item is present before reporting success.
 *
 * Does not proceed to checkout or payment.
 */
class AmazonOrderFlow(private val searchQuery: String) : Flow {

    override val name: String = "Amazon Add to Cart"

    override suspend fun run(x: ActionExecutor, autoConfirm: Boolean): FlowResult {
        val pkg = PACKAGES.firstOrNull { x.isInstalled(it) }
            ?: return FlowResult.Failed("launch", "no Amazon shopping app installed")
        AgentLog.info("using Amazon package $pkg")

        val query = searchQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.isBlank()) {
            return FlowResult.Failed("search", "search query is empty")
        }
        AgentLog.info("searching Amazon for: \"$query\"")

        // 1 — Deep-link straight into in-app search results
        val encoded = URLEncoder.encode(query, "UTF-8")
        if (!x.launchUri("https://www.amazon.in/s?k=$encoded", pkg)) {
            return FlowResult.Failed("search", "could not open the search deep link")
        }
        if (!x.awaitPackage(pkg)) {
            return FlowResult.Failed(
                "search",
                "Amazon never came to the foreground — the app may not handle /s?k= deep links",
            )
        }
        delay(SETTLE_MS)
        x.dismissInterstitials()

        // If Amazon opened search suggestions screen, tap first suggestion to load product results
        val suggestion = x.awaitNode(
            query(
                "search suggestion",
                Selector(id = "sac-suggestion-row-1"),
                Selector(id = "sac-suggestion-row-1-cell-1"),
                Selector(id = "sac-suggestion-row-2"),
                Selector(id = "sac-suggestion-row-2-cell-1"),
                Selector(id = "sac-suggestion-row-3"),
                Selector(id = "search_suggestions_frame_layout"),
            ),
            timeoutMs = 2_500,
        )
        if (suggestion != null) {
            AgentLog.info("Tapping search suggestion '${suggestion.label}' to load full search results...")
            x.tapAt(suggestion.centerX, suggestion.centerY)
            x.tap(suggestion)
            delay(SETTLE_MS + 1_000)
            x.dismissInterstitials()
        }

        // 2 — Find the FIRST search result whose title matches the requested product
        var matchedProductNode: UiNode? = null
        var matchedTitle: String = ""

        val maxScanScrolls = 4
        for (scroll in 0..maxScanScrolls) {
            val snapshot = x.snapshot()
            val match = findMatchingProductCard(snapshot, query)
            if (match != null) {
                matchedProductNode = match.first
                matchedTitle = match.second
                AgentLog.success("found matching product: \"$matchedTitle\"")
                break
            }
            if (scroll < maxScanScrolls) {
                AgentLog.info("no match on visible screen (scroll $scroll/$maxScanScrolls) — scrolling down")
                x.swipe(500, 1800, 500, 700)
                delay(1_200)
                x.dismissInterstitials()
            }
        }

        if (matchedProductNode == null) {
            AgentLog.error("no search result reasonably matched \"$query\" — refusing to add a random product")
            return FlowResult.Failed(
                "search_match",
                "no matching product found for \"$query\" among search results",
            )
        }

        // 3 — Open the matching product
        AgentLog.step("Opening product: \"$matchedTitle\"")
        if (!x.tapAt(matchedProductNode.centerX, matchedProductNode.centerY) && !x.tap(matchedProductNode)) {
            return FlowResult.Failed("results", "could not tap product card")
        }
        delay(SETTLE_MS + 500)
        x.dismissInterstitials()

        // 4 — Fresh observation on Product Detail Page: verify title matches requested product
        val productPageTitle = verifyProductPageTitle(x, query)
        if (productPageTitle == null) {
            AgentLog.error("product page title does not match requested query \"$query\" — aborting")
            return FlowResult.Failed(
                "product_verify",
                "product page title did not match requested query \"$query\"",
            )
        }
        AgentLog.success("verified product page title: \"$productPageTitle\"")

        // 5 — Add to Cart (scroll down until Add to Cart button is visible)
        AgentLog.step("Locating 'Add to Cart' button...")
        val addToCart = x.scrollUntilVisible(ADD_TO_CART, maxScrolls = 8)
            ?: return FlowResult.Failed("product", "'Add to Cart' button not found on product page")

        if (!x.tapAt(addToCart.centerX, addToCart.centerY) && !x.tap(addToCart)) {
            return FlowResult.Failed("product", "could not tap 'Add to Cart'")
        }
        delay(SETTLE_MS)
        x.dismissInterstitials(rounds = 2)

        // 6 — Open Cart and verify the requested product is present
        AgentLog.step("Opening cart to verify product...")
        val cartTab = x.awaitNode(
            query("cart tab", Selector(desc = "Cart Tab"), Selector(desc = "Cart"), Selector(textContains = "Cart")),
            timeoutMs = 1_500,
        )
        if (cartTab != null) {
            x.tapAt(cartTab.centerX, cartTab.centerY)
            x.tap(cartTab)
        } else {
            x.launchUri("https://www.amazon.in/gp/cart/view.html", pkg)
        }
        if (!x.awaitPackage(pkg)) {
            return FlowResult.Failed("cart", "left Amazon on the way to cart")
        }
        delay(SETTLE_MS + 500)
        x.dismissInterstitials()

        val cartVerified = verifyProductInCart(x, query)
        if (!cartVerified) {
            AgentLog.error("cart verification failed: \"$query\" not found in cart")
            return FlowResult.Failed(
                "cart_verify",
                "could not verify that \"$query\" is present in the cart",
            )
        }

        AgentLog.success("Verified: \"$query\" is in the Amazon cart!")
        return FlowResult.Success("Added and verified in Amazon cart: \"$productPageTitle\"")
    }

    companion object {
        const val MAX_QUERY_LENGTH = 80
        const val SETTLE_MS = 1_800L

        val PACKAGES = listOf(
            "in.amazon.mShop.android.shopping",
            "com.amazon.mShop.android.shopping",
        )

        val STOP_WORDS = setOf(
            "a", "an", "the", "for", "in", "to", "and", "with", "of", "on", "me",
            "add", "cart", "buy", "order", "please", "my", "from", "item", "product",
        )

        val ADD_TO_CART = query(
            "Add to Cart",
            Selector(id = "add_to_cart"),
            Selector(id = "add-to-cart-button"),
            Selector(text = "Add to Cart"),
            Selector(textContains = "Add to Cart"),
            Selector(textContains = "Add to cart"),
            Selector(desc = "Add to Cart"),
            Selector(desc = "Add to cart"),
        )

        /**
         * Checks if [candidate] title is a reasonable match for [query].
         * Tokenizes and ignores common stop words, requiring essential keywords
         * (e.g. brand + item name) to be present.
         */
        fun isReasonableMatch(candidate: String, query: String): Boolean {
            val queryTokens = extractKeywords(query)
            if (queryTokens.isEmpty()) return false

            val candidateNormalized = " " + normalize(candidate) + " "
            if (candidateNormalized.isBlank()) return false

            var matchedCount = 0
            for (token in queryTokens) {
                if (candidateNormalized.contains(token)) {
                    matchedCount++
                }
            }

            val minMatch = when {
                queryTokens.size <= 2 -> queryTokens.size
                queryTokens.size == 3 -> 2
                else -> (queryTokens.size * 3) / 4
            }
            return matchedCount >= minMatch
        }

        fun extractKeywords(text: String): List<String> {
            return normalize(text)
                .split("\\s+".toRegex())
                .filter { it.length >= 1 && it !in STOP_WORDS }
        }

        fun normalize(text: String): String {
            return text.lowercase()
                .replace("[^a-z0-9\\s]".toRegex(), " ")
                .trim()
        }

        /**
         * Scans visible search result nodes from top to bottom and returns the first
         * node whose title reasonably matches the query.
         */
        fun findMatchingProductCard(nodes: List<UiNode>, query: String): Pair<UiNode, String>? {
            val sortedNodes = nodes
                .filter {
                    it.bounds.height() > 20 && it.bounds.width() > 20 &&
                        it.bounds.top >= 360 && it.bounds.bottom <= 2900 &&
                        !it.viewId.contains("search", ignoreCase = true) &&
                        !it.viewId.contains("chrome", ignoreCase = true) &&
                        !it.viewId.contains("suggestion", ignoreCase = true) &&
                        !it.viewId.contains("autocomplete", ignoreCase = true)
                }
                .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))

            for (node in sortedNodes) {
                val label = node.label.trim()
                if (label.length < 10) continue
                if (label.startsWith("http") || label.contains("Search Amazon") || label.contains("Deliver to")) continue
                if (label.equals("Sponsored", ignoreCase = true) || label.equals("Results", ignoreCase = true) || label.contains("filters", ignoreCase = true)) continue

                if (isReasonableMatch(label, query)) {
                    return Pair(node, label)
                }
            }
            return null
        }

        /**
         * Verifies that the currently open Product Detail Page has a title matching the query.
         */
        suspend fun verifyProductPageTitle(x: ActionExecutor, query: String): String? {
            val deadline = System.currentTimeMillis() + 8_000
            while (System.currentTimeMillis() < deadline) {
                val snapshot = x.snapshot()
                for (node in snapshot) {
                    val label = node.label.trim()
                    if (label.length >= 10 && isReasonableMatch(label, query)) {
                        return label
                    }
                }
                // Try scrolling up in case the page loaded scrolled down
                x.swipe(500, 800, 500, 1800)
                delay(800)
                val snapshotAfterScroll = x.snapshot()
                for (node in snapshotAfterScroll) {
                    val label = node.label.trim()
                    if (label.length >= 10 && isReasonableMatch(label, query)) {
                        return label
                    }
                }
                delay(500)
            }
            return null
        }

        /**
         * Verifies that an item matching the query is present in the Amazon cart.
         */
        suspend fun verifyProductInCart(x: ActionExecutor, query: String): Boolean {
            val deadline = System.currentTimeMillis() + 8_000
            while (System.currentTimeMillis() < deadline) {
                val snapshot = x.snapshot()
                for (node in snapshot) {
                    val label = node.label.trim()
                    if (label.length >= 10 && isReasonableMatch(label, query)) {
                        AgentLog.info("verified item in cart: \"$label\"")
                        return true
                    }
                }
                x.swipe(500, 1600, 500, 1000)
                delay(800)
            }
            return false
        }
    }
}
