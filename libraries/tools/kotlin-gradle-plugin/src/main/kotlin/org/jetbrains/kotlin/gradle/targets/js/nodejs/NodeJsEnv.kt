package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.tasks.internal.DownloadedFile

internal data class NodeJsEnv(
    val nodeDir: DownloadedFile,
    val nodeBinDir: DownloadedFile,
    val nodeExecutable: String,

    val platformName: String,
    val architectureName: String,
    val ivyDependency: String
) {
    val isWindows: Boolean
        get() = platformName == "win"
}
