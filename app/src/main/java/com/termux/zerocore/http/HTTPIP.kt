package com.termux.zerocore.http;

object HTTPIP {
    public const val IP = "https://od.ixcmstudio.cn"
    //还得脚本位置
    public const val QEMU_HAI = "$IP/"
    //论坛
    public const val ZERO_BBS = "https://termbbs.ixcm.org/"

    // ===== 正式版更新渠道（三个仓库各自的最新 Release）=====
    public const val REPO_MAIN = "HANPU5838/ZeroTermux-hardened"
    public const val REPO_API = "HANPU5838/ZeroTermux-API"
    public const val REPO_X11 = "HANPU5838/ZeroTermux-X11"

    public const val GITHUB_VERSION = "https://api.github.com/repos/$REPO_MAIN/releases/latest"
    public const val GITHUB_VERSION_API = "https://api.github.com/repos/$REPO_API/releases/latest"
    public const val GITHUB_VERSION_X11 = "https://api.github.com/repos/$REPO_X11/releases/latest"

    // 左侧菜单包网络更新地址
    public const val MENU_PACKAGE_URL = "$IP/repository/main/menu/menu_latest.zip"
}
