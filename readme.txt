C2CFastPay Android 應用程式

 一、專案簡介
本專案為一 Android 行動應用程式，主要提供使用者進行 C2C 快速付款與交易相關操作。  
系統採用 Kotlin 與 Jetpack Compose 進行開發，並整合 Firebase Authentication 作為使用者登入與電子郵件驗證機制。

本專案為課程專題成果，供指導教授進行系統驗收與功能測試使用。

---

 二、開發與執行環境

（一）開發環境
- 作業系統：Windows
- 開發IDE：Android Studio
- 程式語言：Kotlin
- UI Framework：Jetpack Compose
- 建置工具：Gradle
- 雲端服務：Firebase Authentication、DataBase、Storage

（二）系統執行環境
- 裝置類型：Android 智慧型手機
- 作業系統：
　　最低支援版本：	Android 8.0（API Level 26）
　　目標版本：		Android 16 （API Level 36）
　　建議版本：		Android 12 （API Level 31）

- 硬體要求
　　CPU：最低四核心 1.5GHz 以上處理器
　　RAM：最低3 GB
　　儲存空間：最低200MB以上
　　其餘硬體設備：相機鏡頭、網路模組
---

三、專案目錄結構說明

C2CFastPay/
├─ app/ Android 應用程式原始碼
├─ docs/ 系統文件（系統說明書、使用者手冊等）
├─ gradle/ Gradle 設定檔
├─ build.gradle(.kts) 專案建置設定
├─ settings.gradle(.kts)
└─ README.md 本說明文件

---

四、安裝與執行步驟

（一）、安裝開發工具
1. 安裝 Android Studio
2. 確認已安裝 JDK（Android Studio 內建即可）
3. 確認可正常連線至網際網路

---

（二）、開啟專案
1. 啟動 Android Studio
2. 選擇「Open」
3. 選取本專案資料夾
4. 等待 Gradle 同步完成

---

（三）、Firebase 設定說明
本系統使用 Firebase Authentication 作為登入與驗證機制。

- Firebase 專案已事先建立
- 如需重新設定，請於 Firebase Console 建立專案並下載 `google-services.json`
- 將該檔案放置於 `app/` 目錄底下

---

（四）、執行專案
1. 連接 Android 實體裝置或啟動 Android Emulator
2. 點擊 Android Studio 上方「Run」
3. 等待系統編譯並安裝完成

---

---
 五、系統功能測試方式

1. 啟動 App
2. 進入登入畫面
3. 使用測試帳號登入
4. 驗證登入成功後是否正確導向主功能頁面
5. 測試系統各功能畫面是否可正常操作

---

六、注意事項
- 本專案需於有網際網路環境下執行
- 若無法登入，請確認 Firebase 服務設定是否正確
- 若發生建置錯誤，請確認 Gradle 同步是否完成

---

七、授權說明
本專案為課程專題成果，僅供教學、展示與學術用途使用，未作任何商業用途。
