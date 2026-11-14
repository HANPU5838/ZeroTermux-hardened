# ZeroTermux 加固版（ZeroTermux Hardened）

> **第三方衍生构建声明**：本项目不是官方发布。上游原作者为 [hanxinhao000/ZeroTermux](https://github.com/hanxinhao000/ZeroTermux)；本项目仅在其源码基础上做构建工具链现代化、依赖升级、X11 集成与打包整理，不改变项目归属。
> 许可证：**GPLv3 only**（含例外条款，见 [LICENSE.md](LICENSE.md)）。
> 维护者主页：**<https://hanpuhan.cn>**

---

## 📦 本项目的三个软件

本仓库在 [Releases](https://github.com/HANPU5838/ZeroTermux-hardened/releases) 中提供 **3 个可独立下载、独立安装的 App**，三者签名一致（同一 `phone.jks`），可互相覆盖升级，数据不丢：

| # | 软件 | 包名 | 作用 | 是否必装 |
| --- | --- | --- | --- | --- |
| ① | **ZeroTermux 加固版**（主程序） | `com.termux` | 完整终端环境 | ✅ 必装 |
| ② | **Termux:API**（配套组件） | `com.termux.api` | 让终端调用手机 API（剪贴板、传感器、通知等） | 按需 |
| ③ | **Termux:X11**（外部通道） | `com.termux.x11` | 独立 X11 显示 App（外部通道时使用） | 按需 |

> 只装 ① 就能正常用终端；②③ 是扩展组件，按需单独下载即可。

---

## ① ZeroTermux 加固版（主程序）

**是什么**：基于上游 ZeroTermux 源码的第三方构建，终端 + Linux 环境主体。

| 项 | 值 |
| --- | --- |
| 当前版本 | **0.118.3.70**（versionCode 309） |
| 安装包 | [`ZeroTermux-0.118.3.70-release_arm64-v8a.apk`](https://github.com/HANPU5838/ZeroTermux-hardened/releases/download/v0.118.3.70/ZeroTermux-0.118.3.70-release_arm64-v8a.apk) |
| 大小 | 80,792,182 字节（≈77.0 MiB） |
| ABI | 仅 `arm64-v8a` |
| SHA-256 | `a3b1d4f24a4a663bf1d99b8a596c93171329685ac8cf0bec92c58c4749cdf9e7` |
| 系统要求 | Android 7.0+（minSdk 23），targetSdk 28 |

**本版更新**（完整记录见 [`升级说明-0.118.3.70.md`](升级说明-0.118.3.70.md)）：

- 修复 X11 内部通道 `loader.apk` 反复提示「删除失败」的问题
- 并入 `termux-x11`（zt 分支）源码，CI 从源码构建 X11 AAR 与内部通道 loader
- X11 设置界面中文化
- 构建工具链升级：AGP 9.5.1 / Gradle 9.9.1 / compileSdk 37

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
| 安装包 | [`Termux-API-0.53.0-zero.14.apk`](https://github.com/HANPU5838/ZeroTermux-hardened/releases/download/v0.118.3.70/Termux-API-0.53.0-zero.14.apk) |
| 大小 | 7,292,589 字节（≈7.0 MiB） |
| ABI | 全架构（无原生库） |
| SHA-256 | `ce7c7a2455737fade160622a8ecbfa73417e2371e3f7e28cb07497f3452ec12c` |

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
| 安装包 | [`Termux-X11-universal.apk`](https://github.com/HANPU5838/ZeroTermux-hardened/releases/download/v0.118.3.70/Termux-X11-universal.apk) |
| 大小 | 13,985,846 字节（≈13.3 MiB） |
| ABI | 全架构 universal（arm64 / arm / x86 / x86_64） |
| SHA-256 | `f20d041b7c9bb37fbc44742dcdce2848edf073228291871b49351d250f4d634b` |
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
| 本仓库（加固版源码） | <https://github.com/HANPU5838/ZeroTermux-hardened> |
| 下载页（Releases） | <https://github.com/HANPU5838/ZeroTermux-hardened/releases> |
| 上游原项目（原作者） | <https://github.com/hanxinhao000/ZeroTermux> |
| Termux:X11 上游 | <https://github.com/termux/termux-x11> |
| Termux:X11（zt 分支，内部通道 AAR 来源） | <https://github.com/XiaoTong6666/termux-x11> |

---

## 🛠 构建与发布（开发向）

三个软件都由 GitHub Actions 构建（`.github/workflows/build.yml`）：

| 软件 | 构建方式 |
| --- | --- |
| ① 主程序 | `./gradlew :app:assembleRelease`（仓库根） |
| ② Termux:API | 版本固定，源码不在本仓库；安装包存于 `apk/`，CI 直接带入发布产物 |
| ③ Termux:X11 | `x11/termux-x11-standalone/`（上游 master 源码 + 预编译 `libXlorie.so`） |

**CI 流程**：Checkout → JDK 21 → Android SDK → 构建内部通道 X11 AAR/loader 并注入主程序 → 构建独立 Termux:X11 APK → 构建主 APK → 收集三个 APK + `SHA256SUMS.txt`；推送 `v*` tag 时自动挂到 Release。

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
