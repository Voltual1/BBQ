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
                    // TODO: 如果开启自动签到，立即执行一次签到
                }
            },
            modifier = Modifier.fillMaxWidth()
        )        
        
        Spacer(modifier = Modifier.height(24.dp))        
        
    }
}