package org.jetbrains.settingsRepository

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import java.awt.Container
import javax.swing.Action
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ArrayUtil
import javax.swing.AbstractAction
import com.intellij.openapi.util.text.StringUtil
import java.awt.event.ActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import org.jetbrains.settingsRepository.actions.NOTIFICATION_GROUP
import com.intellij.notification.NotificationType
import java.awt.Component

fun updateSyncButtonState(url: String?, syncActions: Array<Action>) {
  val enabled: Boolean
  try {
    enabled = url != null && url.length() > 1 && IcsManager.getInstance().repositoryService.checkUrl(url, null);
  }
  catch (e: Exception) {
    enabled = false;
  }

  for (syncAction in syncActions) {
    syncAction.setEnabled(enabled);
  }
}

fun createMergeActions(project: Project?, urlTextField: TextFieldWithBrowseButton, dialogParent: Container, okAction: (() -> Unit)): Array<Action> {
  var syncTypes = SyncType.values()
  if (SystemInfo.isMac) {
    syncTypes = ArrayUtil.reverseArray(syncTypes)
  }

  val icsManager = IcsManager.getInstance()

  return Array(3) {
    val syncType = syncTypes[it]
    object : AbstractAction(IcsBundle.message("action." + (if (syncType == SyncType.MERGE) "Merge" else (if (syncType == SyncType.RESET_TO_THEIRS) "ResetToTheirs" else "ResetToMy")) + "Settings.text")) {
      fun saveRemoteRepositoryUrl(): Boolean {
        val url = StringUtil.nullize(urlTextField.getText())
        if (url != null && !icsManager.repositoryService.checkUrl(url, dialogParent)) {
          return false
        }

        val repositoryManager = icsManager.repositoryManager
        repositoryManager.createRepositoryIfNeed()
        repositoryManager.setUpstream(url, null)
        return true
      }

      override fun actionPerformed(event: ActionEvent) {
        val repositoryWillBeCreated = !icsManager.repositoryManager.isRepositoryExists()
        var upstreamSet = false
        try {
          if (!saveRemoteRepositoryUrl()) {
            if (repositoryWillBeCreated) {
              // remove created repository
              icsManager.repositoryManager.deleteRepository()
            }
            return
          }
          [suppress("UNUSED_VALUE")]
          (upstreamSet = true)

          if (repositoryWillBeCreated && syncType != SyncType.RESET_TO_THEIRS) {
            ApplicationManager.getApplication().saveSettings()

            icsManager.sync(syncType, project, { copyLocalConfig() })
          }
          else {
            icsManager.sync(syncType, project, null)
          }
        }
        catch (e: Throwable) {
          if (repositoryWillBeCreated) {
            // remove created repository
            icsManager.repositoryManager.deleteRepository()
          }

          LOG.warn(e)

          if (!upstreamSet || e is NoRemoteRepositoryException) {
            Messages.showErrorDialog(dialogParent, IcsBundle.message("set.upstream.failed.message", e.getMessage()), IcsBundle.message("set.upstream.failed.title"))
          }
          else {
            Messages.showErrorDialog(dialogParent, StringUtil.notNullize(e.getMessage(), "Internal error"), IcsBundle.message(if (e is AuthenticationException) "sync.not.authorized.title" else "sync.rejected.title"))
          }
          return
        }


        NOTIFICATION_GROUP.createNotification(IcsBundle.message("sync.done.message"), NotificationType.INFORMATION).notify(project)
        okAction()
      }
    }
  }
}