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
require 'yaml'
require 'open3'

GRAALVM_VERSION = '0.18'

JRUBY_DIR = File.expand_path('../..', __FILE__)
M2_REPO = File.expand_path('~/.m2/repository')
SULONG_HOME = ENV['SULONG_HOME']

JDEBUG_PORT = 51819
JDEBUG = "-J-agentlib:jdwp=transport=dt_socket,server=y,address=#{JDEBUG_PORT},suspend=y"
JDEBUG_TEST = "-Dmaven.surefire.debug=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=#{JDEBUG_PORT} -Xnoagent -Djava.compiler=NONE"
JEXCEPTION = "-Xtruffle.exceptions.print_java=true"
METRICS_REPS = 10

VERBOSE = ENV.include? 'V'

MAC = `uname -a`.include?('Darwin')

if MAC
  SO = 'dylib'
else
  SO = 'so'
end

# Expand GEM_HOME relative to cwd so it cannot be misinterpreted later.
ENV['GEM_HOME'] = File.expand_path(ENV['GEM_HOME']) if ENV['GEM_HOME']

LIBXML_HOME = ENV['LIBXML_HOME'] = ENV['LIBXML_HOME'] || '/usr'
LIBXML_LIB_HOME = ENV['LIBXML_LIB_HOME'] = ENV['LIBXML_LIB_HOME'] || "#{LIBXML_HOME}/lib"
LIBXML_INCLUDE = ENV['LIBXML_INCLUDE'] = ENV['LIBXML_INCLUDE'] || "#{LIBXML_HOME}/include/libxml2"
LIBXML_LIB = ENV['LIBXML_LIB'] = ENV['LIBXML_LIB'] || "#{LIBXML_LIB_HOME}/libxml2.#{SO}"

OPENSSL_HOME = ENV['OPENSSL_HOME'] = ENV['OPENSSL_HOME'] || '/usr'
OPENSSL_LIB_HOME = ENV['OPENSSL_LIB_HOME'] = ENV['OPENSSL_LIB_HOME'] || "#{OPENSSL_HOME}/lib"
OPENSSL_INCLUDE = ENV['OPENSSL_INCLUDE'] = ENV['OPENSSL_INCLUDE'] || "#{OPENSSL_HOME}/include"
OPENSSL_LIB = ENV['OPENSSL_LIB'] = ENV['OPENSSL_LIB'] || "#{OPENSSL_LIB_HOME}/libssl.#{SO}"

# wait for sub-processes to handle the interrupt
trap(:INT) {}

