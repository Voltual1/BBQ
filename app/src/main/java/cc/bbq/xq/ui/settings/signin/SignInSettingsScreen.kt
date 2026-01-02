package cc.bbq.xq.ui.settings.signin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQOutlinedButton
import cc.bbq.xq.ui.theme.SwitchWithText
import cc.bbq.xq.ui.theme.BBQSnackbarHost
import cc.bbq.xq.ui.theme.BBQSuccessSnackbar
import cc.bbq.xq.ui.theme.BBQErrorSnackbar
import cc.bbq.xq.ui.theme.BBQInfoSnackbar
import kotlinx.coroutines.launch

@Composable
fun SignInSettingsScreen(
    viewModel: SignInSettingsViewModel = viewModel(),
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val autoSignIn by viewModel.autoSignIn.collectAsState(initial = false)
    val signInState by viewModel.signInState.collectAsState()
    val context = LocalContext.current
    
    // 监听签到状态变化，显示Snackbar
    LaunchedEffect(signInState) {
        when (val state = signInState) {
            is SignInState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            is SignInState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            is SignInState.Info -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "签到设置",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 自动签到开关
        SwitchWithText(
            text = "开启自动签到",
            checked = autoSignIn,
            onCheckedChange = { checked ->
                scope.launch {
                    viewModel.setAutoSignIn(checked)
                    // 如果开启自动签到，立即执行一次签到
                    if (checked) {
                        viewModel.signIn(context)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 自动签到说明
        Text(
            text = "开启后，每天首次打开应用时会自动签到",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, bottom = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 手动签到部分
        Text(
            text = "手动签到",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 签到按钮
            BBQButton(
                onClick = {
                    viewModel.signIn(context)
                },
                enabled = signInState !is SignInState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                text = {
                    if (signInState is SignInState.Loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "立即签到",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 签到说明
            Text(
                text = "每日签到可获得经验和硬币奖励",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 测试按钮（仅在调试时显示）
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))
                
                BBQOutlinedButton(
                    onClick = {
                        scope.launch {
                            // 模拟签到成功
                            snackbarHostState.showSnackbar("签到成功: 获得10经验和5硬币")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Text("测试签到成功")
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BBQOutlinedButton(
                    onClick = {
                        scope.launch {
                            // 模拟签到失败
                            snackbarHostState.showSnackbar("签到失败: 今日已签到")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Text("测试今日已签到")
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BBQOutlinedButton(
                    onClick = {
                        scope.launch {
                            // 模拟错误
                            snackbarHostState.showSnackbar("签到失败: 网络连接错误")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Text("测试网络错误")
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 签到提示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "签到提示",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "• 每天只能签到一次\n" +
                           "• 连续签到可获得额外奖励\n" +
                           "• 签到时间以服务器时间为准",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5
                )
            }
        }
    }
}