package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.tasks.DownloadedFile
import java.io.File

internal data class NodeJsEnv(
    val nodeDir: DownloadedFile,
    val nodeBinDir: File,
    val nodeExecutable: String,

    val platformName: String,
    val architectureName: String,
    val ivyDependency: String
) {
    val isWindows: Boolean
        get() = platformName == "win"
}
