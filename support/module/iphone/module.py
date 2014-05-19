#!/usr/bin/env python
#
# Appcelerator Titanium Module Packager
#
#
import os, subprocess, sys, glob, string, optparse, subprocess
import zipfile
from datetime import date

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

def generate_doc(config):
    docdir = os.path.join(cwd,'documentation')
    if not os.path.exists(docdir):
        warn("Couldn't find documentation file at: %s" % docdir)
        return None

    try:
        import markdown2 as markdown
    except ImportError:
        import markdown
    documentation = []
    for file in os.listdir(docdir):
        if file in ignoreFiles or os.path.isdir(os.path.join(docdir, file)):
            continue
        md = open(os.path.join(docdir,file)).read()
        html = markdown.markdown(md)
        documentation.append({file:html});
    return documentation

def compile_js(manifest,config):
    js_file = os.path.join(cwd,'assets','akylas.mapbox.js')
    if not os.path.exists(js_file): return

    from compiler import Compiler
    try:
        import json
    except:
        import simplejson as json

    compiler = Compiler(cwd, manifest['moduleid'], manifest['name'], 'commonjs')
    root_asset, module_assets = compiler.compile_module()

    root_asset_content = """
%s

    return filterDataInRange([NSData dataWithBytesNoCopy:data length:sizeof(data) freeWhenDone:NO], ranges[0]);
""" % root_asset

    module_asset_content = """
%s

    NSNumber *index = [map objectForKey:path];
    if (index == nil) {
        return nil;
    }
    return filterDataInRange([NSData dataWithBytesNoCopy:data length:sizeof(data) freeWhenDone:NO], ranges[index.integerValue]);
""" % module_assets

    from tools import splice_code

    assets_router = os.path.join(cwd,'Classes','AkylasMapboxModuleAssets.m')
    splice_code(assets_router, 'asset', root_asset_content)
    splice_code(assets_router, 'resolve_asset', module_asset_content)

    # Generate the exports after crawling all of the available JS source
    exports = open('metadata.json','w')
    json.dump({'exports':compiler.exports }, exports)
    exports.close()

def die(msg):
    print msg
    sys.exit(1)

def info(msg):
    print "[INFO] %s" % msg

def warn(msg):
    print "[WARN] %s" % msg

ignoreFiles = ['.DS_Store','.gitignore','libTitanium.a','titanium.jar','README']
ignoreDirs = ['.DS_Store','.svn','.git','CVSROOT']

def zip_dir(zf,dir,basepath,ignoreExt=[]):
    if not os.path.exists(dir): return
    for root, dirs, files in os.walk(dir):
        for name in ignoreDirs:
            if name in dirs:
                dirs.remove(name)   # don't visit ignored directories
        for file in files:
            if file in ignoreFiles: continue
            e = os.path.splitext(file)
            if len(e) == 2 and e[1] in ignoreExt: continue
            from_ = os.path.join(root, file)
            to_ = from_.replace(dir, '%s/%s'%(basepath,dir), 1)
            zf.write(from_, to_)

def glob_libfiles(moduleid):
    files = []
    for libfile in glob.glob('build/**/*.a'):
        if libfile.find(moduleid)!=-1:
            files.append(libfile)
    return files

def build_module(manifest,config):
    from tools import ensure_dev_path
    ensure_dev_path()
    buildpath = os.path.join(cwd, "build")
    print(buildpath)
    rc = os.system("xcodebuild -sdk iphoneos -configuration Release clean build CONFIGURATION_BUILD_DIR=\"%s\"" % os.path.join(buildpath, "iphoneos"))
    if rc != 0:
        die("xcodebuild failed")
    rc = os.system("xcodebuild -sdk iphonesimulator -configuration Release clean build CONFIGURATION_BUILD_DIR=\"%s\"" % os.path.join(buildpath, "iphonesimulator"))
    if rc != 0:
        die("xcodebuild failed")
    # build the merged library using lipo
    moduleid = manifest['moduleid']
    libpaths = ''
    for libfile in glob_libfiles(moduleid):
        libpaths+='%s ' % libfile
    os.system("lipo %s -create -output build/lib%s.a" %(libpaths,moduleid))
    
