package com.lody.virtual.helper.memory;

import java.io.File;

/**
 * Author: Saurik
 * WEB: https://www.fou.ink/
 * 2019/05/26
 */
public class SKWinNTUtils
{
    static public final  int
            MEM_COMMIT=
            0x00001000;
    static public final  int
            MEM_RESERVE=
            0x00002000;
    static public final  int
            MEM_RESET=
            0x00080000;
    static public final  int
            MEM_RESET_UNDO=
            0x1000000;
    static public final  int
            MEM_LARGE_PAGES=
            0x20000000;
    static public   final  int
            MEM_PHYSICAL=
            0x00400000;
    static public  final   int
            MEM_TOP_DOWN=
            0x00100000;

    static public  final int MEM_COALESCE_PLACEHOLDERS=
            0x00000001;
    static public  final  int
            MEM_PRESERVE_PLACEHOLDER=
            0x00000002;
    static public  final  int
            MEM_DECOMMIT=
            0x4000;
    static public final int
            MEM_RELEASE=
            0x8000;


    public static native long GetCurrentProcess();

    public static native long VirtualAllocEx(long hProcess,long lpMemory,long dwSize,int flAllocationType,int flProtect);
    public static native long VirtualFreeEx(long hProcess,long lpAddress,long dwSize, int dwFreeType);
    public static native boolean VirtualProtectEx(
            long hProcess,
            long lpAddress,
            long dwSize,
            int  flNewProtect,
            long lpflOldProtect
    );
    public static native void CopyMemory(long dst, final long src, long size);
    public static native long MapViewOfFile(
            long hFileMappingObject,
            int  dwDesiredAccess,
            int  dwFileOffsetHigh,
            int  dwFileOffsetLow,
            long dwNumberOfBytesToMap
    );

    int VMOfferPriorityNormal=
            0x00002000;

    public static native int OfferVirtualMemory(
            long          VirtualAddress,
            long         Size,
            long Priority
    );
    public static native boolean UnmapViewOfFile(
            long lpBaseAddress
    );
    public static native long VirtualQueryEx(
            long                    hProcess,
            long                   lpAddress,
            long lpBuffer,
            long                    dwLength
    );
    public static native boolean WriteProcessMemory(
            long  hProcess,
            long  lpBaseAddress,
            long lpBuffer,
            long  nSize,
            long lpNumberOfBytesWritten // Pointer of 32/64 bits memory field.
    );

    public static native long OpenFileMappingW(
            int   dwDesiredAccess,
            int    bInheritHandle, // typedef int BOOL;
            long lpName
    );

    public static native long CreateFileMappingW(
            long                hFile,
            long lpFileMappingAttributes,
            int                 flProtect,
            int                 dwMaximumSizeHigh,
            int                 dwMaximumSizeLow,
            long               lpName
    );

    public static native int FlushViewOfFile(
            long lpBaseAddress,
            long  dwNumberOfBytesToFlush
    );

    public static native long CreateFileA(
            final long                lpFileName, // Must alloc a memory address that
            // contains string without unicode char.
            int                 dwDesiredAccess,
            int                 dwShareMode,
            // 0/1/2/4 0/FILE_SHARE_READ/FILE_SHARE_WRITE/FILE_SHARE_DELETE
            long lpSecurityAttributes,
            int                 dwCreationDisposition,
            // CREATE_NEW = 1 OPEN_EXISTING = 3 CREATE_ALWAYS = 2
            int                 dwFlagsAndAttributes,
            // FILE_ATTRIBUTE_NORMAL = 128 FILE_ATTRIBUTE_TEMPORARY = 256
            long                hTemplateFile
    );

    public static native long OpenProcess(
            int dwDesiredAccess,
            int  bInheritHandle,
            int dwProcessId     // Not pid but VProcess id.
    );

    public static native long CreateRemoteThreadEx(
            long                       hProcess,
            long        lpThreadAttributes,
            long                       dwStackSize,
            long       lpStartAddress,
            long                       lpParameter,
            int                        dwCreationFlags,
            long lpAttributeList,
            long                      lpThreadId
    );

    public static native int TlsAlloc(

    );

    public static native int TlsFree(
            int dwTlsIndex
    );

    public static native int GetThreadContext(
            long    hThread,
            long lpContext
    );

    public static native int QueueUserAPC(
            long  pfnAPC,
            long    hThread,
            long dwData
    );

    public static native int TerminateProcess(
            long hProcess,
            int   uExitCode
    );

    public static native long OpenThread(
            int dwDesiredAccess,
            int  bInheritHandle,
            int dwThreadId
    );

    public static native int ResumeThread(
            long hThread
    );

    public static native int SuspendThread(
            long hThread
    );

    public static native int SwitchToThread(
            // yield
    );

    public static native int CreateProcessAsUserW(
            long                hToken,
            long               lpApplicationName,
            long                lpCommandLine,
            long lpProcessAttributes,
            long lpThreadAttributes,
            int                  bInheritHandles,
            int                 dwCreationFlags,
            long                lpEnvironment,
            long               lpCurrentDirectory,
            long        lpStartupInfo,
            long lpProcessInformation
    );

    static public class ntHelpUtils
    {
        public static native long File2HFILE(File fils);
        public static native int linuxPid2VProcessId(int pid);
    };
};
