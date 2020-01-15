/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import kotlin.reflect.KProperty

open class ConfigurationPhaseAware {
    private var built = false

    protected fun markBuilt() {
        built = true
    }

    protected fun requireNotBuilt() {
        check(!built) { "environment already built for previous property values" }
    }

    inner class Property<T>(var value: T) {
        operator fun getValue(receiver: Any, property: KProperty<*>): T = value

        operator fun setValue(receiver: Any, property: KProperty<*>, value: T) {
            requireNotBuilt()
            this.value = value
        }
    }
}