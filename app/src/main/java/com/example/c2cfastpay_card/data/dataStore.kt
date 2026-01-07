package com.example.c2cfastpay_card.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// 這一行程式碼會建立一個 dataStore 實例
// 並將它作為一個擴充屬性 "dataStore" 附加到 Context 上
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "c2c_settings")