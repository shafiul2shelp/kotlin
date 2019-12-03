/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.experimental_space


// https://android.googlesource.com/platform/tools/base/+/studio-master-dev/build-system/profile/src/main/java/com/android/builder/profile/ProcessProfileWriterFactory.java#
// https://android.googlesource.com/platform/tools/base/+/studio-master-dev/build-system/gradle-core/src/main/java/com/android/build/gradle/internal/profile/ProfilerInitializer.java?autodive=0%2F
/*
TODO:
1. add initialisation on loading plugin
2. by default save settings in .gradle/metrics
    - add housekeeping: delete every N days
    - delete on importing in IDE
 */

// TODO add metrics which could be merged automatically
data class LongMetric(val name: String, val value: Long) //TODO how to identify build (build session, build location)

data class EnvironmentMetric(val os: String, val proc: String) //TODO add much more parameters