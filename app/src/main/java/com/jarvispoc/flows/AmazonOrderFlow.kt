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
 * Search Amazon, add the first result to the cart, choose Cash/Pay on Delivery,
 * and walk to the final confirm screen.
 *
 * Two deliberate shortcuts:
 *  - Search is a deep link rather than driving the search box. Same result,
 *    three fewer steps that can fail.
 *  - The cart is reached by deep link too, instead of hunting the cart icon.
 *
 * **Payment safety.** The flow tracks whether it actually selected Cash/Pay on
 * Delivery, and independently re-verifies it on the final review screen. If it
 * cannot prove COD is the selected method it will NOT place the order, even
 * with autoConfirm on — a silent failure to select COD would otherwise fall
 * through to whatever card is saved on the account, which is exactly the
 * outcome choosing COD was meant to avoid.
 *
 * SELECTOR HEALTH WARNING: every Query below is a first guess. Run the screen
 * dumper on real Amazon screens and correct these against actual resource ids
 * before expecting a clean pass.
 */
class AmazonOrderFlow(private val searchQuery: String) : Flow {

    override val name: String = "Amazon COD order"

    override suspend fun run(x: ActionExecutor, autoConfirm: Boolean): FlowResult {
        val pkg = PACKAGES.firstOrNull { x.isInstalled(it) }
            ?: return FlowResult.Failed("launch", "no Amazon shopping app installed")
        AgentLog.info("using Amazon package $pkg")

        // A rambling mis-transcription searched verbatim returns nothing, and
        // the flow would then blame RESULT_ITEM rather than the query.
        val query = searchQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.length < searchQuery.trim().length) {
            AgentLog.warn("search text truncated to $MAX_QUERY_LENGTH chars: \"$query\"")
        }
        if (query.firstOrNull()?.isDigit() == true) {
            AgentLog.warn(
                "query starts with a number (\"$query\") — this flow always adds ONE " +
                    "unit, quantities in the phrase are not honoured"
            )
        }

        // 1 — deep-link straight into in-app search results
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

        // 2 — first genuine result
        val result = x.awaitNode(RESULT_ITEM, timeoutMs = 15_000)
            ?: return FlowResult.Failed(
                "results",
                "no search result matched. Dump this screen and fix RESULT_ITEM.",
            )
        if (!x.tap(result)) return FlowResult.Failed("results", "could not tap the first result")
        delay(SETTLE_MS)
        x.dismissInterstitials()

        // 3 — add to cart (usually below the fold)
        val addToCart = x.scrollUntilVisible(ADD_TO_CART, maxScrolls = 10)
            ?: return FlowResult.Failed("product", "'Add to Cart' not found on the product page")
        if (!x.tap(addToCart)) return FlowResult.Failed("product", "could not tap 'Add to Cart'")
        delay(SETTLE_MS)
        // protection-plan and warranty upsells land here
        x.dismissInterstitials(rounds = 3)

        // 4 — cart
        if (!x.launchUri("https://www.amazon.in/gp/cart/view.html", pkg)) {
            return FlowResult.Failed("cart", "could not open the cart deep link")
        }
        if (!x.awaitPackage(pkg)) {
            return FlowResult.Failed("cart", "left the Amazon app on the way to the cart")
        }
        delay(SETTLE_MS)
        // Checkout takes the WHOLE cart, not just what this run added. Running
        // the flow repeatedly (the reliability test asks for 5 passes) stacks
        // items up, so an auto-confirmed run buys every one of them.
        AgentLog.warn(
            "checkout covers the entire cart, not only the item this run added — " +
                "clear the cart between test runs"
        )
        val proceed = x.scrollUntilVisible(PROCEED_TO_BUY, maxScrolls = 6)
            ?: return FlowResult.Failed("cart", "'Proceed to Buy' not found — is the cart empty?")
        if (!x.tap(proceed)) return FlowResult.Failed("cart", "could not tap 'Proceed to Buy'")
        delay(SETTLE_MS)

