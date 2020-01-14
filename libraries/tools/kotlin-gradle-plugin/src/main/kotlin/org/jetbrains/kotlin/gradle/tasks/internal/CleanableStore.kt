/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import java.io.File
import java.nio.file.Files
import java.time.Instant

/**
 * CleanableStore register all stores that could be cleaned with CleanTask
 *
 * To register store call CleanableStore[<path_to_dir>]. Now you will be able to access files via
 * CleanableStore[<path_to_dir>][<file_name>].file and it would update usage of th store.
 */
interface CleanableStore {

    fun cleanDir(expirationDate: Instant)

    operator fun get(fileName: String): DownloadedFile

    companion object {
        private val cleanableStore = mutableMapOf<String, CleanableStore>()

        fun getStores(): Map<String, CleanableStore> = cleanableStore.toMap()

        operator fun get(path: String): CleanableStore =
            cleanableStore.getOrPut(path) { CleanableStoreImpl(path) }
    }

}

private class CleanableStoreImpl(dirPath: String) : CleanableStore {
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