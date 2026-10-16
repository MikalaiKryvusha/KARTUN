package com.kriniks.kartun

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

/**
 * [openError] picks the line the user sees when a photo does not open (the owner 2026-10-16 ≈ 16:26: «можем в ошибке
 * писать причину понятную?»). The ImageDecoder.DecodeException branches cannot be built on the JVM (an android.jar stub);
 * they are walked on the Titan - testcases/reports/2026-10-16_open_with_titan.md.
 */
class OpenErrorTest {

    @Test fun memoryIsNamed() = assertEquals(R.string.error_memory, openError(OutOfMemoryError("Failed to allocate")))

    @Test fun noPermissionIsNoAccess() = assertEquals(R.string.error_no_access, openError(SecurityException("Permission Denial")))

    /** A cloud photo or a deleted file: the resolver throws FileNotFoundException, sometimes wrapped. */
    @Test fun missingFileIsNoAccessEvenWrapped() =
        assertEquals(R.string.error_no_access, openError(IOException("open", FileNotFoundException("No content provider"))))

    /** A HEIC cut in half on the Titan: the decoder starts, then fails reading the pixels. */
    @Test fun readErrorIsDamagedFile() =
        assertEquals(R.string.error_not_photo, openError(IOException("getPixels failed with error invalid input")))

    @Test fun anythingElseKeepsTheGeneralLine() = assertEquals(R.string.error_decode, openError(IllegalStateException("x")))
}
