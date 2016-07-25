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
from threading import Thread
from os.path import join, exists

import mx
import mx_benchmark

_suite = mx.suite('jruby')

TimeStampFile = mx.TimeStampFile

# Project and BuildTask classes

class ArchiveProject(mx.Project):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.Project.__init__(self, suite, name, "", [], deps, workingSets, _suite.dir, theLicense)
        assert 'prefix' in args

    def getBuildTask(self, args):
        return ArchiveBuildTask(self, args)

    def isNativeProject(self):
        return True # for archiving purposes

    def getOutput(self):
        return '.'

    def getResults(self):
        result = []
        output_dir = join(_suite.dir, self.prefix)
        for root, _, files in os.walk(output_dir):
            for name in files:
                path = join(root, name)
                result.append(path)
        return result

class ArchiveBuildTask(mx.BuildTask):
    def __init__(self, project, args):
        mx.BuildTask.__init__(self, project, args, 1)

    def __str__(self):
        return 'Archive {}'.format(self.subject)

    def needsBuild(self, newestInput):
        return (False, 'Files are already on disk')

    def newestOutput(self):
        return TimeStampFile.newest(self.subject.getResults())

    def build(self):
        pass

    def clean(self, forBuild=False):
        pass

class LicensesProject(ArchiveProject):
    license_files = ['BSDL', 'COPYING', 'LICENSE.RUBY']

    def getResults(self):
        return [join(_suite.dir, f) for f in self.license_files]

class AntlrProject(mx.Project):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.Project.__init__(self, suite, name, "", [], deps, workingSets, _suite.dir, theLicense)
        assert 'outputDir' in args
        assert 'sourceDir' in args
        assert 'grammars' in args
        self.doNotArchive = True

    def getBuildTask(self, args):
        return AntlrBuildTask(self, args)

class AntlrBuildTask(mx.BuildTask):
    def __init__(self, project, args):
        mx.BuildTask.__init__(self, project, args, 1)

    def __str__(self):
        return 'Generate Antlr grammar for {}'.format(self.subject)

    def needsBuild(self, newestInput):
        sup = mx.BuildTask.needsBuild(self, newestInput)
        if sup[0]:
            return sup
        newestOutput = self.newestOutput()
        if not newestOutput:
            return (True, "no class files yet")
        src = join(_suite.dir, self.subject.sourceDir)
        for root, _, files in os.walk(src):
            for name in files:
                file = TimeStampFile(join(root, name))
                if file.isNewerThan(newestOutput):
                    return (True, file)
        return (False, 'all files are up to date')

    def newestOutput(self):
        out = join(_suite.dir, self.subject.outputDir)
        newest = None
        for root, _, files in os.walk(out):
            for name in files:
                file = TimeStampFile(join(root, name))
                if not newest or file.isNewerThan(newest):
                    newest = file
        return newest

    def build(self):
        antlr4 = None
        for lib in _suite.libs:
            if lib.name == "ANTLR4_MAIN":
                antlr4 = lib
        assert antlr4

        antlr4jar = antlr4.classpath_repr()
        out = join(_suite.dir, self.subject.outputDir)
        src = join(_suite.dir, self.subject.sourceDir)
        for grammar in self.subject.grammars:
            pkg = os.path.dirname(grammar).replace('/', '.')
            mx.run_java(['-jar', antlr4jar, '-o', out, '-package', pkg, grammar], cwd=src)

    def clean(self, forBuild=False):
        pass

# Call to Maven to build everything

class JRubyCoreMavenProject(mx.MavenProject):
    def getBuildTask(self, args):
        return MavenBuildTask(self, args)

