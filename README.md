# APMng
Xposed 应用唤醒，Alarm限制。应用后台活动限制。
<pre>
关闭屏幕时立刻进入Doze模式（可能导致部分闹钟问题)
应用从最近任务划出就杀死。
应用切换到后台就停止活动。
禁止wifi相关Alarm唤醒手机。
禁止所有用户Wakelocks,Alarm.
禁止应用broadcast链式唤醒。
禁止某些服务启动
禁止国内应用用来自启的jobscheduler
</pre>
