import mx
import os
from os.path import join, exists
import subprocess

_suite = mx.suite('jruby')


def build(args):
    mx.log('Building JRuby')

    rubyDir = _suite.dir

    # HACK: since the maven executable plugin does not configure the
    # java executable that is used we unfortunately need to append it to the PATH
    javaHome = os.getenv('JAVA_HOME')
    if javaHome:
        os.environ["PATH"] = os.environ["JAVA_HOME"] + '/bin' + os.pathsep + os.environ["PATH"]

    mx.logv('Setting PATH to {}'.format(os.environ["PATH"]))
    mx.logv('Calling java -version')
    mx.run(['java', '-version'])

    def apply_to_file(filename, function):
        contents = open(filename).read()
        contents = function(contents)
        open(filename, 'w').write(contents)

#    # Repository and commit of JRuby we want to build
#    commit = open(_rubyVersionFile()).read().strip()

    # Truffle version

    truffle = mx.suite('truffle')
    truffle_commit = subprocess.check_output(
        ['git', 'log', '-n', '1', '--format=%H'], cwd=truffle.dir).strip()

    mx.run_mx(['build'], suite=truffle)
    mx.run_mx(['maven-install'], suite=truffle)

    # Build jruby-truffle and

    mx.run_maven(['--version'], nonZeroIsFatal=False, cwd=rubyDir)

    mx.run_maven(['-DskipTests', '-Dtruffle.version=' + truffle_commit], cwd=rubyDir)
    mx.run_maven(['-Pcomplete', '-DskipTests', '-Dtruffle.version=' + truffle_commit], cwd=rubyDir)
#    mx.run(['zip', '-d', 'maven/jruby-complete/target/jruby-complete-graal-vm.jar', 'META-INF/jruby.home/lib/*'], cwd=rubyDir)
    mx.run(['bin/jruby', 'bin/gem', 'install', 'bundler', '-v', '1.10.6'], cwd=rubyDir)

def clean(args):
    rubyDir = _suite.dir
    mx.run_maven(['clean'], nonZeroIsFatal=False, cwd=rubyDir)


mx.update_commands(_suite, {
    'build': [build, ''],
    'clean': [clean, ''],
})