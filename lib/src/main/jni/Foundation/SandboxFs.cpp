#include <cstdlib>
#include "SandboxFs.h"
#include "Path.h"
#include <list>
#include <iostream>
using namespace std;
/*
PathItem *keep_items;
PathItem *forbidden_items;
ReplaceItem *replace_items;
 */
list<PathItem>keep_items,forbidden_items;
list<ReplaceItem> replace_items;
/*
int keep_item_count;
int forbidden_item_count;
int replace_item_count;
 */

int add_keep_item(const char *path) {
    if(!path)return -1;
    char keep_env_name[25];
    sprintf(keep_env_name, "V_KEEP_ITEM_%d", (int)keep_items.size());
    setenv(keep_env_name, path, 1);
    /*
    keep_items = (PathItem *) realloc(keep_items,
                                      keep_item_count * sizeof(PathItem) + sizeof(PathItem));
                                      */
    PathItem item;
    item.path = strdup(path);
    item.size = strlen(path);
    keep_items.push_back(item);
    //return ++keep_item_count;
    return (int)keep_items.size();
}

int add_forbidden_item(const char *path) {
    if(!path)return -1;
    char forbidden_env_name[25];
    sprintf(forbidden_env_name, "V_FORBID_ITEM_%d", (int)forbidden_items.size());
    setenv(forbidden_env_name, path, 1);
    /*
    forbidden_items = (PathItem *) realloc(forbidden_items,
                                           forbidden_item_count * sizeof(PathItem) +
                                           sizeof(PathItem));
                                           */
    PathItem item;
    item.path = strdup(path);
    item.size = strlen(path);
    item.is_folder = (path[strlen(path) - 1] == '/');
    forbidden_items.push_back(item);
    // PathItem &item = forbidden_items[forbidden_item_count];
    // return ++forbidden_item_count;
    return (int)forbidden_items.size();
}

int add_replace_item(const char *orig_path, const char *new_path) {
    if(!orig_path || !new_path)return  -1;
    char src_env_name[25];
    char dst_env_name[25];
    sprintf(src_env_name, "V_REPLACE_ITEM_SRC_%d", (int)replace_items.size());
    sprintf(dst_env_name, "V_REPLACE_ITEM_DST_%d", (int)replace_items.size());
    setenv(src_env_name, orig_path, 1);
    setenv(dst_env_name, new_path, 1);
/*
    replace_items = (ReplaceItem *) realloc(replace_items,
                                            replace_item_count * sizeof(ReplaceItem) +
                                            sizeof(ReplaceItem));*/
    ReplaceItem item;
    item.orig_path = strdup(orig_path);
    item.orig_size = strlen(orig_path);
    item.new_path = strdup(new_path);
    item.new_size = strlen(new_path);
    item.is_folder = (orig_path[strlen(orig_path) - 1] == '/');
    replace_items.push_back(item);
    //return ++replace_item_count;
    return (int)replace_items.size();
}


PathItem *get_keep_items() {
    return NULL;
}

PathItem *get_forbidden_item() {
    return NULL;
}

ReplaceItem *get_replace_items() {
    return NULL;
}

int get_keep_item_count() {
    return (int)keep_items.size();
}

int get_forbidden_item_count() {
    return (int)forbidden_items.size();
}

int get_replace_item_count() {
    return (int)replace_items.size();
}

inline bool match_path(bool is_folder, size_t size, const char *item_path, const char *path) {
    if (is_folder) {
        if (strlen(path) < size) {
            // ignore the last '/'
            return strncmp(item_path, path, size - 1) == 0;
        }
    }
    return strncmp(item_path, path, size) == 0;
}


const char *relocate_path(const char *path, int *result) {
    if (!path) {
        *result = NOT_MATCH;
        return NULL;
    }
    for (auto &item : keep_items)
    {
        if (strcmp(item.path, path) == 0) {
            *result = KEEP;
            return path;
        }
    }
    for (auto &item : forbidden_items)
    {
        if (match_path(item.is_folder, item.size, item.path, path)) {
            *result = FORBID;
            // Permission denied
            errno = 13;
            return NULL;
        }
    }
    for (auto &item : replace_items)
    {
        if (match_path(item.is_folder, item.orig_size, item.orig_path, path)) {
            *result = MATCH;
            int len = (int)strlen(path);
            if (len < item.orig_size) {
                //remove last /
                std::string redirect_path(item.new_path, 0, item.new_size - 1);
                return strdup(redirect_path.c_str());
            } else {
                std::string redirect_path(item.new_path);
                redirect_path += path + item.orig_size;
                return strdup(redirect_path.c_str());
            }
        }
    }
    *result = NOT_MATCH;
    return path;
}


int relocate_path_inplace(char *_path, size_t size, int *result) {
    const char *redirect_path = relocate_path(_path, result);
    if (redirect_path && redirect_path != _path) {
        if (strlen(redirect_path) <= size) {
            strcpy(_path, redirect_path);
        } else {
            return -1;
        }
        free((void *) redirect_path);
    }
    return 0;
}


const char *reverse_relocate_path(const char *_path) {
    if (!_path) {
        return nullptr;
    }
    char *path = canonicalize_filename(_path);
    for (auto &item : keep_items)
    {
        if (strcmp(item.path, path) == 0) {
            free(path);
            return _path;
        }
    }
    for (auto &item : replace_items)
    {
        if (match_path(item.is_folder, item.new_size, item.new_path, path)) {
            int len = (int)strlen(path);
            if (len < item.new_size) {
                //remove last /
                std::string reverse_path(item.orig_path, 0, item.orig_size - 1);
                free(path);
                return strdup(reverse_path.c_str());
            } else {
                std::string reverse_path(item.orig_path);
                reverse_path += path + item.new_size;
                free(path);
                return strdup(reverse_path.c_str());
            }
        }
    }
    return _path;
}


int reverse_relocate_path_inplace(char *_path, size_t size) {
    const char *redirect_path = reverse_relocate_path(_path);
    if (redirect_path && redirect_path != _path) {
        if (strlen(redirect_path) <= size) {
            strcpy(_path, redirect_path);
        } else {
            return -1;
        }
        free((void *) redirect_path);
    }
    return 0;
}