def generate_apidoc(apidoc_build_path):
    global options
    
    if options.skip_docs:
        info("Skipping documentation generation.")
        return False
    else:
        info("Module apidoc generation can be skipped using --skip-docs")
    apidoc_path = os.path.join(cwd, "apidoc")
    if not os.path.exists(apidoc_path):
        warn("Skipping apidoc generation. No apidoc folder found at: %s" % apidoc_path)
        return False
        
    if not os.path.exists(apidoc_build_path):
        os.makedirs(apidoc_build_path)
    ti_root = string.strip(subprocess.check_output(["echo $TI_ROOT"], shell=True))
    if not len(ti_root) > 0:
        warn("Not generating documentation from the apidoc folder. The titanium_mobile repo could not be found.")
        warn("Set the TI_ROOT environment variable to the parent folder where the titanium_mobile repo resides (eg.'export TI_ROOT=/Path').")
        return False
    docgen = os.path.join(ti_root, "titanium_mobile", "apidoc", "docgen.py")
    if not os.path.exists(docgen):
        warn("Not generating documentation from the apidoc folder. Couldn't find docgen.py at: %s" % docgen)
        return False
        
    info("Generating documentation from the apidoc folder.")
    rc = os.system("\"%s\" --format=jsca,modulehtml --css=styles.css -o \"%s\" -e \"%s\"" % (docgen, apidoc_build_path, apidoc_path))
    if rc != 0:
        die("docgen failed")
    return True

def package_module(manifest,mf,config):
    name = manifest['name'].lower()
    moduleid = manifest['moduleid'].lower()
    version = manifest['version']
    modulezip = '%s-iphone-%s.zip' % (moduleid,version)
    if os.path.exists(modulezip): os.remove(modulezip)
    zf = zipfile.ZipFile(modulezip, 'w', zipfile.ZIP_DEFLATED)
    modulepath = 'modules/iphone/%s/%s' % (moduleid,version)
    zf.write(mf,'%s/manifest' % modulepath)
    libname = 'lib%s.a' % moduleid
    zf.write('build/%s' % libname, '%s/%s' % (modulepath,libname))
    if not options.skip_docs:
        docs = generate_doc(config)
        if docs!=None:
            for doc in docs:
                for file, html in doc.iteritems():
                    filename = string.replace(file,'.md','.html')
                    zf.writestr('%s/documentation/%s'%(modulepath,filename),html)
                    
        apidoc_build_path = os.path.join(cwd, "build", "apidoc")
        if generate_apidoc(apidoc_build_path):
            for file in os.listdir(apidoc_build_path):
                if file in ignoreFiles or os.path.isdir(os.path.join(apidoc_build_path, file)):
                    continue
                zf.write(os.path.join(apidoc_build_path, file), '%s/documentation/apidoc/%s' % (modulepath, file))
    
    zip_dir(zf,'assets',modulepath,['.pyc','.js'])
    zip_dir(zf,'example',modulepath,['.pyc'])
    zip_dir(zf,'platform',modulepath,['.pyc','.js'])
    zf.write('LICENSE','%s/LICENSE' % modulepath)
    zf.write('module.xcconfig','%s/module.xcconfig' % modulepath)
    exports_file = 'metadata.json'
    if os.path.exists(exports_file):
        zf.write(exports_file, '%s/%s' % (modulepath, exports_file))
    zf.close()


def buildModule(modulepath, command, opts, manifest,mf,config):
    os.chdir(modulepath)
    global cwd
    global options
    cwd = modulepath
    options = opts
    if (command == 'compilejs'):
        compile_js(manifest,config)
    elif(command == 'build'):
        compile_js(manifest,config)
        build_module(manifest,config)
    else:
        compile_js(manifest,config)
        build_module(manifest,config)
        package_module(manifest,mf,config)
