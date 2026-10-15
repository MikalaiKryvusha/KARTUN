package com.kriniks.kartun

import java.io.File

/** The real catalogue the APK carries (app/src/main/assets/catalogue.json, written by tools/catalogue.mjs) - the tests
 *  read the shipped file, not a fixture, so a catalogue edit that breaks the app reddens here. */
object TestCatalogue {
    val catalogue: Catalogue by lazy { ModelCatalog.parse(File("src/main/assets/catalogue.json").readText()) }
}
