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
            //XposedBridge.log("check "+b+"  "+sk);
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
    String LastF="nil";
    private void SigPKG(Object thi,String name,String opt)throws Throwable {
        //TODO: check name
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
                    if(safe(getObjectField(app,"processName")).contains(name)) {
                        if(((int)getObjectField(getObjectField(app,"info"),"flags")&1)!=0) {
                            //skip sys app
                            continue;
                        }
                        if(safe(getObjectField(app,"processName")).equals("com.netease.cloudmusic") || safe(getObjectField(app,"processName")).equals("com.netease.cloudmusic:play")) continue;
                        log("kill -"+safe(getObjectField(app,"processName")));
                        pos.write(("kill -"+opt+" "+safe(getObjectField(app,"pid"))+"\n").getBytes());
                        pos.flush();
                    }
                }
            }

        } catch(Exception e) {
            log(e.toString());
        }
    }
    private void HookTask(final ClassLoader clo) {
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.wm.WindowManagerService",
        clo), "notifyFocusChanged", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object sb=callMethod(param.thisObject,"getFocusedWindow");
                String gg=callMethod(sb,"getOwningPackage").toString();
                //log("--FOCUS  "+gg+"  last"+LastF);
                if(gg.equals(LastF)) return;
                Object act=getAdditionalStaticField(findClass("com.android.server.am.ActivityManagerService",clo),"instance");
                SigPKG(act,LastF,"STOP");
                SigPKG(act,gg,"CONT");
                LastF=gg;
            }
        });
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.wm.WindowManagerService",
        clo), "notifyAppResumed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String fk=param.args[0].toString().split(" ")[3].split("/")[0];
                //log("--Resume  "+fk);
                Object act=getAdditionalStaticField(findClass("com.android.server.am.ActivityManagerService",clo),"instance");
                SigPKG(act,fk,"CONT");
            }
        });
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "cleanUpRemovedTaskLocked", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //					log("--remove "+dumparr(param.args));
                //TaskRecord tr = mStackSupervisor.anyTaskForIdLocked(
                //taskId, !RESTORE_FROM_RECENTS, INVALID_STACK_ID);

                if(param.args[1].equals(true) && param.args[2].equals(true)) {
                    Object tr=param.args[0];
                    if(tr!=null) {
                        //kill it
                        String pkg=safe(callMethod(callMethod(callMethod(tr,"getBaseIntent"),"getComponent"),"getPackageName"));
                        log("killing "+pkg);
                        callMethod(param.thisObject,"forceStopPackageLocked",pkg,-1,false,false,true,false,false,0,"AM");
                    }
                }
            }
        }
                                   );
    }
    private void SysHook(final ClassLoader clo) {
        HookTask(clo);
        /*XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "android.hardware.SystemSensorManager",
        clo), "registerListenerImpl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if((!AndroidAppHelper.currentProcessName().equals("android")) || (!dumparr(param.args).contains("stk3x1x")))
                    param.setResult(true);
            }
        }
                                   );*/
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
                                        "com.android.server.AnyMotionDetector",
        clo), "checkForAnyMotion", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.DeviceIdleController.LocalService",
        clo), "setAlarmsActive", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.notification.ScheduleConditionProvider",
        clo), "updateAlarm", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.DeviceIdleController",
        clo), "onBootPhase", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // XposedBridge.log("--Deviceidle Boot--");
                Intent fk=new Intent("Intent.ACTION_TIME_TICK");
                fk.setPackage("com.android.systemui");
                mPE=PendingIntent.getBroadcast((Context)callMethod(param.thisObject,"getContext"),0,fk,0);
                param.setResult(null);
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "startService", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("--SERV  "+safe( param.args[1]));
                if(BLOCKSERV(safe(param.args[1]))) param.setResult(new ComponentName("com.nmsl.apk","dummy"));
                else
                    XposedBridge.log("--SERV  "+safe( param.args[1]));
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "bindService", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("--SERV  "+safe( param.args[2]));
                if(BLOCKSERV(safe(param.args[2]))) param.setResult(1);
                else
                    XposedBridge.log("--SERV  "+safe( param.args[2]));
            }
        }
                                   );

        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.am.ActivityManagerService",
        clo), "broadcastIntentLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String pkg=safe(param.args[1]);
                if(pkg.contains("clock")) {
                    /*fix for alarm*/return;
                }
                Intent fk=(Intent)(param.args[2]);
                /*String dst="nope";
                if(fk.getComponent()!=null){
                	dst=fk.getComponent().getPackageName();
                	fk.getComponent().setPackageName(pkg);
                }*/
                int cuid=(int)param.args[14];
                int nuid=(int)param.args[15];
                //log("--- CAST "+pkg+" "+dst+" "+safe(cuid)+" "+safe(nuid)+" "+safe(fk));
                if(cuid==1001 || cuid==1002) return;
                if(cuid==1000) {
                    if(NoWakeUPIntent(fk.getAction())) {
                        fk.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        return;
                    } else {
                        return;
                    }
                }
                //if(!dst.equals(pkg)) {param.setResult(1);}
                fk.setPackage(pkg);
                //fk.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            }
        }
                                   );

        XposedBridge.hookAllConstructors(XposedHelpers.findClass(
                                             "com.android.server.AlarmManagerService",
        clo), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setAdditionalStaticField(param.thisObject,"instance",param.thisObject);
            }
        });
        XposedBridge.hookAllConstructors(XposedHelpers.findClass(
                                             "com.android.server.am.ActivityManagerService",
        clo), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setAdditionalStaticField(param.thisObject,"instance",param.thisObject);
            }
        });
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.power.PowerManagerService",
        clo), "wakeUpNoUpdateLocked", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("---WAKE UP ---  ");
                callMethod(param.thisObject,"setDeviceIdleModeInternal",false);
                callMethod(getAdditionalStaticField(findClass("com.android.server.AlarmManagerService",clo),"instance"),"removeLocked",mPE,null);
                callMethod(getAdditionalStaticField(findClass("com.android.server.AlarmManagerService",clo),"instance"),"restorePendingWhileIdleAlarmsLocked");
                Object act=getAdditionalStaticField(findClass("com.android.server.am.ActivityManagerService",clo),"instance");
                SigPKG(act,LastF,"CONT");
            }
        }
                                   );
        XposedBridge.hookAllMethods(XposedHelpers.findClass(
                                        "com.android.server.power.PowerManagerService",
        clo), "goToSleepNoUpdateLocked"/*"goToSleepInternal"*/, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //XposedBridge.log("---SLEEP ---  ");
                callMethod(param.thisObject,"setDeviceIdleModeInternal",true);
                callMethod(getAdditionalStaticField(findClass("com.android.server.AlarmManagerService",clo),"instance"),"setImpl",3, SystemClock.elapsedRealtime()+48*60*60*1000, 0, 0,mPE,null,"apmng.deep",1<<4,null,null,android.os. Process.myUid(), "android");
                Object act=getAdditionalStaticField(findClass("com.android.server.am.ActivityManagerService",clo),"instance");
                SigPKG(act,LastF,"STOP");
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
                    Tag=(String)callMethod(param.args[4],"getTag","");
                } else {
                    Tag=(String )param.args[6];
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
                // XposedBridge.log("--- alarm ---"+Tag);
            }
        }
                                   );
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
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        log("Loaded app: " + lpparam.processName);
        if(lpparam.processName.equals("android") && !hook) {
            try {
                Process pp=Runtime.getRuntime().exec("su");
                pos = new DataOutputStream(pp.getOutputStream());
                SysHook(lpparam.classLoader);
            } catch(Exception e) {
                log(e.toString());
            }
            hook=true;
        }
    }
}
