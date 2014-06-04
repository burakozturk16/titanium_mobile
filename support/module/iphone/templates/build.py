#!/usr/bin/env python
#
# Appcelerator Titanium Module Packager
#
#
import os, subprocess, sys, glob, string, optparse, subprocess
import zipfile
from datetime import date
import imp

cwd = os.path.abspath(os.path.dirname(sys._getframe(0).f_code.co_filename))
os.chdir(cwd)

def find_sdk(config):
    sdk = config['TITANIUM_SDK']
    return os.path.expandvars(os.path.expanduser(sdk))

def replace_vars(config,token):
    idx = token.find('$(')
    while idx != -1:
        idx2 = token.find(')',idx+2)
        if idx2 == -1: break
        key = token[idx+2:idx2]
        if not config.has_key(key): break
        token = token.replace('$(%s)' % key, config[key])
        idx = token.find('$(')
    return token


def read_ti_xcconfig():
    contents = open(os.path.join(cwd,'titanium.xcconfig')).read()
    config = {}
    for line in contents.splitlines(False):
        line = line.strip()
        if line[0:2]=='//': continue
        idx = line.find('=')
        if idx > 0:
            key = line[0:idx].strip()
            value = line[idx+1:].strip()
            config[key] = replace_vars(config,value)
    return config

def die(msg):
    print msg
    sys.exit(1)

def info(msg):
    print "[INFO] %s" % msg

def warn(msg):
    print "[WARN] %s" % msg


ignoreFiles = ['.DS_Store','.gitignore','libTitanium.a','titanium.jar','README']
ignoreDirs = ['.DS_Store','.svn','.git','CVSROOT']

def glob_libfiles():
    files = []
    moduleid = manifest['moduleid']
    for libfile in glob.glob('build/**/*.a'):
        if libfile.find(moduleid)!=-1:
            files.append(libfile)
    return files
    
if __name__ == '__main__':
    global options
    parser = optparse.OptionParser()
    parser.add_option("-s", "--skip-docs",
            dest="skip_docs",
            action="store_true",
            help="Will skip building documentation in apidoc folder",
            default=False)
    (options, args) = parser.parse_args()
    
    
    config = read_ti_xcconfig()

    command = 'package'
    args = sys.argv
    if (len(args) > 1):
        command = args[1]
    modulePath = os.path.dirname(os.path.abspath(args[0]))

    sdk = find_sdk(config)
    sys.path.insert(0,os.path.join(sdk,'iphone'))
    sys.path.append(os.path.join(sdk, "common"))
    sys.path.append(os.path.join(sdk, "module", 'iphone'))
    import module
    module.buildModule(modulePath, command, options, config)
    sys.exit(0)

