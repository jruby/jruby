# Tasks for the generated jruby.1 man page.
#
# The page is rendered by tool/man/man_page.rb from an ERB template, reading
# the options table live from the built JRuby runtime (the same table that
# backs `jruby --help`, see core/src/main/java/org/jruby/util/cli/OutputStrings.java).
# Run these tasks with the built JRuby: `bin/jruby -S rake man:generate`.

require 'fileutils'
require 'tmpdir'

MANPAGE_TOOL = File.join(File.dirname(__FILE__), '..', 'tool', 'man', 'man_page.rb')
MANPAGE_TEMPLATE = File.join(File.dirname(__FILE__), '..', 'tool', 'man', 'jruby.1.erb')
MANPAGE_OUTPUT = File.join('core', 'target', 'generated-man', 'jruby.1')
MANPAGE_MIN_OPTIONS = 40
MANPAGE_ENV_ENTRIES = 2

def manpage_tool
  abort "man: tool not found: #{MANPAGE_TOOL}" unless File.exist?(MANPAGE_TOOL)
  abort "man: template not found: #{MANPAGE_TEMPLATE}" unless File.exist?(MANPAGE_TEMPLATE)
  require MANPAGE_TOOL
  unless RUBY_ENGINE == 'jruby'
    abort 'man: must run on JRuby (`bin/jruby -S rake man:generate`) so the options table comes from the runtime'
  end
end

def manpage_generate(output, date_arg = nil)
  manpage_tool
  ManPage.write(output, date_arg: date_arg)
end

namespace :man do
  desc 'Generate the jruby.1 man page from the --help options table'
  task :generate do
    manpage_generate(MANPAGE_OUTPUT)
  end

  desc 'Generate and render the jruby.1 man page for review'
  task :preview => :generate do
    if system('command -v man > /dev/null 2>&1')
      sh('man', '-l', MANPAGE_OUTPUT)
    elsif system('command -v groff > /dev/null 2>&1')
      sh("groff -man -Tascii #{MANPAGE_OUTPUT} | ${PAGER:-less}")
    else
      puts "man page generated at #{MANPAGE_OUTPUT} (install man or groff to preview it)"
    end
  end

  desc 'Verify the generated man page: deterministic output that parses cleanly'
  task :check do
    manpage_tool
    Dir.mktmpdir('manpage') do |dir|
      a = File.join(dir, 'a.1')
      b = File.join(dir, 'b.1')
      old_epoch = ENV['SOURCE_DATE_EPOCH']
      ENV['SOURCE_DATE_EPOCH'] = '1759440000'
      begin
        ManPage.write(a)
        ManPage.write(b)
      ensure
        old_epoch.nil? ? ENV.delete('SOURCE_DATE_EPOCH') : ENV['SOURCE_DATE_EPOCH'] = old_epoch
      end
      abort 'man:check: generator output is not deterministic' unless FileUtils.compare_file(a, b)

      text = File.read(a)
      abort 'man:check: missing .TH JRUBY header' unless text.include?('.TH JRUBY 1')
      abort 'man:check: missing OPTIONS section' unless text.include?('.SH OPTIONS')
      options = text.scan(/^\.TP/).size - MANPAGE_ENV_ENTRIES
      abort "man:check: only #{options} options rendered, expected at least #{MANPAGE_MIN_OPTIONS}" if options < MANPAGE_MIN_OPTIONS

      if system('command -v groff > /dev/null 2>&1')
        sh("groff -man -z #{a}")
      else
        puts 'man:check: groff not found, skipping parse check'
      end
      puts "man:check: OK (#{options} options, deterministic, parses cleanly)"
    end
  end
end

desc 'Generate the jruby.1 man page (alias for man:generate)'
task :man => 'man:generate'
