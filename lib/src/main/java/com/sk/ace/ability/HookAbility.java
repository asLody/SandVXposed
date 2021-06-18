package com.sk.ace.ability;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class HookAbility {
    public static class StubMethodStructure
    {
        Member whichToHook;
        Method whichIsTrampoline;
        Method whichIsBackup;
        public StubMethodStructure(Member target, Method toWhere, Method storeBackupMethod)
        {
            // Powered by SHook!
            this.whichToHook = target;
            this.whichIsTrampoline = toWhere;
            this.whichIsBackup = storeBackupMethod;
        }

        public Member getWhichToHook() {
            return whichToHook;
        }

        public Method getWhichIsBackup() {
            return whichIsBackup;
        }

        public Method getWhichIsTrampoline() {
            return whichIsTrampoline;
        }
    }
}