        // 5 — checkout funnel: address, then payment, then review.
        // Amazon varies the order and count of these, so rather than hardcoding
        // a sequence we loop: bail out when the final screen appears, grab the
        // COD option whenever it shows up, otherwise press whatever advances.
        var codSelected = false
        for (step in 1..MAX_CHECKOUT_STEPS) {
            if (x.awaitNode(PLACE_ORDER, timeoutMs = 2_000) != null) break

            if (!codSelected) {
                val cod = findCodOption(x)
                if (cod != null && x.tap(cod)) {
                    codSelected = true
                    AgentLog.success("selected Cash / Pay on Delivery")
                    delay(1_500)
                    // Some builds nest a Cash vs Card-on-delivery sub-choice.
                    // Guard on bounds: the sub-option query can resolve to the
                    // very row we just tapped (both say "Cash on Delivery"), and
                    // tapping a radio twice deselects it — which would leave
                    // codSelected lying and fall the order through to a card.
                    x.awaitNode(COD_SUBOPTION, timeoutMs = 2_000)?.let { sub ->
                        if (sub.bounds != cod.bounds) {
                            x.tap(sub)
                            delay(1_200)
                        } else {
                            AgentLog.info("sub-option is the same row — not tapping twice")
                        }
                    }
                }
            }

            // Once COD is chosen, stop offering the address buttons as advance
            // candidates: a collapsed "Deliver to this address" summary row on
            // the payment screen would send us back a step and lose the choice.
            val advanceQuery = if (codSelected) PAYMENT_ADVANCE else CHECKOUT_ADVANCE
            // Scroll fallback: hunting COD can leave the page scrolled past a
            // non-sticky continue button, and giving up here would strand the
            // flow one tap short of the review screen.
            val advance = x.awaitNode(advanceQuery, timeoutMs = 2_000)
                ?: x.scrollUntilVisible(advanceQuery, maxScrolls = 4)
            if (advance == null) {
                AgentLog.warn("checkout stalled on an unrecognised screen (step $step)")
                break
            }
            x.tap(advance)
            delay(SETTLE_MS)
        }

        val placeOrder = x.awaitNode(PLACE_ORDER, timeoutMs = 10_000)
            ?: return FlowResult.Failed(
                "checkout",
                "never reached the 'Place your order' screen — likely a login, OTP or captcha wall",
            )

        // 6 — independently confirm COD on the review screen. `codSelected` only
        // says we tapped something; this says the order summary agrees.
        val codOnReview = x.awaitNode(COD_CONFIRMATION, timeoutMs = 3_000) != null
        val codProven = codSelected && codOnReview
        AgentLog.info("COD check — tapped=$codSelected, shown on review=$codOnReview")

        // 7 — the money step
        if (!autoConfirm) {
            AgentLog.halt(
                if (codProven) {
                    "STOPPED on '${placeOrder.label}' with Pay on Delivery selected. Tap it yourself."
                } else {
                    "STOPPED on '${placeOrder.label}' — but COD was NOT confirmed. " +
                        "Check the payment method before tapping anything."
                }
            )
            return FlowResult.AwaitingUser(
                if (codProven) "Parked on the final confirm screen, COD selected. No order placed."
                else "Parked on the final confirm screen. COD NOT confirmed — check payment method."
            )
        }

        if (!codProven) {
            // Refusing on purpose: without COD this would charge a saved card.
            AgentLog.halt(
                "REFUSING to place the order — auto-confirm is on but Cash/Pay on Delivery " +
                    "could not be confirmed (tapped=$codSelected, review=$codOnReview). " +
                    "Placing now could charge a saved card instead."
            )
            return FlowResult.AwaitingUser(
                "Stopped short of ordering: COD unconfirmed, and placing anyway risks charging a card."
            )
        }

        AgentLog.warn("auto-confirm is ON and COD is confirmed — placing the order for real")
        if (!x.tap(placeOrder)) return FlowResult.Failed("checkout", "could not tap 'Place your order'")

