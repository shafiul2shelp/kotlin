/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

interface ReportStatisticsValue<T> {
    val name: String
    val value: T
}

class ReportOnceStatisticsValue<T>(override val name: String, override val value: T) : ReportStatisticsValue<T>

interface AdditiveStatisticsValue<T> : ReportStatisticsValue<T> {
    fun addValue(t: T)
}

//TODO (critical): limit with simple types only
interface IStatisticsValuesConsumer {
    fun reportValueOnce(value: ReportOnceStatisticsValue<Any>)

    fun reportValueAdd(value: AdditiveStatisticsValue<Any>)
}