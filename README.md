# ZeroTermux 加固版（ZeroTermux Hardened）

> **第三方衍生构建声明**：本项目不是官方发布。上游原作者为 [hanxinhao000/ZeroTermux](https://github.com/hanxinhao000/ZeroTermux)；本项目仅在其源码基础上做构建工具链现代化、依赖升级、X11 集成与打包整理，不改变项目归属。
> 许可证：**GPLv3 only**（含例外条款，见 [LICENSE.md](LICENSE.md)）。
> 维护者主页：**<https://hanpuhan.cn>**

## 联系方式

| 平台 | 账号 | 名称 |
| --- | --- | --- |
| 🌐 网站 | [hanpuhan.cn](https://hanpuhan.cn) | 清崖文存 |
| ✉️ 邮箱 | [640615397@qq.com](mailto:640615397@qq.com) | — |
| 📱 快手 | QQ640615397 | 涵清 |
| 💬 QQ 群 | 9591166552 | 清崖文存 |

---

---

## 📦 本项目的三个软件

三个软件分别在**各自的正式版仓库**发布，签名一致（同一 `phone.jks`），可互相覆盖升级，数据不丢。主程序内置「检查更新」，会同时查询这三个仓库的最新 Release 并可一键下载安装：

| # | 软件 | 包名 | 正式版仓库 | 是否必装 |
| --- | --- | --- | --- | --- |
| ① | **ZeroTermux 加固版**（主程序） | `com.termux` | [ZeroTermux-hardened](https://github.com/HANPU5838/ZeroTermux-hardened/releases) | ✅ 必装 |
| ② | **Termux:API**（配套组件） | `com.termux.api` | [ZeroTermux-API](https://github.com/HANPU5838/ZeroTermux-API/releases) | 按需 |
| ③ | **Termux:X11**（外部通道） | `com.termux.x11` | [ZeroTermux-X11](https://github.com/HANPU5838/ZeroTermux-X11/releases) | 按需 |

> 只装 ① 就能正常用终端；②③ 是扩展组件，按需单独下载即可。各仓库的 **Releases 只保留当前正式版**，版本号、大小与 SHA-256 以 Release 页面为准。

---

## ① ZeroTermux 加固版（主程序）

**是什么**：基于上游 ZeroTermux 源码的第三方构建，终端 + Linux 环境主体。

| 项 | 值 |
| --- | --- |
| 当前正式版 | 见 [Releases 页面](https://github.com/HANPU5838/ZeroTermux-hardened/releases)（版本号、大小、SHA-256 均在 Release 里） |
| ABI | `arm64-v8a`（CI 默认） |
| 系统要求 | Android 7.0+（minSdk 23），targetSdk 28 |
| 更新检测 | 主程序 → 版本号旁「有新版本」，自动比对三个仓库的最新 Release |

**本版更新**：

- 新增**正式版一键更新**：检测三个仓库最新版本、下载 APK 并唤起安装；下载完成后按钮变为「安装」，唤起失败可手动重试且不重复下载
- 更新入口从菜单底部移到**版本号旁的红字「有新版本」**，检测到更新才出现
- 「支持作者」从底部整宽卡片改为**常用功能菜单项**，与其它菜单同款网格样式
- 常用功能里「DeepSeek Harness」改为「官网」，点击跳转系统浏览器打开 `hanpuhan.cn`
- 关于页与所有仓库链接统一指向本加固版仓库，保留上游原作者与 GPLv3 声明
- INFO 卡片加入官方群聊与个人 QQ；「支持作者」改为常用功能里的菜单项（扫码赞赏弹窗）
- 左侧菜单默认展开分组，移除设置页里多余的「菜单设置」入口

**安装**：

1. 允许浏览器/文件管理器「安装未知来源应用」；
2. 直接安装 APK（若已装旧版本可覆盖升级，数据不丢）；
3. 首次启动会解压 bootstrap，请耐心等待，**不要中途杀进程**。

> 若已装官方 Termux 且签名不同，需先卸载官方版（数据会丢，请先备份 `~`）。

---

## ② Termux:API（配套组件）

**是什么**：Termux 生态的 API 桥接 App，让终端命令（`termux-clipboard-set`、`termux-battery-status`、`termux-notification` 等）能调用 Android API。**不是必装**，用到相关命令时再装。

| 项 | 值 |
| --- | --- |
| 版本 | **0.53.0-zero.14**（versionCode 1016） |
| 正式版下载 | [ZeroTermux-API Releases](https://github.com/HANPU5838/ZeroTermux-API/releases) |
| ABI | 全架构（无原生库） |

**安装**：

1. 先装好 ① ZeroTermux 主程序；
2. 再安装本 APK（签名一致，可共存/升级）；
3. 如需要无障碍相关能力，到系统「设置 → 无障碍」开启 `Termux:API` 服务。

---

## ③ Termux:X11（外部通道 X11）

**是什么**：独立的 X11 显示 App。ZeroTermux 的 X11 有两条通道：

- **内部通道**（默认）：X11 直接显示在 ZeroTermux 里，**无需安装本软件**；
- **外部通道**：使用本独立 App 显示 X11 画面，适合多显示器/独立窗口场景。

| 项 | 值 |
| --- | --- |
| 版本 | **1.03.01-fa6d456-14.11.26**（versionCode 17） |
| 源码基线 | [termux/termux-x11](https://github.com/termux/termux-x11) master @ `488dbbd`（与官方 nightly 同 commit） |
| 正式版下载 | [ZeroTermux-X11 Releases](https://github.com/HANPU5838/ZeroTermux-X11/releases) |
| ABI | 全架构 universal（arm64 / arm / x86 / x86_64） |
| 界面语言 | **中文**（本地化自上游英文文案） |

**使用（外部通道）**：

1. 安装本 APK；
2. 打开 ZeroTermux → 左上角菜单 →「X11 设置」→ 选择「外部通道」（会自动安装通道文件）；
3. 在终端里启动 X 服务，例如 `termux-x11 :0`；
4. 打开 Termux:X11 App，即可看到 X11 画面。

> 内部通道用户不需要安装本软件：直接在「X11 设置」里选「内部通道」即可。

---

## 🔗 相关链接

| 用途 | 地址 |
| --- | --- |
| 维护者主页 | **<https://hanpuhan.cn>** |
| 本仓库（主程序正式版） | <https://github.com/HANPU5838/ZeroTermux-hardened/releases> |
| Termux:API 正式版 | <https://github.com/HANPU5838/ZeroTermux-API/releases> |
| Termux:X11 正式版 | <https://github.com/HANPU5838/ZeroTermux-X11/releases> |
| 上游原项目（原作者） | <https://github.com/hanxinhao000/ZeroTermux> |
| Termux:X11 上游 | <https://github.com/termux/termux-x11> |
| Termux:X11（zt 分支，内部通道 AAR 来源） | <https://github.com/XiaoTong6666/termux-x11> |

---

## 🛠 构建与发布（开发向）

主程序由 GitHub Actions 构建（`.github/workflows/build.yml`）；两个配套组件在各自仓库发布正式版 APK，源码暂不迁入。

| 软件 | 构建方式 |
| --- | --- |
| ① 主程序 | `./gradlew :app:assembleRelease`（仓库根） |
| ② Termux:API | 版本固定，正式版 APK 存放于 [ZeroTermux-API](https://github.com/HANPU5838/ZeroTermux-API) |
| ③ Termux:X11 | `x11/termux-x11-standalone/` 构建，正式版 APK 存放于 [ZeroTermux-X11](https://github.com/HANPU5838/ZeroTermux-X11) |

**CI 流程**：Checkout → JDK 21 → Android SDK → 构建内部通道 X11 AAR/loader 并注入主程序 → 构建主 APK → 收集产物与 `SHA256SUMS.txt`。**主仓库的正式版 Release 只放主程序 APK**，配套组件在各自仓库发布；三个仓库的 Release 均只保留当前正式版。

```bash
gh workflow run build.yml -R HANPU5838/ZeroTermux-hardened -f arch=arm64
```

**本机构建**：

```bash
# 需要 JDK 21 + Android SDK（compileSdk 37）+ NDK r30

# 1. 内部通道 X11（AAR + loader）
(cd x11/termux-x11 && ./gradlew :app:assembleDebug :shell-loader:assembleDebug)
cp x11/termux-x11/app/build/outputs/aar/app-debug.aar app/libs/termux-x11.aar
cp x11/termux-x11/shell-loader/build/outputs/apk/debug/shell-loader-debug-unsigned.apk \
   app/src/main/assets/x11/aisle_zt_loader.apk

# 2. 独立 Termux:X11（外部通道）
(cd x11/termux-x11-standalone && ./gradlew :lorie-app:assembleDebug)

# 3. 主程序
gradle :app:assembleRelease -Darch=arm64
```

签名配置见 `app/build.gradle.kts`（使用仓库根 `phone.jks`，与上游同一证书；可用环境变量 `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_PASSWORD` 覆盖）。

### 源码目录

```text
app/  termux-shared/  terminal-emulator/  terminal-view/   # ① 主程序
apk/                                                      # ② Termux:API 安装包
x11/termux-x11/                                           # ③ 内部通道 AAR/loader 源码（zt 分支）
x11/termux-x11-standalone/                                # ③ 外部通道独立 APK 源码（上游 master）
```

---

## 📜 许可证与声明

- 本项目遵循上游 **GPLv3 only** 许可证及例外条款，见 [LICENSE.md](LICENSE.md)。
- `terminal-emulator` / `terminal-view` 模块另含 Apache-2.0 代码。
- `x11/termux-x11*` 目录下代码来自 [termux/termux-x11](https://github.com/termux/termux-x11)（GPL-3.0）及其衍生分支，版权归各自作者所有。
- 本项目为**非官方**第三方构建，与原作者无隶属关系；使用风险自负。
- 本项目只做构建与整合，**未新增业务功能**；数据收集情况请以源码为准自行审阅。
- 请勿将本项目安装包的问题反馈给上游作者。
- 若上游作者认为本仓库有不妥之处，可提出，会立即配合调整或下架。
