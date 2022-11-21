package cz.jaro.dpmcb

import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.reversedIf
import org.junit.Assert.assertEquals
import org.junit.Test

class ReversedIfTest {

    @Test
    fun `chci otocit`() {
        assertEquals(
            listOf("d", "c", "b", "a"),
            listOf("a", "b", "c", "d").reversedIf { true }
        )
    }

    @Test
    fun `nechci otocit`() {
        assertEquals(
            listOf("a", "b", "c", "d"),
            listOf("a", "b", "c", "d").reversedIf { false }
        )
    }
}
