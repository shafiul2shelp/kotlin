/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import java.io.File
import java.nio.file.Files
import java.time.Instant
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask

/**
 * Store of tracked directories that can be cleaned with [CleanDataTask].
 * All directories that was not accessed [CleanDataTask.timeToLiveInDays] will be removed.
 *
 * To register store call `CleanableStore["/path/to/dir"]`.
 * Now you will be able to access files via `CleanableStore["/path/to/dir"]["file/name"].use()`
 * and it would update usage of th store.
 */
interface CleanableStore {
    fun cleanDir(expirationDate: Instant)

    operator fun get(fileName: String): DownloadedFile

    companion object {
        private val mutableStores = mutableMapOf<String, CleanableStore>()

        val stores: Map<String, CleanableStore>
            get() = mutableStores.toMap()

        operator fun get(path: String): CleanableStore =
            mutableStores.getOrPut(path) { CleanableStoreImpl(path) }
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