class MavenBuildTask(ArchiveBuildTask):
    def __str__(self):
        return 'Building {} with Maven'.format(self.subject)

    def needsBuild(self, newestInput):
        sup = mx.BuildTask.needsBuild(self, newestInput)
        if sup[0]:
            return sup

        jar = self.newestOutput()

        if not jar.exists():
            return (True, 'no jar yet')

        jni_libs = join(_suite.dir, 'lib', 'jni')
        if not exists(jni_libs) or not os.listdir(jni_libs):
            return (True, jni_libs)

        for watched in self.subject.watch:
            if not exists(watched):
                return (True, watched)
            elif os.path.isfile(watched) and TimeStampFile(watched).isNewerThan(jar):
                return (True, watched)
            else:
                for root, _, files in os.walk(watched):
                    for name in files:
                        source = join(root, name)
                        if TimeStampFile(source).isNewerThan(jar):
                            return (True, source)

        return (False, 'all files are up to date')

    def newestOutput(self):
        return TimeStampFile(self.subject.jar)

    def build(self):
        mx.log('...perform build of {}'.format(self.subject))

        cwd = _suite.dir
        mavenDir = join(cwd, 'mxbuild', 'mvn')

        # HACK: since the maven executable plugin does not configure the
        # java executable that is used we unfortunately need to append it to the PATH
        javaHome = os.getenv('JAVA_HOME')
        if javaHome:
            os.environ["PATH"] = os.environ["JAVA_HOME"] + '/bin' + os.pathsep + os.environ["PATH"]
            mx.logv('Setting PATH to {}'.format(os.environ["PATH"]))

        mx.run(['java', '-version'])

        # Truffle version
        truffle = mx.suite('truffle')
        truffle_commit = truffle.vc.parent(truffle.dir)
        maven_version_arg = '-Dtruffle.version=' + truffle_commit
        maven_repo_arg = '-Dmaven.repo.local=' + mavenDir

        for name in ['truffle-api', 'truffle-debug', 'truffle-dsl-processor', 'truffle-tck']:
            jar_path = join(mavenDir, 'com', 'oracle', 'truffle', name, truffle_commit, "%s-%s.jar" % (name, truffle_commit))
            if not exists(jar_path):
                mx.run_mx(['maven-install', '--repo', mavenDir, '--only', 'TRUFFLE_API,TRUFFLE_DEBUG,TRUFFLE_DSL_PROCESSOR,TRUFFLE_TCK'], suite=truffle)

        # Build jruby-truffle
        env = os.environ.copy()
        env['JRUBY_BUILD_MORE_QUIET'] = 'true'

        mx.log("Building jruby-core with Maven")
        mx.run_maven(['-q', '-DskipTests', maven_version_arg, maven_repo_arg, '-pl', 'core'], cwd=cwd, env=env)

        # mx.run(['bin/jruby', 'bin/gem', 'install', 'bundler', '-v', '1.10.6'], cwd=cwd)
    
        mx.log('...finished build of {}'.format(self.subject))

    def clean(self, forBuild=False):
        if forBuild:
            return
        mx.run_maven(['-q', 'clean'], nonZeroIsFatal=False, cwd=_suite.dir)
        jar = self.newestOutput()
        if jar.exists():
            os.remove(jar.path)

# Utilities

def jt(args, suite=None, nonZeroIsFatal=True, out=None, err=None, timeout=None, env=None, cwd=None):
    rubyDir = _suite.dir
    jt = join(rubyDir, 'tool', 'jt.rb')
    return mx.run(['ruby', jt] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, timeout=timeout, env=env, cwd=cwd)

class BackgroundServerTask:
    FNULL = open(os.devnull, 'w')

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

##############
# BENCHMARKS #
##############

class RubyBenchmarkSuite(mx_benchmark.BenchmarkSuite):
    def group(self):
        return 'Graal'

    def subgroup(self):
        return 'jrubytruffle'

    def vmArgs(self, bmSuiteArgs):
        return mx_benchmark.splitArgs(bmSuiteArgs, bmSuiteArgs)[0]

    def runArgs(self, bmSuiteArgs):
        return mx_benchmark.splitArgs(bmSuiteArgs, bmSuiteArgs)[1]
    
    def default_benchmarks(self):
        return self.benchmarks()

    def run(self, benchmarks, bmSuiteArgs):
        def fixUpResult(result):
            result.update({
                'host-vm': os.environ.get('HOST_VM', 'host-vm'),
                'host-vm-config': os.environ.get('HOST_VM_CONFIG', 'host-vm-config'),
                'guest-vm': os.environ.get('GUEST_VM', 'guest-vm'),
                'guest-vm-config': os.environ.get('GUEST_VM_CONFIG', 'guest-vm-config')
            })
            return result
        
        return [fixUpResult(r) for b in benchmarks or self.default_benchmarks() for r in self.runBenchmark(b, bmSuiteArgs)]
    
    def runBenchmark(self, benchmark, bmSuiteArgs):
        raise NotImplementedError()

metrics_benchmarks = {
    'hello': ['-e', "puts 'hello'"],
    'compile-mandelbrot': ['--graal', 'bench/truffle/metrics/mandelbrot.rb']
}

default_metrics_benchmarks = ['hello']

class MetricsBenchmarkSuite(RubyBenchmarkSuite):
    def benchmarks(self):
        return metrics_benchmarks.keys()
    
    def default_benchmarks(self):
        return default_metrics_benchmarks

