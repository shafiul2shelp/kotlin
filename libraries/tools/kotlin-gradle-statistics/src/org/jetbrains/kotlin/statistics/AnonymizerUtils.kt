/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import java.io.File
import java.security.MessageDigest

internal val salt: String by lazy {
    val env = System.getenv()
    "${env["HOSTNAME"]}${env["COMPUTERNAME"]}"
}

fun anonymizeComponentVersion(version: String): String {
    return version.replace('-', '.')
        .split(".")
        .filterIndexed { i, _ -> i < 2 }
        .map { s -> s.toIntOrNull() ?: "?" }
        .joinToString(".")
}

internal fun hashFilePath(file: File): String {
    return hashFilePath(file.absolutePath)
}

internal fun hashFilePath(file: String): String {
    return sha256(salt + file)
}

internal fun sha256(s: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(s.toByteArray())
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}
