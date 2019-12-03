/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.fileloggers.IRecordLogger
import java.lang.StringBuilder

class BuildSession(
    private val projectRootPathHash: String,
    private val gradleVersion: String,
    private val buildSinceDaemonStart: Long,
    private val buildStartedTime: Long?
): IStatisticsValuesConsumer {
    private val projectEvaluatedTime = System.currentTimeMillis()

    private val reportOnceStatisticsValues = HashMap<String, ReportOnceStatisticsValue<Any>>()

    private val additiveStatisticsValue = HashMap<String, AdditiveStatisticsValue<Any>>()

    //TODO strongly refactor this method!!!
    @Synchronized
    fun finishBuildSession(trackingFile: IRecordLogger?) {
        if (trackingFile == null) {
            return
        }
        val finishTime = System.currentTimeMillis()
        // save processor, OS, ...
        // save gradle version
        reportValueOnce(ReportOnceStatisticsValue("gradle_version", anonymizeComponentVersion(gradleVersion)))
        reportValueOnce(ReportOnceStatisticsValue("build_duration", if (buildStartedTime != null) (finishTime - buildStartedTime) else -1))
        reportValueOnce(ReportOnceStatisticsValue("execution_time", finishTime - projectEvaluatedTime))
        reportValueOnce(ReportOnceStatisticsValue("build_number_since_daemon_start", buildSinceDaemonStart))
//        reportValue(ReportOnceStatisticsValue("", ))
//        reportValue(ReportOnceStatisticsValue("", ))
//        reportValue(ReportOnceStatisticsValue("", ))


        val builder = StringBuilder()
        builder.append("[old version] Starting build session\r\n")
        for (v in reportOnceStatisticsValues.values.union(additiveStatisticsValue.values)) {
            builder.append("${v.name}=${v.value}\r\n")
        }
        builder.append("Finish build session\r\n\r\n")
        trackingFile.writeString(builder.toString())
    }

    @Synchronized
    override fun reportValueOnce(value: ReportOnceStatisticsValue<Any>) {
        reportOnceStatisticsValues[value.name] = value
    }

    @Synchronized
    override fun reportValueAdd(value: AdditiveStatisticsValue<Any>) {
        additiveStatisticsValue[value.name]?.addValue(value.value)
        additiveStatisticsValue.putIfAbsent(value.name, value)
    }
}