class AllocationBenchmarkSuite(MetricsBenchmarkSuite):
    def name(self):
        return 'allocation'

    def runBenchmark(self, benchmark, bmSuiteArgs):
        out = mx.OutputCapture()

        jt(['metrics', 'alloc', '--json'] + metrics_benchmarks[benchmark] + bmSuiteArgs, out=out)
        
        data = json.loads(out.data)
        
        return [{
            'benchmark': benchmark,
            'metric.name': 'memory',
            'metric.value': sample,
            'metric.unit': 'B',
            'metric.better': 'lower',
            'metric.iteration': n,
            'extra.metric.human': '%d/%d %s' % (n, len(data['samples']), data['human'])
        } for n, sample in enumerate(data['samples'])]

class MinHeapBenchmarkSuite(MetricsBenchmarkSuite):
    def name(self):
        return 'minheap'

    def runBenchmark(self, benchmark, bmSuiteArgs):
        out = mx.OutputCapture()

        jt(['metrics', 'minheap', '--json'] + metrics_benchmarks[benchmark] + bmSuiteArgs, out=out)
        
        data = json.loads(out.data)
        
        return [{
            'benchmark': benchmark,
            'metric.name': 'memory',
            'metric.value': data['min'],
            'metric.unit': 'MiB',
            'metric.better': 'lower',
            'extra.metric.human': data['human']
        }]

class TimeBenchmarkSuite(MetricsBenchmarkSuite):
    def name(self):
        return 'time'

    def runBenchmark(self, benchmark, bmSuiteArgs):
        out = mx.OutputCapture()

        jt(['metrics', 'time', '--json'] + metrics_benchmarks[benchmark] + bmSuiteArgs, out=out)
        
        data = json.loads(out.data)

        return [{
            'benchmark': benchmark,
            'extra.metric.region': region,
            'metric.name': 'time',
            'metric.value': sample,
            'metric.unit': 's',
            'metric.better': 'lower',
            'metric.iteration': n,
            'extra.metric.human': '%d/%d %s' % (n, len(region_data['samples']), region_data['human'])
        } for region, region_data in data.items() for n, sample in enumerate(region_data['samples'])]

class AllBenchmarksBenchmarkSuite(RubyBenchmarkSuite):
    def benchmarks(self):
        raise NotImplementedError()
    
    def name(self):
        raise NotImplementedError()
    
    def time(self):
        raise NotImplementedError()
    
    def directory(self):
        return self.name()

    def runBenchmark(self, benchmark, bmSuiteArgs):
        arguments = ['benchmark']
        if 'MX_NO_GRAAL' in os.environ:
            arguments.extend(['--no-graal'])
        arguments.extend(['--simple', '--elapsed'])
        arguments.extend(['--time', str(self.time())])
        if ':' in benchmark:
            benchmark_file, benchmark_name = benchmark.split(':')
            benchmark_names = [benchmark_name]
        else:
            benchmark_file = benchmark
            benchmark_names = []
        if '.rb' in benchmark_file:
            arguments.extend([benchmark_file])
        else:
            arguments.extend([self.directory() + '/' + benchmark_file + '.rb'])
        arguments.extend(benchmark_names)
        arguments.extend(bmSuiteArgs)
        out = mx.OutputCapture()
        
        if jt(arguments, out=out, nonZeroIsFatal=False) == 0:
            lines = out.data.split('\n')[1:-1]
            
            if lines[-1] == 'optimised away':
                data = [float(s) for s in lines[0:-2]]
                elapsed = [d for n, d in enumerate(data) if n % 2 == 0]
                samples = [d for n, d in enumerate(data) if n % 2 == 1]
                
                return [{
                    'benchmark': benchmark,
                    'metric.name': 'throughput',
                    'metric.value': sample,
                    'metric.unit': 'op/s',
                    'metric.better': 'higher',
                    'metric.iteration': n,
                    'extra.metric.warmedup': 'false',
                    'extra.metric.elapsed-num': e,
                    'extra.metric.human': 'optimised away'
                } for n, (e, sample) in enumerate(zip(elapsed, samples))] + [{
                    'benchmark': benchmark,
                    'metric.name': 'throughput',
                    'metric.value': 2147483647, # arbitrary high value (--simple won't run more than this many ips)
                    'metric.unit': 'op/s',
                    'metric.better': 'higher',
                    'metric.iteration': len(samples),
                    'extra.metric.warmedup': 'true',
                    'extra.metric.elapsed-num': elapsed[-1] + 2.0, # just put the data point beyond the last one a bit
                    'extra.metric.human': 'optimised away',
                    'extra.error': 'optimised away'
                }]
            else:
                data = [float(s) for s in lines]
                elapsed = [d for n, d in enumerate(data) if n % 2 == 0]
                samples = [d for n, d in enumerate(data) if n % 2 == 1]
                
                if len(samples) > 1:
                    warmed_up_samples = [sample for n, sample in enumerate(samples) if n / float(len(samples)) >= 0.5]
                else:
                    warmed_up_samples = samples
                    
                warmed_up_mean = sum(warmed_up_samples) / float(len(warmed_up_samples))
                
                return [{
                    'benchmark': benchmark,
                    'metric.name': 'throughput',
                    'metric.value': sample,
                    'metric.unit': 'op/s',
                    'metric.better': 'higher',
                    'metric.iteration': n,
                    'extra.metric.warmedup': 'true' if n / float(len(samples)) >= 0.5 else 'false',
                    'extra.metric.elapsed-num': e,
                    'extra.metric.human': '%d/%d %f op/s' % (n, len(samples), warmed_up_mean)
                } for n, (e, sample) in enumerate(zip(elapsed, samples))]
        else:
            sys.stderr.write(out.data)
            
            return [{
                'benchmark': benchmark,
                'metric.name': 'throughput',
                'metric.value': 0,
                'metric.unit': 'op/s',
                'metric.better': 'higher',
                'extra.metric.warmedup': 'true',
                'extra.error': 'failed'
            }]

