# APMng
Xposed 应用唤醒，Alarm,Wakelocks限制。应用后台活动限制。
<pre>
系统应用自动白名单（白名单检测函数isWhitelist）
关闭屏幕时立刻进入Doze模式(函数FastDoze)
禁止Provider或Broadcast,receiver形式启动应用。（函数RecvHook)
禁止后台应用使用revciver（函数RecvHook)
应用切换到后台自动冻结(FreezeTask)
禁止jobscheduler（被某些sdk用来应用保活自启(NoJobs)
应用从recent tasks中划出后就强行停止(HookTask)
Alarm,Wakelocks严格白名单(Syshook)
</pre>
# 应用兼容性测试：
基于安卓7.1.2API编写。<br>
无崩溃应用：QQ，知乎，微博极速版，网易云音乐（歌词有时不显示，需要在通知栏重新打开），bilibili（后台播放支持）,Root explorer（后台操作支持）<br>
闹钟：正常
