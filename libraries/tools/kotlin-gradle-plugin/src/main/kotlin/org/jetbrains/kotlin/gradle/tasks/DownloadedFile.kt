/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.util.GFileUtils
import java.io.File

interface DownloadedFile {
    val file: File
}

private fun markAsRead(file: File): File {
    if (file.exists()) {
        GFileUtils.touchExisting(file)
    }
    return file
}

internal class DownloadedFileImpl(private val _file: File) : DownloadedFile {
    override val file: File
        get(): File = markAsRead(_file)

}