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
import shutil
import json

import mx
import mx_benchmark

_suite = mx.suite('jruby')

def jt(args, suite=None, nonZeroIsFatal=True, out=None, err=None, timeout=None, env=None, cwd=None):
    rubyDir = _suite.dir
    jt = os.path.join(rubyDir, 'tool', 'jt.rb')
    return mx.run(['ruby', jt] + args, nonZeroIsFatal=nonZeroIsFatal, out=out, err=err, timeout=timeout, env=env, cwd=cwd)

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
        
        env = {'JRUBY_BUILD_MORE_QUIET': 'true'}

        mx.run_maven(['-q', '--version', maven_repo_arg], nonZeroIsFatal=False, cwd=rubyDir, env=env)

        mx.log('Building without tests')

        mx.run_maven(['-q', '-DskipTests', maven_version_arg, maven_repo_arg], cwd=rubyDir, env=env)

        mx.log('Building complete version')

        mx.run_maven(['-q', '-Pcomplete', '-DskipTests', maven_version_arg, maven_repo_arg], cwd=rubyDir, env=env)
        mx.run(['zip', '-d', 'maven/jruby-complete/target/jruby-complete-graal-vm.jar', 'META-INF/jruby.home/lib/*'], cwd=rubyDir)
        mx.run(['bin/jruby', 'bin/gem', 'install', 'bundler', '-v', '1.10.6'], cwd=rubyDir)
        mx.log('...finished build of {}'.format(self.subject))

    def clean(self, forBuild=False):
        if forBuild:
            return
        rubyDir = _suite.dir
        mx.run_maven(['-q', 'clean'], nonZeroIsFatal=False, cwd=rubyDir)

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
        if 'MX_BENCHMARK_OPTS' in os.environ:
            arguments.extend(os.environ['MX_BENCHMARK_OPTS'].split(' '))
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
            data = [float(s) for s in out.data.split('\n')[1:-1]]
            elapsed = [d for n, d in enumerate(data) if n % 2 == 0]
            samples = [d for n, d in enumerate(data) if n % 2 == 1]
            
            warmed_up_samples = [sample for n, sample in enumerate(samples) if n / float(len(samples)) >= 0.5]
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
                'extra.metric.human': '%d/%d %fs' % (n, len(samples), warmed_up_mean)
            } for n, (e, sample) in enumerate(zip(elapsed, samples))]
        else:
            sys.stderr.write(out.data)
            
            # TODO CS 24-Jun-16, how can we fail the wider suite?
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
        for root, dirs, files in os.walk(os.path.join(all_ruby_benchmarks, 'micro')):
            for name in files:
                if name.endswith('.rb'):
                    benchmark_file = os.path.join(root, name)[len(all_ruby_benchmarks)+1:]
                    out = mx.OutputCapture()
                    if jt(['benchmark', 'list', benchmark_file], out=out):
                        benchmarks.extend([benchmark_file + ':' + b.strip() for b in out.data.split('\n') if len(b.strip()) > 0])
                    else:
                        sys.stderr.write(out.data)
        return benchmarks
    
    def time(self):
        return micro_benchmark_time

mx_benchmark.add_bm_suite(AllocationBenchmarkSuite())
mx_benchmark.add_bm_suite(MinHeapBenchmarkSuite())
mx_benchmark.add_bm_suite(TimeBenchmarkSuite())
mx_benchmark.add_bm_suite(ClassicBenchmarkSuite())
mx_benchmark.add_bm_suite(ChunkyBenchmarkSuite())
mx_benchmark.add_bm_suite(PSDBenchmarkSuite())
mx_benchmark.add_bm_suite(SyntheticBenchmarkSuite())
mx_benchmark.add_bm_suite(MicroBenchmarkSuite())
