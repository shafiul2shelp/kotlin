/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CompilerPhase
import org.jetbrains.kotlin.backend.common.PhaseRunner
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.dump
import kotlin.system.measureTimeMillis

enum class JvmLoweringPhase(
    override val description: String,
    vararg prerequisiteVararg: JvmLoweringPhase
) : CompilerPhase {
    START_LOWERING("State before lowering starts"),

    COERCION_TO_UNIT_PATCHER("Patch coercion to unit"),
    FILE_CLASS("File class lowering"),
    KCALLABLE_NAME_PROPERTY("KCallable name property lowering"),
    LATEINIT("Lateinit lowering"),
    CONST_AND_JVM_PROPERTIES("Const and JvmField properties lowering"),
    PROPERTIES("Properties lowering"),
    ANNOTATION("Annotation lowering"),
    DEFAULT_ARGUMENT_STUB_GENERATOR("Default argument stub generator", ANNOTATION),
    INTERFACE("Interface lowering", DEFAULT_ARGUMENT_STUB_GENERATOR),
    INTERFACE_DELEGATION("Interface delegation lowering"),
    SHARED_VARIABLES("Shared variables lowering"),
    PATCH_PARENTS_1("Patch declaration parents"),
    LOCAL_DECLARATIONS("Local declarations lowering"),
    CALLABLE_REFERENCE("Callable reference lowering"),
    FUNCTIONN_VARARG_INVOKE("FunctionN vararg invoke lowering"),
    INNER_CLASSES("Inner classes lowering"),
    INNER_CLASS_CONSTRUCTOR_CALLS("Inner classs constructor calls"),
    PATCH_PARENTS_2("Patch declaration parents"),
    ENUM_CLASS("Enum class lowering"),
    OBJECT_CLASS("Object class lowering"),
    INITIALIZERS("Initializers lowering"),
    SINGLETON_REFERENCES("Singleton references lowering"),
    SYNTHETIC_ACCESSOR("Synthetic accessor lowering", OBJECT_CLASS),
    BRIDGE("Bridge lowering"),
    JVM_OVERLOADS_ANNOTATION("jvmOverloads annotation lowering"),
    JVM_STATIC_ANNOTATION("JvmStatic annotation lowering"),
    STATIC_DEFAULT_FUNCTION("Static default function lowering"),
    TAILREC("Tailrec lowering"),
    TO_ARRAY("toArray lowering"),
    PATCH_PARENTS_3("Patch declaration parents"),

    END_LOWERING("State at the end of lowering");

    override val prerequisite = prerequisiteVararg.toSet()
}

enum class BeforeOrAfter { BEFORE, AFTER }

object JvmPhaseRunner: PhaseRunner<JvmBackendContext, IrFile> {
    override fun reportBefore(context: JvmBackendContext, data: IrFile, phase: CompilerPhase, depth: Int) {
        if (phase in context.phases.toDumpStateBefore) {
            dumpFile(data, phase, BeforeOrAfter.BEFORE)
        }
    }

    override fun runBody(context: JvmBackendContext, phase: CompilerPhase, body: () -> Unit) {
        val runner = when {
            phase == JvmLoweringPhase.START_LOWERING -> ::justRun
            phase == JvmLoweringPhase.END_LOWERING -> ::justRun
            (context.state.configuration.get(CommonConfigurationKeys.PROFILE_PHASES) == true) -> ::runAndProfile
            else -> ::justRun
        }

        context.inVerbosePhase = (phase in context.phases.verbose)

        runner(phase.description, body)

        context.inVerbosePhase = false
    }

    override fun reportAfter(context: JvmBackendContext, data: IrFile, phase: CompilerPhase, depth: Int) {
        if (phase in context.phases.toDumpStateAfter) {
            dumpFile(data, phase, BeforeOrAfter.AFTER)
        }
    }
}

private fun runAndProfile(message: String, body: () -> Unit) {
    val msec = measureTimeMillis(body)
    println("$message: $msec msec")
}

@Suppress("UNUSED_PARAMETER")
private fun justRun(message: String, body: () -> Unit) = body()

private fun separator(title: String) {
    println("\n\n--- ${title} ----------------------\n")
}

private fun dumpFile(irFile: IrFile, phase: CompilerPhase, beforeOrAfter: BeforeOrAfter) {
    // Exclude nonsensical combinations
    if (phase == JvmLoweringPhase.START_LOWERING && beforeOrAfter == BeforeOrAfter.AFTER) return
    if (phase == JvmLoweringPhase.END_LOWERING && beforeOrAfter == BeforeOrAfter.BEFORE) return

    val title = when (phase) {
        JvmLoweringPhase.START_LOWERING -> "IR for ${irFile.name} at the start of lowering process"
        JvmLoweringPhase.END_LOWERING -> "IR for ${irFile.name} at the end of lowering process"
        else -> {
            val beforeOrAfterStr = beforeOrAfter.name.toLowerCase()
            "IR for ${irFile.name} $beforeOrAfterStr ${phase.description}"
        }
    }
    separator(title)
    println(irFile.dump())
}