#!/usr/bin/env ruby
# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved.
# This code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# A workflow tool for JRuby+Truffle development

# Recommended: function jt { ruby tool/jt.rb "$@"; }

require 'fileutils'
require 'digest/sha1'

GRAALVM_VERSION = "0.10"

JRUBY_DIR = File.expand_path('../..', __FILE__)

JDEBUG_PORT = 51819
JDEBUG = "-J-agentlib:jdwp=transport=dt_socket,server=y,address=#{JDEBUG_PORT},suspend=y"
JDEBUG_TEST = "-Dmaven.surefire.debug=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=#{JDEBUG_PORT} -Xnoagent -Djava.compiler=NONE"
JEXCEPTION = "-Xtruffle.exceptions.print_java=true"
METRICS_REPS = 10

# wait for sub-processes to handle the interrupt
trap(:INT) {}

module Utilities

  def self.truffle_version
    File.foreach("#{JRUBY_DIR}/truffle/pom.rb") do |line|
      if /\btruffle_version = '(\d+\.\d+(?:-SNAPSHOT)?)'/ =~ line
        break $1
      end
    end
  end

  def self.find_graal
    graal_locations = [
      ENV['GRAAL_BIN'],
      ENV["GRAAL_BIN_#{mangle_for_env(git_branch)}"],
      "GraalVM-#{GRAALVM_VERSION}/jre/bin/javao",
      "../GraalVM-#{GRAALVM_VERSION}/jre/bin/javao",
      "../../GraalVM-#{GRAALVM_VERSION}/jre/bin/javao",
    ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

    not_found = -> {
      raise "couldn't find graal - download it as described in https://github.com/jruby/jruby/wiki/Downloading-GraalVM and extract it into the JRuby repository or parent directory"
    }

    graal_locations.find(not_found) do |location|
      File.executable?(location)
    end
  end

  def self.find_graal_js
    jar = ENV['GRAAL_JS_JAR']
    return jar if jar
    raise "couldn't find trufflejs.jar - download GraalVM as described in https://github.com/jruby/jruby/wiki/Downloading-GraalVM and find it in there"
  end

  def self.jruby_eclipse?
    # tool/jruby_eclipse only works on release currently
    ENV["JRUBY_ECLIPSE"] == "true" and !truffle_version.end_with?('SNAPSHOT')
  end

  def self.find_ruby
    if ENV["RUBY_BIN"]
      ENV["RUBY_BIN"]
    else
      version = `ruby -e 'print RUBY_VERSION' 2>/dev/null`
      if version.start_with?("2.")
        "ruby"
      else
        find_jruby
      end
    end
  end

  def self.find_jruby
    if jruby_eclipse?
      "#{JRUBY_DIR}/tool/jruby_eclipse"
    elsif ENV['RUBY_BIN']
      ENV['RUBY_BIN']
    else
      "#{JRUBY_DIR}/bin/jruby"
    end
  end

  def self.find_jruby_bin_dir
    if jruby_eclipse?
      JRUBY_DIR + "/bin"
    else
      File.dirname(find_jruby)
    end
  end

  def self.git_branch
    @git_branch ||= `GIT_DIR="#{JRUBY_DIR}/.git" git rev-parse --abbrev-ref HEAD`.strip
  end

  def self.mangle_for_env(name)
    name.upcase.tr('-', '_')
  end

  def self.find_graal_parent
    graal = File.expand_path('../../../../../graal-compiler', find_graal)
    raise "couldn't find graal - set GRAAL_BIN, and you need to use a checkout of Graal, not a build" unless Dir.exist?(graal)
    graal
  end

  def self.find_graal_mx
    mx = File.expand_path('../../../../../../mx/mx', find_graal)
    raise "couldn't find mx - set GRAAL_BIN, and you need to use a checkout of Graal, not a build" unless File.executable?(mx)
    mx
  end

  def self.igv_running?
    `ps ax`.include? 'IdealGraphVisualizer'
  end

  def self.ensure_igv_running
    unless igv_running?
      Dir.chdir(find_graal_parent + "/../jvmci") do
        spawn "#{find_graal_mx} --vm server igv", pgroup: true
      end

      sleep 5
      puts
      puts
      puts "-------------"
      puts "Waiting for IGV start"
      puts "The first time you run IGV it may take several minutes to download dependencies and compile"
      puts "Press enter when you see the IGV window"
      puts "-------------"
      puts
      puts
      $stdin.gets
    end
  end

  def self.find_bench
    bench_locations = [
      ENV['BENCH_DIR'],
      'bench9000',
      '../bench9000'
    ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

    not_found = -> {
      raise "couldn't find bench9000 - clone it from https://github.com/jruby/bench9000.git into the JRuby repository or parent directory"
    }

    bench_locations.find(not_found) do |location|
      Dir.exist?(location)
    end
  end

  def self.jruby_version
    File.read("#{JRUBY_DIR}/VERSION").strip
  end

end

module ShellUtils
  private

  def raw_sh(*args)
    if args.last == :no_print_cmd
      args.pop
    else
      puts "$ #{printable_cmd(args)}"
    end
    continue_on_failure = false
    if args.last == :continue_on_failure
      args.pop
      continue_on_failure = true
    end
    result = system(*args)
    if result
      true
    else
      if continue_on_failure
        false
      else
        $stderr.puts "FAILED (#{$?}): #{printable_cmd(args)}"
        if $? and $?.exitstatus
          exit $?.exitstatus
        else
          exit 1
        end
      end
    end
  end

  def printable_cmd(args)
    env = {}
    if Hash === args.first
      env, *args = args
    end
    env = env.map { |k,v| "#{k}=#{shellescape(v)}" }.join(' ')
    args = args.map { |a| shellescape(a) }.join(' ')
    env.empty? ? args : "#{env} #{args}"
  end

  def shellescape(str)
    return str unless str.is_a?(String)
    if str.include?(' ')
      if str.include?("'")
        require 'shellwords'
        Shellwords.escape(str)
      else
        "'#{str}'"
      end
    else
      str
    end
  end

  def sh(*args)
    Dir.chdir(JRUBY_DIR) do
      raw_sh(*args)
    end
  end

  def mvn(*args)
    sh './mvnw', *(['-q'] + args)
  end

  def mspec(command, *args)
    env_vars = {}
    if command.is_a?(Hash)
      env_vars = command
      command, *args = args
    end

    if Utilities.jruby_eclipse?
      args.unshift "-ttool/jruby_eclipse"
    end

    sh env_vars, Utilities.find_ruby, 'spec/mspec/bin/mspec', command, '--config', 'spec/truffle/truffle.mspec', *args
  end
end

module Commands
  include ShellUtils

  def help
    puts 'jt checkout name                               checkout a different Git branch and rebuild'
    puts 'jt build [options]                             build'
    puts 'jt build truffle [options]                     build only the Truffle part, assumes the rest is up-to-date'
    puts 'jt rebuild [options]                           clean and build'
    puts '    --no-tests       don\'t run JUnit unit tests'
    puts 'jt clean                                       clean'
    puts 'jt irb                                         irb'
    puts 'jt rebuild                                     clean and build'
    puts 'jt run [options] args...                       run JRuby with -X+T and args'
    puts '    --graal         use Graal (set GRAAL_BIN or it will try to automagically find it)'
    puts '    --js            add Graal.js to the classpath (set GRAAL_JS_JAR)'
    puts '    --asm           show assembly (implies --graal)'
    puts '    --server        run an instrumentation server on port 8080'
    puts '    --igv           make sure IGV is running and dump Graal graphs after partial escape (implies --graal)'
    puts '        --full      show all phases, not just up to the Truffle partial escape'
    puts '    --jdebug        run a JDWP debug server on #{JDEBUG_PORT}'
    puts '    --jexception[s] print java exceptions'
    puts 'jt e 14 + 2                                    evaluate an expression'
    puts 'jt puts 14 + 2                                 evaluate and print an expression'
    puts 'jt test                                        run all mri tests, specs and integration tests'
    puts 'jt test tck [--jdebug]                         run the Truffle Compatibility Kit tests'
    puts 'jt test mri                                    run mri tests'
    puts 'jt test specs                                  run all specs'
    puts 'jt test specs fast                             run all specs except sub-processes, GC, sleep, ...'
    puts 'jt test spec/ruby/language                     run specs in this directory'
    puts 'jt test spec/ruby/language/while_spec.rb       run specs in this file'
    puts 'jt test compiler                               run compiler tests (uses the same logic as --graal to find Graal)'
    puts '    --no-java-cmd   don\'t set JAVACMD - rely on bin/jruby or RUBY_BIN to have Graal already'
    puts 'jt test integration [fast|long|all]            runs bigger integration tests (fast is default)'
    puts '    --no-gems       don\'t run tests that install gems'
    puts 'jt tag spec/ruby/language                      tag failing specs in this directory'
    puts 'jt tag spec/ruby/language/while_spec.rb        tag failing specs in this file'
    puts 'jt tag all spec/ruby/language                  tag all specs in this file, without running them'
    puts 'jt untag spec/ruby/language                    untag passing specs in this directory'
    puts 'jt untag spec/ruby/language/while_spec.rb      untag passing specs in this file'
    puts 'jt bench debug [options] [vm-args] benchmark   run a single benchmark with options for compiler debugging'
    puts '    --igv                                      make sure IGV is running and dump Graal graphs after partial escape (implies --graal)'
    puts '        --full                                 show all phases, not just up to the Truffle partial escape'
    puts '    --ruby-backtrace                           print a Ruby backtrace on any compilation failures'
    puts 'jt bench reference [benchmarks]                run a set of benchmarks and record a reference point'
    puts 'jt bench compare [benchmarks]                  run a set of benchmarks and compare against a reference point'
    puts '    benchmarks can be any benchmarks or group of benchmarks supported'
    puts '    by bench9000, eg all, classic, chunky, 3, 5, 10, 15 - default is 5'
    puts 'jt metrics [--score name] alloc ...            how much memory is allocated running a program (use -X-T to test normal JRuby on this metric and others)'
    puts '    --score name                               report results as scores'
    puts 'jt metrics ... minheap ...                     what is the smallest heap you can use to run an application'
    puts 'jt metrics ... time ...                        how long does it take to run a command, broken down into different phases'
    puts 'jt install ..../graal/mx/suite.py              install a JRuby distribution into an mx suite'
    puts
    puts 'you can also put build or rebuild in front of any command'
    puts
    puts 'recognised environment variables:'
    puts
    puts '  RUBY_BIN                                     The JRuby+Truffle executable to use (normally just bin/jruby)'
    puts '  GRAAL_BIN                                    GraalVM executable (java command) to use'
    puts '  GRAAL_BIN_...git_branch_name...              GraalVM executable to use for a given branch'
    puts '           branch names are mangled - eg truffle-head becomes GRAAL_BIN_TRUFFLE_HEAD'
    puts '  GRAAL_JS_JAR                                 The location of trufflejs.jar'
  end

  def checkout(branch)
    sh 'git', 'checkout', branch
    rebuild
  end

  def build(project = nil)
    opts = %w[-DskipTests]
    case project
    when 'truffle'
      mvn *opts, '-pl', 'truffle', 'package'
    when nil
      mvn *opts, 'package'
    else
      raise ArgumentError, project
    end
  end

  def clean
    mvn 'clean'
  end

  def irb(*args)
    run(*%w[-S irb], *args)
  end

  def rebuild
    FileUtils.cp("#{JRUBY_DIR}/bin/jruby.bash", "#{JRUBY_DIR}/bin/jruby")
    clean
    build
  end

  def run(*args)
    env_vars = args.first.is_a?(Hash) ? args.shift : {}
    jruby_args = [
      '-X+T',
      "-Xtruffle.core.load_path=#{JRUBY_DIR}/truffle/src/main/ruby",
      '-Xtruffle.graal.warn_unless=false'
    ]

    { '--asm' => '--graal', '--igv' => '--graal' }.each_pair do |arg, dep|
      args.unshift dep if args.include?(arg)
    end

    if args.delete('--graal')
      env_vars["JAVACMD"] = Utilities.find_graal
      jruby_args << '-J-server'
    end

    if args.delete('--js')
      jruby_args << '-J-classpath'
      jruby_args << Utilities.find_graal_js
    end

    if args.delete('--asm')
      jruby_args += %w[-J-XX:+UnlockDiagnosticVMOptions -J-XX:CompileCommand=print,*::callRoot]
    end

    if args.delete('--jdebug')
      jruby_args << JDEBUG
    end

    if args.delete('--jexception') || args.delete('--jexceptions')
      jruby_args << JEXCEPTION
    end

    if args.delete('--server')
      jruby_args += %w[-Xtruffle.instrumentation_server_port=8080]
    end

    if args.delete('--igv')
      warn "warning: --igv might not work on master - if it does not, use truffle-head instead which builds against latest graal" if Utilities.git_branch == 'master'
      Utilities.ensure_igv_running
      if args.delete('--full')
        jruby_args += %w[-J-G:Dump=Truffle]
      else
        jruby_args += %w[-J-G:Dump=TrufflePartialEscape]
      end
    end

    raw_sh env_vars, Utilities.find_jruby, *jruby_args, *args
  end
  alias ruby run

  def e(*args)
    run '-e', args.join(' ')
  end

  def command_puts(*args)
    e 'puts begin', *args, 'end'
  end

  def command_p(*args)
    e 'p begin', *args, 'end'
  end

  def test(*args)
    path, *rest = args

    case path
    when nil
      test_tck
      test_specs('run')
      # test_mri # TODO (pitr-ch 29-Mar-2016): temporarily disabled
      test_integration({'CI' => 'true', 'HAS_REDIS' => 'true'}, 'all')
      test_compiler
    when 'compiler' then test_compiler(*rest)
    when 'integration' then test_integration({}, *rest)
    when 'specs' then test_specs('run', *rest)
    when 'tck' then
      args = []
      if rest.include? '--jdebug'
        args << JDEBUG_TEST
      end
      test_tck *args
    when 'mri' then test_mri(*rest)
    else
      if File.expand_path(path).start_with?("#{JRUBY_DIR}/test")
        test_mri(*args)
      else
        test_specs('run', *args)
      end
    end
  end

  def test_mri(*args)
    env_vars = {
      "EXCLUDES" => "test/mri/excludes_truffle"
    }
    jruby_args = %w[-J-Xmx2G -Xtruffle.exceptions.print_java]

    if args.empty?
      args = File.readlines("#{JRUBY_DIR}/test/mri_truffle.index").grep(/^[^#]\w+/).map(&:chomp)
    end

    command = %w[test/mri/runner.rb -v --color=never --tty=no -q]
    run(env_vars, *jruby_args, *command, *args)
  end
  private :test_mri

  def test_compiler(*args)
    env_vars = {}
    env_vars["JRUBY_OPTS"] = '-Xtruffle.graal.warn_unless=false'
    env_vars["JAVACMD"] = Utilities.find_graal unless args.delete('--no-java-cmd')
    env_vars["PATH"] = "#{Utilities.find_jruby_bin_dir}:#{ENV["PATH"]}"
    Dir["#{JRUBY_DIR}/test/truffle/compiler/*.sh"].each do |test_script|
      sh env_vars, test_script
    end
  end
  private :test_compiler

  def test_integration(env, *args)
    no_gems = args.delete('--no-gems')

    all  = args.delete('all')
    long = args.delete('long') || all
    fast = args.delete('fast') || all ||
        !long # fast is the default

    env_vars   = env
    jruby_opts = []

    jruby_opts << '-Xtruffle.graal.warn_unless=false'

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-classpath'
      jruby_opts << Utilities.find_graal_js
    end

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    env_vars["PATH"]       = "#{Utilities.find_jruby_bin_dir}:#{ENV["PATH"]}"
    integration_path       = "#{JRUBY_DIR}/test/truffle/integration"
    long_tests             = File.read(File.join(integration_path, 'long-tests.txt')).lines.map(&:chomp)
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{integration_path}/#{test_names}.sh"].each do |test_script|
      is_long     = long_tests.include?(File.basename(test_script))
      gem_check   = !(no_gems && File.read(test_script).include?('gem install'))
      group_check = (is_long && long) || (!is_long && fast)
      run         = single_test || group_check && gem_check

      sh env_vars, test_script if run
    end
  end
  private :test_integration

  def test_specs(command, *args)
    env_vars = {}
    options = []

    case command
    when 'run'
      options += %w[--excl-tag fails]
    when 'tag'
      options += %w[--add fails --fail]
    when 'untag'
      options += %w[--del fails --pass]
      command = 'tag'
    when 'tag_all'
      options += %w[--unguarded --all --dry-run --add fails]
      command = 'tag'
    else
      raise command
    end

    if args.first == 'fast'
      args.shift
      options += %w[--excl-tag slow -T-Xtruffle.backtraces.limit=4]
    end

    if args.delete('--graal')
      env_vars["JAVACMD"] = Utilities.find_graal
      options << '-T-J-server'
    end

    if args.delete('--jdebug')
      options << "-T#{JDEBUG}"
    end

    if args.delete('--jexception') || args.delete('--jexceptions')
      options << "-T#{JEXCEPTION}"
    end

    if args.delete('--truffle-formatter')
      options += %w[--format spec/truffle/truffle_formatter.rb]
    end

    if ENV['TRAVIS']
      # Need lots of output to keep Travis happy
      options += %w[--format specdoc]
    end

    mspec env_vars, command, *options, *args
  end
  private :test_specs

  def test_tck(*args)
    mvn *args + ['test']
  end
  private :test_tck

  def tag(path, *args)
    return tag_all(*args) if path == 'all'
    test_specs('tag', path, *args)
  end

  # Add tags to all given examples without running them. Useful to avoid file exclusions.
  def tag_all(*args)
    test_specs('tag_all', *args)
  end
  private :tag_all

  def untag(path, *args)
    puts
    puts "WARNING: untag is currently not very reliable - run `jt test #{[path,*args] * ' '}` after and manually annotate any new failures"
    puts
    test_specs('untag', path, *args)
  end

  def bench(command, *args)
    bench_dir = Utilities.find_bench
    env_vars = {
      "JRUBY_DEV_DIR" => JRUBY_DIR,
      "GRAAL_BIN" => Utilities.find_graal,
    }
    bench_args = ["#{bench_dir}/bin/bench9000"]
    case command
    when 'debug'
      vm_args = ['-G:+TraceTruffleCompilation', '-G:+DumpOnError']
      if args.delete '--igv'
        warn "warning: --igv might not work on master - if it does not, use truffle-head instead which builds against latest graal" if Utilities.git_branch == 'master'
        Utilities.ensure_igv_running

        if args.delete('--full')
          vm_args.push '-G:Dump=Truffle'
        else
          vm_args.push '-G:Dump=TrufflePartialEscape'
        end
      end
      if args.delete '--ruby-backtrace'
        vm_args.push '-G:+TruffleCompilationExceptionsAreThrown'
      else
        vm_args.push '-G:+TruffleCompilationExceptionsAreFatal'
      end
      remaining_args = []
      args.each do |arg|
        if arg.start_with? '-'
          vm_args.push arg
        else
          remaining_args.push arg
        end
      end
      env_vars["JRUBY_OPTS"] = vm_args.map{ |a| '-J' + a }.join(' ')
      bench_args += ['score', '--config', "#{bench_dir}/benchmarks/default.config.rb", 'jruby-dev-truffle-graal', '--show-commands', '--show-samples']
      raise 'specify a single benchmark for run - eg classic-fannkuch-redux' if remaining_args.size != 1
      args = remaining_args
    when 'reference'
      bench_args += ['reference', '--config', "#{bench_dir}/benchmarks/default.config.rb", 'jruby-dev-truffle-graal', '--show-commands']
      args << "5" if args.empty?
    when 'compare'
      bench_args += ['compare-reference', '--config', "#{bench_dir}/benchmarks/default.config.rb", 'jruby-dev-truffle-graal']
      args << "5" if args.empty?
    else
      raise ArgumentError, command
    end
    raw_sh env_vars, "ruby", *bench_args, *args
  end

  def metrics(command, *args)
    trap(:INT) { puts; exit }
    args = args.dup
    if args.first == '--score'
      args.shift
      score_name = args.shift
    else
      score_name = nil
    end
    case command
    when 'alloc'
      metrics_alloc score_name, *args
    when 'minheap'
        metrics_minheap score_name, *args
    when 'time'
        metrics_time score_name, *args
    else
      raise ArgumentError, command
    end
  end
  
  def metrics_alloc(score_name, *args)
    samples = []
    METRICS_REPS.times do
      log '.', 'sampling'
      r, w = IO.pipe
      run '-Xtruffle.metrics.memory_used_on_exit=true', '-J-verbose:gc', *args, {err: w, out: w}, :no_print_cmd
      w.close
      samples.push memory_allocated(r.read)
      r.close
    end
    log "\n", nil
    mean = samples.inject(:+) / samples.size
    if score_name
      puts "alloc-#{score_name}: #{mean}"
    else
      puts "#{human_size(mean)}, max #{human_size(samples.max)}"
    end
  end

  def memory_allocated(trace)
    allocated = 0
    trace.lines do |line|
      case line
      when /(\d+)K->(\d+)K/
        before = $1.to_i * 1024
        after = $2.to_i * 1024
        collected = before - after
        allocated += collected
      when /^allocated (\d+)$/
        allocated += $1.to_i
      end
    end
    allocated
  end
  
  def metrics_minheap(score_name, *args)
    heap = 10
    log '>', "Trying #{heap} MB"
    until can_run_in_heap(heap, *args)
      heap += 10
      log '>', "Trying #{heap} MB"
    end
    heap -= 9
    heap = 1 if heap == 0
    successful = 0
    loop do
      if successful > 0
        log '?', "Verifying #{heap} MB"
      else
        log '+', "Trying #{heap} MB"
      end
      if can_run_in_heap(heap, *args)
        successful += 1
        break if successful == METRICS_REPS
      else
        heap += 1
        successful = 0
      end
    end
    log "\n", nil
    if score_name
      puts "minheap-#{score_name}: #{heap*1024*1024}"
    else
      puts "#{heap} MB"
    end
  end
  
  def can_run_in_heap(heap, *command)
    run("-J-Xmx#{heap}M", *command, {err: '/dev/null', out: '/dev/null'}, :continue_on_failure, :no_print_cmd)
  end
  
  def metrics_time(score_name, *args)
    samples = []
    METRICS_REPS.times do
      log '.', 'sampling'
      r, w = IO.pipe
      start = Time.now
      run '-Xtruffle.metrics.time=true', *args, {err: w, out: w}, :no_print_cmd
      finish = Time.now
      w.close
      samples.push get_times(r.read, finish - start)
      r.close
    end
    log "\n", nil
    samples[0].each_key do |region|
      region_samples = samples.map { |s| s[region] }
      mean = region_samples.inject(:+) / samples.size
      if score_name
        puts "time-#{region.strip}-#{score_name}: #{(mean*1000).round}"
      else
        puts "#{region} #{mean.round(2)} s"
      end
    end
  end

  def get_times(trace, total)
    start_times = {}
    times = {}
    depth = 1
    accounted_for = 0
    trace.lines do |line|
      if line =~ /^([a-z\-]+) (\d+\.\d+)$/
        region = $1
        time = $2.to_f
        if region.start_with? 'before-'
          depth += 1
          region = (' ' * depth + region['before-'.size..-1])
          start_times[region] = time
        elsif region.start_with? 'after-'
          region = (' ' * depth + region['after-'.size..-1])
          depth -= 1
          elapsed = time - start_times[region]
          times[region] = elapsed
          accounted_for += elapsed if depth == 2
        end
      end
    end
    times[' jvm'] = total - times['  main']
    times['total'] = total
    times['unaccounted'] = total - accounted_for
    times
  end

  def human_size(bytes)
    if bytes < 1024
      "#{bytes} B"
    elsif bytes < 1000**2
      "#{(bytes/1024.0).round(2)} KB"
    elsif bytes < 1000**3
      "#{(bytes/1024.0**2).round(2)} MB"
    elsif bytes < 1000**4
      "#{(bytes/1024.0**3).round(2)} GB"
    else
      "#{(bytes/1024.0**4).round(2)} TB"
    end
  end
  
  def log(tty_message, full_message)
    if STDOUT.tty?
      print(tty_message) unless tty_message.nil?
    else
      puts full_message unless full_message.nil?
    end
  end

  def check_ambiguous_arguments
    ENV.delete "JRUBY_ECLIPSE" # never run from the Eclipse launcher here
    clean
    # modify pom
    pom = "#{JRUBY_DIR}/truffle/pom.rb"
    contents = File.read(pom)
    contents.gsub!(/^(\s+)'source'\s*=>.+'1.7'.+,\n\s+'target'\s*=>.+\s*'1.7.+,\n/) do
      indent = $1
      $&.gsub("1.7", "1.8") + "#{indent}'fork' => 'true',\n"
    end
    contents.sub!(/^(\s+)('-J-Dfile.encoding=UTF-8')(.+\n)(?!\1'-parameters')/) do
      "#{$1}#{$2},\n#{$1}'-parameters'#{$3}"
    end
    File.write pom, contents

    build
    run({ "TRUFFLE_CHECK_AMBIGUOUS_OPTIONAL_ARGS" => "true" }, '-e', 'exit')
  end

  def install(arg)
    case arg
    when /.*suite.*\.py$/
      rebuild
      mvn '-Pcomplete'

      suite_file = arg
      suite_lines = File.readlines(suite_file)
      version = Utilities.jruby_version

      [
        ['maven/jruby-complete/target', "jruby-complete"],
        ['truffle/target', "jruby-truffle"]
      ].each do |dir, name|
        jar_name = "#{name}-#{version}.jar"
        source_jar_path = "#{dir}/#{jar_name}"
        shasum = Digest::SHA1.hexdigest File.read(source_jar_path)
        jar_shasum_name = "#{name}-#{version}-#{shasum}.jar"
        FileUtils.cp source_jar_path, "#{File.expand_path('../..', suite_file)}/lib/#{jar_shasum_name}"
        line_index = suite_lines.find_index { |line| line.start_with? "      \"path\" : \"lib/#{name}" }
        suite_lines[line_index] = "      \"path\" : \"lib/#{jar_shasum_name}\",\n"
        suite_lines[line_index + 1] = "      \#\"urls\" : [\"http://lafo.ssw.uni-linz.ac.at/truffle/ruby/#{jar_shasum_name}\"],\n"
        suite_lines[line_index + 2] = "      \"sha1\" : \"#{shasum}\"\n"
      end

      File.write(suite_file, suite_lines.join())
    else
      raise ArgumentError, kind
    end
  end

end

class JT
  include Commands

  def main(args)
    args = args.dup

    if args.empty? or %w[-h -help --help].include? args.first
      help
      exit
    end

    case args.first
    when "rebuild"
      send(args.shift)
    when "build"
      command = [args.shift]
      command << args.shift if args.first == "truffle"
      send(*command)
    end

    return if args.empty?

    commands = Commands.public_instance_methods(false).map(&:to_s)

    command, *rest = args
    command = "command_#{command}" if %w[p puts].include? command

    abort "no command matched #{command.inspect}" unless commands.include?(command)

    begin
      send(command, *rest)
    rescue
      puts "Error during command: #{args*' '}"
      raise $!
    end
  end
end

JT.new.main(ARGV)
