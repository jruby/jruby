# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

import sys
import os
import subprocess
import pipes
import shutil
import json
import time
import tarfile
import zipfile
from threading import Thread
from os.path import join, exists, isdir

import mx
import mx_benchmark
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

def unittest_use_distribution_jars(config):
    """use the distribution jars instead of the class files"""
    vmArgs, mainClass, mainClassArgs = config
    cpIndex, _ = mx.find_classpath_arg(vmArgs)
    junitCP = [mx.classpath("com.oracle.mxtool.junit")]
    rubyCP = [mx.classpath(mx.distribution(distr)) for distr in rubyDists]
    vmArgs[cpIndex] = ":".join(junitCP + rubyCP)
    return (vmArgs, mainClass, mainClassArgs)

mx_unittest.add_config_participant(unittest_use_distribution_jars)

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

def mavenSetup():
    buildPack = join(_suite.dir, 'jruby-build-pack/maven')
    mavenDir = buildPack if isdir(buildPack) else join(_suite.dir, 'mxbuild/mvn')
    maven_repo_arg = '-Dmaven.repo.local=' + mavenDir
    env = os.environ.copy()
    env['JRUBY_BUILD_MORE_QUIET'] = 'true'
    # HACK: since the maven executable plugin does not configure the
    # java executable that is used we unfortunately need to prepend it to the PATH
    javaHome = os.getenv('JAVA_HOME')
    if javaHome:
        env["PATH"] = javaHome + '/bin' + os.pathsep + env["PATH"]
        mx.logv('Setting PATH to {}'.format(os.environ["PATH"]))
    mx.run(['java', '-version'], env=env)
    return maven_repo_arg, env

class JRubyCoreMavenProject(mx.MavenProject):
    def getBuildTask(self, args):
        return JRubyCoreBuildTask(self, args, 1)

    def getResults(self):
        return None

    def get_source_path(self, resolve):
        with open(join(_suite.dir, 'VERSION')) as f:
            version = f.readline().strip()
        return join(_suite.dir, 'core/target/jruby-core-' + version + '-shaded-sources.jar')

class JRubyCoreBuildTask(mx.BuildTask):
    def __str__(self):
        return 'Building {} with Maven'.format(self.subject)

    def newestOutput(self):
        return TimeStampFile(join(_suite.dir, self.subject.jar))

    def needsBuild(self, newestInput):
        sup = mx.BuildTask.needsBuild(self, newestInput)
        if sup[0]:
            return sup

        jar = self.newestOutput()

        if not jar.exists():
            return (True, 'no jar yet')

        jni_libs = join(_suite.dir, 'lib/jni')
        if not exists(jni_libs) or not os.listdir(jni_libs):
            return (True, jni_libs)

        bundler = join(_suite.dir, 'lib/ruby/gems/shared/gems/bundler-1.10.6')
        if not exists(bundler) or not os.listdir(bundler):
            return (True, bundler)

        for watched in self.subject.watch:
            watched = join(_suite.dir, watched)
            if not exists(watched):
                return (True, watched + ' does not exist')
            elif os.path.isfile(watched) and TimeStampFile(watched).isNewerThan(jar):
                return (True, watched + ' is newer than the jar')
            else:
                for root, _, files in os.walk(watched):
                    for name in files:
                        source = join(root, name)
                        if TimeStampFile(source).isNewerThan(jar):
                            return (True, source + ' is newer than the jar')

        return (False, 'all files are up to date')

    def build(self):
        cwd = _suite.dir
        maven_repo_arg, env = mavenSetup()
        mx.log("Building jruby-core with Maven")
        mx.run_maven(['-q', '-DskipTests', maven_repo_arg, '-Dcreate.sources.jar', '-pl', 'core,lib'], cwd=cwd, env=env)
        # Install Bundler
        gem_home = join(_suite.dir, 'lib', 'ruby', 'gems', 'shared')
        env['GEM_HOME'] = gem_home
        env['GEM_PATH'] = gem_home
        mx.run(['bin/jruby', 'bin/gem', 'install', 'bundler', '-v', '1.10.6'], cwd=cwd, env=env)

    def clean(self, forBuild=False):
        if forBuild:
            return
        mx.run_maven(['-q', 'clean'], nonZeroIsFatal=False, cwd=_suite.dir)
        jar = self.newestOutput()
        if jar.exists():
            os.remove(jar.path)

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
                pass # Just drop it
            elif arg == '-Xclassic':
                classic = True
            elif arg == '-J-cmd':
                print_command = True
            elif arg.startswith('-J-G:+'):
                vmArgs.append('-Dgraal.'+arg[6:]+'=true')
            elif arg.startswith('-J-G:-'):
                vmArgs.append('-Dgraal.'+arg[6:]+'=false')
            elif arg.startswith('-J-G:'):
                vmArgs.append('-Dgraal.'+arg[5:])
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
    return vmArgs, rubyArgs, classpath, print_command, classic

