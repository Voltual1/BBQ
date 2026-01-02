// 文件路径: cc/bbq/xq/util/FileActionUtil.kt
package cc.bbq.xq.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object FileActionUtil {

    /**
     * 打开文件
     * 自动识别文件类型并调用系统对应的应用打开
     * 如果是APK文件，则执行安装操作
     *
     * @param context 上下文
     * @param file 要打开的文件
     */
    fun openFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent: Intent
            
            // 获取文件的 MIME 类型
            val mimeType = getMimeType(file)
            
            // 检查是否是APK文件
            val isApkFile = file.extension.equals("apk", ignoreCase = true) || 
                           mimeType == "application/vnd.android.package-archive"
            
            if (isApkFile) {
                // APK文件：使用安装意图
                intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
                
                // Android 7.0+ 需要添加此标志
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                // 其他文件：使用查看意图
                intent = Intent(Intent.ACTION_VIEW)
            }
            
            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 需要使用 FileProvider
                val authority = "${context.packageName}.provider"
                uri = FileProvider.getUriForFile(context, authority, file)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(file)
            }

            // 设置Intent的数据和类型
            intent.setDataAndType(uri, mimeType)
            
            // 如果是安装APK，设置安装后是否返回结果
            if (isApkFile && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 如果是APK安装，需要在Android 8.0+处理未知来源安装权限
            if (isApkFile && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    // 没有权限，提示用户去设置
                    Toast.makeText(context, "需要允许安装来自此来源的应用", Toast.LENGTH_LONG).show()
                    // 可以在这里跳转到设置页面
                    val intentSettings = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    intentSettings.data = Uri.parse("package:${context.packageName}")
                    intentSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intentSettings)
                    return
                }
            }

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到可打开此文件的应用", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(context, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * 获取文件的 MIME 类型
     */
    private fun getMimeType(file: File): String {
        val extension = getExtension(file.name)
        if (extension.isNotEmpty()) {
            // 特殊处理APK文件
            if (extension.equals("apk", ignoreCase = true)) {
                return "application/vnd.android.package-archive"
            }
            
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
            if (mime != null) {
                return mime
            }
        }
        return "*/*" // 未知类型
    }

    /**
     * 获取文件扩展名
     */
    private fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1)
        } else {
            ""
        }
    }
    
    /**
     * 检查是否有安装APK的权限（针对Android 8.0+）
     */
    fun checkInstallPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // Android 8.0以下不需要此权限
        }
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format(Locale.getDefault(), "%.2f GB", size.toFloat() / gb)
            size >= mb -> String.format(Locale.getDefault(), "%.2f MB", size.toFloat() / mb)
            size >= kb -> String.format(Locale.getDefault(), "%.2f KB", size.toFloat() / kb)
            else -> "$size B"
        }
    }
}