import os
import subprocess
import shutil
import json

import mx
import mx_benchmark

_suite = mx.suite('jruby')

def jt(args, suite=None, nonZeroIsFatal=True, out=None, err=None, timeout=None, env=None, cwd=None):
    rubyDir = _suite.dir
    jt = os.path.join(rubyDir, 'tool', 'jt.rb')
    mx.run(['ruby', jt] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, timeout=timeout, env=env, cwd=cwd)

class MavenProject(mx.Project):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.Project.__init__(self, suite, name, "", [], deps, workingSets, _suite.dir, theLicense)
        self.javaCompliance = "1.7"
        self.build = hasattr(args, 'build')
        self.prefix = args['prefix']

    def source_dirs(self):
        return []

    def output_dir(self):
        dir = os.path.join(_suite.dir, self.prefix)
        return dir.rstrip('/')

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

    def annotation_processors(self):
        return []

    def find_classes_with_matching_source_line(self, pkgRoot, function, includeInnerClasses=False):
        return dict()

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
        mavenDir = os.path.join(rubyDir, 'mxbuild', 'mvn')

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
        maven_version_arg = '-Dtruffle.version=' + truffle_commit
        maven_repo_arg = '-Dmaven.repo.local=' + mavenDir

        mx.run_mx(['maven-install', '--repo', mavenDir], suite=truffle)

        open(os.path.join(rubyDir, 'VERSION'), 'w').write('graal-vm\n')

        # Build jruby-truffle

        mx.run_maven(['--version', maven_repo_arg], nonZeroIsFatal=False, cwd=rubyDir)

        mx.log('Building without tests')

        mx.run_maven(['-DskipTests', maven_version_arg, maven_repo_arg], cwd=rubyDir)

        mx.log('Building complete version')

        mx.run_maven(['-Pcomplete', '-DskipTests', maven_version_arg, maven_repo_arg], cwd=rubyDir)
        mx.run(['zip', '-d', 'maven/jruby-complete/target/jruby-complete-graal-vm.jar', 'META-INF/jruby.home/lib/*'], cwd=rubyDir)
        mx.run(['bin/jruby', 'bin/gem', 'install', 'bundler', '-v', '1.10.6'], cwd=rubyDir)
        mx.log('...finished build of {}'.format(self.subject))

    def clean(self, forBuild=False):
        if forBuild:
            return
        rubyDir = _suite.dir
        mx.run_maven(['clean'], nonZeroIsFatal=False, cwd=rubyDir)

class RubyBenchmarkSuite(mx_benchmark.BenchmarkSuite):
    def group(self):
        return 'Graal'

    def subgroup(self):
        return 'jrubytruffle'

    def vmArgs(self, bmSuiteArgs):
        return mx_benchmark.splitArgs(bmSuiteArgs, bmSuiteArgs)[0]

    def runArgs(self, bmSuiteArgs):
        return mx_benchmark.splitArgs(bmSuiteArgs, bmSuiteArgs)[1]

    def run(self, benchmarks, bmSuiteArgs):
        def fixUpResult(result):
            result.update({
                'host-vm': os.environ['HOST_VM'],
                'host-vm-config': os.environ['HOST_VM_CONFIG'],
                'guest-vm': os.environ['GUEST_VM'],
                'guest-vm-config': os.environ['GUEST_VM_CONFIG']
            })
            return result
        
        return [fixUpResult(self.runBenchmark(b, bmSuiteArgs)) for b in benchmarks or self.benchmarks()]
    
    def runBenchmark(self, benchmark, bmSuiteArgs):
        raise NotImplementedError()

allocation_benchmarks = {
    'hello': ['-e', "puts 'hello'"]
}

class AllocationBenchmarkSuite(RubyBenchmarkSuite):
    def name(self):
        return 'allocation'

    def benchmarks(self):
        return allocation_benchmarks.keys()

    def runBenchmark(self, benchmark, bmSuiteArgs):
        out = mx.OutputCapture()
        
        options = []
        
        jt(['metrics', 'alloc', '--json'] + allocation_benchmarks[benchmark] + bmSuiteArgs, out=out)
        
        data = json.loads(out.data)
        
        result = {
            'benchmark': benchmark,
            'metric.name': 'memory',
            'metric.value': data['median'],
            'metric.unit': 'B',
            'metric.better': 'lower',
            'extra.metric.error-num': data['error'],
            'extra.metric.human': data['human']
        }
        
        return result

mx_benchmark.add_bm_suite(AllocationBenchmarkSuite())
