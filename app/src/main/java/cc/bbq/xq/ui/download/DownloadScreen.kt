//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.download

import android.content.Intent // 添加 Intent 导入
import android.net.Uri // 添加 Uri 导入
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modposer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.service.download.DownloadStatus
import cc.bbq.xq.service.download.DownloadTask
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQIconButton
import cc.bbq.xq.util.FileActionUtil
import androidx.compose.foundation.shape.CircleShape

// ... [formatFileSize 函数保持不变] ...

@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = viewModel()
) {
    val status by viewModel.downloadStatus.collectAsState()
    val downloadTasks by viewModel.downloadTasks.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (downloadTasks.isEmpty()) {
                // ... [EmptyDownloadState, PendingDownloadState等保持不变] ...
                 AnimatedContent(targetState = status, label = "DownloadStatus") { currentStatus ->
                    when (currentStatus) {
                        is DownloadStatus.Idle -> EmptyDownloadState()
                        is DownloadStatus.Pending -> PendingDownloadState()
                        is DownloadStatus.Downloading -> DownloadingState(
                            status = currentStatus,
                            onCancel = { viewModel.cancelDownload() }
                        )
                        is DownloadStatus.Paused -> PausedState(currentStatus)
                        is DownloadStatus.Success -> SuccessState(currentStatus, viewModel, null) // null 表示不是列表项中的任务
                        is DownloadStatus.Error -> ErrorState(currentStatus)
                    }
                }
            } else {
                LazyColumn {
                    items(downloadTasks) { task ->
                        // 传递 viewModel 给 DownloadTaskItem
                        DownloadTaskItem(task = task, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    viewModel: DownloadViewModel // 传递 ViewModel
) {
    val context = LocalContext.current
    // 增强鲁棒性：安全地从数据库字符串状态转换为密封类实例
    val status = when (task.status) {
        DownloadStatus.Idle::class.java.simpleName -> DownloadStatus.Idle
        DownloadStatus.Pending::class.java.simpleName -> DownloadStatus.Pending
        DownloadStatus.Downloading::class.java.simpleName -> DownloadStatus.Downloading(
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speed = "" // 速度信息在数据库中没有存储，需要从服务中获取
        )
        DownloadStatus.Paused::class.java.simpleName -> DownloadStatus.Paused(
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes
        )
        DownloadStatus.Success::class.java.simpleName -> {
            val file = java.io.File(task.savePath, task.fileName)
            DownloadStatus.Success(file = file)
        }
        DownloadStatus.Error::class.java.simpleName -> DownloadStatus.Error(message = "未知错误")
        else -> {
            // 处理未知或损坏的状态字符串
            Log.w("DownloadScreen", "Unknown status string: ${task.status} for URL: ${task.url}")
            DownloadStatus.Error(message = "状态错误: ${task.status}")
        }
    }

    // 根据转换后的状态渲染UI
    when (status) {
        is DownloadStatus.Idle -> EmptyDownloadState()
        is DownloadStatus.Pending -> PendingDownloadState()
        is DownloadStatus.Downloading -> DownloadingState(
            status = status,
            onCancel = { viewModel.cancelDownload() }
        )
        is DownloadStatus.Paused -> PausedState(status)
        is DownloadStatus.Success -> SuccessState(status, viewModel, task) // 传递 task 用于删除
        is DownloadStatus.Error -> ErrorState(status)
    }
    // 为所有状态添加“浏览链接”按钮
    // 放在卡片底部或状态渲染之后
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        BBQButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.url))
                // 确保有应用可以处理这个Intent
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // 可选：通知用户没有默认浏览器
                    // 可以使用 Snackbar 或 Toast
                }
            },
            text = { Text("浏览链接") },
            modifier = Modifier // 可以添加 modifier 如 size 或 padding
        )
    }
}


// ... [EmptyDownloadState, PendingDownloadState, DownloadingState, PausedState 保持不变] ...

// 更新 SuccessState 以包含删除按钮
@Composable
fun SuccessState(
    status: DownloadStatus.Success,
    viewModel: DownloadViewModel, // 添加 ViewModel 参数
    task: DownloadTask? // 添加 task 参数，可能为 null (如果是当前下载而非列表项)
) {
    val context = LocalContext.current
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "下载完成",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // 查看文件按钮
                BBQButton(
                    onClick = {
                        FileActionUtil.openFile(context, status.file)
                    },
                    text = { Text("查看文件") },
                    modifier = Modifier // 可以添加间距 modifier 如 padding(end = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) // 按钮间添加间距

                // 删除任务按钮 - 仅当 task 存在时显示 (即列表项)
                task?.let {
                    BBQButton(
                        onClick = {
                            viewModel.deleteDownloadTask(it) // 调用 ViewModel 的删除方法
                        },
                        text = { Text("删除任务") },
                        // 可以根据需要调整按钮样式，例如使用 OutlinedButton 或不同的颜色
                        // 例如： colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorState(status: DownloadStatus.Error) {
    BBQCard(
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "下载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

// 辅助函数：格式化文件大小
fun formatFileSize(context: Context, size: Long): String {
    return Formatter.formatFileSize(context, size)
}