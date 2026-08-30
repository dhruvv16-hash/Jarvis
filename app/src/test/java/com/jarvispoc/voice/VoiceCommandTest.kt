package com.jarvispoc.voice

import com.jarvispoc.flows.AmazonOrderFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandTest {

    @Test
    fun testRequiredAmazonQueryParsing() {
        // 1. "Buy earbuds" -> query = "earbuds"
        val cmd1 = VoiceCommand.parse("Buy earbuds")
        assertEquals(VoiceCommand.Target.AMAZON, cmd1.target)
        assertEquals("earbuds", cmd1.searchQuery)

        // 2. "Buy earbuds under ₹1000" -> query = "earbuds under 1000"
        val cmd2 = VoiceCommand.parse("Buy earbuds under ₹1000")
        assertEquals(VoiceCommand.Target.AMAZON, cmd2.target)
        assertEquals("earbuds under 1000", cmd2.searchQuery)

        // 3. "Buy headphones below 1500" -> query = "headphones under 1500"
        val cmd3 = VoiceCommand.parse("Buy headphones below 1500")
        assertEquals(VoiceCommand.Target.AMAZON, cmd3.target)
        assertEquals("headphones under 1500", cmd3.searchQuery)

        // 4. "Buy AirPods for ₹2000" -> preserve existing behavior
        val cmd4 = VoiceCommand.parse("Buy AirPods for ₹2000")
        assertEquals(VoiceCommand.Target.AMAZON, cmd4.target)
        assertEquals("airpods for ₹2000", cmd4.searchQuery)
    }

    @Test
    fun testAdditionalPricePhrasingsAndNormalization() {
        // "Purchase a mouse below 999 rupees" -> "mouse under 999"
        val cmd1 = VoiceCommand.parse("Purchase a mouse below 999 rupees")
        assertEquals(VoiceCommand.Target.AMAZON, cmd1.target)
        assertEquals("mouse under 999", cmd1.searchQuery)

        // "Order earbuds under Rs 1000 on Amazon" -> "earbuds under 1000"
        val cmd2 = VoiceCommand.parse("Order earbuds under Rs 1000 on Amazon")
        assertEquals(VoiceCommand.Target.AMAZON, cmd2.target)
        assertEquals("earbuds under 1000", cmd2.searchQuery)

        // "Order a usb c cable under Rs. 500" -> "usb c cable under 500"
        val cmd3 = VoiceCommand.parse("Order a usb c cable under Rs. 500")
        assertEquals(VoiceCommand.Target.AMAZON, cmd3.target)
        assertEquals("usb c cable under 500", cmd3.searchQuery)

        // "Buy keyboard below ₹ 2,000" -> "keyboard under 2000"
        val cmd4 = VoiceCommand.parse("Buy keyboard below ₹ 2,000")
        assertEquals(VoiceCommand.Target.AMAZON, cmd4.target)
        assertEquals("keyboard under 2000", cmd4.searchQuery)

        // Empty product with just price should safely reject
        val cmd5 = VoiceCommand.parse("Buy under 1000")
        assertNull(cmd5.searchQuery)
    }

    @Test
    fun testAmazonReasonableMatchWithPriceQuery() {
        // When searching for "earbuds under 1000", product title matching should match the product keywords
        val title = "boAt Airdopes 141 Bluetooth Truly Wireless in Ear Earbuds with 42H Playtime"
        assertTrue(AmazonOrderFlow.isReasonableMatch(title, "earbuds under 1000"))
        assertTrue(AmazonOrderFlow.isReasonableMatch(title, "earbuds"))

        val mouseTitle = "Logitech B100 Wired Optical Mouse (Black)"
        assertTrue(AmazonOrderFlow.isReasonableMatch(mouseTitle, "mouse under 999"))
        assertTrue(AmazonOrderFlow.isReasonableMatch(mouseTitle, "mouse"))
    }
}
