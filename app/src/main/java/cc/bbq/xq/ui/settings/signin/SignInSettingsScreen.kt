//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.settings.signin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.ui.theme.SwitchWithText
import kotlinx.coroutines.launch

@Composable
fun SignInSettingsScreen(
    viewModel: SignInSettingsViewModel = viewModel(),
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val autoSignIn by viewModel.autoSignIn.collectAsState(initial = false)
    val context = LocalContext.current
    
    // 状态用于显示额外信息
    var signInStatus by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 检查签到状态（只在首次加载时）
    LaunchedEffect(Unit) {
        viewModel.checkSignInStatus { isSignedIn, message ->
            signInStatus = if (isSignedIn) {
                "今日已签到"
            } else {
                "今日未签到"
            }
            message?.let {
                scope.launch {
                    snackbarHostState.showSnackbar(it)
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 自动签到开关
        SwitchWithText(
            text = "开启自动签到",
            checked = autoSignIn,
            onCheckedChange = { checked ->
                scope.launch {
                    viewModel.setAutoSignIn(checked)
                    if (checked) {
                        // 如果开启自动签到，立即执行一次签到
                        viewModel.performAutoSignIn { success, message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 开关下方的说明文本
        Text(
            text = if (autoSignIn) {
                "自动签到已开启，每日将自动为您签到"
            } else {
                "自动签到已关闭，需手动签到"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 显示签到状态
        signInStatus?.let { status ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "签到状态",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 手动签到按钮
        Button(
            onClick = {
                if (!isLoading) {
                    isLoading = true
                    viewModel.performAutoSignIn { success, message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                            if (success) {
                                signInStatus = "今日已签到"
                            }
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("签到中...")
            } else {
                Text("立即签到")
            }
        }
        
        // 按钮下方提示
        Text(
            text = "手动签到每日只能进行一次",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}