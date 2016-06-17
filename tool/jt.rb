#!/usr/bin/env ruby
# encoding: utf-8

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
require 'json'
require 'timeout'

GRAALVM_VERSION = "0.11"

JRUBY_DIR = File.expand_path('../..', __FILE__)
M2_REPO = File.expand_path('~/.m2/repository')

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
      if /'truffle\.version' => '(\d+\.\d+|\h+-SNAPSHOT)'/ =~ line
        break $1
      end
    end
  end

  def self.graal_locations
    from_branch = ENV["GRAAL_BIN_#{mangle_for_env(git_branch)}"]
    yield File.expand_path(from_branch) if from_branch
    
    from_env = ENV['GRAAL_BIN']
    yield File.expand_path(from_env) if from_env

    rel_java_bin = "bin/java" # "jre/bin/javao"
    ['', '../', '../../'].each do |prefix|
      %w[dk re].each do |kind|
        path = "#{prefix}graalvm-#{GRAALVM_VERSION}-#{kind}/#{rel_java_bin}"
        yield File.expand_path(path, JRUBY_DIR)
      end
    end

    ['', '../', '../../'].each do |prefix|
      path = Dir["#{prefix}graal/jvmci/jdk1.8.*/product/#{rel_java_bin}"].sort.last
      yield File.expand_path(path, JRUBY_DIR) if path
    end
  end

  def self.find_graal
    graal_locations do |location|
      return location if File.executable?(location)
    end
    raise "couldn't find graal - download it as described in https://github.com/jruby/jruby/wiki/Downloading-GraalVM and extract it into the JRuby repository or parent directory"
  end

  def self.find_sulong_graal(dir)
    searches = [
      "#{dir}/../jvmci/jdk*/product/bin/java",
      "#{dir}/../graal-core/mx.imports/binary/jvmci/jdk*/product/bin/java"
    ].map { |path| File.expand_path(path) }

    searches.each do |search|
      java = Dir[search].first
      return java if java
    end

    raise "couldn't find the Java build in the Sulong repository - you need to check it out and build it"
  end

  def self.find_graal_js
    jar = ENV['GRAAL_JS_JAR']
    return jar if jar
    raise "couldn't find trufflejs.jar - download GraalVM as described in https://github.com/jruby/jruby/wiki/Downloading-GraalVM and find it in there"
  end

  def self.find_sl
    jar = ENV['SL_JAR']
    return jar if jar
    raise "couldn't find truffle-sl.jar - build Truffle and find it in there"
  end

  def self.find_sulong_dir
    dir = ENV['SULONG_DIR']
    return dir if dir
    raise "couldn't find the Sulong repository - you need to check it out and build it"
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

  def self.find_repo(name)
    [JRUBY_DIR, "#{JRUBY_DIR}/.."].each do |dir|
      found = Dir.glob("#{dir}/#{name}*").first
      return File.expand_path(found) if found
    end
    raise "Can't find the #{name} repo - clone it into the repository directory or its parent"
  end
  
  def self.find_benchmark(benchmark)
    if File.exist?(benchmark)
      benchmark
    else
      File.join(find_repo('all-ruby-benchmarks'), benchmark)
    end
  end

  def self.find_gem(name)
    ["#{JRUBY_DIR}/lib/ruby/gems/shared/gems"].each do |dir|
      found = Dir.glob("#{dir}/#{name}*").first
      return File.expand_path(found) if found
    end
    
    [JRUBY_DIR, "#{JRUBY_DIR}/.."].each do |dir|
      found = Dir.glob("#{dir}/#{name}").first
      return File.expand_path(found) if found
    end
    raise "Can't find the #{name} gem - gem install it in this repository, or put it in the repository directory or its parent"
  end

  def self.git_branch
    @git_branch ||= `GIT_DIR="#{JRUBY_DIR}/.git" git rev-parse --abbrev-ref HEAD`.strip
  end

  def self.mangle_for_env(name)
    name.upcase.tr('-', '_')
  end

  def self.find_jvmci
    jvmci_locations = [
      ENV['JVMCI_DIR'],
      ENV["JVMCI_DIR#{mangle_for_env(git_branch)}"]
    ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

    not_found = -> {
      raise "couldn't find JVMCI"
    }

    jvmci_locations.find(not_found) do |location|
      Dir.exist?(location)
    end
  end

  def self.igv_running?
    `ps ax`.include?('idealgraphvisualizer')
  end

  def self.ensure_igv_running
    unless igv_running?
      Dir.chdir(find_jvmci) do
        spawn 'mx --vm server igv', pgroup: true
      end

      puts
      puts
      puts "-------------"
      puts "Waiting for IGV start"
      puts "The first time you run IGV it may take several minutes to download dependencies and compile"
      puts "-------------"
      puts
      puts

      sleep 3

      until igv_running?
        puts 'still waiting for IGV to appear in ps ax...'
        sleep 3
      end

      puts 'just a few more seconds...'
      sleep 6
    end
  end

  def self.jruby_version
    File.read("#{JRUBY_DIR}/VERSION").strip
  end
  
  def self.human_size(bytes)
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

  def self.log(tty_message, full_message)
    if STDERR.tty?
      STDERR.print tty_message unless tty_message.nil?
    else
      STDERR.print full_message unless full_message.nil?
    end
  end

end

module ShellUtils
  private

  def system_timeout(timeout, *args)
    begin
      pid = Process.spawn(*args)
    rescue SystemCallError
      return nil
    end
    
    begin
      Timeout.timeout timeout do
        Process.waitpid pid
        $?.exitstatus == 0
      end
    rescue Timeout::Error
      Process.kill('TERM', pid)
      nil
    end
  end

  def raw_sh(*args)
    continue_on_failure = false
    if args.last.is_a?(Hash) && args.last.delete(:continue_on_failure)
      continue_on_failure = true
    end
    if !args.last.is_a?(Hash) || !args.last.delete(:no_print_cmd)
      STDERR.puts "$ #{printable_cmd(args)}"
    end
    timeout = nil
    if args.last.is_a?(Hash)
      timeout = args.last.delete(:timeout)
    end
    if timeout
      result = system_timeout(timeout, *args)
    else
      result = system(*args)
    end
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
  
  def maven_options(*options)
    maven_options = []
    offline = options.delete('--offline')
    if offline
      maven_options.push "-Dmaven.repo.local=#{Utilities.find_repo('jruby-build-pack')}/maven"
      maven_options.push '--offline'
    end
    return [maven_options, options]
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
    puts 'jt bootstrap [options]                         run the build system\'s bootstrap phase'
    puts 'jt build [options]                             build'
    puts 'jt build truffle [options]                     build only the Truffle part, assumes the rest is up-to-date'
    puts 'jt rebuild [options]                           clean and build'
    puts '    --offline                                  use the build pack to build offline'
    puts 'jt clean                                       clean'
    puts 'jt irb                                         irb'
    puts 'jt rebuild                                     clean and build'
    puts 'jt run [options] args...                       run JRuby with -X+T and args'
    puts '    --graal         use Graal (set GRAAL_BIN or it will try to automagically find it)'
    puts '    --js            add Graal.js to the classpath (set GRAAL_JS_JAR)'
    puts '    --sulong        add Sulong to the classpath (set SULONG_DIR, implies --graal but finds it from the SULONG_DIR)'
    puts '    --asm           show assembly (implies --graal)'
    puts '    --server        run an instrumentation server on port 8080'
    puts '    --igv           make sure IGV is running and dump Graal graphs after partial escape (implies --graal)'
    puts '        --full      show all phases, not just up to the Truffle partial escape'
    puts "    --jdebug        run a JDWP debug server on #{JDEBUG_PORT}"
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
    puts 'jt test integration                            runs all integration tests'
    puts 'jt test integration TESTS                      runs the given integration tests'
    puts 'jt test gems                                   tests using gems'
    puts 'jt test ecosystem                              tests using the wider ecosystem such as bundler, Rails, etc'
    puts 'jt test cexts                                  run C extension tests (set SULONG_DIR)'
    puts 'jt test report :language                       build a report on language specs'
    puts '               :core                               (results go into test/target/mspec-html-report)'
    puts '               :library'
    puts 'jt tag spec/ruby/language                      tag failing specs in this directory'
    puts 'jt tag spec/ruby/language/while_spec.rb        tag failing specs in this file'
    puts 'jt tag all spec/ruby/language                  tag all specs in this file, without running them'
    puts 'jt untag spec/ruby/language                    untag passing specs in this directory'
    puts 'jt untag spec/ruby/language/while_spec.rb      untag passing specs in this file'
    puts 'jt metrics alloc [--json] ...                  how much memory is allocated running a program (use -X-T to test normal JRuby on this metric and others)'
    puts 'jt metrics minheap ...                         what is the smallest heap you can use to run an application'
    puts 'jt metrics time ...                            how long does it take to run a command, broken down into different phases'
    puts 'jt tarball                                     build the and test the distribution tarball'
    puts 'jt benchmark [options] args...                 run benchmark-interface (implies --graal)'
    puts '    --no-graal       don\'t imply --graal'
    puts '    note that to run most MRI benchmarks, you should translate them first with normal Ruby and cache the result, such as'
    puts '        benchmark bench/mri/bm_vm1_not.rb --cache'
    puts '        jt benchmark bench/mri/bm_vm1_not.rb --use-cache'
    puts
    puts 'you can also put build or rebuild in front of any command'
    puts
    puts 'recognised environment variables:'
    puts
    puts '  RUBY_BIN                                     The JRuby+Truffle executable to use (normally just bin/jruby)'
    puts '  GRAAL_BIN                                    GraalVM executable (java command) to use'
    puts '  GRAAL_BIN_...git_branch_name...              GraalVM executable to use for a given branch'
    puts '           branch names are mangled - eg truffle-head becomes GRAAL_BIN_TRUFFLE_HEAD'
    puts '  JVMCI_DIR                                    JMVCI repository checkout to use when running IGV (mx must already be on the $PATH)'
    puts '  JVMCI_DIR_...git_branch_name...              JMVCI repository to use for a given branch'
    puts '  GRAAL_JS_JAR                                 The location of trufflejs.jar'
    puts '  SL_JAR                                       The location of truffle-sl.jar'
    puts '  SULONG_DIR                                   The location of a built checkout of the Sulong repository'
    puts '  SULONG_CLASSPATH                             An explicit classpath to use for Sulong, rather than working it out from SULONG_DIR'
  end

  def checkout(branch)
    sh 'git', 'checkout', branch
    rebuild
  end

  def bootstrap(*options)
    maven_options, other_options = maven_options(*options)
    mvn *maven_options, '-Pbootstrap-no-launcher'
  end

  def build(*options)
    maven_options, other_options = maven_options(*options)
    project = other_options.first
    case project
    when 'truffle'
      mvn *maven_options, '-pl', 'truffle', 'package'
    when nil
      mvn *maven_options, 'package'
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
    
    if ENV['JRUBY_OPTS'] && ENV['JRUBY_OPTS'].include?('-X-T')
      jruby_args.delete '-X+T'
    end

    {
      '--asm' => '--graal',
      '--igv' => '--graal'
    }.each_pair do |arg, dep|
      args.unshift dep if args.include?(arg)
    end

    if args.delete('--graal')
      if graal_home = ENV["GRAAL_HOME"]
        graal_home = File.expand_path(graal_home)
        command_line = `mx -v -p #{graal_home} vm -version 2>/dev/null`.lines.last
        vm_args = command_line.split
        vm_args.pop # Drop "-version"
        graal_bin = vm_args.shift
        jruby_args = vm_args.map { |arg| "-J#{arg}" } + jruby_args
      else
        graal_bin = Utilities.find_graal
      end

      env_vars["JAVACMD"] = graal_bin
      jruby_args.delete('-Xtruffle.graal.warn_unless=false')
    end

    if args.delete('--js')
      jruby_args << '-J-classpath'
      jruby_args << Utilities.find_graal_js
    end

    if args.delete('--sulong')
      collect_sulong_args(env_vars, jruby_args)
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

    if args.delete('--profile')
      v = Utilities.truffle_version
      jruby_args << "-J-Xbootclasspath/a:#{M2_REPO}/com/oracle/truffle/truffle-debug/#{v}/truffle-debug-#{v}.jar"
      jruby_args << "-J-Dtruffle.profiling.enabled=true"
      jruby_args << "-Xtruffle.profiler=true"
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
      # test_mri # TODO (pitr-ch 29-Mar-2016): temporarily disabled since it uses refinements
      test_integration
      test_gems
      test_ecosystem 'HAS_REDIS' => 'true'
      test_compiler
      test_cexts if ENV['SULONG_DIR']
    when 'compiler' then test_compiler(*rest)
    when 'cexts' then test_cexts(*rest)
    when 'report' then test_report(*rest)
    when 'integration' then test_integration({}, *rest)
    when 'gems' then test_gems({}, *rest)
    when 'ecosystem' then test_ecosystem({}, *rest)
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
    jruby_opts = []
    jruby_opts << '-J-Djvmci.Compiler=graal'
    jruby_opts << '-Xtruffle.graal.warn_unless=false'

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-classpath'
      jruby_opts << Utilities.find_graal_js
    end

    jruby_opts << '-Xtruffle.exceptions.print_java=true'

    env_vars = {}
    env_vars["JAVACMD"] = Utilities.find_graal unless args.delete('--no-java-cmd')
    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')
    env_vars["PATH"] = "#{Utilities.find_jruby_bin_dir}:#{ENV["PATH"]}"

    Dir["#{JRUBY_DIR}/test/truffle/compiler/*.sh"].each do |test_script|
      sh env_vars, test_script
    end
  end
  private :test_compiler

  def test_cexts(*args)
    output_file = 'cext-output.txt'
    Dir["#{JRUBY_DIR}/test/truffle/cexts/*"].each do |dir|
      sh Utilities.find_jruby, "#{JRUBY_DIR}/bin/jruby-cext-c", dir
      name = File.basename(dir)
      run '--sulong', '-I', "#{dir}/lib", "#{dir}/bin/#{name}", :out => output_file
      unless File.read(output_file) == File.read("#{dir}/expected.txt")
        abort "c extension #{dir} didn't work as expected"
      end
    end
  ensure
    File.delete output_file rescue nil
  end
  private :test_cexts

  def test_report(component)
    test 'specs', '--truffle-formatter', component
    sh 'ant', '-f', 'spec/truffle/buildTestReports.xml'
  end
  private :test_cexts

  def test_integration(env={}, *args)
    env_vars   = env
    jruby_opts = []

    jruby_opts << '-Xtruffle.graal.warn_unless=false'

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-classpath'
      jruby_opts << Utilities.find_graal_js
    end

    if ENV['SL_JAR']
      jruby_opts << '-J-classpath'
      jruby_opts << Utilities.find_sl
    end

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    env_vars["PATH"]       = "#{Utilities.find_jruby_bin_dir}:#{ENV["PATH"]}"
    tests_path             = "#{JRUBY_DIR}/test/truffle/integration"
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{tests_path}/#{test_names}.sh"].each do |test_script|
      sh env_vars, test_script
    end
  end
  private :test_integration

  def test_gems(env={}, *args)
    env_vars   = env
    jruby_opts = []

    jruby_opts << '-Xtruffle.graal.warn_unless=false'

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-classpath'
      jruby_opts << Utilities.find_graal_js
    end

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    env_vars["PATH"]       = "#{Utilities.find_jruby_bin_dir}:#{ENV["PATH"]}"
    tests_path             = "#{JRUBY_DIR}/test/truffle/gems"
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{tests_path}/#{test_names}.sh"].each do |test_script|
      next if test_script.end_with?('/install-gems.sh')
      sh env_vars, test_script
    end
  end
  private :test_gems

  def test_ecosystem(env={}, *args)
    env_vars   = env
    jruby_opts = []

    jruby_opts << '-Xtruffle.graal.warn_unless=false'

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    env_vars["PATH"]       = "#{Utilities.find_jruby_bin_dir}:#{ENV["PATH"]}"
    tests_path             = "#{JRUBY_DIR}/test/truffle/ecosystem"
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{tests_path}/#{test_names}.sh"].each do |test_script|
      sh env_vars, test_script
    end
  end
  private :test_ecosystem

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

    if args.delete('--sulong')
      collect_sulong_args(env_vars, options, '-T')
    end

    if ENV['CI']
      # Need lots of output to keep Travis happy
      options += %w[--format specdoc]
    end

    mspec env_vars, command, *options, *args
  end
  private :test_specs

  def test_tck(*args)
    mvn *args + ['-Ptck']
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

  def metrics(command, *args)
    trap(:INT) { puts; exit }
    args = args.dup
    case command
    when 'alloc'
      metrics_alloc *args
    when 'minheap'
        metrics_minheap *args
    when 'time'
        metrics_time *args
    else
      raise ArgumentError, command
    end
  end

  def metrics_alloc(*args)
    use_json = args.delete '--json'
    samples = []
    METRICS_REPS.times do
      Utilities.log '.', "sampling\n"
      r, w = IO.pipe
      run '-Xtruffle.metrics.memory_used_on_exit=true', '-J-verbose:gc', *args, {err: w, out: w, no_print_cmd: true}
      w.close
      samples.push memory_allocated(r.read)
      r.close
    end
    Utilities.log "\n", nil
    range = samples.max - samples.min
    error = range / 2
    median = samples.min + error
    human_readable = "#{Utilities.human_size(median)} ± #{Utilities.human_size(error)}"
    if use_json
      puts JSON.generate({
          samples: samples,
          median: median,
          error: error,
          human: human_readable
      })
    else
      puts human_readable
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

  def metrics_minheap(*args)
    use_json = args.delete '--json'
    heap = 10
    Utilities.log '>', "Trying #{heap} MB\n"
    until can_run_in_heap(heap, *args)
      heap += 10
      Utilities.log '>', "Trying #{heap} MB\n"
    end
    heap -= 9
    heap = 1 if heap == 0
    successful = 0
    loop do
      if successful > 0
        Utilities.log '?', "Verifying #{heap} MB\n"
      else
        Utilities.log '+', "Trying #{heap} MB\n"
      end
      if can_run_in_heap(heap, *args)
        successful += 1
        break if successful == METRICS_REPS
      else
        heap += 1
        successful = 0
      end
    end
    Utilities.log "\n", nil
    human_readable = "#{heap} MB"
    if use_json
      puts JSON.generate({
          min: heap,
          human: human_readable
      })
    else
      puts human_readable
    end
  end

  def can_run_in_heap(heap, *command)
    run("-J-Xmx#{heap}M", *command, {err: '/dev/null', out: '/dev/null', no_print_cmd: true, continue_on_failure: true, timeout: 60})
  end

  def metrics_time(*args)
    use_json = args.delete '--json'
    samples = []
    METRICS_REPS.times do
      Utilities.log '.', "sampling\n"
      r, w = IO.pipe
      start = Time.now
      run '-Xtruffle.metrics.time=true', *args, {err: w, out: w, no_print_cmd: true}
      finish = Time.now
      w.close
      samples.push get_times(r.read, finish - start)
      r.close
    end
    Utilities.log "\n", nil
    results = {}
    samples[0].each_key do |region|
      region_samples = samples.map { |s| s[region] }
      mean = region_samples.inject(:+) / samples.size
      human = "#{region.strip} #{mean.round(2)} s"
      results[region] = {
          samples: region_samples,
          mean: mean,
          human: human
      }
      if use_json
        file = STDERR
      else
        file = STDOUT
      end
      file.puts region[/\s*/] + human
    end
    if use_json
      puts JSON.generate(Hash[results.map { |key, values| [key.strip, values] }])
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
    times['unaccounted'] = total - accounted_for if times['    load-core']
    times
  end

  def tarball(*options)
    maven_options, other_options = maven_options(*options)
    mvn *maven_options, '-Pdist'
    generated_file = "#{JRUBY_DIR}/maven/jruby-dist/target/jruby-dist-#{Utilities.jruby_version}-bin.tar.gz"
    final_file = "#{JRUBY_DIR}/jruby-bin-#{Utilities.jruby_version}.tar.gz"
    FileUtils.copy generated_file, final_file
    FileUtils.copy "#{generated_file}.sha256", "#{final_file}.sha256"
    sh 'test/truffle/tarball.sh', final_file
  end

  def benchmark(*args)
    benchmark = args.pop
    raise 'no benchmark given' unless benchmark
    benchmark = Utilities.find_benchmark(benchmark)
    raise 'benchmark not found' unless File.exist?(benchmark)
    run_args = []
    run_args.push '--graal' unless args.delete('--no-graal')
    run_args.push '-I', "#{Utilities.find_gem('deep-bench')}/lib" rescue nil
    run_args.push '-I', "#{Utilities.find_gem('benchmark-ips')}/lib" rescue nil
    run_args.push "#{Utilities.find_gem('benchmark-interface')}/bin/benchmark"
    run_args.push benchmark
    run_args.push *args
    run *run_args
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

  def collect_sulong_args(env_vars, args, arg_prefix='')
    dir = Utilities.find_sulong_dir
    env_vars["JAVACMD"] = Utilities.find_sulong_graal(dir)

    if ENV["SULONG_CLASSPATH"]
      args << "#{arg_prefix}-J-classpath" << "#{arg_prefix}#{ENV["SULONG_CLASSPATH"]}"
    else
      args << "#{arg_prefix}-J-classpath" << "#{arg_prefix}#{dir}/lib/*"
      args << "#{arg_prefix}-J-classpath" << "#{arg_prefix}#{dir}/build/sulong.jar"
      nfi_classes = File.expand_path('../graal-core/mxbuild/graal/com.oracle.nfi/bin', dir)
      args << "#{arg_prefix}-J-classpath" << "#{arg_prefix}#{nfi_classes}"
    end

    args << "#{arg_prefix}-J-XX:-UseJVMCIClassLoader"
  end
  private :collect_sulong_args

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
      command << args.shift if args.first == "--offline"
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
