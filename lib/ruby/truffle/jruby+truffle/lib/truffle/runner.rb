# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'optparse'
require 'pp'
require 'yaml'
require 'fileutils'
require 'shellwords'
require 'pathname'
require 'rbconfig'
require 'rubygems'

module Truffle
  class Runner
    module Utils
      def execute_cmd(cmd, dir: nil, raise: true, print: false)
        result      = nil
        system_call = proc do
          begin
            pid = Process.spawn(*print_cmd(cmd, dir, print))
            Process.wait pid
            result = $?.success?
          rescue SignalException => e
            # terminate the child process on signal received
            Process.kill 'TERM', pid
            raise e
          end
        end

        if dir
          Dir.chdir dir, &system_call
        else
          system_call.call
        end

        raise 'command failed' if raise && !result

        return result
      end

      def print_cmd(cmd, dir, print)
        return cmd unless print

        formatted_cmd = if String === cmd
                          cmd
                        else
                          cmd.map do |v|
                            if Hash === v
                              v.map { |k, v| "#{k}=#{v}" }.join(' ')
                            else
                              Shellwords.escape v
                            end
                          end.join(' ')
                        end
        puts '$ ' + formatted_cmd + (dir ? " (in #{dir})" : '')
        return cmd
      end

    end

    include Utils

    attr_reader :options

    EXECUTABLE        = File.basename($PROGRAM_NAME)
    BRANDING          = EXECUTABLE.include?('jruby') ? 'JRuby+Truffle' : 'RubyTruffle'
    LOCAL_CONFIG_FILE = '.jruby+truffle.yaml'
    ROOT              = Pathname(__FILE__).dirname.parent.parent.expand_path
    JRUBY_PATH        = ROOT.join('../../../..').expand_path

    module OptionBlocks
      STORE_NEW_VALUE         = -> (new, old, _) { new }
      STORE_NEW_NEGATED_VALUE = -> (new, old, _) { !new }
      ADD_TO_ARRAY            = -> (new, old, _) { old << new }
      MERGE_TO_HASH           = -> ((k, v), old, _) { old.merge k => v }
    end

    include OptionBlocks

    begin
      apply_pattern      = -> (pattern, old, options) do
        Dir.glob(pattern) do |file|
          if options[:exclude_pattern].any? { |p| /#{p}/ =~ file }
            puts "skipped: #{file}" if verbose?
            next
          end
          options[:require] << File.expand_path(file)
        end
        old << pattern
      end

      # Format:
      #   subcommand_name: {
      #     :'option_name (also used as a key in yaml)' => [option_parser_on_method_args,
      #                                                     -> (new_value, old_value) { result_of_this_block_is_stored },
      #                                                     default_value]
      #   }
      OPTION_DEFINITIONS = {
          global: {
              verbose:             ['-v', '--verbose', 'Run verbosely (prints options)', STORE_NEW_VALUE, false],
              help:                ['-h', '--help', 'Show this message', STORE_NEW_VALUE, false],
              debug_port:          ['--debug-port PORT', 'Debug port', STORE_NEW_VALUE, '51819'],
              debug_option:        ['--debug-option OPTION', 'Debug JVM option', STORE_NEW_VALUE,
                                    '-J-agentlib:jdwp=transport=dt_socket,server=y,address=%d,suspend=y'],
              truffle_bundle_path: ['--truffle-bundle-path NAME', 'Bundle path', STORE_NEW_VALUE, '.jruby+truffle_bundle'],
              graal_path:          ['--graal-path PATH', 'Path to Graal', STORE_NEW_VALUE, (JRUBY_PATH + '../GraalVM-0.10/jre/bin/javao').to_s],
              mock_load_path:      ['--mock-load-path PATH',
                                    'Path of mocks & monkey-patches (prepended in $:, relative to --truffle_bundle_path)',
                                    STORE_NEW_VALUE, 'mocks'],
              use_fs_core:         ['--[no-]use-fs-core', 'Use core from the filesystem rather than the JAR',
                                    STORE_NEW_VALUE, true],
              bundle_options:      ['--bundle-options OPTIONS', 'bundle options separated by space', STORE_NEW_VALUE, ''],
              configuration:       ['--config GEM_NAME', 'Load configuration for specified gem', STORE_NEW_VALUE, nil],
              dir:                 ['--dir DIRECTORY', 'Set working directory', STORE_NEW_VALUE, nil],
          },
          setup:  {
              help:    ['-h', '--help', 'Show this message', STORE_NEW_VALUE, false],
              before:  ['--before SH_CMD', 'Commands to execute before setup', ADD_TO_ARRAY, []],
              after:   ['--after SH_CMD', 'Commands to execute after setup', ADD_TO_ARRAY, []],
              file:    ['--file NAME,CONTENT', Array, 'Create file in truffle_bundle_path', MERGE_TO_HASH, {}],
              without: ['--without GROUP', 'Do not install listed gem group by bundler', ADD_TO_ARRAY, []]
          },
          run:    {
              help:             ['-h', '--help', 'Show this message', STORE_NEW_VALUE, false],
              interpreter_path: ['--interpreter-path PATH', "Path to #{BRANDING} interpreter executable", STORE_NEW_VALUE,
                                 JRUBY_PATH.join('bin', 'jruby')],
              no_truffle:       ['-n', '--no-truffle', "Use conventional JRuby instead of #{BRANDING}", STORE_NEW_NEGATED_VALUE, false],
              graal:            ['-g', '--graal', 'Run on graal', STORE_NEW_VALUE, false],
              build:            ['-b', '--build', 'Run `jt build` using conventional JRuby', STORE_NEW_VALUE, false],
              rebuild:          ['--rebuild', 'Run `jt rebuild` using conventional JRuby', STORE_NEW_VALUE, false],
              debug:            ['-d', '--debug', 'JVM remote debugging', STORE_NEW_VALUE, false],
              require:          ['-r', '--require FILE', 'Files to require, same as Ruby\'s -r', ADD_TO_ARRAY, []],
              require_pattern:  ['--require-pattern DIR_GLOB_PATTERN', 'Files matching the pattern will be required', apply_pattern, []],
              exclude_pattern:  ['--exclude-pattern REGEXP', 'Files matching the regexp will not be required by --require-pattern (applies to subsequent --require-pattern options)', ADD_TO_ARRAY, []],
              load_path:        ['-I', '--load-path LOAD_PATH', 'Paths to add to load path, same as Ruby\'s -I', ADD_TO_ARRAY, [ROOT.join('lib').to_s]],
              executable:       ['-S', '--executable NAME', 'finds and runs an executable of a gem', STORE_NEW_VALUE, nil],
              jexception:       ['--jexception', 'print Java exceptions', STORE_NEW_VALUE, false],
              environment:      ['--environment NAME,VALUE', Array, 'Set environment variables', MERGE_TO_HASH, {}],
              xmx:              ['--xmx SIZE', 'Max memory size', STORE_NEW_VALUE, '2G'],
              no_asserts:       ['--no-asserts', 'Disable asserts -ea -esa', STORE_NEW_NEGATED_VALUE, false]
          },
          ci:     {
              batch:      ['--batch FILE', 'Run batch of ci tests supplied in a file. One ci command options per line. If FILE is in or stdin it reads from $stdin.',
                           STORE_NEW_VALUE, nil],
              definition: ['--definition NAME', 'Specify which definition file to use', STORE_NEW_VALUE, nil],
              help:       ['-h', '--help', 'Show this message', STORE_NEW_VALUE, false]
          },
          clean:  {
              help: ['-h', '--help', 'Show this message', STORE_NEW_VALUE, false]
          },
          readme: {
              help: ['-h', '--help', 'Show this message', STORE_NEW_VALUE, false]
          }
      }.each { |group, options| options.each { |name, definition| definition.last.freeze } }
    end

    begin
      global_help = <<-TXT.gsub(/^ {6}/, '')
      Usage: #{EXECUTABLE} [options] [subcommand [subcommand-options]]
      Subcommands are: #{(OPTION_DEFINITIONS.keys - [:global]).map(&:to_s).join(', ')}

      Allows to execute gem/app on #{BRANDING} until it's more complete. Environment
      has to be set up first with setup subcommand then run subcommand can be used.

      Run #{EXECUTABLE} readme to see README.
      Run #{EXECUTABLE} subcommand --help to see its help.

      TXT

      setup_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] setup [subcommand-options]

      Creates environment for running gem/app on #{BRANDING}.

      TXT

      run_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] run [subcommand-options] -- [ruby-options] [RUBY-FILE]
      Usage: #{EXECUTABLE} [options] run [subcommand-options] -- [prepended-ruby-options] -- [appended-ruby-options] [RUBY-FILE]
      Usage: #{EXECUTABLE} [options] run [subcommand-options] -S GEM_EXECUTABLE -- [ruby-options] -- [gem-executable-options]
      Usage: #{EXECUTABLE} [options] run [subcommand-options] -S GEM_EXECUTABLE -- [gem-executable-options]
      ('--' divides different kind of options)

      Runs file, -e expr, etc in setup environment on #{BRANDING}

      Examples: #{EXECUTABLE} run -- a_file.rb
                #{EXECUTABLE} run -- -S irb
                #{EXECUTABLE} run -- -e 'puts :v'
                #{EXECUTABLE} run -- -I test test/a_test_file_test.rb
                #{EXECUTABLE} run -- -J-Xmx2G -- -I test test/a_test_file_test.rb
                #{EXECUTABLE} --verbose run -S rspec -- -J-Xmx2G -- spec/some_spec.rb --format progress

      TXT

      clean_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] clean [subcommand-options]

      Deletes all files created by setup subcommand.

      TXT

      ci_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] ci [subcommand-options] GEM_NAME [options-declared-in-CI-definition]

      Runs CI tests for predefined gems or it uses default CI definition. CI Definitions
      are stored in gem_ci directory. The CI definition files are evaluated in CIEnvironment
      instances which are providing the helper methods.

      The command is creating a directory for testing based on GEM_NAME in current
      working directory (change with --dir).

      Examples: #{EXECUTABLE} ci activesupport
                    (Runs gem_ci/activesupport.rb CI definition)
                #{EXECUTABLE} ci concurrent-ruby --git https://github.com/ruby-concurrency/concurrent-ruby.git
                    (Runs gem_ci/default.rb CI definition which declared and uses git option)
                #{EXECUTABLE} ci --help
                    (Shows options of ci command)
                #{EXECUTABLE} ci concurrent-ruby --help
                    (Shows options defined by ci definition for concurrent-ruby)

      TXT

      HELP = { global: global_help, setup: setup_help, run: run_help, clean: clean_help, ci: ci_help }
    end


    def initialize(argv)
      @options        = construct_default_options
      @option_parsers = build_option_parsers

      @subcommand, *argv_after_global = @option_parsers[:global].order argv
      @called_from_dir                = Dir.pwd
      @options[:global][:dir]         = File.expand_path(@options[:global][:dir] || @called_from_dir)

      Dir.chdir @options[:global][:dir] do
        puts "pwd: #{Dir.pwd}" if verbose?

        load_gem_configuration
        load_local_configuration

        if @subcommand.nil?
          print_options
          help :global
        end
        help :global if @options[:global][:help]

        @subcommand = @subcommand.to_sym

        subcommand_option_parser = @option_parsers[@subcommand] || raise("unknown subcommand: #{@subcommand}")
        @argv_after_subcommand   = subcommand_option_parser.order argv_after_global

        print_options
        help @subcommand if @options[@subcommand][:help] && @subcommand != :readme
      end
    end

    def run
      Dir.chdir @options[:global][:dir] do
        send "subcommand_#{@subcommand}", @argv_after_subcommand
      end
    end

    def print_options
      if verbose?
        puts 'Options:'
        pp @options
      end
    end

    private

    def verbose?
      @options[:global][:verbose]
    end

    def build_option_parsers
      option_parsers = OPTION_DEFINITIONS.each_with_object({}) do |(name, parser_options), parsers|
        parsers[name] = build_option_parser(parser_options, @options.fetch(name))
      end

      option_parsers.each { |key, option_parser| option_parser.banner = HELP[key] }
    end

    def self.build_option_parser(parser_options, options_hash, option_parser: OptionParser.new)
      parser_options.each do |option, data|
        *args, description, block, default = data

        option_parser.on(*args, description + " (default: #{default.inspect})") do |new_value|
          old_value            = options_hash[option]
          options_hash[option] = instance_exec new_value, old_value, options_hash, &block
        end
      end

      option_parser
    end

    def build_option_parser(parser_options, options_hash)
      self.class.build_option_parser(parser_options, options_hash)
    end

    def load_gem_configuration
      candidates = @options[:global][:configuration] ? [@options[:global][:configuration]] : Dir['*.gemspec']

      if candidates.size == 1
        gem_name, _ = candidates.first.split('.')
        yaml_path   = ROOT.join 'gem_configurations', "#{gem_name}.yaml"
      end

      apply_yaml_to_configuration(yaml_path)
    end

    def load_local_configuration
      yaml_path = File.join Dir.pwd, LOCAL_CONFIG_FILE
      apply_yaml_to_configuration(yaml_path)
    end

    def apply_yaml_to_configuration(yaml_path)
      if yaml_path && File.exist?(yaml_path)
        yaml_data = YAML.load_file(yaml_path)
        @options  = deep_merge @options, yaml_data
        puts "loading #{yaml_path}"
      end
    end

    def construct_default_options
      OPTION_DEFINITIONS.each_with_object({}) do |(group, group_options), options|
        options[group] = default_option_values(group_options)
      end
    end

    def self.default_option_values(group_options)
      group_options.each_with_object({}) do |(option, data), group_option_defaults|
        *args, block, default = data
        unless [TrueClass, FalseClass, NilClass, Fixnum].any? { |v| v === default }
          default = default.dup
        end
        group_option_defaults[option] = default
      end
    end

    def default_option_values(group_options)
      self.class.default_option_values(group_options)
    end

    def help(key = nil)
      parsers = if key
                  [@option_parsers[key]]
                else
                  @option_parsers.values
                end
      puts *parsers
      exit
    end

    def deep_merge(a, b)
      if Hash === a
        if Hash === b
          return a.merge!(b) { |k, ov, nv| deep_merge ov, nv }
        else
          return a
        end
      end

      if Array === a
        if Array === b
          return a.concat b
        else
          return a
        end
      end

      b
    end

    def subcommand_setup(rest)
      bundle_options = @options[:global][:bundle_options].split(' ')
      bundle_path    = File.expand_path(@options[:global][:truffle_bundle_path])

      @options[:setup][:before].each do |cmd|
        execute_cmd cmd
      end

      bundle_cli([*bundle_options,
                  'install',
                  '--standalone',
                  '--path', bundle_path,
                  *(['--without', @options[:setup][:without].join(' ')] unless @options[:setup][:without].empty?)])

      jruby_truffle_path = File.join(bundle_path, 'jruby+truffle')
      FileUtils.ln_s File.join(bundle_path, RUBY_ENGINE),
                     jruby_truffle_path,
                     verbose: verbose? unless File.exists? jruby_truffle_path

      jruby_truffle_22_path = File.join(bundle_path, 'jruby+truffle', '2.2.0')
      FileUtils.ln_s File.join(bundle_path, 'jruby+truffle', '2.3.0'),
                     jruby_truffle_22_path,
                     verbose: verbose? unless File.exists? jruby_truffle_22_path

      mock_path = "#{bundle_path}/#{@options[:global][:mock_load_path]}"
      FileUtils.mkpath mock_path

      @options[:setup][:file].each do |name, content|
        puts "creating file: #{mock_path}/#{name}" if verbose?
        FileUtils.mkpath File.dirname("#{mock_path}/#{name}")
        File.write "#{mock_path}/#{name}", content
      end

      File.open("#{bundle_path}/bundler/setup.rb", 'a') do |f|
        f.write %[$:.unshift "\#{path}/../#{@options[:global][:mock_load_path]}"]
      end

      @options[:setup][:after].each do |cmd|
        execute_cmd cmd
      end

      true
    rescue => e
      puts format('%s: %s\n%s', e.class, e.message, e.backtrace.join("\n"))
      false
    end

    def bundle_cli(argv)
      ruby = Pathname(RbConfig::CONFIG['bindir']).join('jruby')
      execute_cmd [ruby.to_s, "#{Gem.bindir}/bundle", *argv]
    end

    def subcommand_run(rest)
      jruby_path = Pathname("#{@options[:run][:interpreter_path]}/../..").expand_path
      raise unless jruby_path.absolute?
      ruby_options, rest = if rest.include?('--')
                             split = rest.index('--')
                             [rest[0...split], rest[(split+1)..-1]]
                           else
                             [[], rest]
                           end

      unless jruby_path.absolute?
        jruby_path = jruby_path.relative_path_from(Pathname('.'))
      end

      if @options[:run][:build] || @options[:run][:rebuild]
        Dir.chdir jruby_path do
          execute_cmd "./tool/jt.rb #{'re' if @options[:run][:rebuild]}build"
        end
      end

      executable = if @options[:run][:executable]
                     executables = Dir.glob("#{@options[:global][:truffle_bundle_path]}/jruby+truffle/*/gems/*/{bin,exe}/*")
                     executables.find { |path| File.basename(path) == @options[:run][:executable] } or
                         raise "no executable with name '#{@options[:run][:executable]}' found"
                   end

      core_load_path = jruby_path.join 'truffle/src/main/ruby'

      missing_core_load_path = !File.exists?(core_load_path)
      puts "Core load path: #{core_load_path} does not exist, fallbacking to --no-use-fs-core" if missing_core_load_path

      truffle_options = [
          '-X+T',
          "-J-Xmx#{@options[:run][:xmx]}",
          *(%w[-J-ea -J-esa] unless @options[:run][:no_asserts]),
          ("-Xtruffle.core.load_path=#{core_load_path}" if @options[:global][:use_fs_core] && !missing_core_load_path),
          ('-Xtruffle.exceptions.print_java=true' if @options[:run][:jexception])
      ]

      bundler_setup = "./#{@options[:global][:truffle_bundle_path]}/bundler/setup.rb"
      cmd_options   = [
          *(truffle_options unless @options[:run][:no_truffle]),
          (format(@options[:global][:debug_option], @options[:global][:debug_port]) if @options[:run][:debug]),
          *ruby_options,
          *(['-r', bundler_setup] if File.exist? bundler_setup),
          *@options[:run][:load_path].flat_map { |v| ['-I', v] },
          *@options[:run][:require].flat_map { |v| ['-r', v] }
      ].compact

      env            = @options[:run][:environment]
      env['JAVACMD'] = @options[:global][:graal_path] if @options[:run][:graal]
      env.each { |k, v| env[k] = v.to_s }

      cmd = [(env unless env.empty?),
             @options[:run][:interpreter_path].to_s,
             *cmd_options,
             executable,
             *rest
      ].compact

      execute_cmd(cmd, raise: false, print_always: true)
      $?.success?
    end

    def subcommand_clean(rest)
      FileUtils.rm_rf @options[:global][:truffle_bundle_path]
      true
    end

    def subcommand_readme(rest)
      puts File.read(ROOT.join('README.md'))
      true
    end

    def subcommand_ci(rest)
      if (path = options[:ci][:batch])
        batch = if path =~ /^in|stdin$/
                  $stdin.read
                else
                  path = Pathname(path)
                  if path.absolute?
                    File.read(path)
                  else
                    File.read(Pathname(@called_from_dir).join(path))
                  end
                end

        results = batch.each_line.map do |line|
          next if line =~ /^#/ || line.strip.empty?

          options       = {}
          option_parser = build_option_parser OPTION_DEFINITIONS[:ci], options
          rest          = option_parser.order line.split

          gem_name = rest.first
          CIEnvironment.new(@options[:global][:dir], gem_name, rest[1..-1], verbose: verbose?).success?
        end

        results.all?
      else
        gem_name = rest.first
        ci       = CIEnvironment.new @options[:global][:dir], gem_name, rest[1..-1], definition: options[:ci][:definition], verbose: verbose?
        ci.success?
      end
    end

    def execute_cmd(cmd, dir: nil, raise: true, print_always: false)
      super cmd, dir: dir, raise: raise, print: verbose? || print_always
    end

    class CIEnvironment
      include Utils
      include OptionBlocks

      def self.define_dsl_attr(*names, &conversion)
        nothing = Object.new

        names.each do |name|
          ivar_name = :"@#{name}"

          define_method name do |value = nothing|
            if value == nothing
              instance_variable_get ivar_name
            else
              conversion ||= -> v { v }
              instance_variable_set ivar_name, conversion.call(value)
            end
          end
        end
      end

      define_dsl_attr :repository_name, :subdir
      define_dsl_attr(:working_dir) { |v| Pathname(v) }
      attr_reader :gem_name

      def initialize(working_dir, gem_name, rest, definition: nil, verbose: false)
        @options  = {}
        @gem_name = gem_name
        @rest     = rest
        @verbose  = verbose

        @working_dir     = Pathname(working_dir)
        @repository_name = gem_name
        @subdir          = '.'
        @result          = nil

        option_parser         = @option_parser = OptionParser.new
        @option_parser.banner = "\nUsage: #{EXECUTABLE} [options] ci [subcommand-options] #{gem_name} [options-declared-in-CI-definition]\n\n"
        @option_parsed        = false

        declare_options parse_options: false, help: ['-h', '--help', 'Show this message', -> (new, old, _) { puts option_parser; exit }, false]
        if definition
          do_definition(definition)
        else
          do_definition(gem_name, raise: false) || do_definition('default')
        end
      end

      def do_definition(name, raise: true)
        ci_file = Dir.glob(ROOT.join('gem_ci', "{#{name}}.rb")).first
        if ci_file.nil?
          if raise
            raise "no ci definition with name: #{name}"
          else
            return false
          end
        else
          puts "Using CI definition: #{ci_file}"
          catch :cancel_ci! do
            begin
              instance_eval File.read(ci_file), ci_file, 1
            rescue => e
              puts format('%s: %s\n%s', e.class, e.message, e.backtrace.join("\n"))
              result false
            end
          end

          return true
        end
      end

      def declare_options(parse_options: true, **parser_options)
        raise 'cannot declare options after they were parsed' if @option_parsed

        Runner.build_option_parser(parser_options, @options, option_parser: @option_parser)
        @options.merge! Runner.default_option_values(parser_options)

        if !@option_parsed && parse_options
          @option_parsed = true
          @option_parser.order @rest
        end

        nil
      end

      def repository_dir
        working_dir.join repository_name
      end

      def testing_dir
        repository_dir.join subdir
      end

      def jruby_path
        JRUBY_PATH
      end

      def jruby_truffle_path
        jruby_path.join 'bin', 'jruby+truffle'
      end

      def option(key)
        @options.fetch(key)
      end

      def git_clone(url)
        execute_cmd %W[git clone #{url} #{repository_name}], dir: working_dir, print: true
      end

      def git_checkout(target)
        return if target.nil?
        execute_cmd %W[git checkout --force #{target}], dir: repository_dir, print: true
      end

      def git_tag(version)
        return nil if version.nil?

        tag = git_tags.find { |l| l.include? version }
        raise "tag for #{version} was not found" if tag.nil?
        tag
      end

      def git_tags
        tags = Dir.chdir(repository_dir) { `git tag -l`.lines }
        raise "fetching tags failed" if !$?.success?
        tags
      end

      def setup
        # noinspection RubyArgCount
        Dir.chdir(testing_dir) { Runner.new([('-v' if @verbose), 'setup'].compact).run }
      end

      def cancel_ci!(result = false)
        throw :cancel_ci!, result
      end

      def has_to_succeed(result)
        result or cancel_ci! result
      end

      def run(options, raise: true)
        raise ArgumentError unless options.is_a? Array
        # noinspection RubyArgCount
        Dir.chdir(testing_dir) { Runner.new([('-v' if @verbose), 'run', *options].compact).run }
      end

      def execute(cmd, dir: testing_dir, raise: true)
        execute_cmd cmd, dir: dir, print: true, raise: raise
      end

      def clean
        FileUtils.rmtree repository_dir
      end

      def result(boolean)
        @result = boolean
      end

      def success?
        @result
      end

      def use_only_https_git_paths!
        gemfile_path = repository_dir.join('Gemfile')
        File.write gemfile_path,
                   File.read(gemfile_path).
                       gsub(/github: ("|')/, 'git: \1https://github.com/').
                       gsub(/git:\/\//, 'https://')

      end

      def delete_gemfile_lock!
        path = repository_dir.join('Gemfile.lock')
        FileUtils.rm path if File.exists? path
      end

    end
  end
end
