//
// Created by user on 2019/5/8.
//

#include <iostream>
#include "vpathRed.h"
using namespace std;

std::map<std::string,std::shared_ptr<SK_RedirectIO::pathUtil>> SK_RedirectIO::IORedirectMap;


std::shared_ptr<SK_RedirectIO::pathUtil> SK_RedirectIO::getResult(std::string lpPath)
{
    if(lpPath.back()=='/')lpPath.pop_back();
    auto lpPara = SK_RedirectIO::IORedirectMap.begin();
    while(lpPara!=SK_RedirectIO::IORedirectMap.end())
    {
        if(lpPath.find(lpPara->first)==0)break;
        lpPara++;
    }
    if(lpPara != SK_RedirectIO::IORedirectMap.end())
    {
        if(lpPara->second->is_keep||lpPara->second->is_forbid)
            return lpPara->second;

        string lpPathSk = lpPara->second->redirectedPath + string((const char*)(lpPath.c_str()+lpPara->second->dwOrigSize));
        lpPara->second->newPath = lpPathSk;
        return lpPara->second;
    }
    return std::shared_ptr<SK_RedirectIO::pathUtil>();
}

SK_RedirectIO::BOOL SK_RedirectIO::addForbid(std::string szPath)
{
    if(szPath.back()=='/')szPath.pop_back();
    try
    {
        std::shared_ptr<SK_RedirectIO::pathUtil> lpInstance(new SK_RedirectIO::pathUtil);
        lpInstance->cb = sizeof(SK_RedirectIO::pathUtil);
        lpInstance->is_forbid = TRUE;
        // Redirect!
        SK_RedirectIO::IORedirectMap[szPath] = lpInstance;
    }
    catch (...)
    {
        return FALSE;
    }
    return TRUE;
}

SK_RedirectIO::BOOL SK_RedirectIO::addPath(std::string szOrig, std::string szRedirect)
{
    if(szOrig.back()=='/')szOrig.pop_back();
    try
    {
        std::shared_ptr<SK_RedirectIO::pathUtil> lpInstance(new SK_RedirectIO::pathUtil);
        lpInstance->cb = sizeof(SK_RedirectIO::pathUtil);
        lpInstance->redirectedPath = szRedirect;
        lpInstance->dwOrigSize = szOrig.size();
        // Redirect!
        SK_RedirectIO::IORedirectMap[szOrig] = lpInstance;
    }
    catch (...)
    {
        return FALSE;
    }
    return TRUE;
}

SK_RedirectIO::BOOL SK_RedirectIO::addKeep(std::string szPath)
{
    if(szPath.back()=='/')szPath.pop_back();
    try
    {
        std::shared_ptr<SK_RedirectIO::pathUtil> lpInstance(new SK_RedirectIO::pathUtil);
        lpInstance->cb = sizeof(SK_RedirectIO::pathUtil);
        lpInstance->is_keep = TRUE;
        // Redirect!
        SK_RedirectIO::IORedirectMap[szPath] = lpInstance;
    }
    catch (...)
    {
        return FALSE;
    }
    return TRUE;
}

SK_RedirectIO::BOOL
SK_RedirectIO::addPath(std::string szOrig, std::string szRedirect, SK_RedirectIO::BOOL is_folder)
{
    if(szOrig.back()=='/')szOrig.pop_back();
    try
    {
        std::shared_ptr<SK_RedirectIO::pathUtil> lpInstance(new SK_RedirectIO::pathUtil);
        lpInstance->cb = sizeof(SK_RedirectIO::pathUtil);
        lpInstance->redirectedPath = szRedirect;
        lpInstance->dwOrigSize = szOrig.size();
        lpInstance->is_folder = is_folder;
        // Redirect!
        SK_RedirectIO::IORedirectMap[szOrig] = lpInstance;
    }
    catch (...)
    {
        return FALSE;
    }
    return TRUE;
}

void SK_RedirectIO::removeAll()
{
    SK_RedirectIO::IORedirectMap.clear();
}

SK_RedirectIO::BOOL SK_RedirectIO::addForbid(std::string szPath, SK_RedirectIO::BOOL is_folder)
{
    if(szPath.back()=='/')szPath.pop_back();
    try
    {
        std::shared_ptr<SK_RedirectIO::pathUtil> lpInstance(new SK_RedirectIO::pathUtil);
        lpInstance->cb = sizeof(SK_RedirectIO::pathUtil);
        lpInstance->is_forbid = TRUE;
        lpInstance->is_folder = is_folder;
        // Redirect!
        SK_RedirectIO::IORedirectMap[szPath] = lpInstance;
    }
    catch (...)
    {
        return FALSE;
    }
    return TRUE;
}
