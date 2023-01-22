package cz.jaro.dpmcb

/*import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.cas
import cz.jaro.dpmcb.data.helperclasses.Cas.Companion.toCas
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.hod
import cz.jaro.dpmcb.data.helperclasses.Trvani.Companion.min
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
    fun `Prevadeni cas - trvani`() {
        assertEquals(1.hod, Cas(1, 0).toTrvani())
        assertEquals(1.5.hod, Cas(1, 30).toTrvani())
        assertEquals(48.min, Cas(0, 48).toTrvani())
        assertEquals((99.hod + 99.min), Cas(99, 99).toTrvani())
        assertEquals(5.5.min, Cas(0, 5, 30).toTrvani())
    }

    @Test
    fun `Prevadeni trvani - cas`() {
        assertEquals(Cas(1, 0), 1.hod.toCas())
        assertEquals(Cas(1, 30), 1.5.hod.toCas())
        assertEquals(Cas(0, 48), 48.min.toCas())
        assertEquals(Cas(99, 99), (99.hod + 99.min).toCas())
        assertEquals(Cas(0, 5, 30), 5.5.min.toCas())
    }

    @Test
    fun scitani() {
        assertEquals(10 cas 40, (10 cas 10) + 30.min)
        assertEquals(11 cas 10, (10 cas 10) + 60.min)
        assertEquals(11 cas 10, (10 cas 30) + 40.min)
    }

    @Test
    fun `odcitani cisel`() {
        assertEquals(9 cas 40, (10 cas 10) - 30.min)
        assertEquals(9 cas 10, (10 cas 10) - 60.min)
        assertEquals(9 cas 50, (10 cas 30) - 40.min)
    }

    @Test
    fun `odcitani casu`() {
        assertEquals(0, (10 cas 10) - (10 cas 10))
        assertEquals(90, (11 cas 40) - (10 cas 10))
        assertEquals(20, (10 cas 30) - (10 cas 10))
    }
}*/
