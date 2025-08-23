# frozen_string_literal: true

version = ENV['JRUBY_VERSION'] ||
          File.read(File.join(basedir, '..', 'VERSION')).strip

project 'JRuby Benchmark' do
  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-benchmark'

  properties("polyglot.dump.pom": 'pom.xml',
             "polyglot.dump.readonly": true,
             "maven.build.timestamp.format": 'yyyy-MM-dd',
             "maven.test.skip": 'true',
             "build.date": '${maven.build.timestamp}',
             "main.basedir": '${project.parent.basedir}',
             "jruby.basedir": '${basedir}/..',
             "jmh.version": '1.19')

  IO.foreach(File.join(basedir, '..', 'default.build.properties')) do |line|
    line.chomp!
    # skip comments
    next if line =~ /(^\W*#|^$)/

    # build const name
    name, value = line.split('=', 2)
    properties name => value
  end

  jar 'org.jruby:jruby-core:${project.version}'
  jar 'org.openjdk.jmh:jmh-core:${jmh.version}'
  jar 'org.openjdk.jmh:jmh-generator-annprocess:${jmh.version}'
  jar 'org.openjdk.jmh:jmh-core-benchmarks:${jmh.version}'

  plugin(:compiler,
         encoding: 'utf-8',
         debug: 'true',
         verbose: 'true',
         fork: 'true',
         compilerArgs: { arg: '-J-Xmx1G' },
         showWarnings: 'true',
         showDeprecation: 'true',
         source: ['${base.java.version}', '1.8'],
         target: ['${base.javac.version}', '1.8'],
         useIncrementalCompilation: 'false')

  plugin :shade do
    execute_goals('shade',
                  id: 'create jruby-benchmark.jar',
                  phase: 'package',
                  outputFile: '${project.build.directory}/jruby-benchmark.jar',
                  transformers: [{ :@implementation => 'org.apache.maven.plugins.shade.resource.ManifestResourceTransformer',
                                   :mainClass => 'org.openjdk.jmh.Main' }])
  end
end
