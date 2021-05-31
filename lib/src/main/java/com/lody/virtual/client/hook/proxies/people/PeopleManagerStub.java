package com.lody.virtual.client.hook.proxies.people;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.helper.compat.BuildCompat;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

/**
 * @author ganyao
 * @web www.die.lu
 * @author QQ 647564826
 * @since Android 12
 * @see android.app.people.PeopleManager
 */
@TargetApi(Build.VERSION_CODES.S)
public class PeopleManagerStub extends BinderInvocationProxy {
    private static class IPeopleManager {
        public static Class<?> TYPE = RefClass.load(IPeopleManager.class, "android.app.people.IPeopleManager");

        public static class Stub {
            public static Class<?> TYPE = RefClass.load(IPeopleManager.Stub.class, "android.app.people.IPeopleManager$Stub");
            @MethodParams({IBinder.class})
            public static RefStaticMethod<IInterface> asInterface;
        }
    }
    public PeopleManagerStub() {
        super(IPeopleManager.Stub.asInterface,
                BuildCompat.isS()?
                        Context.PEOPLE_SERVICE : "people");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceCallingPkgMethodProxy("addOrUpdateStatus"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("clearStatus"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("clearStatuses"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getStatuses"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getLastInteraction"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("removeRecentConversation"));
    }
}
