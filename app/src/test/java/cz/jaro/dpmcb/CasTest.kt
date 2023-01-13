package cz.jaro.dpmcb

import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import org.junit.Assert.assertEquals
import org.junit.Test

class CasTest {

    @Test
    fun `Prevadeni text - cas`() {
        assertEquals(Cas(1, 0), "1:00".toCas())
        assertEquals(Cas(1, 30), "1:30".toCas())
        assertEquals(Cas(0, 48), "0:48".toCas())
        assertEquals(Cas(99, 99), "99:99".toCas())
        assertEquals(Cas(0, 5, 30), "0:5:30".toCas())
    }

    @Test
    fun `Prevadeni cas - text`() {
        assertEquals("1:00", Cas(1, 0).toString())
        assertEquals("1:30", Cas(1, 30).toString())
        assertEquals("0:48", Cas(0, 48).toString())
        assertEquals("99:99", Cas(99, 99).toString())
        assertEquals("0:05:30", Cas(0, 5, 30).toString(true))
    }

    @Test
    fun `Prevadeni cas - int`() {
        assertEquals(3600, Cas(1, 0).toInt())
        assertEquals(5400, Cas(1, 30).toInt())
        assertEquals(2880, Cas(0, 48).toInt())
        assertEquals(362340, Cas(99, 99).toInt())
        assertEquals(330, Cas(0, 5, 30).toInt())
    }

    @Test
    fun `Prevadeni int - cas`() {
        assertEquals(Cas(1, 0), 3600.toCas())
        assertEquals(Cas(1, 30), 5400.toCas())
        assertEquals(Cas(0, 48), 2880.toCas())
        assertEquals(Cas(99, 99), 362340.toCas())
        assertEquals(Cas(0, 5, 30), 330.toCas())
    }

    @Test
    fun scitani() {
        assertEquals(10 cas 40, (10 cas 10) + 30)
        assertEquals(11 cas 10, (10 cas 10) + 60)
        assertEquals(11 cas 10, (10 cas 30) + 40)
    }

    @Test
    fun `odcitani cisel`() {
        assertEquals(9 cas 40, (10 cas 10) - 30)
        assertEquals(9 cas 10, (10 cas 10) - 60)
        assertEquals(9 cas 50, (10 cas 30) - 40)
    }

    @Test
    fun `odcitani casu`() {
        assertEquals(0, (10 cas 10) - (10 cas 10))
        assertEquals(90, (11 cas 40) - (10 cas 10))
        assertEquals(20, (10 cas 30) - (10 cas 10))
    }
}
