/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.fileloggers

import org.jetbrains.kotlin.statistics.*
import org.jetbrains.kotlin.statistics.hashFilePath
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BuildSessionLogger(rootPath: File) : IStatisticsValuesConsumer {
    companion object {
        val MAX_PROFILE_FILES = 1_000
        val MAX_PROFILE_FILE_SIZE = 100_000L
    }

    private val profileFileNameFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd-HH-mm-ss-SSS'.profile'")
    private val statisticsFolder: File = File(rootPath, "kotlin-profile").also { it.mkdirs() }

    private var buildSession: BuildSession? = null
    private var trackingFile: IRecordLogger? = null

    @Synchronized
    fun startBuildSession(projectRootPath: File, gradleVersion: String, buildSinceDaemonStart: Long, buildStartedTime: Long?) {
        buildSession =
            BuildSession(hashFilePath(projectRootPath), anonymizeComponentVersion(gradleVersion), buildSinceDaemonStart, buildStartedTime)
        initTrackingFile()
    }

    @Synchronized
    fun isBuildSessionStarted() = buildSession != null

    @Synchronized
    private fun closeTrackingFile() {
        trackingFile?.close()
        trackingFile = null
    }

    @Synchronized
    private fun initTrackingFile() {
        closeTrackingFile()

        // Get list of existing files. Try to create folder if possible, return from function if failed to create folder
        val fileCandidates = statisticsFolder.listFiles()?.toMutableList() ?: if (statisticsFolder.mkdirs()) emptyList<File>() else return

        for (i in 0..(fileCandidates.size - MAX_PROFILE_FILES)) {
            val file2delete = fileCandidates[i]
            if (file2delete.isFile) {
                file2delete.delete()
            }
        }

        // emergency check. What if a lot of files are locked due to some reason
        if (statisticsFolder.listFiles()?.size ?: 0 > MAX_PROFILE_FILES * 2) {
            return
        }

        val lastFile = fileCandidates.lastOrNull() ?: File(statisticsFolder, profileFileNameFormatter.format(LocalDateTime.now()))

        trackingFile = try {
            if (lastFile.length() < MAX_PROFILE_FILE_SIZE) {
                FileRecordLogger(lastFile)
            } else {
                null
            }
        } catch (e: IOException) {
            try {
                FileRecordLogger(File(statisticsFolder, profileFileNameFormatter.format(LocalDateTime.now())))
            } catch (e: IOException) {
                NullRecordLogger()
            }
        }
    }

    @Synchronized
    fun finishBuildSession(action: String?, failure: Throwable?) {
        buildSession?.finishBuildSession(trackingFile)
        buildSession = null
    }

    @Synchronized
    fun unlockJournalFile() {
        closeTrackingFile()
    }

    override fun reportValueOnce(value: ReportOnceStatisticsValue<Any>) {
        buildSession?.reportValueOnce(value)
    }

    override fun reportValueAdd(value: AdditiveStatisticsValue<Any>) {
        buildSession?.reportValueAdd(value)
    }

    //TODO add functions for reporting parameters

    // TODO add disable option
    // add file rotation on maximal file size
    // add file rotation on maximal number of records to be written


    //serialisation tollerant to absence of classes, properties, ... Should it be binary?

}