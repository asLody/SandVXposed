package com.lody.virtual.client.hook.proxies.atm;

import android.annotation.TargetApi;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.Inject;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;

import java.lang.reflect.Method;

import mirror.android.app.IActivityTaskManager;

//
// Created by Swift Gan on 2019/3/18.
//

//Android Q Task Manager
@Inject(MethodProxies.class)
@TargetApi(29)
public class ActivityTaskManagerStub extends BinderInvocationProxy {

    public ActivityTaskManagerStub() {
        super(IActivityTaskManager.Stub.asInterface, "activity_task");
    }

    @Override
    protected void onBindMethods()
    {
        super.onBindMethods();

        addMethodProxy(new com.lody.virtual.client.hook.proxies.am.MethodProxies.GetContentProvider());
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getAppTasks"));
    }
}
