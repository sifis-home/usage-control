package com.example.ucsxposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.app.AndroidAppHelper;
import android.content.Context;

import com.example.ucsxposed.pep.PEPHook;
import com.example.ucsxposed.pep.PEPHookProperties;
import com.example.ucsxposed.pep.PEPHookReverter;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Class that contains all hooks that were implemented in order to test, showcase and assess the performance of
 * Usage Control Model (UCON) on Android.
 */
public class PackageHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String hookedapp = "com.example.hookedapp";
        String youtube = "com.google.android.youtube";
        String chrome = "com.android.chrome";
        String telecom = "com.android.server.telecom";
        String camera = "com.android.camera2";

        if(lpparam.packageName.equals(hookedapp)){
            //For experiment
            findAndHookMethod(hookedapp + ".ExperimentActivity", lpparam.classLoader,
                "method_to_hook", "java.lang.String", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) AndroidAppHelper.currentApplication();
                    String policyPath = (String) param.args[0];
                    XposedBridge.log("Policy path: " + policyPath);
                    PEPHookProperties pepHookProperties = new PEPHookProperties(policyPath, policyPath, "xmls/request.xml");
                    PEPHook pepHook = new PEPHook(pepHookProperties, context);
                    if(!pepHook.tryAccess()){
                        param.setResult(null);
                    }
                    pepHook.destroy();
                    }
                });

            findAndHookMethod(hookedapp + ".ExperimentActivity", lpparam.classLoader,
                "method_to_hook_start_access", "java.lang.String", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) AndroidAppHelper.currentApplication();
                    Class<?> clazz = findClass(hookedapp + ".ExperimentActivity", lpparam.classLoader);
                    Method method = clazz.getMethod("method_to_hook_revoke_access");
                    String policyPath = (String) param.args[0];
                    XposedBridge.log("Policy path: " + policyPath);
                    PEPHookReverter pepHookReverter = new PEPHookReverter( clazz, method, param.thisObject);
                    PEPHookProperties pepHookProperties = new PEPHookProperties(policyPath, policyPath, "xmls/request.xml");
                    PEPHook pepHook = new PEPHook(pepHookProperties, context, pepHookReverter);
                    if(!pepHook.startAccess()){
                        pepHookReverter.revert();
                        param.setResult(null);
                    }
                    }
                });


            findAndHookMethod(hookedapp + ".MainActivity", lpparam.classLoader,
                "switch_color", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) AndroidAppHelper.currentApplication();
                Class<?> clazz = findClass(hookedapp + ".MainActivity", lpparam.classLoader);
                Method method = clazz.getMethod("switch_color_to_default");

                PEPHookReverter pepHookReverter = new PEPHookReverter( clazz, method, param.thisObject);
                PEPHookProperties pepHookProperties = new PEPHookProperties("switch_color",
                        "xmls/policy-watch.xml", "xmls/request.xml");
                PEPHook pepHook = new PEPHook(pepHookProperties, context, pepHookReverter);

                if(!pepHook.startAccess()){
                    XposedBridge.log("Invoked revoke method");
                    pepHookReverter.revert();
                    param.setResult(null);
                }

                }
            });

            findAndHookMethod(hookedapp + ".MainActivity", lpparam.classLoader,
                "switch_color_sensor", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) AndroidAppHelper.currentApplication();
                Class<?> clazz = findClass(hookedapp + ".MainActivity", lpparam.classLoader);
                Method method = clazz.getMethod("switch_color_sensor_to_default");

                PEPHookReverter pepHookReverter = new PEPHookReverter( clazz, method, param.thisObject);
                PEPHookProperties pepHookProperties = new PEPHookProperties("switch_color_sensor",
                        "xmls/policy-sensor1.xml", "xmls/request.xml");
                PEPHook pepHook = new PEPHook(pepHookProperties, context, pepHookReverter);

                if(!pepHook.startAccess()){
                    XposedBridge.log("Invoked revoke method");
                    pepHookReverter.revert();
                    param.setResult(null);
                }
                }
            });
        } else if(lpparam.packageName.equals("com.google.android.webview") ||
                lpparam.packageName.equals(chrome)){

            //For implementing http denial in Android Webview application
            findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
            "loadUrl", "java.lang.String", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String url = (String) param.args[0];
                XposedBridge.log("loaded url: " + url);
                Context context = (Context) AndroidAppHelper.currentApplication();
                PEPHookProperties pepHookProperties = new PEPHookProperties("onCreate","xmls/experiments/policy-network.xml", "xmls/request.xml");
                if(!new PEPHook(pepHookProperties, context).tryAccess()){
                    //Now allow HTTP websearch
                    //TODO: move in the request XACML file
                    if(url.startsWith("http://"))
                        param.setResult(null);
                }
                }
            });

            findAndHookMethod("android.webkit.WebView", lpparam.classLoader,
                "loadUrl", "java.lang.String", "java.util.Map", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String url = (String) param.args[0];
                    XposedBridge.log("loaded url: " + url);
                    Context context = (Context) AndroidAppHelper.currentApplication();
                    PEPHookProperties pepHookProperties = new PEPHookProperties("onCreate","xmls/experiments/policy-network.xml", "xmls/request.xml");
                    if(!new PEPHook(pepHookProperties, context).tryAccess()){
                        //Now allow HTTP websearch
                        //TODO: move in the request XACML file
                        if(url.startsWith("http://"))
                            param.setResult(null);
                    }
                    }
                });
        } else if(lpparam.packageName.equals(youtube)){

            //For implementing Youtube usage control with respect to certain coordinates
            findAndHookMethod("com.google.android.apps.youtube.app.application.Shell_HomeActivity", lpparam.classLoader,
                    "onCreate", "android.os.Bundle", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Init of youtube app");
                    Context context = (Context) AndroidAppHelper.currentApplication();
                    Class<?> clazz = findClass("com.google.android.apps.youtube.app.application.Shell_HomeActivity", lpparam.classLoader);
                    Method method = clazz.getDeclaredMethod("onCreate", Class.forName("android.os.Bundle"));
                    method.setAccessible(true);
                    PEPHookReverter pepHookReverter = new PEPHookReverter( clazz, method, param.thisObject);
                    PEPHookProperties pepHookProperties = new PEPHookProperties("onCreate","xmls/experiments/policy-position.xml", "xmls/request.xml");
                    PEPHook pepHook = new PEPHook(pepHookProperties, context, pepHookReverter);
                    if(!pepHook.startAccess()){
                        param.setResult(null);
                    }
                }
                });
        } else if(lpparam.packageName.equals(telecom)){

            //For implementing Incoming calls popup access control over certain hours/days
            findAndHookMethod("com.android.server.telecom.CallsManager", lpparam.classLoader,
                "onSuccessfulIncomingCall", "com.android.server.telecom.Call", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Class<?> callClazz = findClass("com.android.server.telecom.Call", lpparam.classLoader);
                    Method getPhoneNumber = callClazz.getDeclaredMethod("getPhoneNumber");
                    String phoneNumber = (String) getPhoneNumber.invoke((callClazz.cast(param.args[0])));
                    XposedBridge.log("Incoming call successful, target phoneNumber: " + phoneNumber);

                    Context context = (Context) AndroidAppHelper.currentApplication();
                    PEPHookProperties pepHookProperties = new PEPHookProperties("onSuccessfulIncomingCall","xmls/experiments/policy-time.xml", "xmls/request.xml");
                    if(!new PEPHook(pepHookProperties, context).tryAccess()){
                        //TODO: add in the XACML request
                        if(phoneNumber.equals("(650) 555-1213"))
                            param.setResult(null);
                    }
                    }
                });
        } else if(lpparam.packageName.equals(camera)){
            findAndHookMethod("android.hardware.camera2.impl.CameraDeviceImpl", lpparam.classLoader,
                    "createCaptureSessionInternal", "android.hardware.camera2.params.InputConfiguration", "java.util.List", "android.hardware.camera2.CameraCaptureSession.StateCallback","java.util.concurrent.Executor", "int", "android.hardware.camera2.CaptureRequest", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) AndroidAppHelper.currentApplication();
                            Class<?> clazz = findClass("android.hardware.camera2.impl.CameraDeviceImpl", lpparam.classLoader);
                            Method method = clazz.getDeclaredMethod("close");
                            PEPHookReverter pepHookReverter = new PEPHookReverter( clazz, method, param.thisObject);
                            PEPHookProperties pepHookProperties = new PEPHookProperties("onCreate","xmls/experiments/policy-position.xml", "xmls/request.xml");
                            PEPHook pepHook = new PEPHook(pepHookProperties, context, pepHookReverter);
                            if(!pepHook.startAccess()){
                                param.setResult(null);
                            }
                        }
                    });

        }
    }


}
