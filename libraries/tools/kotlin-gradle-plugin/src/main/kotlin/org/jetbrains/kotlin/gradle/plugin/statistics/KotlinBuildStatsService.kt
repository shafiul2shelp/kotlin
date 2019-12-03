/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.initialization.BuildCompletionListener
import org.gradle.initialization.BuildRequestMetaData
import org.gradle.invocation.DefaultGradle
import org.jetbrains.kotlin.statistics.AdditiveStatisticsValue
import org.jetbrains.kotlin.statistics.IStatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.ReportOnceStatisticsValue
import org.jetbrains.kotlin.statistics.fileloggers.BuildSessionLogger
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean

//TODO configure log levels in one place. Add possibility to disable logging at all

interface KotlinBuildStatsMXBean : IStatisticsValuesConsumer {

    fun onProjectEvaluated(projectDir: File, gradleVersion: String, gradleBuildStartTime: Long?)

    fun onBuildFinished(action: String, failure: Throwable?)

    fun onCompleted()
}

internal abstract class KotlinBuildStatsService(private val gradle: Gradle) : BuildAdapter(), KotlinBuildStatsMXBean,
    BuildCompletionListener {
    companion object {
        //Think twice before renaming this constant
        const val JMX_BEAN_NAME = "org.jetbrains.kotlin.gradle.plugin.statistics:type=StatsService"
        private var instance: KotlinBuildStatsService? = null
        private var statisticsIsEnabled: Boolean = true

        @JvmStatic
        @Synchronized
        fun getInstance(): IStatisticsValuesConsumer? {
            if (!statisticsIsEnabled) {
                return null
            }
            return instance
        }

        @JvmStatic
        @Synchronized
        internal fun getOrCreateInstance(gradle: Gradle): IStatisticsValuesConsumer? {
            try {
                //TODO return nullInstance if disabled by flags
                //TODO set statisticsIsEnabled
                if (!statisticsIsEnabled) {
                    return null
                }
                val log = getLogger()

                if (instance != null) {
                    log.error("${KotlinBuildStatsService::class.java} is already instantiated. Current instance is $instance")
                } else {
                    val beanName = ObjectName(JMX_BEAN_NAME)
                    val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
                    if (mbs.isRegistered(beanName)) {
                        log.error("${KotlinBuildStatsService::class.java} is already instantiated in another classpath. Creating JMX-wrapper")
                        instance = JMXKotlinBuildStatsService(mbs, beanName, gradle)
                    } else {
                        instance = DefaultKotlinBuildStatsService(gradle)
                        log.error("Instantiated ${KotlinBuildStatsService::class.java}: new instance $instance")
                        mbs.registerMBean(StandardMBean(instance, KotlinBuildStatsMXBean::class.java), beanName)
                    }
                }
                gradle.addBuildListener(instance)
                return instance!!
            } catch (e: Exception) {
                getLogger().warn("Could not crate ${KotlinBuildStatsService::class.java}: ${e.message}")
                getLogger().debug(e.message, e)
                return null
            }
        }

        @JvmStatic
        internal fun getLogger() = Logging.getLogger(KotlinBuildStatsService::class.java)
    }

    private fun gradleBuildStartTime(gradle: Gradle): Long? {
        return (gradle as? DefaultGradle)?.services?.get(BuildRequestMetaData::class.java)?.startTime
    }

    final override fun projectsEvaluated(gradle: Gradle) {
        try {
            onProjectEvaluated(
                gradle.rootProject.projectDir,
                gradle.gradleVersion,
                gradleBuildStartTime(gradle)
            )
        } catch (e: Exception) {
            getLogger().warn("Could not initialize the Kotlin profiler")
            getLogger().debug(e.message, e)
        }
    }

    final override fun buildFinished(result: BuildResult) {
        try {
            onBuildFinished(result.action, result.failure)
        } catch (e: Exception) {
            getLogger().warn("Could not dispose the Kotlin profiler")
            getLogger().debug(e.message, e)
        }
    }

    final override fun completed() {
        try {
            getLogger()?.debug("Unregistering ${KotlinBuildStatsService::class.java}.")
            gradle.removeListener(this)
        } finally {
            try {
                onCompleted()
            } catch (e: Exception) {
                getLogger().warn("Could not dispose the Kotlin profiler")
                getLogger().debug(e.message, e)
            }
        }
    }
}

internal class JMXKotlinBuildStatsService(private val mbs: MBeanServer, private val beanName: ObjectName, gradle: Gradle) :
    KotlinBuildStatsService(gradle) {
    override fun onCompleted() {
        mbs.invoke(beanName, "onCompleted", null, null)
    }

    override fun onProjectEvaluated(projectDir: File, gradleVersion: String, gradleBuildStartTime: Long?) {
        mbs.invoke(
            beanName,
            "onProjectEvaluated",
            arrayOf(projectDir, gradleVersion, gradleBuildStartTime),
            arrayOf("java.io.File", "java.lang.String", "java.lang.Long")
        )
    }

    override fun onBuildFinished(action: String, failure: Throwable?) {
        mbs.invoke(
            beanName,
            "onBuildFinished",
            arrayOf(action, failure),
            arrayOf("java.lang.String", "java.lang.Throwable")
        )
    }

    override fun reportValueOnce(value: ReportOnceStatisticsValue<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reportValueAdd(value: AdditiveStatisticsValue<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

internal class DefaultKotlinBuildStatsService internal constructor(
    gradle: Gradle
) : KotlinBuildStatsService(gradle) {

    private val sessionLogger = BuildSessionLogger(gradle.gradleUserHomeDir)

    private val buildNumberSinceDaemonStart = AtomicLong()

    @Synchronized
    override fun onProjectEvaluated(projectDir: File, gradleVersion: String, gradleBuildStartTime: Long?) {
        if (!sessionLogger.isBuildSessionStarted()) {
            sessionLogger.startBuildSession(
                projectDir,
                gradleVersion,
                buildNumberSinceDaemonStart.incrementAndGet(),
                gradleBuildStartTime
            )
        }
    }

    override fun onBuildFinished(action: String, failure: Throwable?) {
        sessionLogger.finishBuildSession(action, failure)
    }

    @Synchronized
    override fun onCompleted() {
        sessionLogger.unlockJournalFile()
    }

    override fun reportValueOnce(value: ReportOnceStatisticsValue<Any>) {
        sessionLogger.reportValueOnce(value)
    }

    override fun reportValueAdd(value: AdditiveStatisticsValue<Any>) {
        sessionLogger.reportValueAdd(value)
    }
}
