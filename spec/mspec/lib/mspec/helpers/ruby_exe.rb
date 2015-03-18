require 'mspec/utils/ruby_name'
require 'mspec/guards/platform'

# The ruby_exe helper provides a wrapper for invoking the
# same Ruby interpreter as the one running the specs and
# getting the output from running the code. If +code+ is a
# file that exists, it will be run. Otherwise, +code+ should
# be Ruby code that will be run with the -e command line
# option. For example:
#
#   ruby_exe('path/to/some/file.rb')
#
# will be executed as
#
#   `#{RUBY_EXE} #{'path/to/some/file.rb'}`
#
# while
#
#   ruby_exe('puts "hello, world."')
#
# will be executed as
#
#   `#{RUBY_EXE} -e #{'puts "hello, world."'}`
#
# The ruby_exe helper also accepts an options hash with three
# keys: :options, :args and :env. For example:
#
#   ruby_exe('file.rb', :options => "-w",
#                       :args => "> file.txt",
#                       :env => { :FOO => "bar" })
#
# will be executed as
#
#   `#{RUBY_EXE} -w #{'file.rb'} > file.txt`
#
# with access to ENV["FOO"] with value "bar".
#
# If +nil+ is passed for the first argument, the command line
# will be built only from the options hash.
#
# The RUBY_EXE constant can be set explicitly since the value
# is used each time ruby_exe is invoked. The mspec runner script
# will set ENV['RUBY_EXE'] to the name of the executable used
# to invoke the mspec-run script. The value of RUBY_EXE will be
# constructed as follows:
#
#   1. the value of ENV['RUBY_EXE']
#   2. an explicit value based on RUBY_NAME
#   3. cwd/(RUBY_NAME + $(EXEEXT) || $(exeext) || '')
#   4. $(bindir)/$(RUBY_INSTALL_NAME)
#
# The value will only be used if the file exists and is executable.
#
# These 4 ways correspond to the following scenarios:
#
#   1. Using the MSpec runner scripts, the name of the
#      executable is explicitly passed by ENV['RUBY_EXE']
#      so there is no ambiguity.
#
#  Otherwise, if using RSpec (or something else)
#
#   2. Running the specs while developing an alternative
#      Ruby implementation. This explicitly names the
#      executable in the development directory based on
#      the value of RUBY_NAME, which is probably initialized
#      from the value of RUBY_ENGINE.
#   3. Running the specs within the source directory for
#      some implementation. (E.g. a local build directory.)
#   4. Running the specs against some installed Ruby
#      implementation.

class Object
  def ruby_exe_options(option)
    case option
    when :env
      ENV['RUBY_EXE']
    when :engine
      case RUBY_NAME
      when 'rbx'
        if SpecGuard.ruby_version < "1.9"
          "bin/rbx"
        else
          "bin/rbx -X19"
        end
      when 'jruby'
        "bin/jruby"
      when 'maglev'
        "maglev-ruby"
      when 'topaz'
        "topaz"
      when 'ironruby'
        "ir"
      end
    when :name
      bin = RUBY_NAME + (RbConfig::CONFIG['EXEEXT'] || RbConfig::CONFIG['exeext'] || '')
      File.join(".", bin)
    when :install_name
      bin = RbConfig::CONFIG["RUBY_INSTALL_NAME"] || RbConfig::CONFIG["ruby_install_name"]
      bin << (RbConfig::CONFIG['EXEEXT'] || RbConfig::CONFIG['exeext'] || '')
      File.join(RbConfig::CONFIG['bindir'], bin)
    end
  end

  def resolve_ruby_exe
    [:env, :engine, :name, :install_name].each do |option|
      next unless cmd = ruby_exe_options(option)
      exe, *rest = cmd.split(" ")

      if File.file?(exe) and File.executable?(exe)
        return [File.expand_path(exe), *rest].join(" ")
      end
    end
    nil
  end

  def ruby_exe(code, opts = {})
    env = opts[:env] || {}
    working_dir = opts[:dir] || "."
    Dir.chdir(working_dir) do
      saved_env = {}
      env.each do |key, value|
        key = key.to_s
        saved_env[key] = ENV[key] if ENV.key? key
        ENV[key] = value
      end

      begin
        platform_is_not :opal do
          `#{ruby_cmd(code, opts)}`
        end
      ensure
        saved_env.each { |key, value| ENV[key] = value }
        env.keys.each do |key|
          key = key.to_s
          ENV.delete key unless saved_env.key? key
        end
      end
    end
  end

  def ruby_cmd(code, opts = {})
    body = code

    if code and not File.exist?(code)
      if opts[:escape]
        heredoc_separator = "END_OF_RUBYCODE"
        lines = code.lines
        until lines.none? {|line| line.start_with? heredoc_separator }
          heredoc_separator << heredoc_separator
        end

        body = %Q!-e "$(cat <<'#{heredoc_separator}'\n#{code}\n#{heredoc_separator}\n)"!
      else
        body = "-e #{code.inspect}"
      end
    end

    [RUBY_EXE, ENV['RUBY_FLAGS'], opts[:options], body, opts[:args]].compact.join(' ')
  end

  unless Object.const_defined?(:RUBY_EXE) and RUBY_EXE
    require 'rbconfig'

    RUBY_EXE = resolve_ruby_exe or
      raise Exception, "Unable to find a suitable ruby executable."
  end
end
