package com.runanywhere.sdk

import org.junit.Test
import org.junit.Assert.*

class ModelLoaderTest {

    @Test
    fun testModelLoaderCreation() {
        val modelLoader = ModelLoader("test-model")
        assertNotNull(modelLoader)
        assertEquals("test-model", modelLoader.modelName)
    }

    @Test
    fun testModelLoaderWithEmptyName() {
        val modelLoader = ModelLoader("")
        assertNotNull(modelLoader)
        assertEquals("", modelLoader.modelName)
    }

    @Test
    fun testModelLoaderWithLongName() {
        val longName = "a".repeat(1000)
        val modelLoader = ModelLoader(longName)
        assertNotNull(modelLoader)
        assertEquals(longName, modelLoader.modelName)
    }

    @Test
    fun testModelLoaderStateInitialization() {
        val modelLoader = ModelLoader("test-model")
        assertEquals(ModelState.IDLE, modelLoader.state)
        assertEquals(0, modelLoader.progress)
        assertNull(modelLoader.error)
    }

    @Test
    fun testModelLoaderToString() {
        val modelLoader = ModelLoader("test-model")
        val expected = "ModelLoader(modelName='test-model', state=IDLE, progress=0)"
        assertEquals(expected, modelLoader.toString())
    }
}
