package com.example.ucsxposed.pep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class contain the method to be called doing a policy enforcement when an ongoing UCS session
 * does not respect the related XACML policy
 */
public class PEPHookReverter {
    private Class<?> revokeClass = null;
    private Method revokeMethod = null;
    private Object revokeObject = null;

    public PEPHookReverter( Class<?> revokeClass, Method revokeMethod, Object revokeObject){
        this.revokeClass = revokeClass;
        this.revokeMethod = revokeMethod;
        this.revokeObject = revokeObject;
    }

    public PEPHookReverter(){}

    public void revert(){
        if(revokeClass != null){
            try {
                revokeMethod.invoke((revokeClass.cast(revokeObject)));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
