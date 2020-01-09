/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Files
import java.time.Instant

interface CleanDataProvider {

    fun cleanDir(expirationDate: Instant)

    fun markAsRead(fileName: String)

    fun markAsReadByPath(path: String)

}

class CleanDataProviderImpl(dirPath: String) : CleanDataProvider {
    private val dir = File(dirPath)
    private val accessFileSuffix = "-access"

    override fun markAsRead(fileName: String) {
        val accessFileName = "$fileName$accessFileSuffix"

        val accessFile = File(dir, accessFileName)
        accessFile.createNewFile()
    }

    override fun markAsReadByPath(path: String) {
        val folderPath = dir.path
        if (!path.startsWith(folderPath)) {
            throw IllegalStateException("CleanDataProvider register for $folderPath. Try to mark access for file $path from different folder")
        }
        val fileName = path.substring(folderPath.length)
        markAsRead(fileName)
    }

    override fun cleanDir(expirationDate: Instant) {
        fun modificationDate(file: File): Instant {
            return Files.getLastModifiedTime(file.toPath()).toInstant()
        }

        fun getAllLinkedFileNames(accessFileName: String): List<String> {
            return listOf(accessFileName, accessFileName.removeSuffix(accessFileSuffix))
        }

        val filesInDirByName = dir.listFiles()?.map { it.name to it }?.toMap()

        filesInDirByName
            ?.filter { (fileName, file) ->
                file.isFile && fileName.endsWith(accessFileSuffix)
                        && modificationDate(file).isBefore(expirationDate)
            }
            ?.flatMap { (filename, _) -> getAllLinkedFileNames(filename) }
            ?.forEach { fileName -> filesInDirByName[fileName]?.deleteRecursively() }
    }
}