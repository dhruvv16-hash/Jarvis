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

        // 7 — Click "Proceed to Buy"
        AgentLog.step("Clicking 'Proceed to Buy'...")
        val proceedToBuy = x.awaitNode(PROCEED_TO_BUY, timeoutMs = 4_000)
            ?: x.scrollUntilVisible(PROCEED_TO_BUY, maxScrolls = 4)
            ?: return FlowResult.Failed("cart", "could not find 'Proceed to Buy' button in cart")

        if (!x.tapAt(proceedToBuy.centerX, proceedToBuy.centerY) && !x.tap(proceedToBuy)) {
            return FlowResult.Failed("cart", "could not tap 'Proceed to Buy'")
        }
        delay(SETTLE_MS + 1_000)
        x.dismissInterstitials()

        // 8 — Navigate through checkout to Payment screen, choose Cash on Delivery, and tap Continue
        var codSelected = false
        var continueTapped = false

        for (step in 1..MAX_CHECKOUT_STEPS) {
            // Check if we are already parked on final review screen
            val placeOrder = x.awaitNode(PLACE_ORDER, timeoutMs = 1_000)
            if (placeOrder != null && codSelected && continueTapped) {
                AgentLog.success("Parked on final review screen with Cash on Delivery selected!")
                return FlowResult.AwaitingUser(
                    "Parked on final review screen with Cash on Delivery selected. Tap 'Place Your Order' to complete."
                )
            }

            // Look for Cash on Delivery / Pay on Delivery option
            val cod = findCodOption(x)
            if (cod != null) {
                AgentLog.step("Found Cash / Pay on Delivery option — selecting it")
                if (!x.tapAt(cod.centerX, cod.centerY) && !x.tap(cod)) {
                    AgentLog.warn("could not tap COD option directly, trying sub-options")
                }
                delay(600)

                // Select Cash sub-option if present (e.g. "Cash" radio button)
                val cashRadio = x.awaitNode(COD_SUBOPTION, timeoutMs = 1_200)
                if (cashRadio != null && cashRadio.bounds.top > cod.bounds.top) {
                    x.tapAt(cashRadio.centerX, cashRadio.centerY)
                    x.tap(cashRadio)
                    delay(400)
                }
                codSelected = true

                // Now locate the Continue / Use this payment method button
                AgentLog.step("Locating 'Continue' / 'Use this payment method' button...")
                val paymentAdvance = x.awaitNode(PAYMENT_ADVANCE, timeoutMs = 2_000)
                    ?: x.scrollUntilVisible(PAYMENT_ADVANCE, maxScrolls = 6)

                if (paymentAdvance != null) {
                    AgentLog.step("Tapping Continue on payment screen...")
                    x.tapAt(paymentAdvance.centerX, paymentAdvance.centerY)
                    x.tap(paymentAdvance)
                    continueTapped = true
                    delay(SETTLE_MS + 1_000)
                    x.dismissInterstitials()

                    AgentLog.success("Successfully selected Cash on Delivery and clicked Continue!")
                    return FlowResult.AwaitingUser(
                        "Parked on final review screen with Cash on Delivery selected. Stopped before placing order."
                    )
                } else {
                    AgentLog.warn("Payment advance button not found after selecting COD")
                }
            }

            // If we are on an intermediate screen (e.g. Address selection)
            val advance = x.awaitNode(CHECKOUT_ADVANCE, timeoutMs = 1_500)
            if (advance != null) {
                AgentLog.step("Advancing through intermediate checkout step: '${advance.label}'")
                x.tapAt(advance.centerX, advance.centerY)
                x.tap(advance)
                delay(SETTLE_MS + 800)
                x.dismissInterstitials()
                continue
            }

            // Settle and try again
            AgentLog.info("Checkout step $step: looking for payment or advance buttons...")
            delay(800)
        }

        if (codSelected && continueTapped) {
            return FlowResult.AwaitingUser("Parked on the final review screen with Cash on Delivery selected.")
        } else if (codSelected) {
            return FlowResult.AwaitingUser("Selected Cash on Delivery on the payment screen. Please review and continue.")
        } else {
            return FlowResult.Failed("checkout", "could not find Cash on Delivery option during checkout")
        }
    }

    private suspend fun findCodOption(x: ActionExecutor): UiNode? {
        val visibleCod = x.awaitNode(COD_OPTION, timeoutMs = 1_000)
        if (visibleCod != null) return visibleCod

        if (x.awaitNode(PAYMENT_SCREEN, timeoutMs = 1_000) != null) {
            AgentLog.step("on payment screen — scrolling to find Cash / Pay on Delivery")
            return x.scrollUntilVisible(COD_OPTION, maxScrolls = 6)
        }
        return null
    }

    companion object {
        const val MAX_CHECKOUT_STEPS = 5
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

        val PROCEED_TO_BUY = query(
            "Proceed to Buy",
            Selector(textContains = "Proceed to Buy"),
            Selector(textContains = "Proceed to checkout"),
            Selector(id = "proceed_to_checkout"),
            Selector(textContains = "Proceed to"),
        )

        val PAYMENT_SCREEN = query(
            "payment screen",
            Selector(textContains = "payment method"),
            Selector(textContains = "Select a payment"),
            Selector(textContains = "Other payment options"),
            Selector(textContains = "Net Banking"),
            Selector(textContains = "Credit or debit card"),
            Selector(textContains = "Credit/Debit"),
            Selector(textContains = "Amazon Pay balance"),
            Selector(textContains = "Pay on Delivery"),
            Selector(textContains = "Cash on Delivery"),
        )

        val COD_OPTION = query(
            "Cash / Pay on Delivery",
            Selector(id = "cod"),
            Selector(id = "pay_on_delivery"),
            Selector(textContains = "Pay on Delivery"),
            Selector(textContains = "Cash on Delivery"),
            Selector(textContains = "Cash/Card on Delivery"),
            Selector(desc = "Pay on Delivery"),
            Selector(desc = "Cash on Delivery"),
        )

        val COD_SUBOPTION = query(
            "Cash sub-option",
            Selector(text = "Cash"),
            Selector(text = "Cash on Delivery"),
        )

        val CHECKOUT_ADVANCE = query(
            "checkout continue",
            Selector(textContains = "Deliver to this address"),
            Selector(textContains = "Use this address"),
            Selector(textContains = "Use this payment method"),
            Selector(text = "Continue"),
        )

        val PAYMENT_ADVANCE = query(
            "payment continue",
            Selector(textContains = "Use this payment method"),
            Selector(text = "Continue"),
            Selector(textContains = "Continue"),
        )

        val PLACE_ORDER = query(
            "Place your order",
            Selector(textContains = "Place your order"),
            Selector(textContains = "Place Your Order"),
            Selector(textContains = "Place order"),
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
