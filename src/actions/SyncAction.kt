package org.jetbrains.settingsRepository.actions

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.settingsRepository.PLUGIN_NAME
import org.jetbrains.settingsRepository.SyncType
import org.jetbrains.settingsRepository.IcsManager
import org.jetbrains.settingsRepository.IcsBundle
import com.intellij.openapi.project.Project
import org.jetbrains.settingsRepository.IcsSettingsEditor

val NOTIFICATION_GROUP = NotificationGroup.balloonGroup(PLUGIN_NAME)

abstract class SyncAction(private val syncType: SyncType) : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    e.getPresentation().setEnabledAndVisible(IcsManager.getInstance().repositoryManager.hasUpstream())
  }

  override fun actionPerformed(event: AnActionEvent) {
    syncAndNotify(syncType, event.getProject())
  }
}

fun syncAndNotify(syncType: SyncType, project: Project?, notifyIfUpToDate: Boolean = true) {
  try {
    if (IcsManager.getInstance().sync(syncType, project) == null && !notifyIfUpToDate) {
      return
    }
  }
  catch (e: Exception) {
    NOTIFICATION_GROUP.createNotification(IcsBundle.message("sync.rejected.title"), e.getMessage() ?: "Internal error", NotificationType.ERROR, null).notify(project)
  }
  NOTIFICATION_GROUP.createNotification(IcsBundle.message("sync.done.message"), NotificationType.INFORMATION).notify(project)
}

// we don't
class MergeAction : SyncAction(SyncType.MERGE)
class ResetToTheirsAction : SyncAction(SyncType.RESET_TO_THEIRS)
class ResetToMyAction : SyncAction(SyncType.RESET_TO_MY)

class ConfigureIcsAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    IcsManager.getInstance().runInAutoCommitDisabledMode {
      IcsSettingsEditor(e.getProject()).show()
    }
  }

  override fun update(e: AnActionEvent) {
    e.getPresentation().setIcon(null)
  }
}