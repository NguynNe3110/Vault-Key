package com.example

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ArchitectureTest {
    private val root = File("src/main/java/com/example")
    private fun sources(path: String) = File(root, path).walkTopDown().filter { it.extension == "kt" }.toList()
    @Test fun domainIsIndependentOfFrameworksAndImplementations() {
        val forbidden = Regex("(?m)^import (android\\.|androidx\\.|com\\.example\\.(data|feature|ui)\\.|com\\.squareup\\.moshi)")
        sources("domain").forEach { assertFalse(it.path, forbidden.containsMatchIn(it.readText())) }
    }
    @Test fun screensAndViewModelsDoNotAccessTheDataLayer() {
        sources("feature").forEach { file ->
            assertFalse(file.path, Regex("(?m)^import com\\.example\\.data\\.").containsMatchIn(file.readText()))
        }
    }
    @Test fun sharedComponentsDoNotDependOnFeatures() {
        sources("ui/component").forEach { assertFalse(it.path, it.readText().contains("import com.example.feature.")) }
    }
    @Test fun packageMatchesFileLocation() {
        sources("").forEach { file ->
            val expected = "com.example." + checkNotNull(file.parentFile).relativeTo(root).path.replace(File.separatorChar, '.')
            val actual = Regex("(?m)^package ([a-zA-Z0-9_.]+)").find(file.readText().removePrefix("\uFEFF"))!!.groupValues[1]
            assertEquals(file.path, expected.trimEnd('.'), actual)
        }
    }
    @Test fun productDoesNotUseStockMaterialControlsOrRoutineSyncBanners() {
        val stock = Regex("\\b(OutlinedTextField|FilterChip|SuggestionChip|NavigationBarItem|AlertDialog|DropdownMenu|ExtendedFloatingActionButton)\\s*\\(")
        sources("feature").plus(sources("ui")).forEach { file ->
            assertFalse(file.path, stock.containsMatchIn(file.readText()))
            assertFalse(file.path, file.readText().contains("Tự động đồng bộ đang bật"))
        }
    }
}
