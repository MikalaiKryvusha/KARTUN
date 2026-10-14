package com.kriniks.kartun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM hygiene for «Вставить ссылку»: which link is taken from the clipboard text. The clipboard often holds a sentence
 * («смотри: https://…/photo.jpg.») - the link alone must come out, without the trailing punctuation. Hygiene only: the
 * download itself is read on the device.
 */
class LinkTest {

    @Test fun bareLink() = assertEquals("https://example.com/a.jpg", LinkDownloader.findUrl("https://example.com/a.jpg"))

    @Test fun linkInsideASentenceLosesTrailingPunctuation() =
        assertEquals("https://example.com/cat.png", LinkDownloader.findUrl("смотри: https://example.com/cat.png."))

    @Test fun linkInGuillemets() = assertEquals("http://x.org/p.webp", LinkDownloader.findUrl("«http://x.org/p.webp»"))

    @Test fun noLink() = assertNull(LinkDownloader.findUrl("просто текст без ссылки"))

    @Test fun emptyClipboard() = assertNull(LinkDownloader.findUrl(null))

    // Only http and https: a file or content link must not be fetched by the network button.
    @Test fun otherSchemesAreNotLinks() = assertNull(LinkDownloader.findUrl("file:///sdcard/a.jpg"))
}
