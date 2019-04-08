package com.lody.virtual.client.sandxposed;

import com.swift.sandhook.SandHook;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SandHookHelper {


    public static void initHookPolicy() {
        SandHook.disableVMInline();
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
