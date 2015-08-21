#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# A workflow tool for JRuby+Truffle development

# Recommended: function jt { ruby tool/jt.rb "$@"; }

require 'fileutils'
require 'digest/sha1'

JRUBY_DIR = File.expand_path('../..', __FILE__)

JDEBUG_PORT = 51819
JDEBUG = "-J-agentlib:jdwp=transport=dt_socket,server=y,address=#{JDEBUG_PORT},suspend=y"
JEXCEPTION = "-Xtruffle.exceptions.print_java=true"

# wait for sub-processes to handle the interrupt
trap(:INT) {}

module Utilities

  def self.graal_version
    File.foreach("#{JRUBY_DIR}/truffle/pom.rb") do |line|
      if /jar 'com.oracle:truffle:(\d+\.\d+(?:-SNAPSHOT)?)'/ =~ line
        break $1
      end
    end
  end

  def self.find_graal
    graal_locations = [
      ENV['GRAAL_BIN'],
      ENV["GRAAL_BIN_#{mangle_for_env(git_branch)}"],
      "graalvm-jdk1.8.0/bin/java",
      "../graalvm-jdk1.8.0/bin/java",
      "../../graalvm-jdk1.8.0/bin/java",
    ].compact.map { |path| File.expand_path(path, JRUBY_DIR) }

    not_found = -> {
      raise "couldn't find graal - download it from http://lafo.ssw.uni-linz.ac.at/graalvm/ and extract it into the JRuby repository or parent directory"
    }

    graal_locations.find(not_found) do |location|
      File.executable?(location)
    end
  end

  def self.git_branch
    @git_branch ||= `git rev-parse --abbrev-ref HEAD`.strip
  end

  def self.mangle_for_env(name)
    name.upcase.tr('-', '_')
  end

  def self.find_graal_parent
    File.expand_path('../../../../../graal', find_graal)
  end

  def self.find_graal_mx
    mx = File.expand_path('../../../../../mx/mx', find_graal)
    raise "couldn't find mx - set GRAAL_BIN, and you need to use a checkout of Graal, not a build" unless File.executable?(mx)
    mx
  end

  def self.igv_running?
    `ps a`.lines.any? { |p| p.include? 'mx/mx.py igv' }
  end

  def self.ensure_igv_running
    unless igv_running?
      Dir.chdir(find_graal_parent) do
        spawn "#{find_graal_mx} igv", pgroup: true
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
    puts "$ #{printable_cmd(args)}"
    result = system(*args)
    unless result
      $stderr.puts "FAILED (#{$?}): #{printable_cmd(args)}"
      exit $?.exitstatus
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
    sh './mvnw', *args
  end

  def mspec(command, *args)
    env_vars = {}
    if command.is_a?(Hash)
      env_vars = command
      command, *args = args
    end

    if ENV["JRUBY_ECLIPSE"] == "true"
      args.unshift "-ttool/jruby_eclipse"
    end

    sh env_vars, 'ruby', 'spec/mspec/bin/mspec', command, '--config', 'spec/truffle/truffle.mspec', *args
  end
end

