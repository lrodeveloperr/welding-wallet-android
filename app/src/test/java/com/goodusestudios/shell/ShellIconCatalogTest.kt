package com.goodusestudios.weldinggaswallet

import com.goodusestudios.weldinggaswallet.ui.ShellIconCatalog
import com.goodusestudios.weldinggaswallet.ui.ShellIconCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellIconCatalogTest {
    @Test fun idsAreUnique() =
        assertEquals(ShellIconCatalog.all.size, ShellIconCatalog.all.map { it.id }.distinct().size)

    @Test fun searchMatchesLabelIdAndCategory() {
        assertEquals("settings", ShellIconCatalog.search("Settings").single().id)
        assertEquals("shopping_cart", ShellIconCatalog.search("shopping_cart").single().id)
        assertTrue(ShellIconCatalog.search("communication").all { it.category == ShellIconCategory.Communication })
    }
}
