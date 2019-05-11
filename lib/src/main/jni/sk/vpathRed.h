//
// Created by Saurik on 2019/5/8.
//

#ifndef SXP_VPATHRED_H
#define SXP_VPATHRED_H

#include <map>
#include <string>
#include <memory>

#ifndef FALSE
#define FALSE 0
#define TRUE 1
#endif

class SK_RedirectIO
{
public:
    typedef int BOOL;

    struct pathUtil
    {
        int cb = 0;
        std::string redirectedPath;
        BOOL is_keep = FALSE;
        BOOL is_forbid = FALSE;
        BOOL is_folder = FALSE;
        std::string newPath;
        size_t dwOrigSize = 0;
    };

    static std::map<std::string,std::shared_ptr<SK_RedirectIO::pathUtil>> IORedirectMap;

    static std::shared_ptr<pathUtil> getResult(std::string lpPath);

    static BOOL addPath(std::string szOrig, std::string szRedirect);

    static BOOL addPath(std::string szOrig, std::string szRedirect, BOOL is_folder);

    static BOOL addKeep(std::string szPath);

    static BOOL addForbid(std::string szPath);

    static BOOL addForbid(std::string szPath, BOOL is_folder);

    static void removeAll();
};

#endif //SXP_VPATHRED_H
