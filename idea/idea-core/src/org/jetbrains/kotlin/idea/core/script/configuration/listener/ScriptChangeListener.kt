/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.isScriptChangesNotifierDisabled
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile

/**
 * [ScriptChangesNotifier] will call first applicable [ScriptChangeListener] when editor is activated or document changed.
 *
 * Listener may call [ScriptConfigurationUpdater] to invalidate configuration and schedule reloading.
 *
 * @see DefaultScriptConfigurationManager for more details.
 */
abstract class ScriptChangeListener(protected val project: Project) {
    abstract fun editorActivated(vFile: VirtualFile, updater: ScriptConfigurationUpdater)
    abstract fun documentChanged(vFile: VirtualFile, updater: ScriptConfigurationUpdater)

    abstract fun isApplicable(vFile: VirtualFile): Boolean

    protected fun getAnalyzableKtFileForScript(vFile: VirtualFile): KtFile? {
        if (project.isDisposed) return null

        return (PsiManager.getInstance(project).findFile(vFile) as? KtFile)?.takeIf {
            ProjectRootsUtil.isInProjectSource(it, includeScriptsOutsideSourceRoots = true)
        }
    }

    companion object {
        private val LISTENER: ExtensionPointName<ScriptChangeListener> =
            ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.listener")

        fun getListener(project: Project, file: VirtualFile): ScriptChangeListener? {
            if (project.isDisposed || areListenersDisabled()) return null

            return LISTENER.getPoint(project).extensionList.firstOrNull { it.isApplicable(file) }
                ?: DefaultScriptChangeListener(project).takeIf { it.isApplicable(file) }
        }

        private fun areListenersDisabled(): Boolean {
            return ApplicationManager.getApplication().isUnitTestMode && ApplicationManager.getApplication().isScriptChangesNotifierDisabled == true
        }
    }
}