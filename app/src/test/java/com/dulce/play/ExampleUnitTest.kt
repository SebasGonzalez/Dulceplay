package com.dulce.play

import com.dulce.play.utils.SearchEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testSearch() = runBlocking {
    val engine = SearchEngine()
    val results = engine.buscar("salsa")
    println("RESULTS COUNT: ${results.size}")
    for (r in results) {
        println("Result: ${r.titulo} - ${r.id}")
    }
    assertTrue(results.isNotEmpty())
  }
}