        // 8 — verify rather than assume. Tapping the button is not the same as
        // the order going through: a payment wall, OTP or stock failure all
        // leave you on a different screen. Reporting Success off the back of a
        // tap would be a false success on the single most consequential step.
        val confirmed = x.awaitNode(ORDER_CONFIRMATION, timeoutMs = 15_000) != null
        return if (confirmed) {
            FlowResult.Success("Cash-on-delivery order placed and confirmed")
        } else {
            // Failed (not AwaitingUser) on purpose: it triggers the automatic
            // screen dump, which is exactly what you need to see here.
            FlowResult.Failed(
                "confirm",
                "tapped 'Place your order' but no confirmation screen appeared within 15s. " +
                    "The order MAY still have gone through — check Your Orders in the Amazon app.",
            )
        }
    }

    /**
     * Only ever looks for COD on the payment screen.
     *
     * The gate is load-bearing, not an optimisation. Delivery blurbs elsewhere
     * in checkout ("Pay on Delivery available", on the address step) match
     * COD_OPTION too. Tapping one does nothing but sets `codSelected`, and the
     * flow would then switch to PAYMENT_ADVANCE and sail past the real payment
     * screen without ever choosing COD — a false positive that disables the
     * very selection it claims to have made.
     *
     * If PAYMENT_SCREEN is wrong, COD is never selected, the review check fails
     * and the order is refused. Safe direction to fail in.
     */
    private suspend fun findCodOption(x: ActionExecutor): UiNode? {
        if (x.awaitNode(PAYMENT_SCREEN, timeoutMs = 1_000) == null) return null
        AgentLog.step("on the payment screen — looking for Cash / Pay on Delivery")
        return x.awaitNode(COD_OPTION, timeoutMs = 1_500)
            ?: x.scrollUntilVisible(COD_OPTION, maxScrolls = 5)
    }

    private companion object {
        const val MAX_CHECKOUT_STEPS = 5

        /** Longer than this is a mis-transcription, not a product name. */
        const val MAX_QUERY_LENGTH = 80

        /** Render settle after the target app is confirmed foreground. */
        const val SETTLE_MS = 1_800L

        val PACKAGES = listOf(
            "in.amazon.mShop.android.shopping",
            "com.amazon.mShop.android.shopping",
        )

        /** Ordered from most specific to a last-ditch heuristic. */
        val RESULT_ITEM = query(
            "first search result",
            Selector(id = "search_result", clickable = true),
            Selector(id = "result_item", clickable = true),
            Selector(id = "product_image", clickable = true),
            Selector(desc = "result", clickable = true),
            Selector(clickable = true, minTextLen = 25),
        )

        val ADD_TO_CART = query(
            "Add to Cart",
            Selector(id = "add_to_cart"),
            Selector(text = "Add to Cart"),
            Selector(textContains = "Add to Cart"),
            Selector(textContains = "Add to cart"),
        )

        val PROCEED_TO_BUY = query(
            "Proceed to Buy",
            Selector(textContains = "Proceed to Buy"),
            Selector(textContains = "Proceed to checkout"),
            Selector(id = "proceed_to_checkout"),
        )

        /**
         * Marker that we are on the payment-method screen at all.
         *
         * Broad on purpose: this gates COD selection entirely, so a miss costs
         * the whole feature. The sibling payment methods are included because
         * they co-occur on that screen and essentially nowhere else in checkout.
         */
        val PAYMENT_SCREEN = query(
            "payment screen",
            Selector(textContains = "payment method"),
            Selector(textContains = "Select a payment"),
            Selector(textContains = "Other payment options"),
            Selector(textContains = "Net Banking"),
            Selector(textContains = "Credit or debit card"),
            Selector(textContains = "Credit/Debit"),
            Selector(textContains = "Amazon Pay balance"),
        )

        /**
         * Amazon India currently labels this "Pay on Delivery"; older builds and
         * other marketplaces say "Cash on Delivery". A greyed-out option (COD
         * unavailable for the item or pincode) will not match, because disabled
         * nodes never match any selector — which is what we want here.
         */
        val COD_OPTION = query(
            "Cash / Pay on Delivery",
            Selector(id = "cod"),
            Selector(id = "pay_on_delivery"),
            Selector(textContains = "Pay on Delivery"),
            Selector(textContains = "Cash on Delivery"),
            Selector(textContains = "Cash/Card on Delivery"),
        )

        /**
         * Cash vs card-on-delivery sub-choice, when Amazon shows one.
         *
         * Exact matches only. A `textContains` here would also match the parent
         * "Cash on Delivery" row we just selected; the call site additionally
         * compares bounds, but the narrow selector is the first line of defence.
         */
        val COD_SUBOPTION = query(
            "Cash sub-option",
            Selector(text = "Cash"),
            Selector(text = "Cash on Delivery"),
        )

        /** Must be visible on the review screen before we are allowed to order. */
        val COD_CONFIRMATION = query(
            "COD on review screen",
            Selector(textContains = "Pay on Delivery"),
            Selector(textContains = "Cash on Delivery"),
        )

        val CHECKOUT_ADVANCE = query(
            "checkout continue",
            Selector(textContains = "Deliver to this address"),
            Selector(textContains = "Use this address"),
            Selector(textContains = "Use this payment method"),
            Selector(text = "Continue"),
        )

        /** Post-COD advance. Deliberately excludes the address buttons. */
        val PAYMENT_ADVANCE = query(
            "payment continue",
            Selector(textContains = "Use this payment method"),
            Selector(text = "Continue"),
        )

        val PLACE_ORDER = query(
            "Place your order",
            Selector(textContains = "Place your order"),
            Selector(textContains = "Place Your Order"),
            Selector(textContains = "Place order"),
        )

        /** Proof the order actually went through, not just that we tapped. */
        val ORDER_CONFIRMATION = query(
            "order confirmation",
            Selector(textContains = "Order placed"),
            Selector(textContains = "order has been placed"),
            Selector(textContains = "Thank you for your order"),
            Selector(textContains = "Your order has been"),
            Selector(textContains = "Order confirmed"),
        )
    }
}
