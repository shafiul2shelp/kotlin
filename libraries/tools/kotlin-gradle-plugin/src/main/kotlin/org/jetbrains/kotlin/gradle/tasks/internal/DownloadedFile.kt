/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.gradle.util.GFileUtils
import java.io.File

interface DownloadedFile {
    val file: File

    fun resolve(fileName: String): DownloadedFile
}

private fun use(file: File, dir: File): File {
    if (dir.exists()) {
        GFileUtils.touchExisting(dir)
    }
    return file
}

internal class DownloadedFileImpl( private val _file: File) : DownloadedFile {
    override val file: File
        get(): File = use(_file, dir)

    private var dir: File = _file

    private constructor( _file: File, _dir: File) : this(_file) {
        this.dir = _dir
    }

    override fun resolve(fileName: String): DownloadedFile {
        return DownloadedFileImpl(file.resolve(fileName), dir)
    }

}