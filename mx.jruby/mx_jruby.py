import mx
import os
from os.path import join, exists
import subprocess
import shutil

_suite = mx.suite('jruby')

class MavenProject(mx.Project):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.Project.__init__(self, suite, name, "", [], deps, workingSets, _suite.dir, theLicense)
        self.javaCompliance = "1.7"
        self.build = hasattr(args, 'build')
        self.prefix = args['prefix']

    def source_dirs(self):
        return []

    def output_dir(self):
        return os.path.join(_suite.dir, self.prefix)

    def source_gen_dir(self):
        return None

    def getOutput(self, replaceVar=False):
        return os.path.join(_suite.dir, "target")

    def getResults(self, replaceVar=False):
        return mx.Project.getResults(self, replaceVar=replaceVar)

    def getBuildTask(self, args):
        return MavenBuildTask(self, args, None, None)

    def isJavaProject(self):
        return True

    def archive_prefix(self):
        return self.prefix


class MavenBuildTask(mx.BuildTask):
    def __init__(self, project, args, vmbuild, vm):
        mx.BuildTask.__init__(self, project, args, 1)
        self.vm = vm
        self.vmbuild = vmbuild

    def __str__(self):
        return 'Building Maven for {}'.format(self.subject)

    def needsBuild(self, newestInput):
        return (True, 'Let us re-build everytime')

    def newestOutput(self):
        return None

    def build(self):
        if not self.subject.build:
            mx.log("...skip build of {}".format(self.subject))
            return
        mx.log('...perform build of {}'.format(self.subject))

        rubyDir = _suite.dir

        # HACK: since the maven executable plugin does not configure the
        # java executable that is used we unfortunately need to append it to the PATH
        javaHome = os.getenv('JAVA_HOME')
        if javaHome:
            os.environ["PATH"] = os.environ["JAVA_HOME"] + '/bin' + os.pathsep + os.environ["PATH"]

        mx.logv('Setting PATH to {}'.format(os.environ["PATH"]))
        mx.logv('Calling java -version')
        mx.run(['java', '-version'])

        # Truffle version

        truffle = mx.suite('truffle')
        truffle_commit = truffle.vc.parent(truffle.dir)

        mx.run_mx(['maven-install'], suite=truffle)

        # Ruby version
        def apply_to_file(filename, function):
            contents = open(filename).read()
            contents = function(contents)
            open(filename, 'w').write(contents)
        apply_to_file(join(rubyDir, 'VERSION'), lambda version: 'graal-vm\n')

        # Build jruby-truffle and

        mx.run(['find', '.'], nonZeroIsFatal=False, cwd=rubyDir)
        mx.run_maven(['--version'], nonZeroIsFatal=False, cwd=rubyDir)

        mx.log('Building without tests')

        mx.run_maven(['-DskipTests', '-X', '-Dtruffle.version=' + truffle_commit], cwd=rubyDir)

        mx.log('Building complete version')

        mx.run_maven(['-Pcomplete', '-X', '-DskipTests', '-Dtruffle.version=' + truffle_commit], cwd=rubyDir)
        mx.run(['zip', '-d', 'maven/jruby-complete/target/jruby-complete-graal-vm.jar', 'META-INF/jruby.home/lib/*'], cwd=rubyDir)
        mx.run(['bin/jruby', 'bin/gem', 'install', 'bundler', '-v', '1.10.6'], cwd=rubyDir)
#        shutil.rmtree(os.path.join(_suite.dir, "lib", "target"), True)
#        shutil.rmtree(os.path.join(_suite.dir, 'lib', 'lib', 'jni'), True)
#        shutil.copytree(os.path.join(_suite.dir, 'lib', 'jni'), os.path.join(_suite.dir, 'lib', 'lib', 'jni'))
#        shutil.rmtree(os.path.join(_suite.dir, 'lib', 'jni'), True)

    def clean(self, forBuild=False):
        if forBuild:
            return
        rubyDir = _suite.dir
        mx.run_maven(['clean'], nonZeroIsFatal=False, cwd=rubyDir)