classic_benchmarks = [
    'binary-trees',
    'deltablue',
    'fannkuch',
    'mandelbrot',
    'matrix-multiply',
    'n-body',
    'neural-net',
    'pidigits',
    'red-black',
    'richards',
    'spectral-norm'
]

classic_benchmark_time = 120

class ClassicBenchmarkSuite(AllBenchmarksBenchmarkSuite):
    def name(self):
        return 'classic'
    
    def directory(self):
        return 'classic'
    
    def benchmarks(self):
        return classic_benchmarks
    
    def time(self):
        return classic_benchmark_time

chunky_benchmarks = [
    'chunky-color-r',
    'chunky-color-g',
    'chunky-color-b',
    'chunky-color-a',
    'chunky-color-compose-quick',
    'chunky-canvas-resampling-bilinear',
    'chunky-canvas-resampling-nearest-neighbor',
    'chunky-canvas-resampling-steps-residues',
    'chunky-canvas-resampling-steps',
    'chunky-decode-png-image-pass',
    'chunky-encode-png-image-pass-to-stream',
    'chunky-operations-compose',
    'chunky-operations-replace'
]

chunky_benchmark_time = 120

class ChunkyBenchmarkSuite(AllBenchmarksBenchmarkSuite):
    def name(self):
        return 'chunky'
    
    def directory(self):
        return 'chunky_png'

    def benchmarks(self):
        return chunky_benchmarks
    
    def time(self):
        return chunky_benchmark_time

psd_benchmarks = [
    'psd-color-cmyk-to-rgb',
    'psd-compose-color-burn',
    'psd-compose-color-dodge',
    'psd-compose-darken',
    'psd-compose-difference',
    'psd-compose-exclusion',
    'psd-compose-hard-light',
    'psd-compose-hard-mix',
    'psd-compose-lighten',
    'psd-compose-linear-burn',
    'psd-compose-linear-dodge',
    'psd-compose-linear-light',
    'psd-compose-multiply',
    'psd-compose-normal',
    'psd-compose-overlay',
    'psd-compose-pin-light',
    'psd-compose-screen',
    'psd-compose-soft-light',
    'psd-compose-vivid-light',
    'psd-imageformat-layerraw-parse-raw',
    'psd-imageformat-rle-decode-rle-channel',
    'psd-imagemode-cmyk-combine-cmyk-channel',
    'psd-imagemode-greyscale-combine-greyscale-channel',
    'psd-imagemode-rgb-combine-rgb-channel',
    'psd-renderer-blender-compose',
    'psd-renderer-clippingmask-apply',
    'psd-renderer-mask-apply',
    'psd-util-clamp',
    'psd-util-pad2',
    'psd-util-pad4'
]

psd_benchmark_time = 120

class PSDBenchmarkSuite(AllBenchmarksBenchmarkSuite):
    def name(self):
        return 'psd'
    
    def directory(self):
        return 'psd.rb'

    def benchmarks(self):
        return psd_benchmarks
    
    def time(self):
        return psd_benchmark_time

