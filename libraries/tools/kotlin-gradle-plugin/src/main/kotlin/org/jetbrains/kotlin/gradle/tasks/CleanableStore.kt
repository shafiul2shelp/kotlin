/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import java.io.File
import java.nio.file.Files
import java.time.Instant

interface CleanableStore {

    fun cleanDir(expirationDate: Instant)

    operator fun get(fileName: String): DownloadedFile

    companion object {
        val cleanableStores = mutableMapOf<String, CleanableStore>()

        operator fun get(path: String): CleanableStore =
            cleanableStores.getOrPut(path) { CleanableStoreImpl(path) }
    }

}

class CleanableStoreImpl(dirPath: String) : CleanableStore {
    private val dir = File(dirPath)

    override fun get(fileName: String): DownloadedFile = DownloadedFileImpl(dir.resolve(fileName))

    override fun cleanDir(expirationDate: Instant) {
        fun modificationDate(file: File): Instant {
            return Files.getLastModifiedTime(file.toPath()).toInstant()
        }

        dir.listFiles()
            ?.filter { file ->
                modificationDate(file).isBefore(expirationDate)
            }
            ?.forEach { file -> file.deleteRecursively() }
    }

}