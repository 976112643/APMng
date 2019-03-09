package org.cpera.apmng;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalStaticField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalStaticField;
import de.robv.android.xposed.XC_MethodHook;
import android.os.SystemClock;
import android.app.AndroidAppHelper;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.*;
import android.app.PendingIntent;
import android.util.SparseArray;

import android.app.AlarmManager;

public class HOOK implements IXposedHookLoadPackage {
    boolean hook=false;
    Object mPE=null;
    DataOutputStream pos;
    private boolean check(String a,String b) {
        return a.toLowerCase().contains(b);
    }
    private boolean check2(String a,String b) {
        String[] kk=a.split(",");
        for(String sk:kk) {
            if(check(b,sk)) return true;
        }
        return false;
    }

    private String safe(Object fk) {
        return fk==null?"nil":fk.toString();
    }
    private  String dumparr(Object[] sb) {
        String res="";
        for (Object a:sb) {
            res+=safe(a)+"\n";
        }
        return res;
    }
    private boolean Lock(String fk) {
        return  check(fk,"*job*") || check(fk,"deviceidle_maint") || check(fk,"syncloopwakelock") || check(fk,"location")|| check(fk,"anym")|| check(fk,"networkstats") || check(fk,"dream") || check(fk,"connect");
    }
    private  boolean BLOCKSERV(String  fk) {
        return check2("push,xmsf,dreamservice,miuiwifi,umeng,systemad,miui.daemon,contentext,miaccount,miuilog,syncjob,printsp,wmsvc,ping,juphoon,simacti,.keepalive,xspace,miuivpn",fk);
    }
    private boolean BLOCKALARM(String fk) {
        return check2("trigger_idle,check_time_up,eval,network_stats,*job,",fk);
    }
    private void log(String fk) throws Throwable {
        if(true) return;
        fk+="\n";
        FileOutputStream outputStream = new FileOutputStream("/data/atlog.log",true);
        outputStream.write(fk.getBytes());
        outputStream.close();
    }
    private boolean NoWakeUPIntent(String fk) {
        return check2("sig_str,dropbox,net.wifi,screen,battery,power,usb_state,package_",fk);
    }
    private boolean NoFreeze(String pn) {
        return false;
    }
    private void SigPKG(Object thi,String name,String opt)throws Throwable {
        try {
            //Object thi=com.android.server.am.ActivityManagerService
            final int NP = (int)callMethod(callMethod(getObjectField(thi,"mProcessNames"),"getMap"),"size");
            for (int ip=0; ip<NP; ip++) {
                SparseArray<Object> apps = (SparseArray<Object>)callMethod(callMethod(getObjectField(thi,"mProcessNames"),"getMap"),"valueAt",ip);
                final int NA = apps.size();
                for (int ia=0; ia<NA; ia++) {
                    Object app = apps.valueAt(ia);
                    //ProcessRecord
                    //log(safe(app));
                    String pn=safe(getObjectField(app,"processName"));
                    if(pn.contains(name)) {
                        if(((int)getObjectField(getObjectField(app,"info"),"flags")&1)!=0) {
                            //skip sys app
                            continue;
                        }
                        if(NoFreeze(pn)) continue;
                        log("kill -"+pn);
                        pos.write(("kill -"+opt+" "+pn+"\n").getBytes());
                        pos.flush();
                    }
                }
            }

        } catch(Exception e) {
            log(e.toString());
        }
    }
    Object Ams=null,As=null,PM=null;
    private void HookTask(final ClassLoader clo) {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "cleanUpRemovedTaskLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(param.args[1].equals(true) && param.args[2].equals(true)) {
                    Object tr=param.args[0];
                    if(tr!=null) {
                        //kill it
                        String pkg=safe(callMethod(callMethod(callMethod(tr,"getBaseIntent"),"getComponent"),"getPackageName"));
                        // log("killing "+pkg);
                        callMethod(param.thisObject,"forceStopPackageLocked",pkg,-1,false,false,true,false,false,0,"AM");
                    }
                }
            }
        }
                                   );
    }
    private void HelperHook(final ClassLoader clo) {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.DeviceIdleController",
        clo), "onBootPhase", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent fk=new Intent("Intent.ACTION_TIME_TICK_DUMMY");
                fk.setPackage("com.dummy");
                mPE=PendingIntent.getBroadcast((Context)callMethod(param.thisObject,"getContext"),0,fk,0);
            }
        });
        XposedBridge.hookAllConstructors(XposedHelpers.findClass(
                                             "com.android.server.AlarmManagerService",
        clo), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                As=param.thisObject;
            }
        });
        XposedBridge.hookAllConstructors(XposedHelpers.findClass(
                                             "com.android.server.am.ActivityManagerService",
        clo), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Ams=param.thisObject;
            }
        });
        XposedBridge.hookAllConstructors(XposedHelpers.findClass(
                                             "com.android.server.pm.PackageManagerService",
        clo), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                PM=param.thisObject;
            }
        });
    }
    private void FastDoze(final ClassLoader clo) {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.power.PowerManagerService",
        clo), "wakeUpNoUpdateLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("---WAKE UP ---  ");
                //   callMethod(param.thisObject,"setDeviceIdleModeInternal",false);
                callMethod(As,"removeLocked",mPE,null);
                callMethod(As,"restorePendingWhileIdleAlarmsLocked");
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.power.PowerManagerService",
        clo), "goToSleepNoUpdateLocked"/*"goToSleepInternal"*/, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                callMethod(As,"setImpl",3, SystemClock.elapsedRealtime()+48*60*60*1000, 0, 0,mPE,null,"apmng.deep",1<<4,null,null,android.os. Process.myUid(), "android");
            }
        }
                                   );
    }
    private void NoJobs(final ClassLoader clo) {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.job.JobSchedulerService",
        clo), "scheduleAsPackage", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(1);
            }
        }
                                   );
    }
    private Set<Integer> FreezeList=new HashSet<>();
    private void Freeze(int pid)throws Throwable {
        if(FreezeList.contains(pid)) return;
        FreezeList.add(pid);
        pos.write(("kill -STOP "+safe(pid)+"\n").getBytes());
        pos.flush();
    }
    private void UnFreeze(int pid)throws Throwable {
        if(!FreezeList.contains(pid)) return;
        FreezeList.remove(pid);
        pos.write(("kill -CONT "+safe(pid)+"\n").getBytes());
        pos.flush();
    }
    private boolean isAppForeground(String pn) {
        try {
            if(pn.equals("android")) return true;
            Object inf=callMethod(PM,"getPackageInfo",pn,0,0);
            if(inf==null) {
                log("wtf?! "+pn);
                return false;
            }
            Object uid=getObjectField(getObjectField(inf,"applicationInfo"),"uid");
            Object act=getObjectField(Ams,"mActiveUids");
            Object rec=callMethod(act,"get",uid);
            if(rec==null || (boolean)getObjectField(rec,"idle")) return false;
            int sta=(int)getObjectField(rec,"curProcState");
            /*PROCESS_STATE_BOUND_FOREGROUND_SERVICE	3	1) instrumentation正在运行
            2) 绑定了前台Service
            3) 绑定的Service进程为前台
            4) 使用的provider进程为前台
            PROCESS_STATE_FOREGROUND_SERVICE	4	正在运行前台Service
            PROCESS_STATE_TOP_SLEEPING	5	进入睡眠状态
            PROCESS_STATE_IMPORTANT_FOREGROUND	6	Notification为Service设置了前台token
            */
            return sta<=4;
        }
        catch(Throwable e) {
            return false;
        }
    }
    private boolean isAppWhitelist(String pn) {
        try {
            if(pn.equals("android")) return true;
            Object inf=callMethod(PM,"getPackageInfo",pn,0,0);
            if(inf==null) {
                log("wtf?! "+pn);
                return false;
            }
            if((boolean)callMethod(getObjectField(inf,"applicationInfo"),"isSystemApp"))return true;
            if(isAppForeground(pn)) return true;
            return false;
        }
        catch(Throwable e) {
            return false;
        }
    }
    private void RecvHook(final ClassLoader clo) throws Throwable {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.pm.PackageManagerService",
        clo), "queryIntentServicesInternal", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<Object> ll=(List<Object>)param.getResult();
                List<Object> rt=new ArrayList<>();
                for(Object i:ll) {
                    String pn=safe(getObjectField(getObjectField(callMethod(i,"getComponentInfo"),"applicationInfo"),"packageName"));

                    if(isAppWhitelist(pn)) {
                        //log("recv static "+pn);
                        rt.add(i);
                    }
                }
                param.setResult(rt);
            }
        }
                                   );
        //mReceiverResolver.queryIntent
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.IntentResolver",
        clo), "queryIntent", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<Object> ll=(List<Object>)param.getResult();
                if(ll.size()==0) return;
                String type=ll.get(0).getClass().getSimpleName();
                if(!type.equals("BroadcastFilter")) return;
                List<Object> rt=new ArrayList<>();
                //log("start dump");
                for(Object i:ll) {
                    //log(safe(i));
                    String pn=safe(getObjectField(i,"packageName"));
                    if(isAppWhitelist(pn)) {
                        //  log("name "+pn);
                        rt.add(i);
                    }
                }
                //log("stop dump");
                param.setResult(rt);
            }
        }
                                   );
        /*private final void startProcessLocked(ProcessRecord app, String hostingType,
          String hostingNameStr, String abiOverride, String entryPoint, String[] entryPointArgs)*/
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "startProcessLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(param.args.length!=6) return;
                if(param.args[0]==null) return;
                if(!param.args[0].getClass().getSimpleName().equals("ProcessRecord")) return;
                //log("app "+safe(param.args[0])+" type "+param.args[1]);
                String pn=safe(getObjectField(getObjectField(param.args[0],"info"),"packageName"));
                if(isAppWhitelist(pn) || isAppForeground(pn)) return;
                String type=safe(param.args[1]);
                if(type.equals("broadcast") || type.equals("content provider")) {
                    //log("app "+safe(param.args[0])+" type "+param.args[1]);
                    param.setResult(null);
                }
                //broadcast
                //content provider
            }
        });
    }
    private void FreezeTask(final ClassLoader clo) throws Throwable {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "applyOomAdjLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //log(dumparr(param.args));
                Object app=param.args[0];
                String type="user";
                if(((int)getObjectField(getObjectField(app,"info"),"flags")&1)!=0)
                {
                    type="sys";
                    return;
                }
                //String pn=safe(getObjectField(app,"processName"));
                int pid=(int)getObjectField(app,"pid");
                int adj=(int)(getObjectField(app,"curAdj"));
                int uid=(int)(getObjectField(app,"uid"));
                //if(adj>=900) return;
                //if((boolean)callMethod(Ams,"isAppForeground",uid)) {UnFreeze(pid);return;}
                String pn=safe(getObjectField(getObjectField(app,"info"),"packageName"));
                if(isAppForeground(pn)) {
                    UnFreeze(pid);
                    return;
                }
                if(adj>200/*freeze not vis or not cached*/) {
                    Freeze(pid);
                } else {
                    UnFreeze(pid);
                }
            }
        });
    }
    private void SysHook(final ClassLoader clo) throws Throwable {
        HelperHook(clo);
        HookTask(clo);
        NoJobs(clo);
        FastDoze(clo);
        RecvHook(clo);
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.power.PowerManagerService",
        clo), "acquireWakeLockInternal", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(param.args[3].equals("audioserver")) return;
                if(param.args[3].toString().contains("clock")) return;
                if(param.args[3].equals("com.android.phone")) return;
                if(param.args[3].equals("android")) {
                    if(Lock(safe(param.args[2]))) {
                        param.setResult(null);
                    }
                } else {
                    param.setResult(null);
                }
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.AlarmManagerService",
        clo), "setImpl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String Tag="";
                String pn=safe(param.args[11]).toLowerCase();
                if(param.args[6]==null) {
                    if(param.args[4]==null) {
                        param.setResult(null);
                        return;
                    }
                    Tag=(String)callMethod(param.args[4],"getTag","");
                } else {
                    Tag=(String)param.args[6];
                }
                if(Tag.equals("apmng.deep")) return;
                Tag=Tag.toLowerCase();
                if(pn.contains("clock") && (!Tag.contains("quarter_hour"))) {
                    //wake from idle
                    int flags=(int)param.args[7];
                    flags|=1<<3;//AlarmManager.FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED;
                    param.args[7]=flags;
                    return;
                }
                if(Tag.contains("wifi") || Tag.contains("wlan")) {
                    param.args[0]=3;
                    int flags=(int)param.args[7];
                    flags&=~(1<<3);//AlarmManager.FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED;
                    param.args[7]=flags;
                    return;
                }
                if(pn.equals("android")) {
                    if(BLOCKALARM(Tag)) {
                        param.setResult(null);
                        return;
                    }
                } else {
                    param.setResult(null);
                    return;
                }
            }
        }
                                   );
    }
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        log("Loaded app: " + lpparam.processName);
        if(lpparam.processName.equals("android") && !hook) {
            try {
                Process pp=Runtime.getRuntime().exec("su");
                pos = new DataOutputStream(pp.getOutputStream());
                pos.write("bash\n".getBytes());
                pos.flush();
                SysHook(lpparam.classLoader);
            } catch(Exception e) {
                log(e.toString());
            }
            hook=true;
        }
    }
}