module Commands
  include ShellUtils

  def help
    puts 'jt build                                       build'
    puts 'jt build truffle                               build only the Truffle part, assumes the rest is up-to-date'
    puts 'jt clean                                       clean'
    puts 'jt irb                                         irb'
    puts 'jt rebuild                                     clean and build'
    puts 'jt run [options] args...                       run JRuby with -X+T and args'
    puts '    --graal         use Graal (set GRAAL_BIN or it will try to automagically find it)'
    puts '    --asm           show assembly (implies --graal)'
    puts '    --server        run an instrumentation server on port 8080'
    puts '    --igv           make sure IGV is running and dump Graal graphs after partial escape (implies --graal)'
    puts '    --jdebug        run a JDWP debug server on 8000'
    puts '    --jexception[s] print java exceptions'
    puts 'jt e 14 + 2                                    evaluate an expression'
    puts 'jt puts 14 + 2                                 evaluate and print an expression'
    puts 'jt test                                        run all mri tests and specs'
    puts 'jt test mri                                    run mri tests'
    puts 'jt test specs                                  run all specs'
    puts 'jt test specs fast                             run all specs except sub-processes, GC, sleep, ...'
    puts 'jt test spec/ruby/language                     run specs in this directory'
    puts 'jt test spec/ruby/language/while_spec.rb       run specs in this file'
    puts 'jt test pe                                     run partial evaluation tests'
    puts 'jt tag spec/ruby/language                      tag failing specs in this directory'
    puts 'jt tag spec/ruby/language/while_spec.rb        tag failing specs in this file'
    puts 'jt tag all spec/ruby/language                  tag all specs in this file, without running them'
    puts 'jt untag spec/ruby/language                    untag passing specs in this directory'
    puts 'jt untag spec/ruby/language/while_spec.rb      untag passing specs in this file'
    puts 'jt bench debug [--ruby-backtrace] benchmark    run a single benchmark with options for compiler debugging'
    puts 'jt bench reference [benchmarks]                run a set of benchmarks and record a reference point'
    puts 'jt bench compare [benchmarks]                  run a set of benchmarks and compare against a reference point'
    puts '    benchmarks can be any benchmarks or group of benchmarks supported'
    puts '    by bench9000, eg all, classic, chunky, 3, 5, 10, 15 - default is 5'
    puts 'jt findbugs                                    run findbugs'
    puts 'jt findbugs report                             run findbugs and generate an HTML report'
    puts 'jt install ..../graal/mx/suite.py              install a JRuby distribution into an mx suite'
    puts
    puts 'you can also put build or rebuild in front of any command'
    puts
    puts 'recognised environment variables:'
    puts
    puts '  GRAAL_BIN                                    GraalVM executable (java command) to use'
    puts '  GRAAL_BIN_...git_branch_name...              GraalVM executable to use for a given branch'
    puts '           branch names are mangled - eg truffle-head becomes GRAAL_BIN_TRUFFLE_HEAD'
  end

  def build(project = nil)
    case project
    when 'truffle'
      mvn '-pl', 'truffle', 'package'
    when nil
      mvn 'package'
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
    clean
    build
  end

  def run(*args)
    env_vars = args.first.is_a?(Hash) ? args.shift : {}
    jruby_args = %w[-X+T]

    { '--asm' => '--graal', '--igv' => '--graal' }.each_pair do |arg, dep|
      args.unshift dep if args.include?(arg)
    end

    if args.delete('--graal')
      env_vars["JAVACMD"] = Utilities.find_graal
      jruby_args << '-J-server'
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
      jruby_args += %w[-Xtruffle.instrumentation_server_port=8080 -Xtruffle.passalot=1]
    end

    if args.delete('--igv')
      warn "warning: --igv might not work on master - if it does not, use truffle-head instead which builds against latest graal" if Utilities.git_branch == 'master'
      Utilities.ensure_igv_running
      jruby_args += %w[-J-Djvmci.options=Dump=TrufflePartialEscape]
    end

    if ENV["JRUBY_ECLIPSE"] == "true"
      jruby_bin = "#{JRUBY_DIR}/tool/jruby_eclipse"
    else
      jruby_bin = "#{JRUBY_DIR}/bin/jruby"
    end

    raw_sh env_vars, jruby_bin, *jruby_args, *args
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

  def test(*args)
    path, *rest = args

    case path
    when nil
      test_specs
      test_mri
    when 'pe' then test_pe(*rest)
    when 'specs' then test_specs(*rest)
    when 'mri' then test_mri(*rest)
    else
      if File.expand_path(path).start_with?("#{JRUBY_DIR}/test")
        test_mri(*args)
      else
        test_specs(*args)
      end
    end
  end

  def test_pe(*args)
    run('--graal', *args, 'test/truffle/pe/pe.rb')
  end
  private :test_pe

  def test_specs(*args)
    env_vars = {}

    options = %w[--excl-tag fails]

    if args.first == 'fast'
      args.shift
      options += %w[--excl-tag slow]
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

    mspec env_vars, 'run', *options, *args
  end
  private :test_specs

  def tag(path, *args)
    return tag_all(*args) if path == 'all'
    mspec 'tag', '--add', 'fails', '--fail', path, *args
  end

  # Add tags to all given examples without running them. Useful to avoid file exclusions.
  def tag_all(*args)
    mspec 'tag', *%w[--unguarded --all --dry-run --add fails], *args
  end
  private :tag_all

  def untag(path, *args)
    puts
    puts "WARNING: untag is currently not very reliable - run `jt test #{[path,*args] * ' '}` after and manually annotate any new failures"
    puts
    mspec 'tag', '--del', 'fails', '--pass', path, *args
  end

  def bench(command, *args)
    bench_dir = Utilities.find_bench
    env_vars = {
      "JRUBY_9000_DEV_DIR" => JRUBY_DIR,
      "GRAAL_BIN" => Utilities.find_graal,
    }
    bench_args = ["-I#{bench_dir}/lib", "#{bench_dir}/bin/bench"]
    case command
    when 'debug'
      if args.delete '--ruby-backtrace'
        compilation_exceptions_behaviour = ",+TruffleCompilationExceptionsAreThrown"
      else
        compilation_exceptions_behaviour = ",+TruffleCompilationExceptionsAreFatal"
      end
      env_vars = env_vars.merge({'JRUBY_OPTS' => "-J-Djvmci.options=+TraceTruffleCompilation,+DumpOnError#{compilation_exceptions_behaviour}"})
      bench_args += ['score', 'jruby-9000-dev-truffle-graal', '--show-commands', '--show-samples']
      raise 'specify a single benchmark for run - eg classic-fannkuch-redux' if args.size != 1
    when 'reference'
      bench_args += ['reference', 'jruby-9000-dev-truffle-graal', '--show-commands']
      args << "5" if args.empty?
    when 'compare'
      bench_args += ['compare-reference', 'jruby-9000-dev-truffle-graal']
      args << "5" if args.empty?
    else
      raise ArgumentError, command
    end
    raw_sh env_vars, "ruby", *bench_args, *args
  end

  def findbugs(report=nil)
    case report
    when 'report'
      sh 'tool/truffle-findbugs.sh', '--report'
      sh 'open', 'truffle-findbugs-report.html'
    when nil
      sh 'tool/truffle-findbugs.sh'
    else
      raise ArgumentError, report
    end
  end

  def check_ambiguous_arguments
    ENV.delete "JRUBY_ECLIPSE" # never run from the Eclipse launcher here
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
    FileUtils::Verbose.rm_r "#{JRUBY_DIR}/truffle/target/classes"
    build('truffle')
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
