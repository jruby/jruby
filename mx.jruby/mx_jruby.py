# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

import sys
import os
import pipes
import shutil
import tarfile
from os.path import join, exists, isdir

import mx
import mx_unittest

TimeStampFile = mx.TimeStampFile

_suite = mx.suite('jruby')

rubyDists = [
    'RUBY',
    'RUBY-TEST'
]

def deploy_binary_if_truffle_head(args):
    """If the active branch is 'truffle-head', deploy binaries for the primary suite to remote maven repository."""
    primary_branch = 'truffle-head'
    active_branch = mx.VC.get_vc(_suite.dir).active_branch(_suite.dir)
    if active_branch == primary_branch:
        return mx.command_function('deploy-binary')(args)
    else:
        mx.log('The active branch is "%s". Binaries are deployed only if the active branch is "%s".' % (active_branch, primary_branch))
        return 0

# Project and BuildTask classes

class ArchiveProject(mx.ArchivableProject):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.ArchivableProject.__init__(self, suite, name, deps, workingSets, theLicense)
        assert 'prefix' in args
        assert 'outputDir' in args

    def output_dir(self):
        return join(self.dir, self.outputDir)

    def archive_prefix(self):
        return self.prefix

    def getResults(self):
        return mx.ArchivableProject.walk(self.output_dir())

class LicensesProject(ArchiveProject):
    license_files = ['BSDL', 'COPYING', 'LICENSE.RUBY']

    def getResults(self):
        return [join(_suite.dir, f) for f in self.license_files]

# Commands

def extractArguments(cli_args):
    vmArgs = []
    rubyArgs = []
    classpath = []
    print_command = False
    classic = False

    jruby_opts = os.environ.get('JRUBY_OPTS')
    if jruby_opts:
        jruby_opts = jruby_opts.split(' ')

    for args in [jruby_opts, cli_args]:
        while args:
            arg = args.pop(0)
            if arg == '-X+T':
                classic = False
            elif arg == '-Xclassic':
                classic = True
            elif arg == '-J-cmd':
                print_command = True
            elif arg.startswith('-J-G:+'):
                rewritten = '-Dgraal.'+arg[6:]+'=true'
                mx.warn(arg + ' is deprecated - use -J' + rewritten + ' instead')
                vmArgs.append(rewritten)
            elif arg.startswith('-J-G:-'):
                rewritten = '-Dgraal.'+arg[6:]+'=false'
                mx.warn(arg + ' is deprecated - use -J' + rewritten + ' instead')
                vmArgs.append(rewritten)
            elif arg.startswith('-J-G:'):
                rewritten = '-Dgraal.'+arg[5:]
                mx.warn(arg + ' is deprecated - use -J' + rewritten + ' instead')
                vmArgs.append(rewritten)
            elif arg == '-J-cp' or arg == '-J-classpath':
                cp = args.pop(0)
                if cp[:2] == '-J':
                    cp = cp[2:]
                classpath.append(cp)
            elif arg.startswith('-J-'):
                vmArgs.append(arg[2:])
            elif arg.startswith('-X+') or arg.startswith('-X-'):
                rubyArgs.append(arg)
            elif arg.startswith('-X'):
                vmArgs.append('-Djruby.'+arg[2:])
            else:
                rubyArgs.append(arg)
                rubyArgs.extend(args)
                break
    if classic:
        main_class = "org.jruby.Main"
    else:
        main_class = "org.jruby.truffle.Main"
    return vmArgs, rubyArgs, classpath, print_command, classic, main_class

def setup_jruby_home():
    rubyZip = mx.distribution('RUBY-ZIP').path
    assert exists(rubyZip)
    extractPath = join(_suite.dir, 'mxbuild', 'ruby-zip-extracted')
    if TimeStampFile(extractPath).isOlderThan(rubyZip):
        if exists(extractPath):
            shutil.rmtree(extractPath)
        with tarfile.open(rubyZip, 'r:') as tf:
            tf.extractall(extractPath)
    env = os.environ.copy()
    env['JRUBY_HOME'] = extractPath
    return env

# Print to stderr, mx.log() outputs to stdout
def log(msg):
    print >> sys.stderr, msg

def ruby_command(args):
    """runs Ruby"""
    java_home = os.getenv('JAVA_HOME', '/usr')
    java = os.getenv('JAVACMD', java_home + '/bin/java')
    argv0 = java

    vmArgs, rubyArgs, user_classpath, print_command, classic, main_class = extractArguments(args)
    classpath = mx.classpath(['TRUFFLE_API', 'RUBY']).split(':')
    truffle_api, classpath = classpath[0], classpath[1:]
    assert os.path.basename(truffle_api) == "truffle-api.jar"
    # Give precedence to graal classpath and VM options
    classpath = user_classpath + classpath
    vmArgs = vmArgs + [
        # '-Xss2048k',
        '-Xbootclasspath/a:' + truffle_api,
        '-cp', ':'.join(classpath),
        main_class
    ]
    if not classic:
        vmArgs = vmArgs + ['-X+T']
    allArgs = vmArgs + rubyArgs

    env = setup_jruby_home()

    if print_command:
        if mx.get_opts().verbose:
            log('Environment variables:')
            for key in sorted(env.keys()):
                log(key + '=' + env[key])
        log(java + ' ' + ' '.join(map(pipes.quote, allArgs)))
    return os.execve(java, [argv0] + allArgs, env)

def ruby_tck(args):
    env = setup_jruby_home()
    os.environ["JRUBY_HOME"] = env["JRUBY_HOME"]
    mx_unittest.unittest(['--verbose', '--suite', 'jruby'])

mx.update_commands(_suite, {
    'ruby' : [ruby_command, '[ruby args|@VM options]'],
    'rubytck': [ruby_tck, ''],
    'deploy-binary-if-truffle-head': [deploy_binary_if_truffle_head, ''],
})
