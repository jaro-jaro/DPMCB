package cz.jaro.dpmcb

import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.cas
import org.junit.Assert.assertEquals
import org.junit.Test

class CasPlusTest {

    @Test
    fun `Kdyz bude cas 10 10 a prictu 30 bude 10 40`() {
        val vysledek = (10 cas 10).plus(30)

        assertEquals(vysledek, 10 cas 40)
    }
    @Test
    fun `Kdyz bude cas 10 10 a prictu 60 bude 11 10`() {
        val vysledek = (10 cas 10).plus(60)

        assertEquals(vysledek, 11 cas 10)
    }
    @Test
    fun `Kdyz bude cas 10 30 a prictu 40 bude 11 10`() {
        val vysledek = (10 cas 30).plus(40)

        assertEquals(vysledek, 11 cas 10)
    }
}