def extractTarball(file, target_dir):
    if file.endswith('tar'):
        with tarfile.open(file, 'r:') as tf:
            tf.extractall(target_dir)
    elif file.endswith('jar') or file.endswith('zip'):
        with zipfile.ZipFile(file, "r") as zf:
            zf.extractall(target_dir)
    else:
        mx.abort('Unsupported compressed file ' + file)

def setup_jruby_home():
    rubyZip = mx.distribution('RUBY-ZIP').path
    assert exists(rubyZip)
    extractPath = join(_suite.dir, 'mxbuild', 'ruby-zip-extracted')
    if TimeStampFile(extractPath).isOlderThan(rubyZip):
        if exists(extractPath):
            shutil.rmtree(extractPath)
        extractTarball(rubyZip, extractPath)
    env = os.environ.copy()
    env['JRUBY_HOME'] = extractPath
    return env

def log(msg):
    print >> sys.stderr, msg

def ruby_command(args):
    """runs Ruby"""
    java_home = os.getenv('JAVA_HOME', '/usr')
    java = os.getenv('JAVACMD', java_home + '/bin/java')
    argv0 = java

    vmArgs, rubyArgs, user_classpath, print_command, classic = extractArguments(args)
    classpath = mx.classpath(['TRUFFLE_API', 'RUBY']).split(':')
    truffle_api, classpath = classpath[0], classpath[1:]
    classpath += user_classpath
    assert os.path.basename(truffle_api) == "truffle-api.jar"
    vmArgs = [
        # '-Xss2048k',
        '-Xbootclasspath/a:' + truffle_api,
        '-cp', ':'.join(classpath),
    ] + vmArgs
    vmArgs = vmArgs + ['org.jruby.Main']
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

# Utilities

def jt(args, suite=None, nonZeroIsFatal=True, out=None, err=None, timeout=None, env=None, cwd=None):
    rubyDir = _suite.dir
    jt = join(rubyDir, 'tool', 'jt.rb')
    return mx.run(['ruby', jt] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, timeout=timeout, env=env, cwd=cwd)

FNULL = open(os.devnull, 'w')

class BackgroundServerTask:
    def __init__(self, args):
        self.args = args

    def __enter__(self):
        preexec_fn, creationflags = mx._get_new_progress_group_args()
        if mx._opts.verbose:
            mx.log(' '.join(['(background)'] + map(pipes.quote, self.args)))
        self.process = subprocess.Popen(self.args, preexec_fn=preexec_fn, creationflags=creationflags, stdout=FNULL, stderr=FNULL)
        mx._addSubprocess(self.process, self.args)

    def __exit__(self, type, value, traceback):
        self.process.kill()
        self.process.wait()

    def is_running(self):
        return self.process.poll() is None

class BackgroundJT(BackgroundServerTask):
    def __init__(self, args):
        rubyDir = _suite.dir
        jt = join(rubyDir, 'tool', 'jt.rb')
        BackgroundServerTask.__init__(self, ['ruby', jt] + args)
