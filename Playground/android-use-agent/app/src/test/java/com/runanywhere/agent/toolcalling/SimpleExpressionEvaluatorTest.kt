package com.runanywhere.agent.toolcalling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class SimpleExpressionEvaluatorTest {

    @Test
    fun `evaluate simple addition`() {
        val result = SimpleExpressionEvaluator.evaluate("2 + 3")
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `evaluate simple subtraction`() {
        val result = SimpleExpressionEvaluator.evaluate("10 - 4")
        assertEquals(6.0, result, 0.0001)
    }

    @Test
    fun `evaluate simple multiplication`() {
        val result = SimpleExpressionEvaluator.evaluate("5 * 6")
        assertEquals(30.0, result, 0.0001)
    }

    @Test
    fun `evaluate simple division`() {
        val result = SimpleExpressionEvaluator.evaluate("20 / 4")
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with parentheses`() {
        val result = SimpleExpressionEvaluator.evaluate("(2 + 3) * 4")
        assertEquals(20.0, result, 0.0001)
    }

    @Test
    fun `evaluate complex expression`() {
        val result = SimpleExpressionEvaluator.evaluate("2 + 3 * 4")
        assertEquals(14.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with negative numbers`() {
        val result = SimpleExpressionEvaluator.evaluate("-5 + 3")
        assertEquals(-2.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with decimal numbers`() {
        val result = SimpleExpressionEvaluator.evaluate("2.5 + 3.5")
        assertEquals(6.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with spaces`() {
        val result = SimpleExpressionEvaluator.evaluate("10 + 20")
        assertEquals(30.0, result, 0.0001)
    }

    @Test
    fun `evaluate nested parentheses`() {
        val result = SimpleExpressionEvaluator.evaluate("((2 + 3) * (4 - 1))")
        assertEquals(15.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with division by zero`() {
        try {
            SimpleExpressionEvaluator.evaluate("10 / 0")
            assertTrue(false) // Should not reach here
        } catch (e: ArithmeticException) {
            assertTrue(e.message?.contains("Division by zero") == true)
        }
    }

    @Test
    fun `evaluate expression with single number`() {
        val result = SimpleExpressionEvaluator.evaluate("42")
        assertEquals(42.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with double negative`() {
        val result = SimpleExpressionEvaluator.evaluate("-(-5)")
        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with multiple operations`() {
        val result = SimpleExpressionEvaluator.evaluate("1 + 2 + 3 + 4")
        assertEquals(10.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with mixed operations`() {
        val result = SimpleExpressionEvaluator.evaluate("10 - 5 + 3 * 2")
        assertEquals(11.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with decimal multiplication`() {
        val result = SimpleExpressionEvaluator.evaluate("2.5 * 4")
        assertEquals(10.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with decimal division`() {
        val result = SimpleExpressionEvaluator.evaluate("10.0 / 2.5")
        assertEquals(4.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with leading plus`() {
        val result = SimpleExpressionEvaluator.evaluate("+5 + 3")
        assertEquals(8.0, result, 0.0001)
    }

    @Test
    fun `evaluate expression with trailing minus`() {
        val result = SimpleExpressionEvaluator.evaluate("10 - -5")
        assertEquals(15.0, result, 0.0001)
    }
}
