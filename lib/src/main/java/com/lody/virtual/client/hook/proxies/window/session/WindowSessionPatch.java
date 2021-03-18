package com.lody.virtual.client.hook.proxies.window.session;

import android.os.IInterface;

import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.base.MethodInvocationStub;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;

import java.lang.reflect.Method;

/**
 * @author Lody
 */
public class WindowSessionPatch extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {

	public WindowSessionPatch(IInterface session) {
		super(new MethodInvocationStub<>(session));
	}

	@Override
	public void onBindMethods() {
		addMethodProxy(new BaseMethodProxy("add"));
		addMethodProxy(new BaseMethodProxy("addToDisplay"));
		addMethodProxy(new BaseMethodProxy("addToDisplayWithoutInputChannel"));
		addMethodProxy(new BaseMethodProxy("addWithoutInputChannel"));
		addMethodProxy(new BaseMethodProxy("relayout"));
		addMethodProxy(new BaseMethodProxy("addToDisplayAsUser")
		{
			@Override
			public Object call(Object who, Method method, Object... args) throws Throwable {
				MethodParameterUtils.replaceLastUid(args);
				return super.call(who, method, args);
			}
		});
	}


	@Override
	public void inject() throws Throwable {
		// <EMPTY>
	}

	@Override
	public boolean isEnvBad() {
		return getInvocationStub().getProxyInterface() != null;
	}
}
