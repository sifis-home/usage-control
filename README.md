#  Usage Control Model (UCON) on Android

Implementation of Usage Control Model on Android based from an initial version for Desktop implemented by
PISA CNR Researchers.

The main goal of this Framework is to allow developers to define **fine-grained security policies on Android**, based from
attributes that could be obtained from device sensors and system services (i.e. LocationManager for obtaining position information)
and enforce those policies following a Usage Control fashion.

Read the following papers to understand the concepts of ABAC and how UCS implements them, especially the concept of continuous monitoring.
1) https://fardapaper.ir/mohavaha/uploads/2017/10/Implementing-Usage-Control-in-Internet-of-Things.pdf
2) https://www.iit.cnr.it/sites/default/files/Internet%20of%20Things.pdf
3) https://www.iit.cnr.it/sites/default/files/main_22.pdf
4) https://www.iit.cnr.it/sites/default/files/Improving_MQTT_by_inc_of_UCON.pdf

## Prerequisites

In order to exploit the fully potential of UCON on Android the following pre-requisites must be met:

1) Install [XPosed](https://github.com/ElderDrivers/EdXposed) on your Android device (needs Root Privileges)
2) Install ucsapp and ucsxposed apps on the target device
3) Move (from ucsapp assets folder) pips folder under /sdcard/Android/data/com.example.ucsintent/files/ folder (create the missing folders if needed)
4) Activate ucsxposed module on XPosed Manager and reboot your system
5) After rebooting your smartphone, start ucsapp in order to initialise UCSService
6) Enjoy UCON prototype on Android

## Modules

 - **ucsapp**: the core of UCON, it implements the Usage Control System
 - **ucsxposed**: implements Policy Enforcement by exploiting XPosed Hooks
 - **ucslibrary**: common libraries between ucsapp and ucsxposed
 - **hookedapp**: sample application built for experiment and debugging purposes
 - **balana-core** and **balana-utils**: present source code of Balana library, with little modification to deal with Java 8 incompatibilities in Android
 
## Testing UCON

Is possible to test UCON in two different ways. 
For debugging purposes is possible to interact with UCSService
directly via UCSApp by pressing the related buttons. StartAccess and EndAccess requests comes the last registered SessionID.
The second way is by defining hook methods in ucsxposed.

## How to implement a PEP in UCON on Android

Depending on the need, is possible to set two classes of Policy Enforcement:
 - **Access Control**: by invoking a tryAccess action.
 - **Usage Control**: invoke a tryAccess and startAccess action in sequence and define action to be performed in case of change of policy result.

 Both Policy Enforcement mechanisms can be implemented as an XPosed module. To do so is necessary to expand the PackageToHook class by adding needed method hooks.
 As reference is possible to implement Access and Usage control in the following ways:

**Access Control PEP Example**
```Java
findAndHookMethod("class_to_hook", lpparam.classLoader, "method_to_hook", ...,  new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        //Specify relative paths to ucsapp asset storage containing XML files containing policy and request files
        PEPHookProperties pepHookProperties = new PEPHookProperties(id, policyPath, requestPath);
        PEPHook pepHook = new PEPHook(pepHookProperties, context);

        //Invoke tryAccess action
        if(!pepHook.tryAccess()){
            //Skip method implementation in case of policy violation
            param.setResult(null);
        }

        //Unregister PEP broadcast receiver (since it is an Access Request)
        pepHook.destroy();
    }
});
```


**Usage Control PEP Basic Example**
```Java
 findAndHookMethod("class_to_hook", lpparam.classLoader, "method_to_hook", ..., new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        //Define which method will be invoked in case of policy result change via Java Reflection
        PEPHookReverter pepHookReverter = new PEPHookReverter( clazz, method, caller_object);
        //Specify relative path to ucsapp asset storage containing XML files containing policy and request
        PEPHookProperties pepHookProperties = new PEPHookProperties(id, policyPath, requestPath);
        PEPHook pepHook = new PEPHook(pepHookProperties, context, pepHookReverter);

        //Invoke startAccess action (comprehend a tryAccess action)
        if(!pepHook.startAccess()){
            //Skip method implementation in case of policy violation at start of access request
            param.setResult(null);
        }
    }
});
```

## Known Issues

- Android does not allow to start a service from background from a different app. For this reason UCSApp need to be opened once to start UCSService is started as a foreground service, otherwise policy enforcement will result in a thread block.
- As part of Android porting, the xml parsing library was changed to SimpleXML. This means that all POJO classes Java annotations present in ucslibrary (wd_17 package) were changed manually. While most of these changes were tested with sample XACML policies, beware that with more complicated XACML files, bugs may be still present. For reference check the related documentation of [SimpleXML library](http://simple.sourceforge.net/download/stream/doc/tutorial/tutorial.php). 