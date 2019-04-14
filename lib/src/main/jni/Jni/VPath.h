//
// Created by Lianglixin on 2019/4/14.
//

#ifndef SXP_VPATH_H
#define SXP_VPATH_H

#include <string>
#include <list>
#include <filesystem>

class VMPathCover
{
protected:
    typedef int BOOL;
    typedef void* LPVOID;
    typedef unsigned long ULONG;
    typedef unsigned long long UXLONG;
    typedef UXLONG ULONG64;
    constexpr BOOL is_BigEdian = 1<<1;
public:
    std::string XCoverPath_VM(std::string szOriPath,std::string szNew);
    std::string XCoverPath_Data(std::string szPkgName);
    std::string XCoverPath_sdcard(std::string szToRedirect);
    BOOL is_file_exist(const char* lpPath);
    BOOL is_file_exist(std::string szPath);
    LPVOID getIOStream(std::string szPath);
    LPVOID getIOStream(const char* lpPath);
};


#endif //SXP_VPATH_H