module Utilities

  def self.truffle_version
    File.foreach("#{JRUBY_DIR}/truffle/pom.rb") do |line|
      if /'truffle\.version' => '((?:\d+\.\d+|\h+)(?:-SNAPSHOT)?)'/ =~ line
        break $1
      end
    end
  end

  def self.truffle_release?
    !truffle_version.include?('SNAPSHOT')
  end

  def self.find_graal_javacmd_and_options
    graalvm = ENV['GRAALVM_BIN']
    jvmci = ENV['JVMCI_BIN']
    graal_home = ENV['GRAAL_HOME']

    raise "More than one of GRAALVM_BIN, JVMCI_BIN or GRAAL_HOME defined!" if [graalvm, jvmci, graal_home].compact.count > 1

    if !graalvm && !jvmci && !graal_home
      if truffle_release?
        graalvm = ENV['GRAALVM_RELEASE_BIN']
      else
        graal_home = ENV['GRAAL_HOME_TRUFFLE_HEAD']
      end
    end

    if graalvm
      javacmd = File.expand_path(graalvm)
      vm_args = []
      options = []
    elsif jvmci
      javacmd = File.expand_path(jvmci)
      jvmci_graal_home = ENV['JVMCI_GRAAL_HOME']
      raise "Also set JVMCI_GRAAL_HOME if you set JVMCI_BIN" unless jvmci_graal_home
      jvmci_graal_home = File.expand_path(jvmci_graal_home)
      vm_args = [
        '-d64',
        '-XX:+UnlockExperimentalVMOptions',
        '-XX:+EnableJVMCI',
        '--add-exports=java.base/jdk.internal.module=com.oracle.graal.graal_core',
        "--module-path=#{jvmci_graal_home}/../truffle/mxbuild/modules/com.oracle.truffle.truffle_api.jar:#{jvmci_graal_home}/mxbuild/modules/com.oracle.graal.graal_core.jar"
      ]
      options = ['--no-bootclasspath']
    elsif graal_home
      graal_home = File.expand_path(graal_home)
      output = `mx -v -p #{graal_home} vm -version 2>&1`.lines.to_a
      command_line = output.select { |line| line.include? '-version' }
      if command_line.size == 1
        command_line = command_line[0]
      else
        $stderr.puts "Error in mx for setting up Graal:"
        $stderr.puts output
        abort
      end
      vm_args = command_line.split
      vm_args.pop # Drop "-version"
      javacmd = vm_args.shift
      if Dir.exist?("#{graal_home}/mx.sulong")
        sulong_dependencies = "#{graal_home}/lib/*"
        sulong_jars = ["#{graal_home}/build/sulong.jar", "#{graal_home}/build/sulong_options.jar"]
        nfi_classes = File.expand_path('../graal-core/mxbuild/graal/com.oracle.nfi/bin', graal_home)
        vm_args << '-cp'
        vm_args << [nfi_classes, sulong_dependencies, *sulong_jars].join(':')
        vm_args << '-XX:-UseJVMCIClassLoader'
      end
      options = []
    else
      raise 'set one of GRAALVM_BIN or GRAAL_HOME in order to use Graal'
    end
    [javacmd, vm_args.map { |arg| "-J#{arg}" } + options]
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

  def self.mx?
    mx_ruby_jar = "#{JRUBY_DIR}/mxbuild/dists/ruby.jar"
    constants_file = "#{JRUBY_DIR}/core/src/main/java/org/jruby/runtime/Constants.java"
    File.exist?(mx_ruby_jar) && File.mtime(mx_ruby_jar) >= File.mtime(constants_file)
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
    if ENV['RUBY_BIN']
      ENV['RUBY_BIN']
    elsif mx?
      "#{JRUBY_DIR}/tool/jruby_mx"
    else
      "#{JRUBY_DIR}/bin/jruby"
    end
  end

  def self.find_repo(name)
    [JRUBY_DIR, "#{JRUBY_DIR}/.."].each do |dir|
      found = Dir.glob("#{dir}/#{name}*").sort.first
      return File.expand_path(found) if found
    end
    raise "Can't find the #{name} repo - clone it into the repository directory or its parent"
  end

  def self.find_benchmark(benchmark)
    if File.exist?(benchmark)
      benchmark
    else
      begin
        File.join(find_repo('ruby-benchmarks'), benchmark)
      rescue RuntimeError
        File.join(find_repo('all-ruby-benchmarks'), benchmark)
      end
    end
  end

  def self.find_gem(name)
    ["#{JRUBY_DIR}/lib/ruby/gems/shared/gems"].each do |dir|
      found = Dir.glob("#{dir}/#{name}*").sort.first
      return File.expand_path(found) if found
    end

    [JRUBY_DIR, "#{JRUBY_DIR}/.."].each do |dir|
      found = Dir.glob("#{dir}/#{name}").sort.first
      return File.expand_path(found) if found
    end
    raise "Can't find the #{name} gem - gem install it in this repository, or put it in the repository directory or its parent"
  end

  def self.git_branch
    @git_branch ||= `GIT_DIR="#{JRUBY_DIR}/.git" git rev-parse --abbrev-ref HEAD`.strip
  end

  def self.igv_running?
    `ps ax`.include?('idealgraphvisualizer')
  end

  def self.ensure_igv_running
    abort "I can't see IGV running - go to your checkout of Graal and run 'mx igv' in a separate shell, then run this command again" unless igv_running?
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
    options = args.last.is_a?(Hash) ? args.last : {}
    continue_on_failure = options.delete :continue_on_failure
    use_exec = options.delete :use_exec
    timeout = options.delete :timeout
    capture = options.delete :capture

    unless options.delete :no_print_cmd
      STDERR.puts "$ #{printable_cmd(args)}"
    end

    if use_exec
      result = exec(*args)
    elsif timeout
      result = system_timeout(timeout, *args)
    elsif capture
      out, err, status = Open3.capture3(*args)
      result = status.exitstatus == 0
    else
      result = system(*args)
    end

    if result
      if out && err
        [out, err]
      else
        true
      end
    elsif continue_on_failure
      false
    else
      status = $? unless capture
      $stderr.puts "FAILED (#{status}): #{printable_cmd(args)}"

      if capture
        $stderr.puts out
        $stderr.puts err
      end

      if status && status.exitstatus
        exit status.exitstatus
      else
        exit 1
      end
    end
  end

  def printable_cmd(args)
    env = {}
    if Hash === args.first
      env, *args = args
    end
    if Hash === args.last && args.last.empty?
      *args, options = args
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

  def replace_env_vars(string, env = ENV)
    string.gsub(/\$([A-Z_]+)/) {
      var = $1
      abort "You need to set $#{var}" unless env[var]
      env[var]
    }
  end

  def sh(*args)
    Dir.chdir(JRUBY_DIR) do
      raw_sh(*args)
    end
  end

  def mvn(*args)
    if args.first.is_a? Hash
      options = [args.shift]
    else
      options = []
    end

    args = ['-q', *args] unless VERBOSE

    sh *options, './mvnw', *args
  end

  def maven_options(*options)
    maven_options = []
    build_pack = options.delete('--build-pack')
    offline = options.delete('--offline')
    if build_pack || offline
      maven_options.push "-Dmaven.repo.local=#{Utilities.find_repo('jruby-build-pack')}/maven"
    end
    if offline
      maven_options.push '--offline'
    end
    return [maven_options, options]
  end

  def mx(dir, *args)
    command = ['mx', '-p', dir]
    command.push *args
    sh *command
  end

  def mx_sulong(*args)
    abort "You need to set SULONG_HOME" unless SULONG_HOME
    mx SULONG_HOME, *args
  end

  def clang(*args)
    if ENV['USE_SYSTEM_CLANG']
      sh 'clang', *args
    else
      mx_sulong 'su-clang', *args
    end
  end

  def llvm_opt(*args)
    if ENV['USE_SYSTEM_CLANG']
      sh 'opt', *args
    else
      mx_sulong 'su-opt', *args
    end
  end

  def sulong_run(*args)
    mx_sulong 'su-run', *args
  end

  def sulong_link(*args)
    mx_sulong 'su-link', *args
  end

  def mspec(command, *args)
    env_vars = {}
    if command.is_a?(Hash)
      env_vars = command
      command, *args = args
    end

    if Utilities.mx?
      args.unshift "-ttool/jruby_mx"
    end

    sh env_vars, Utilities.find_ruby, 'spec/mspec/bin/mspec', command, '--config', 'spec/truffle/truffle.mspec', *args
  end

  def newer?(input, output)
    return true unless File.exist? output
    File.mtime(input) > File.mtime(output)
  end
