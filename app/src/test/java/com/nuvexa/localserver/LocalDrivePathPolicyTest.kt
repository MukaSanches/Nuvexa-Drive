/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.localserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LocalDrivePathPolicyTest {
    @Test
    fun rejectsTraversalAndSeparators() {
        assertNull(LocalDrivePathPolicy.safeLeafName(".."))
        assertNull(LocalDrivePathPolicy.safeLeafName("../secret"))
        assertNull(LocalDrivePathPolicy.safeLeafName("folder/file"))
        assertNull(LocalDrivePathPolicy.safeLeafName("folder\\\\file"))
    }

    @Test
    fun resolvesOnlyInsideRoot() {
        val root = Files.createTempDirectory("nuvexa-local-test").toFile()
        try {
            val resolved = LocalDrivePathPolicy.resolveChild(root, "docs", "hello.txt")
            assertTrue(resolved != null)
            assertTrue(resolved!!.canonicalPath.startsWith(root.canonicalPath))
            assertEquals("hello.txt", resolved.name)
            assertNull(LocalDrivePathPolicy.resolveDirectory(root, "../outside"))
        } finally {
            root.deleteRecursively()
        }
    }
}
