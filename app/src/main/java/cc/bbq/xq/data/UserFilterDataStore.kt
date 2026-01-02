//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import kotlinx.coroutines.flow.map

// DataStore 实例
val Context.userFilterDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_filter")

/**
 * 用于存储用户筛选信息的 DataStore
 */
@Single
class UserFilterDataStore(private val context: Context) {
    
    companion object {
        private val FILTER_USER_ID = longPreferencesKey("filter_user_id")
        private val FILTER_NICKNAME = stringPreferencesKey("filter_nickname")
        private val IS_FILTER_ACTIVE = booleanPreferencesKey("is_filter_active")
    }
    
    /**
     * 设置用户筛选信息
     */
    suspend fun setUserFilter(userId: Long, nickname: String) {
        context.userFilterDataStore.edit { preferences ->
            preferences[FILTER_USER_ID] = userId
            preferences[FILTER_NICKNAME] = nickname
            preferences[IS_FILTER_ACTIVE] = true
        }
    }
    
    /**
     * 清除用户筛选信息
     */
    suspend fun clearUserFilter() {
        context.userFilterDataStore.edit { preferences ->
            preferences.remove(FILTER_USER_ID)
            preferences.remove(FILTER_NICKNAME)
            preferences[IS_FILTER_ACTIVE] = false
        }
    }
    
    /**
     * 获取当前筛选的用户ID
     */
    val userIdFlow: Flow<Long?> = context.userFilterDataStore.data
        .map { preferences ->
            preferences[FILTER_USER_ID]
        }
    
    /**
     * 获取当前筛选的用户昵称
     */
    val nicknameFlow: Flow<String?> = context.userFilterDataStore.data
        .map { preferences ->
            preferences[FILTER_NICKNAME]
        }
    
    /**
     * 获取筛选是否激活
     */
    val isFilterActiveFlow: Flow<Boolean> = context.userFilterDataStore.data
        .map { preferences ->
            preferences[IS_FILTER_ACTIVE] ?: false
        }
    
    /**
     * 获取完整的筛选信息
     */
    val userFilterFlow: Flow<Pair<Long?, String?>> = context.userFilterDataStore.data
        .map { preferences ->
            val userId = preferences[FILTER_USER_ID]
            val nickname = preferences[FILTER_NICKNAME]
            Pair(userId, nickname)
        }
}