asciidoctor_benchmarks = [
    'asciidoctor:file-lines',
    'asciidoctor:string-lines',
    'asciidoctor:read-line',
    'asciidoctor:restore-line',
    'asciidoctor:load-string',
    'asciidoctor:load-file',
    'asciidoctor:quote-match',
    'asciidoctor:quote-sub',
    'asciidoctor:join-lines',
    'asciidoctor:convert'
]

asciidoctor_benchmark_time = 120

class AsciidoctorBenchmarkSuite(AllBenchmarksBenchmarkSuite):
    def name(self):
        return 'asciidoctor'

    def benchmarks(self):
        return asciidoctor_benchmarks

    def time(self):
        return asciidoctor_benchmark_time

synthetic_benchmarks = [
    'acid'
]

synthetic_benchmark_time = 120

class SyntheticBenchmarkSuite(AllBenchmarksBenchmarkSuite):
    def name(self):
        return 'synthetic'

    def benchmarks(self):
        return synthetic_benchmarks
    
    def time(self):
        return synthetic_benchmark_time

micro_benchmark_time = 30

class MicroBenchmarkSuite(AllBenchmarksBenchmarkSuite):
    def name(self):
        return 'micro'

    def benchmarks(self):
        out = mx.OutputCapture()
        jt(['where', 'repos', 'all-ruby-benchmarks'], out=out)
        all_ruby_benchmarks = out.data.strip()
        benchmarks = []
        for root, dirs, files in os.walk(join(all_ruby_benchmarks, 'micro')):
            for name in files:
                if name.endswith('.rb'):
                    benchmark_file = join(root, name)[len(all_ruby_benchmarks)+1:]
                    out = mx.OutputCapture()
                    jt(['benchmark', 'list', benchmark_file], out=out)
                    benchmarks.extend([benchmark_file + ':' + b.strip() for b in out.data.split('\n') if len(b.strip()) > 0])
        return benchmarks
    
    def time(self):
        return micro_benchmark_time

server_benchmarks = [
    'tcp-server',
    'webrick'
]

server_benchmark_time = 60 * 4 # Seems unstable otherwise

class ServerBenchmarkSuite(RubyBenchmarkSuite):
    def benchmarks(self):
        return server_benchmarks
    
    def name(self):
        return 'server'

    def runBenchmark(self, benchmark, bmSuiteArgs):
        arguments = ['run', '--exec']
        if 'MX_NO_GRAAL' not in os.environ:
            arguments.extend(['--graal', '-J-G:+TruffleCompilationExceptionsAreFatal'])
        arguments.extend(['all-ruby-benchmarks/servers/' + benchmark + '.rb'])
        
        server = BackgroundJT(arguments)
        
        with server:
            time.sleep(10)
            out = mx.OutputCapture()
            if mx.run(
                    ['ruby', 'all-ruby-benchmarks/servers/harness.rb', str(server_benchmark_time)],
                    out=out,
                    nonZeroIsFatal=False) == 0 and server.is_running():
                samples = [float(s) for s in out.data.split('\n')[0:-1]]
                print samples
                half_samples = len(samples) / 2
                used_samples = samples[len(samples)-half_samples-1:]
                ips = sum(used_samples) / float(len(used_samples))
                
                return [{
                    'benchmark': benchmark,
                    'metric.name': 'throughput',
                    'metric.value': ips,
                    'metric.unit': 'op/s',
                    'metric.better': 'higher',
                    'extra.metric.human': str(used_samples)
                }]
            else:
                sys.stderr.write(out.data)
                
                # TODO CS 24-Jun-16, how can we fail the wider suite?
                return [{
                    'benchmark': benchmark,
                    'metric.name': 'throughput',
                    'metric.value': 0,
                    'metric.unit': 'op/s',
                    'metric.better': 'higher',
                    'extra.error': 'failed'
                }]

mx_benchmark.add_bm_suite(AllocationBenchmarkSuite())
mx_benchmark.add_bm_suite(MinHeapBenchmarkSuite())
mx_benchmark.add_bm_suite(TimeBenchmarkSuite())
mx_benchmark.add_bm_suite(ClassicBenchmarkSuite())
mx_benchmark.add_bm_suite(ChunkyBenchmarkSuite())
mx_benchmark.add_bm_suite(PSDBenchmarkSuite())
mx_benchmark.add_bm_suite(AsciidoctorBenchmarkSuite())
mx_benchmark.add_bm_suite(SyntheticBenchmarkSuite())
mx_benchmark.add_bm_suite(MicroBenchmarkSuite())
mx_benchmark.add_bm_suite(ServerBenchmarkSuite())
