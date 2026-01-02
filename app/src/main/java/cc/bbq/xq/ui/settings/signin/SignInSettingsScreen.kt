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
 /*                   if (checked) {
                        // 如果开启自动签到，立即执行一次签到
                        viewModel.performAutoSignIn { success, message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                }*/
            },
            modifier = Modifier.fillMaxWidth()
        )        
        
        Spacer(modifier = Modifier.height(24.dp))        
        
    }
}