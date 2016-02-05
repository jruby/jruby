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

require 'rubygems'
begin
  require 'bundler'
rescue LoadError => e
  puts "Bundler has to be installed.\n"
  raise e
end

class JRubyTruffleRunner
  attr_reader :options

  EXECUTABLE        = File.basename($PROGRAM_NAME)
  BRANDING          = EXECUTABLE.include?('jruby') ? 'JRuby+Truffle' : 'RubyTruffle'
  LOCAL_CONFIG_FILE = '.jruby+truffle.yaml'

  begin
    assign_new_value   = -> (new, old) { new }
    add_to_array       = -> (new, old) { old << new }
    merge_hash         = -> ((k, v), old) { old.merge k => v }
    apply_pattern      = -> (pattern, old) do
      Dir.glob(pattern) do |file|
        if @options[:run][:exclude_pattern].any? { |p| /#{p}/ =~ file }
          puts "skipped: #{file}" if @options[:global][:verbose]
          next
        end
        @options[:run][:require] << File.expand_path(file)
      end
      old
    end

    # Format:
    #   subcommand_name: {
    #     :'option_name (also used as a key in yaml)' => [option_parser_on_method_args,
    #                                                     -> (new_value, old_value) { result_of_this_block_is_stored },
    #                                                     default_value]
    #   }
    OPTION_DEFINITIONS = {
        global: {
            verbose:             ['-v', '--verbose', 'Run verbosely (prints options)', assign_new_value, false],
            help:                ['-h', '--help', 'Show this message', assign_new_value, false],
            debug_port:          ['--debug-port PORT', 'Debug port', assign_new_value, '51819'],
            debug_option:        ['--debug-option OPTION', 'Debug JVM option', assign_new_value,
                                  '-J-agentlib:jdwp=transport=dt_socket,server=y,address=%d,suspend=y'],
            truffle_bundle_path: ['--truffle-bundle-path NAME', 'Bundle path', assign_new_value, '.jruby+truffle_bundle'],
            interpreter_path:    ['--interpreter-path PATH', "Path to #{BRANDING} interpreter executable", assign_new_value,
                                  File.expand_path(File.join(File.dirname(__FILE__), '../../../../bin/jruby'))],
            graal_path:          ['--graal-path PATH', 'Path to Graal', assign_new_value, '../graalvm-jdk1.8.0/bin/java'],
            mock_load_path:      ['--mock-load-path PATH',
                                  'Path of mocks & monkey-patches (prepended in $:, relative to --truffle_bundle_path)',
                                  assign_new_value, 'mocks'],
            use_fs_core:         ['--[no-]use-fs-core', 'Use core from the filesystem rather than the JAR',
                                  assign_new_value, true],
            bundle_options:      ['--bundle-options OPTIONS', 'bundle options separated by space', assign_new_value, ''],
            configuration:       ['--config GEM_NAME', 'Load configuration for specified gem', assign_new_value, nil],
            dir:                 ['--dir DIRECTORY', 'Set working directory', assign_new_value, Dir.pwd],
        },
        setup:  {
            help:    ['-h', '--help', 'Show this message', assign_new_value, false],
            after:   ['--after SH_CMD', 'Commands to execute after setup', add_to_array, []],
            file:    ['--file NAME,CONTENT', Array, 'Create file in truffle_bundle_path', merge_hash, {}],
            without: ['--without GROUP', 'Do not install listed gem group by bundler', add_to_array, []]
        },
        run:    {
            help:            ['-h', '--help', 'Show this message', assign_new_value, false],
            no_truffle:      ['-n', '--no-truffle', "Use conventional JRuby instead of #{BRANDING}", assign_new_value, false],
            graal:           ['-g', '--graal', 'Run on graal', assign_new_value, false],
            build:           ['-b', '--build', 'Run `jt build` using conventional JRuby', assign_new_value, false],
            rebuild:         ['--rebuild', 'Run `jt rebuild` using conventional JRuby', assign_new_value, false],
            debug:           ['-d', '--debug', 'JVM remote debugging', assign_new_value, false],
            require:         ['-r', '--require FILE', 'Files to require, same as Ruby\'s -r', add_to_array, []],
            require_pattern: ['--require-pattern DIR_GLOB_PATTERN', 'Files matching the pattern will be required', apply_pattern, nil],
            exclude_pattern: ['--exclude-pattern REGEXP', 'Files matching the regexp will not be required by --require-pattern (applies to subsequent --require-pattern options)', add_to_array, []],
            load_path:       ['-I', '--load-path LOAD_PATH', 'Paths to add to load path, same as Ruby\'s -I', add_to_array, []],
            executable:      ['-S', '--executable NAME', 'finds and runs an executable of a gem', assign_new_value, nil],
            jexception:      ['--jexception', 'print Java exceptions', assign_new_value, false]
        },
        stored: {
            help: ['-h', '--help', 'Show this message', assign_new_value, false],
            list: ['-l', '--list', 'List stored commands', assign_new_value, false]
        },
        clean:  {
            help: ['-h', '--help', 'Show this message', assign_new_value, false]
        },
        readme: {
            help: ['-h', '--help', 'Show this message', assign_new_value, false]
        }
    }
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

      Usage: #{EXECUTABLE} [options] run [subcommand-options] -- [ruby-options]
      Usage: #{EXECUTABLE} [options] run [subcommand-options] -- [prepended-ruby-options] -- [appended-ruby-options]
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

    stored_help = <<-TXT.gsub(/^ {6}/, '')

      Usage: #{EXECUTABLE} [options] stored [subcommand-options] [COMMAND_NAME]

      Runs stored list of bash commands. They are stored under :stored_commands key
      in options. It can contain single command or an array of commands,
      e.g. to define how to run CI cycle for a given gem/application on JRuby+Truffle.

      Examples: #{EXECUTABLE} stored --list
                #{EXECUTABLE} stored ci

      Stored commands may reference each other by Symbols.

      Configuration example:
        :stored_commands:
          :ci:
            - "jruby+truffle setup"
            - :test
          :test: "jruby+truffle run -S rake -- test"

    TXT

    HELP = { global: global_help, setup: setup_help, run: run_help, clean: clean_help, stored: stored_help }
  end


  def initialize(argv = ARGV)
    construct_default_options
    build_option_parsers

    subcommand, *argv_after_global = @option_parsers[:global].order argv

    Dir.chdir @options[:global][:dir] do
      puts "pwd: #{Dir.pwd}" if verbose?

      load_gem_configuration
      load_local_configuration

      if subcommand.nil?
        print_options
        help :global
      end
      help :global if @options[:global][:help]

      subcommand = subcommand.to_sym

      subcommand_option_parser = @option_parsers[subcommand] || raise("unknown subcommand: #{subcommand}")
      argv_after_subcommand    = subcommand_option_parser.order argv_after_global

      print_options
      help subcommand if @options[subcommand][:help] && subcommand != :readme

      send "subcommand_#{subcommand}", argv_after_subcommand
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
    @option_parsers = OPTION_DEFINITIONS.each_with_object({}) do |(group, group_options), parsers|
      parsers[group] = OptionParser.new do |option_parser|
        group_options.each do |option, data|
          *args, description, block, default = data

          option_parser.on(*args, description + " (default: #{default.inspect})") do |new_value|
            old_value               = @options[group][option]
            @options[group][option] = instance_exec new_value, old_value, &block
          end
        end
      end
    end

    @option_parsers.each { |key, option_parser| option_parser.banner = HELP[key] }
  end

  def load_gem_configuration
    candidates = @options[:global][:configuration] ? [@options[:global][:configuration]] : Dir['*.gemspec']

    if candidates.size == 1
      gem_name, _ = candidates.first.split('.')
      yaml_path   = File.dirname(__FILE__) + "/gem_configurations/#{gem_name}.yaml"
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
    @options = OPTION_DEFINITIONS.each_with_object({}) do |(group, group_options), options|
      group_options.each_with_object(options[group] = {}) do |(option, data), group_option_defaults|
        *args, block, default         = data
        group_option_defaults[option] = default
      end
    end
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
        return a.merge(b) { |k, ov, nv| deep_merge ov, nv }
      else
        return a
      end
    end

    if Array === a
      if Array === b
        return a + b.map { |v| eval_yaml_strings v }
      else
        return a
      end
    end

    eval_yaml_strings b
  end

  def eval_yaml_strings(value)
    if String === value
      begin
        eval('"' + value.gsub(/\\#|"|\\/, '\#' => '\#', '"' => '\"', '\\' => '\\\\') + '"')
      rescue => e
        p value
        raise e
      end
    else
      value
    end
  end

  def subcommand_setup(rest)
    bundle_options = @options[:global][:bundle_options].split(' ')
    bundle_path    = File.expand_path(@options[:global][:truffle_bundle_path])

    bundle_cli([*bundle_options,
                'install',
                '--standalone',
                '--path', bundle_path,
                *(['--without', @options[:setup][:without].join(' ')] unless @options[:setup][:without].empty?)])

    link_path = "#{bundle_path}/jruby+truffle"
    FileUtils.rm link_path if File.exist? link_path
    execute_cmd "ln -s #{bundle_path}/#{RUBY_ENGINE} #{link_path}"

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
  end

  def bundle_cli(argv)
    require 'bundler/friendly_errors'
    Bundler.with_friendly_errors do
      require 'bundler/cli'
      Bundler::CLI.start(argv, :debug => true)
    end
  end

  def subcommand_run(rest)
    jruby_path         = Pathname("#{@options[:global][:interpreter_path]}/../..").expand_path
    ruby_options, rest = if rest.include?('--')
                           split = rest.index('--')
                           [rest[0...split], rest[(split+1)..-1]]
                         else
                           [[], rest]
                         end

    unless jruby_path.absolute?
      jruby_path = jruby_path.relative_path_from(Pathname('.'))
    end

    jruby_path = jruby_path.to_s

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

    core_load_path = "#{jruby_path}/truffle/src/main/ruby"

    truffle_options = [
        ('-X+T'),
        ("-Xtruffle.core.load_path=#{core_load_path}" if @options[:global][:use_fs_core]),
        ('-Xtruffle.exceptions.print_java=true' if @options[:run][:jexception]),
        (format(@options[:global][:debug_option], @options[:global][:debug_port]) if @options[:run][:debug])
    ]

    cmd_options = [
        *(truffle_options unless @options[:run][:no_truffle]),
        *ruby_options,
        '-r', "./#{@options[:global][:truffle_bundle_path]}/bundler/setup.rb",
        *@options[:run][:load_path].flat_map { |v| ['-I', v] },
        *@options[:run][:require].flat_map { |v| ['-r', v] }
    ].compact

    env            = {}
    env['JAVACMD'] = @options[:global][:graal_path] if @options[:run][:graal]

    cmd = [(env unless env.empty?),
           @options[:global][:interpreter_path],
           *cmd_options,
           executable,
           *rest
    ].compact

    execute_cmd(cmd, fail: false, print_always: true)
    exit $?.exitstatus
  end

  def subcommand_stored(rest)
    if @options[:stored][:list]
      pp @options[:stored_commands]
      exit
    end

    commands = get_stored_command rest.first

    puts "executing #{commands.size} commands:"
    commands.each { |cmd| print_cmd cmd, true }
    puts

    commands.each do |cmd|
      unless execute_cmd(cmd, fail: false, print_always: true)
        exit $?.exitstatus
      end
    end
  end

  def get_stored_command(name, fail: true)
    result = @options.fetch(:stored_commands, {})[name.to_sym]
    raise("unknown stored command: #{name}") if fail && !result
    Array(result).flat_map { |c| (c.is_a? Symbol) ? get_stored_command(c) : c }
  end

  def subcommand_clean(rest)
    FileUtils.rm_rf @options[:global][:truffle_bundle_path]
  end

  def subcommand_readme(rest)
    readme_path = File.join File.dirname(__FILE__), 'README.md'
    puts File.read(readme_path)
  end

  def print_cmd(cmd, print_always)
    unless verbose? || print_always
      return cmd
    end

    print = if String === cmd
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
    puts '$ ' + print
    return cmd
  end

  def execute_cmd(cmd, fail: true, print_always: false)
    result = system(*print_cmd(cmd, print_always))
  ensure
    raise 'command failed' if fail && !result
  end
end
