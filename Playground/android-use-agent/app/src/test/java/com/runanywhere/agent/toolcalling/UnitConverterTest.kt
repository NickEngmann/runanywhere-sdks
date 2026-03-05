package com.runanywhere.agent.toolcalling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `convert temperature from Celsius to Fahrenheit`() {
        val result = UnitConverter.convert(0.0, "C", "F")
        assertTrue(result.contains("32.0000"))
        assertTrue(result.contains("°F"))
    }

    @Test
    fun `convert temperature from Fahrenheit to Celsius`() {
        val result = UnitConverter.convert(32.0, "F", "C")
        assertTrue(result.contains("0.0000"))
        assertTrue(result.contains("°C"))
    }

    @Test
    fun `convert temperature from Celsius to Kelvin`() {
        val result = UnitConverter.convert(0.0, "C", "K")
        assertTrue(result.contains("273.1500"))
        assertTrue(result.contains("K"))
    }

    @Test
    fun `convert temperature from Kelvin to Celsius`() {
        val result = UnitConverter.convert(273.15, "K", "C")
        assertTrue(result.contains("0.0000"))
        assertTrue(result.contains("°C"))
    }

    @Test
    fun `convert length from meters to feet`() {
        val result = UnitConverter.convert(1.0, "m", "ft")
        assertTrue(result.contains("3.2808"))
        assertTrue(result.contains("ft"))
    }

    @Test
    fun `convert length from feet to meters`() {
        val result = UnitConverter.convert(3.2808, "ft", "m")
        assertTrue(result.contains("1.0000"))
        assertTrue(result.contains("m"))
    }

    @Test
    fun `convert weight from kilograms to pounds`() {
        val result = UnitConverter.convert(1.0, "kg", "lb")
        assertTrue(result.contains("2.2046"))
        assertTrue(result.contains("lb"))
    }

    @Test
    fun `convert weight from pounds to kilograms`() {
        val result = UnitConverter.convert(2.2046, "lb", "kg")
        assertTrue(result.contains("1.0000"))
        assertTrue(result.contains("kg"))
    }

    @Test
    fun `convert same unit returns same value`() {
        val result = UnitConverter.convert(100.0, "C", "C")
        assertTrue(result.contains("100.0000"))
    }

    @Test
    fun `convert unsupported units returns error message`() {
        val result = UnitConverter.convert(1.0, "invalid", "unit")
        assertTrue(result.contains("Unsupported conversion"))
    }

    @Test
    fun `convert temperature 100 Celsius to Fahrenheit`() {
        val result = UnitConverter.convert(100.0, "C", "F")
        assertTrue(result.contains("212.0000"))
    }

    @Test
    fun `convert temperature 32 Fahrenheit to Celsius`() {
        val result = UnitConverter.convert(32.0, "F", "C")
        assertTrue(result.contains("0.0000"))
    }

    @Test
    fun `convert length 10 meters to feet`() {
        val result = UnitConverter.convert(10.0, "m", "ft")
        assertTrue(result.contains("32.8084"))
    }

    @Test
    fun `convert weight 10 kilograms to pounds`() {
        val result = UnitConverter.convert(10.0, "kg", "lb")
        assertTrue(result.contains("22.0462"))
    }

    @Test
    fun `convert temperature 0 Celsius to Kelvin`() {
        val result = UnitConverter.convert(0.0, "C", "K")
        assertTrue(result.contains("273.1500"))
    }

    @Test
    fun `convert temperature 100 Celsius to Kelvin`() {
        val result = UnitConverter.convert(100.0, "C", "K")
        assertTrue(result.contains("373.1500"))
    }

    @Test
    fun `convert length 100 feet to meters`() {
        val result = UnitConverter.convert(100.0, "ft", "m")
        assertTrue(result.contains("30.4800"))
    }

    @Test
    fun `convert weight 100 pounds to kilograms`() {
        val result = UnitConverter.convert(100.0, "lb", "kg")
        assertTrue(result.contains("45.3592"))
    }

    @Test
    fun `convert temperature with decimal values`() {
        val result = UnitConverter.convert(25.5, "C", "F")
        assertTrue(result.contains("77.9000"))
    }

    @Test
    fun `convert length with decimal values`() {
        val result = UnitConverter.convert(5.25, "m", "ft")
        assertTrue(result.contains("17.2244"))
    }

    @Test
    fun `convert weight with decimal values`() {
        val result = UnitConverter.convert(3.75, "kg", "lb")
        assertTrue(result.contains("8.2677"))
    }
}
