package com.lody.virtual.client.sandxposed;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookMode;
import com.swift.sandhook.wrapper.HookWrapper;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.lang.reflect.Member;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SandHookHelper {


    static SharedPreferences replacementHookList;


    public static void initHookPolicy() {

        String profName = "sandhook_replace_methods@" + MD5(XposedCompat.processName);
        replacementHookList = XposedCompat.context.getSharedPreferences(profName, Context.MODE_PRIVATE);

        SandHook.setHookResultCallBack(new SandHook.HookResultCallBack() {
            @Override
            public void hookResult(boolean success, HookWrapper.HookEntity hookEntity) {
                if (success && hookEntity.hookMode == HookMode.REPLACE) {
                    addReplacementHookMethod(hookEntity.target);
                }
            }
        });

        SandHook.setHookModeCallBack(new SandHook.HookModeCallBack() {
            @Override
            public int hookMode(Member originMethod) {
                if (TextUtils.equals("public void android.app.Application.onCreate()", originMethod.toString())) {
                    return HookMode.REPLACE;
                }
                if (needHookReplacement(originMethod))
                    return HookMode.REPLACE;
                return HookMode.AUTO;
            }
        });

        XposedCompat.clearOatCache();

    }



    private static void addReplacementHookMethod(Member member) {
        replacementHookList.edit().putBoolean(member.toString(), true).apply();
    }

    private static boolean needHookReplacement(Member member) {
        return replacementHookList.contains(member.toString());
    }

    //hash to avoid sandbox io redirect
    public static String MD5(String source) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(source.getBytes());
            return new BigInteger(1, messageDigest.digest()).toString(32);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return source;
    }

}
