// 修改后的 DownloadScreen.kt
package cc.bbq.xq.ui.download

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = koinViewModel()
) {
    val status by viewModel.downloadStatus.collectAsState()
    val downloadTasks by viewModel.downloadTasks.collectAsState()
    val hasActiveTask = status !is DownloadStatus.Idle

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

            // 下载任务列表
            if (downloadTasks.isEmpty()) {
                EmptyDownloadState()
            } else {
                LazyColumn {
                    items(downloadTasks) { task ->
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
    viewModel: DownloadViewModel
) {
    val context = LocalContext.current
    val status = remember(task) { createDownloadStatusFromTask(task) }

    BBQCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题和链接
            Text(
                text = task.fileName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 状态显示
            when (status) {
                is DownloadStatus.Idle -> Text("等待开始", style = MaterialTheme.typography.bodySmall)
                is DownloadStatus.Pending -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("准备中...", style = MaterialTheme.typography.bodySmall)
                }
                is DownloadStatus.Downloading -> {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${formatFileSize(context, status.downloadedBytes)} / ${formatFileSize(context, status.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = status.speed,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is DownloadStatus.Paused -> {
                    Text("已暂停", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "已下载: ${formatFileSize(context, status.downloadedBytes)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                is DownloadStatus.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("下载完成", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = formatFileSize(context, status.file.length()),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is DownloadStatus.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载失败", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮行
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 浏览链接按钮（所有状态都可用）
                BBQButton(
                    onClick = { viewModel.openUrlInBrowser(task.url) },
                    text = { Text("浏览链接") }
                )
                Spacer(modifier = Modifier.width(8.dp))

                // 查看文件按钮（仅成功状态）
                if (status is DownloadStatus.Success) {
                    BBQButton(
                        onClick = {
                            FileActionUtil.openFile(context, status.file)
                        },
                        text = { Text("查看文件") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // 删除任务按钮（所有状态都可用）
                BBQButton(
                    onClick = { viewModel.deleteDownloadTask(task) },
                    text = { Text("删除任务") }
                )
            }
        }
    }
}

// 辅助函数：从数据库任务创建状态对象
private fun createDownloadStatusFromTask(task: DownloadTask): DownloadStatus {
    return when (task.status) {
        DownloadStatus.Idle::class.java.simpleName -> DownloadStatus.Idle
        DownloadStatus.Pending::class.java.simpleName -> DownloadStatus.Pending
        DownloadStatus.Downloading::class.java.simpleName -> DownloadStatus.Downloading(
            progress = task.progress,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speed = task.speed ?: ""
        )
        DownloadStatus.Paused::class.java.simpleName -> DownloadStatus.Paused(
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes
        )
        DownloadStatus.Success::class.java.simpleName -> {
            val file = java.io.File(task.savePath, task.fileName)
            DownloadStatus.Success(file = file)
        }
        DownloadStatus.Error::class.java.simpleName -> DownloadStatus.Error(
            message = task.errorMessage ?: "未知错误"
        )
        else -> DownloadStatus.Idle
    }
}

@Composable
fun EmptyDownloadState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无下载任务",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PendingDownloadState() {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("准备中...")
        }
    }
}

@Composable
fun DownloadingState(
    status: DownloadStatus.Downloading,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "正在下载文件...",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatFileSize(context, status.downloadedBytes)} / ${formatFileSize(context, status.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BBQIconButton(
                    onClick = onCancel,
                    icon = Icons.Default.Close,
                    contentDescription = "取消下载",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(AppShapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(status.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.speed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PausedState(status: DownloadStatus.Paused) {
    BBQCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("下载已暂停", style = MaterialTheme.typography.titleMedium)
                Text(
                    "已下载: ${formatFileSize(LocalContext.current, status.downloadedBytes)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SuccessState(
    status: DownloadStatus.Success,
    onViewFile: (() -> Unit)? = null
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
                if (onViewFile != null) {
                    BBQButton(
                        onClick = onViewFile,
                        text = { Text("查看文件") }
                    )
                } else {
                    BBQButton(
                        onClick = {
                            FileActionUtil.openFile(context, status.file)
                        },
                        text = { Text("查看文件") }
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