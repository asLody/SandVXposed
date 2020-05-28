package com.lody.virtual.sandxposed;

import java.util.HashSet;
import java.util.Set;

public class BlackList {

    private static Set<String> packageList = new HashSet<>();
    private static Set<String> processList = new HashSet<>();

    static {
        //ANR to login error
        processList.add("com.tencent.mm:push");
        processList.add("com.tencent.mobileqq:mini");
        processList.add("com.tencent.mobileqq:peak");
        processList.add("com.tencent.mobileqq:qzone");
        processList.add("com.tencent.mobileqq:picture");
    }

    public static boolean canNotInject(String packageName, String processName) {
        if (packageList.contains(packageName) || processList.contains(processName)) {
            return true;
        } else {
            return false;
        }
    }

}
