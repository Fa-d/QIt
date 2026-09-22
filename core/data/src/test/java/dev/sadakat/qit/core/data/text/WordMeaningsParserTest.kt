package dev.sadakat.qit.core.data.text

import org.junit.Assert.assertEquals
import org.junit.Test

class WordMeaningsParserTest {

    @Test
    fun `meanings by ayah`() {
        val parsed = WordMeaningsParser.parse("""[[1,["In (the) name","(of) Allah"]],[2,["All praises"]]]""")
        assertEquals(mapOf(1 to listOf("In (the) name", "(of) Allah"), 2 to listOf("All praises")), parsed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed json is rejected`() {
        WordMeaningsParser.parse("[[1,")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a row without meanings is rejected`() {
        WordMeaningsParser.parse("[[1]]")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an ayah that is not a number is rejected`() {
        WordMeaningsParser.parse("""[["x",["say"]]]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `meanings that are not an array are rejected`() {
        WordMeaningsParser.parse("""[[1,"say"]]""")
    }
}