end

module Commands
  include ShellUtils

  def help
    puts <<-TXT.gsub(/^#{' '*6}/, '')
      jt checkout name                               checkout a different Git branch and rebuild
      jt bootstrap [options]                         run the build system\'s bootstrap phase
      jt build [options]                             build
      jt rebuild [options]                           clean and build
          truffle                                    build only the Truffle part, assumes the rest is up-to-date
          cexts [--no-openssl]                       build the cext backend (set SULONG_HOME and maybe USE_SYSTEM_CLANG)
          parser                                     build the parser
          --build-pack                               use the build pack
          --offline                                  use the build pack to build offline
      jt clean                                       clean
      jt irb                                         irb
      jt rebuild                                     clean and build
      jt run [options] args...                       run JRuby with -X+T and args
          --graal         use Graal (set either GRAALVM_BIN or GRAAL_HOME)
              --stress    stress the compiler (compile immediately, foreground compilation, compilation exceptions are fatal)
          --js            add Graal.js to the classpath (set GRAAL_JS_JAR)
          --asm           show assembly (implies --graal)
          --server        run an instrumentation server on port 8080
          --igv           make sure IGV is running and dump Graal graphs after partial escape (implies --graal)
              --full      show all phases, not just up to the Truffle partial escape
          --infopoints    show source location for each node in IGV
          --fg            disable background compilation
          --trace         show compilation information on stdout
          --jdebug        run a JDWP debug server on #{JDEBUG_PORT}
          --jexception[s] print java exceptions
          --exec          use exec rather than system
          --no-print-cmd  don\'t print the command
      jt e 14 + 2                                    evaluate an expression
      jt puts 14 + 2                                 evaluate and print an expression
      jt cextc directory clang-args                  compile the C extension in directory, with optional extra clang arguments
      jt test                                        run all mri tests, specs and integration tests (set SULONG_HOME, and maybe USE_SYSTEM_CLANG)
      jt test tck [--jdebug]                         run the Truffle Compatibility Kit tests
      jt test mri                                    run mri tests
          --graal         use Graal (set either GRAALVM_BIN, JVMCI_BIN or GRAAL_HOME)
      jt test specs                                  run all specs
      jt test specs fast                             run all specs except sub-processes, GC, sleep, ...
      jt test spec/ruby/language                     run specs in this directory
      jt test spec/ruby/language/while_spec.rb       run specs in this file
      jt test compiler                               run compiler tests (uses the same logic as --graal to find Graal)
      jt test integration                            runs all integration tests
      jt test integration [TESTS]                    runs the given integration tests
      jt test bundle                                 tests using bundler
      jt test gems                                   tests using gems
      jt test ecosystem [TESTS]                      tests using the wider ecosystem such as bundler, Rails, etc
      jt test cexts [--no-libxml --no-openssl]       run C extension tests
                                                         (implies --graal, where Graal needs to include Sulong, set SULONG_HOME to a built checkout of Sulong, and set GEM_HOME)
      jt test report :language                       build a report on language specs
                     :core                               (results go into test/target/mspec-html-report)
                     :library
      jt tag spec/ruby/language                      tag failing specs in this directory
      jt tag spec/ruby/language/while_spec.rb        tag failing specs in this file
      jt tag all spec/ruby/language                  tag all specs in this file, without running them
      jt untag spec/ruby/language                    untag passing specs in this directory
      jt untag spec/ruby/language/while_spec.rb      untag passing specs in this file
      jt metrics alloc [--json] ...                  how much memory is allocated running a program (use -Xclassic to test normal JRuby on this metric and others)
      jt metrics minheap ...                         what is the smallest heap you can use to run an application
      jt metrics time ...                            how long does it take to run a command, broken down into different phases
      jt tarball                                     build the and test the distribution tarball
      jt benchmark [options] args...                 run benchmark-interface (implies --graal)
          --no-graal              don't imply --graal
          JT_BENCHMARK_RUBY=ruby  benchmark some other Ruby, like MRI
          note that to run most MRI benchmarks, you should translate them first with normal Ruby and cache the result, such as
              benchmark bench/mri/bm_vm1_not.rb --cache
              jt benchmark bench/mri/bm_vm1_not.rb --use-cache
      jt where repos ...                            find these repositories

      you can also put build or rebuild in front of any command

      recognised environment variables:

        RUBY_BIN                                     The JRuby+Truffle executable to use (normally just bin/jruby)
        GRAALVM_BIN                                  GraalVM executable (java command)
        GRAAL_HOME                                   Directory where there is a built checkout of the Graal compiler (make sure mx is on your path)
        JVMCI_BIN                                    JVMCI-enabled (so JDK 9 EA build) java command (aslo set JVMCI_GRAAL_HOME)
        JVMCI_GRAAL_HOME                             Like GRAAL_HOME, but only used for the JARs to run with JVMCI_BIN
        GRAALVM_RELEASE_BIN                          Default GraalVM executable when using a released version of Truffle (such as on master)
        GRAAL_HOME_TRUFFLE_HEAD                      Default Graal directory when using a snapshot version of Truffle (such as on truffle-head)
        SULONG_HOME                                  The Sulong source repository, if you want to run cextc
        USE_SYSTEM_CLANG                             Use the system clang rather than Sulong\'s when compiling C extensions
        GRAAL_JS_JAR                                 The location of trufflejs.jar
        SL_JAR                                       The location of truffle-sl.jar
        LIBXML_HOME, LIBXML_INCLUDE, LIBXML_LIB      The location of libxml2 (the directory containing include etc), and the direct include directory and library file
        OPENSSL_HOME, OPENSSL_INCLUDE, OPENSSL_LIB               ... OpenSSL ...
    TXT
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
    env = VERBOSE ? {} : {'JRUBY_BUILD_MORE_QUIET' => 'true'}
    case project
    when 'truffle'
      mvn env, *maven_options, '-pl', 'truffle', 'package'
    when 'cexts'
      no_openssl = options.delete('--no-openssl')
      build_ruby_su
      unless no_openssl
        cextc "#{JRUBY_DIR}/truffle/src/main/c/openssl"
      end
    when 'parser'
      jay = Utilities.find_repo('jay')
      ENV['PATH'] = "#{jay}/src:#{ENV['PATH']}"
      sh 'sh', 'tool/truffle/generate_parser'
      yytables = 'truffle/src/main/java/org/jruby/truffle/parser/parser/YyTables.java'
      File.write(yytables, File.read(yytables).gsub('package org.jruby.parser;', 'package org.jruby.truffle.parser.parser;'))
    when nil
      mvn env, *maven_options, 'package'
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
    options = args.last.is_a?(Hash) ? args.pop : {}

    jruby_args = ['-X+T']

    if ENV['JRUBY_OPTS'] && ENV['JRUBY_OPTS'].include?('-Xclassic')
      jruby_args.delete '-X+T'
    end

    {
      '--asm' => '--graal',
      '--stress' => '--graal',
      '--igv' => '--graal',
      '--trace' => '--graal',
    }.each_pair do |arg, dep|
      args.unshift dep if args.include?(arg)
    end

    unless args.delete('--no-core-load-path')
      jruby_args << "-Xtruffle.core.load_path=#{JRUBY_DIR}/truffle/src/main/ruby"
    end

    if args.delete('--graal')
      if ENV["RUBY_BIN"]
        # Assume that Graal is automatically set up if RUBY_BIN is set.
        # This will also warn if it's not.
      else
        javacmd, javacmd_options = Utilities.find_graal_javacmd_and_options
        env_vars["JAVACMD"] = javacmd
        jruby_args.push(*javacmd_options)
      end
    else
      jruby_args << '-Xtruffle.graal.warn_unless=false'
    end

    if args.delete('--stress')
      jruby_args << '-J-Dgraal.TruffleCompileImmediately=true'
      jruby_args << '-J-Dgraal.TruffleBackgroundCompilation=false'
      jruby_args << '-J-Dgraal.TruffleCompilationExceptionsAreFatal=true'
    end

    if args.delete('--js')
      jruby_args << '-J-cp'
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

    if args.delete('--profile')
      v = Utilities.truffle_version
      jruby_args << "-J-Xbootclasspath/a:#{M2_REPO}/com/oracle/truffle/truffle-debug/#{v}/truffle-debug-#{v}.jar"
      jruby_args << "-J-Dtruffle.profiling.enabled=true"
    end

    if args.delete('--igv')
      warn "warning: --igv might not work on master - if it does not, use truffle-head instead which builds against latest graal" if Utilities.git_branch == 'master'
      Utilities.ensure_igv_running
      if args.delete('--full')
        jruby_args << "-J-Dgraal.Dump=Truffle"
      else
        jruby_args << "-J-Dgraal.Dump=TruffleTree,PartialEscape"
      end
      jruby_args << "-J-Dgraal.PrintBackendCFG=false"
    end

    if args.delete('--infopoints')
      jruby_args << "-J-XX:+UnlockDiagnosticVMOptions" << "-J-XX:+DebugNonSafepoints"
      jruby_args << "-J-Dgraal.TruffleEnableInfopoints=true"
    end

    if args.delete('--fg')
      jruby_args << "-J-Dgraal.TruffleBackgroundCompilation=false"
    end

    if args.delete('--trace')
      jruby_args << "-J-Dgraal.TraceTruffleCompilation=true"
    end

    if args.delete('--no-print-cmd')
      options[:no_print_cmd] = true
    end

    if args.delete('--exec')
      options[:use_exec] = true
    end

    raw_sh env_vars, Utilities.find_jruby, *jruby_args, *args, options
  end

  # Same as #run but uses exec()
  def ruby(*args)
    run(*args, '--exec')
  end

  def e(*args)
    run '-e', args.join(' ')
  end

  def command_puts(*args)
    e 'puts begin', *args, 'end'
  end

  def command_p(*args)
    e 'p begin', *args, 'end'
  end

  def build_ruby_su(cext_dir=nil)
    abort "You need to set SULONG_HOME" unless SULONG_HOME

    # Ensure ruby.su is up-to-date
    ruby_cext_api = "#{JRUBY_DIR}/truffle/src/main/c/cext"
    ruby_c = "#{JRUBY_DIR}/truffle/src/main/c/cext/ruby.c"
    ruby_h = "#{JRUBY_DIR}/lib/ruby/truffle/cext/ruby.h"
    ruby_su = "#{JRUBY_DIR}/lib/ruby/truffle/cext/ruby.su"
    if cext_dir != ruby_cext_api and (newer?(ruby_h, ruby_su) or newer?(ruby_c, ruby_su))
      puts "Compiling outdated ruby.su"
      cextc ruby_cext_api
    end
  end
  private :build_ruby_su

  def cextc(cext_dir, test_gem=false, *clang_opts)
    build_ruby_su(cext_dir)

    is_ruby = cext_dir == "#{JRUBY_DIR}/truffle/src/main/c/cext"
    gem_name = if is_ruby
                 "ruby"
               else
                 File.basename(cext_dir)
               end

    gem_dir = if cext_dir.start_with?("#{JRUBY_DIR}/truffle/src/main/c")
                cext_dir
              elsif test_gem
                "#{JRUBY_DIR}/test/truffle/cexts/#{gem_name}/ext/#{gem_name}/"
              elsif cext_dir.start_with?(JRUBY_DIR)
                Dir.glob(ENV['GEM_HOME'] + "/gems/#{gem_name}*/")[0] + "ext/#{gem_name}/"
              else
                cext_dir + "/ext/#{gem_name}/"
              end
    copy_target = if is_ruby
                    "#{JRUBY_DIR}/lib/ruby/truffle/cext/ruby.su"
                  elsif cext_dir == "#{JRUBY_DIR}/truffle/src/main/c/openssl"
                    "#{JRUBY_DIR}/truffle/src/main/c/openssl/openssl.su"
                  else
                    "#{JRUBY_DIR}/test/truffle/cexts/#{gem_name}/lib/#{gem_name}/#{gem_name}.su"
                  end

    Dir.chdir(gem_dir) do
      STDERR.puts "in #{gem_dir}..."
      run("extconf.rb")
      raw_sh("make")
      FileUtils.copy_file("#{gem_name}.su", copy_target)
    end
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
      test_cexts
    when 'bundle' then test_bundle(*rest)
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

    if args.count { |arg| !arg.start_with?('-') } == 0
      args += File.readlines("#{JRUBY_DIR}/test/mri_truffle.index").grep(/^[^#]\w+/).map(&:chomp)
    end

    command = %w[test/mri/runner.rb -v --color=never --tty=no -q]
    run(env_vars, *jruby_args, *command, *args)
  end
  private :test_mri

  def test_compiler(*args)
    jruby_opts = []

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-cp'
      jruby_opts << Utilities.find_graal_js
    end

    jruby_opts << '-Xtruffle.exceptions.print_java=true'

    env = { "JRUBY_OPTS" => jruby_opts.join(' ') }

    Dir["#{JRUBY_DIR}/test/truffle/compiler/*.sh"].sort.each do |test_script|
      sh env, test_script
    end
  end
  private :test_compiler

  def test_cexts(*args)
    no_libxml = args.delete('--no-libxml')
    no_openssl = args.delete('--no-openssl')

    # Test that we can compile and run some basic C code that uses libxml and openssl

    unless no_libxml
      clang '-S', '-emit-llvm', "-I#{LIBXML_INCLUDE}", 'test/truffle/cexts/xml/main.c', '-o', 'test/truffle/cexts/xml/main.ll'
      out, _ = sulong_run("-l#{LIBXML_LIB}", 'test/truffle/cexts/xml/main.ll', {capture: true})
      raise unless out == "7\n"
    end

    unless no_openssl
      clang '-S', '-emit-llvm', "-I#{OPENSSL_INCLUDE}", 'test/truffle/cexts/xopenssl/main.c', '-o', 'test/truffle/cexts/xopenssl/main.ll'
      out, _ = sulong_run("-l#{OPENSSL_LIB}", 'test/truffle/cexts/xopenssl/main.ll', {capture: true})
      raise unless out == "5d41402abc4b2a76b9719d911017c592\n"
    end

    # Test that we can run those same test when they're build as a .su and we load the code and libraries from that

    unless no_libxml
      sulong_link '-o', 'test/truffle/cexts/xml/main.su', '-l', "#{LIBXML_LIB}", 'test/truffle/cexts/xml/main.ll'
      out, _ = sulong_run('test/truffle/cexts/xml/main.su', {capture: true})
      raise unless out == "7\n"
    end

    unless no_openssl
      sulong_link '-o', 'test/truffle/cexts/xopenssl/main.su', '-l', "#{OPENSSL_LIB}", 'test/truffle/cexts/xopenssl/main.ll'
      out, _ = sulong_run('test/truffle/cexts/xopenssl/main.su', {capture: true})
      raise unless out == "5d41402abc4b2a76b9719d911017c592\n"
    end

    # Test that we can compile and run some very basic C extensions

    begin
      output_file = 'cext-output.txt'
      ['minimum', 'method', 'module', 'globals', 'xml', 'xopenssl'].each do |gem_name|
        next if gem_name == 'xml' && no_libxml
        next if gem_name == 'xopenssl' && no_openssl
        dir = "#{JRUBY_DIR}/test/truffle/cexts/#{gem_name}"
        cextc dir, true
        name = File.basename(dir)
        next if gem_name == 'globals' # globals is excluded just for running
        run '--graal', "-I#{dir}/lib", "#{dir}/bin/#{name}", :out => output_file
        unless File.read(output_file) == File.read("#{dir}/expected.txt")
          abort "c extension #{dir} didn't work as expected"
        end
      end
    ensure
      File.delete output_file rescue nil
    end

    # Test that we can compile and run some real C extensions

    if ENV['GEM_HOME']
      tests = [
          ['oily_png', ['chunky_png-1.3.6', 'oily_png-1.2.0'], ['oily_png']],
          ['psd_native', ['chunky_png-1.3.6', 'oily_png-1.2.0', 'bindata-2.3.1', 'hashie-3.4.4', 'psd-enginedata-1.1.1', 'psd-2.1.2', 'psd_native-1.1.3'], ['oily_png', 'psd_native']],
          ['nokogiri', [], ['nokogiri']]
      ]

      tests.each do |gem_name, dependencies, libs, gem_root|
        next if gem_name == 'nokogiri' # nokogiri totally excluded
        next if gem_name == 'nokogiri' && no_libxml
        gem_root = "#{JRUBY_DIR}/test/truffle/cexts/#{gem_name}"
        cextc gem_root, false, '-Werror=implicit-function-declaration'

        next if gem_name == 'psd_native' # psd_native is excluded just for running
        run '--graal',
          *dependencies.map { |d| "-I#{ENV['GEM_HOME']}/gems/#{d}/lib" },
          *libs.map { |l| "-I#{JRUBY_DIR}/test/truffle/cexts/#{l}/lib" },
          "#{JRUBY_DIR}/test/truffle/cexts/#{gem_name}/test.rb", gem_root
      end
    end
  end
  private :test_cexts

  def test_report(component)
    test 'specs', '--truffle-formatter', component
    sh 'ant', '-f', 'spec/truffle/buildTestReports.xml'
  end
  private :test_cexts
  
  def check_test_port
    lsof = `lsof -i :14873`
    unless lsof.empty?
      STDERR.puts 'Someone is already listening on port 14873 - our tests can\'t run'
      STDERR.puts lsof
      exit 1
    end
  end

  def test_integration(env={}, *args)
    env_vars   = env
    jruby_opts = []

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-cp'
      jruby_opts << Utilities.find_graal_js
    end

    if ENV['SL_JAR']
      jruby_opts << '-J-cp'
      jruby_opts << Utilities.find_sl
    end

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    tests_path             = "#{JRUBY_DIR}/test/truffle/integration"
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{tests_path}/#{test_names}.sh"].sort.each do |test_script|
      check_test_port
      sh env_vars, test_script
    end
  end
  private :test_integration

  def test_gems(env={}, *args)
    env_vars   = env
    jruby_opts = []

    if ENV['GRAAL_JS_JAR']
      jruby_opts << '-J-cp'
      jruby_opts << Utilities.find_graal_js
    end

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    tests_path             = "#{JRUBY_DIR}/test/truffle/gems"
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{tests_path}/#{test_names}.sh"].sort.each do |test_script|
      next if test_script.end_with?('/install-gems.sh')
      check_test_port
      sh env_vars, test_script
    end
  end
  private :test_gems

  def test_ecosystem(env={}, *args)
    env_vars   = env
    jruby_opts = []

    env_vars["JRUBY_OPTS"] = jruby_opts.join(' ')

    unless File.exist? "#{JRUBY_DIR}/../jruby-truffle-gem-test-pack/gem-testing"
      raise 'missing ../jruby-truffle-gem-test-pack/gem-testing directory'
    end

    tests_path             = "#{JRUBY_DIR}/test/truffle/ecosystem"
    single_test            = !args.empty?
    test_names             = single_test ? '{' + args.join(',') + '}' : '*'

    Dir["#{tests_path}/#{test_names}.sh"].sort.each do |test_script|
      sh env_vars, test_script
    end
  end
  private :test_ecosystem

  def test_bundle(*args)
    gems = [{:url => "https://github.com/sstephenson/hike", :commit => "3abf0b3feb47c26911f8cedf2cd409471fd26da1"}]
    gems.each do |gem|
      gem_url = gem[:url]
      name = gem_url.split('/')[-1]
      require 'tmpdir'
      temp_dir = Dir.mktmpdir(name)
      begin
        Dir.chdir(temp_dir) do
          puts "Cloning gem #{name} into temp directory: #{temp_dir}"
          raw_sh(*['git', 'clone', gem_url])
        end
        gem_dir = File.join(temp_dir, name)
        gem_home = if ENV['GEM_HOME']
                     ENV['GEM_HOME']
                   else
                     tmp_home = File.join(temp_dir, "gem_home")
                     Dir.mkdir(tmp_home)
                     puts "Using temporary GEM_HOME:#{tmp_home}"
                     tmp_home
                   end
        Dir.chdir(gem_dir) do
          if gem.has_key?(:commit)
            raw_sh(*['git', 'checkout', gem[:commit]])
          end
          run({'GEM_HOME' => gem_home}, *["-rbundler-workarounds", "-S", "gem", "install", "bundler", "-v","1.13.5"])
          run({'GEM_HOME' => gem_home}, *["-rbundler-workarounds", "-S", "bundle", "install"])
          # Need to workaround ruby_install name `jruby-truffle` in the gems binstubs (command can't be found )
          # or figure out how to get it on the path to get `bundle exec rake` working
          #run({'GEM_HOME' => gem_home}, *["-rbundler-workarounds", "-S", "bundle", "exec", "rake"])
        end
      ensure
        FileUtils.remove_entry temp_dir
      end
    end
  end

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
      options += %w[--excl-tag slow]
      options << "-T-Xtruffle.backtraces.limit=4" unless args[-2..-1] == %w[-t ruby]
    end

    if args.delete('--graal')
      javacmd, javacmd_options = Utilities.find_graal_javacmd_and_options
      env_vars["JAVACMD"] = javacmd
      options.concat javacmd_options.map { |o| "-T#{o}" }
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

    if ENV['CI']
      # Need lots of output to keep Travis happy
      options += %w[--format specdoc]
    end

    mspec env_vars, command, *options, *args
  end
  private :test_specs

  def test_tck(*args)
    exec 'mx', 'rubytck' if Utilities.mx?
    env = {'JRUBY_BUILD_MORE_QUIET' => 'true'}
    mvn env, *args, '-Ptck'
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
      out, err = run '-Xtruffle.metrics.memory_used_on_exit=true', '-J-verbose:gc', *args, {capture: true, no_print_cmd: true}
      samples.push memory_allocated(out+err)
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
      start = Time.now
      out, err = run '-Xtruffle.metrics.time=true', *args, {capture: true, no_print_cmd: true}
      finish = Time.now
      samples.push get_times(err, finish - start)
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
    args.map! do |a|
      if a.include?('.rb')
        benchmark = Utilities.find_benchmark(a)
        raise 'benchmark not found' unless File.exist?(benchmark)
        benchmark
      else
        a
      end
    end
    
    benchmark_ruby = ENV['JT_BENCHMARK_RUBY']
    
    run_args = []

    unless benchmark_ruby
      run_args.push '--graal' unless args.delete('--no-graal') || args.include?('list')
      run_args.push '-J-Dgraal.TruffleCompilationExceptionsAreFatal=true'
    end
    
    run_args.push "-I#{Utilities.find_gem('deep-bench')}/lib" rescue nil
    run_args.push "-I#{Utilities.find_gem('benchmark-ips')}/lib" rescue nil
    run_args.push "#{Utilities.find_gem('benchmark-interface')}/bin/benchmark"
    run_args.push *args
    
    if benchmark_ruby
      sh benchmark_ruby, *run_args
    else
      run *run_args
    end
  end

  def where(*args)
    case args.shift
    when 'repos'
      args.each do |a|
        puts Utilities.find_repo(a)
      end
    end
  end

  def check_ambiguous_arguments
    clean
    # modify pom
    pom = "#{JRUBY_DIR}/truffle/pom.rb"
    contents = File.read(pom)
    contents.sub!(/^(\s+)('-J-Dfile.encoding=UTF-8')(.+\n)(?!\1'-parameters')/) do
      "#{$1}#{$2},\n#{$1}'-parameters'#{$3}"
    end
    File.write pom, contents

    build
    run({ "TRUFFLE_CHECK_AMBIGUOUS_OPTIONAL_ARGS" => "true" }, '-e', 'exit')
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
      while ['truffle', 'cexts', 'parser', '--offline', '--build-pack', '--no-openssl'].include?(args.first)
        command << args.shift
      end
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
