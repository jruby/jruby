# frozen_string_literal: true

require 'fileutils'
require 'rbconfig'
require 'ripper'
require 'tmpdir'

# Audits a directory of exclude files offline, the way mspec's tag commands audit ruby/spec tags.
#
# An exclude file is named after a test class (Foo::TestBar lives in Foo/TestBar.rb) and holds lines like
#
#   exclude :test_something, "why it fails"
#
# The audit has two steps, so it never changes how the excludes behave in a normal test run.
#
# 1. Discovery loads each test file without running anything and checks that every excluded class and
#    method still exists, which finds excludes for tests that were renamed or removed.
# 2. Each remaining excluded test is run on its own, in a separate process with a time limit. A test that
#    leaves threads behind or changes global state can't affect another one, and one that hangs is killed.
#
# Nothing here depends on JRuby. The caller says how to run a test file by passing a block that returns the
# command and environment for it.
module ExcludesAudit
  Entry = Struct.new(:class_name, :name, :reason, :path, :line) do
    def to_s = "#{class_name}##{name}"
  end

  # Statuses, roughly from most to least actionable.
  #   :passes          ran and passed every time, so the exclude can go
  #   :missing_method  the class exists but has no such test method
  #   :missing_class   no test file defines the class
  #   :not_loaded      the class or method isn't there after loading its file, but it may only be created on some
  #                    platforms, when a gem is installed or while other tests run, so check it by hand
  #   :flaky           passed some runs and failed others
  #   :timeout         killed after the time limit
  #   :crashed         the test process died without reporting a result
  #   :skipped         the test skipped itself, so its exclude may be about the platform
  #   :fails           still fails, so the exclude is doing its job
  #   :pattern         a Regexp exclude, which this tool doesn't audit
  STATUSES = %i[passes missing_method missing_class not_loaded flaky timeout crashed skipped fails pattern].freeze

  Result = Struct.new(:entry, :status, :detail) do
    def removable? = %i[passes missing_method missing_class].include?(status)
  end

  # Reads every exclude file under dir.
  def self.read(dir)
    Dir.glob("**/*.rb", base: dir).sort.flat_map do |relative|
      path = File.join(dir, relative)
      class_name = relative.delete_suffix(".rb").gsub("/", "::")
      Collector.new(class_name, path).tap { |c| c.instance_eval(File.read(path), path) }.entries
    end
  end

  # Evaluates an exclude file, recording each exclude and where it is. Exclude files can have conditions, as in
  # "exclude :test_foo, 'reason' if RUBY_PLATFORM =~ /linux/", so they are evaluated the same way the test
  # runner evaluates them, and the audit should run on the Ruby being tested.
  class Collector
    attr_reader :entries

    def initialize(class_name, path)
      @class_name, @path, @entries = class_name, path, []
    end

    def exclude(name, reason = nil, **)
      line = caller_locations(1, 1).first.lineno
      @entries << Entry.new(@class_name, name.is_a?(Regexp) ? name : name.to_s, reason, @path, line)
    end
  end

  # Finds the files that define each class, by looking for "class Name" or "Name = " in the given test files.
  def self.files_defining(class_names, test_files)
    sources = test_files.to_h { |file| [file, File.read(file, encoding: "BINARY")] }
    class_names.to_h do |class_name|
      last = Regexp.escape(class_name.split("::").last)
      pattern = /^\s*(?:class\s+(?:\w+::)*#{last}\b|(?:\w+::)*#{last}\s*=\s*Class\.new)/
      [class_name, sources.select { |_, source| source.match?(pattern) }.keys]
    end
  end

  # Runs the audit. The block takes a test file and the extra arguments for the runner, and returns
  # [env, command] for running that file. The runner must accept "-n Class#method" to run one test and print a
  # "N tests, N assertions, N failures, N errors, N skips" summary, as test-unit and minitest's runners do.
  class Audit
    DISCOVER = File.expand_path("excludes_audit/discover.rb", __dir__)
    SUMMARY = /(\d+) tests?, \d+ assertions?, (\d+) failures?, (\d+) errors?, (\d+) skips?/

    def initialize(entries, test_files, jobs: 1, time_limit: 60, repeat: 1, log: $stderr, &command)
      @entries, @test_files, @jobs, @time_limit, @repeat, @log, @command =
        entries, test_files, jobs, time_limit, repeat, log, command
    end

    def run
      patterns, named = @entries.partition { |e| e.name.is_a?(Regexp) }
      results = patterns.map { |e| Result.new(e, :pattern, nil) }

      files = ExcludesAudit.files_defining(named.map(&:class_name).uniq, @test_files)
      found, missing = named.partition { |e| files[e.class_name].any? }
      results += missing.map { |e| Result.new(e, :missing_class, "no test file defines #{e.class_name}") }

      to_run = discover(found, files, results)
      @log.puts "running #{to_run.size} excluded tests, #{@jobs} at a time"
      results + parallel(to_run) { |entry, file| run_one(entry, file) }
    end

    private

    # Loads each test file once, with a filter that matches no tests, and asks which excluded classes and
    # methods exist. Returns [entry, file] for each one that does.
    def discover(entries, files, results)
      by_file = entries.group_by { |e| files[e.class_name] }.flat_map do |candidates, list|
        candidates.map { |file| [file, list] }
      end
      @log.puts "loading #{by_file.size} test files to look for #{entries.size} excluded tests"

      found = {}
      class_seen = Hash.new(false)
      parallel(by_file) do |file, list|
        query = list.map(&:to_s).join("\n")
        env, command = @command.call(file, ["-n", "/\\A\\z/"])
        out = capture({ **env, "EXCLUDES_AUDIT_QUERY" => query }, ["-r", DISCOVER], command)
        [file, out]
      end.each do |file, (status, output)|
        output.to_s.scan(/^EXCLUDES_AUDIT (\S+) (class|method)$/) do |test, kind|
          class_seen[test.split("#").first] = true
          found[test] ||= file if kind == "method"
        end
      end

      entries.filter_map do |entry|
        if (file = found[entry.to_s])
          next [entry, file]
        elsif !class_seen[entry.class_name]
          results << Result.new(entry, :not_loaded, "#{entry.class_name} is not defined after loading #{files[entry.class_name].join(", ")}")
        elsif entry.name.match?(/\A\w+[?!]?\z/)
          results << Result.new(entry, :missing_method, "#{entry.class_name} has no method #{entry.name}")
        else
          # a name like test_new@Asia/Tokyo is generated, possibly while other tests run
          results << Result.new(entry, :not_loaded, "#{entry.class_name} has no method #{entry.name} after loading")
        end
        nil
      end
    end

    def run_one(entry, file)
      outcomes = @repeat.times.map do
        env, command = @command.call(file, ["-n", entry.to_s])
        status, output = capture(env, [], command)
        outcome = classify(status, output)
        break [outcome] unless outcome.first == :passes
        outcome
      end
      statuses = outcomes.map(&:first).uniq
      if statuses == [:passes]
        Result.new(entry, :passes, @repeat > 1 ? "passed #{@repeat} times" : nil)
      elsif statuses.include?(:passes)
        Result.new(entry, :flaky, outcomes.map(&:last).compact.first)
      else
        Result.new(entry, *outcomes.last)
      end
    end

    def classify(status, output)
      return [:timeout, "killed after #{@time_limit}s"] if status == :timeout
      summary = output.scan(SUMMARY).last
      return [:crashed, output.lines.last(3).join.strip] unless summary
      tests, failures, errors, skips = summary.map(&:to_i)
      return [:crashed, "the runner found no test to run"] if tests.zero?
      return [:fails, first_problem(output)] if failures + errors > 0
      return [:skipped, first_problem(output)] if skips > 0
      [:passes, nil]
    end

    def first_problem(output)
      output[/^\s*1\) (?:Failure|Error|Skipped):\n(?:.*\n){1,3}/]&.lines&.drop(1)&.map(&:strip)&.join(" ")&.slice(0, 200)
    end

    # Runs the command in its own process group, so a test that spawns processes is cleaned up with it, and in its
    # own temporary directory, since some tests write files into the working directory.
    def capture(env, ruby_args, command)
      command = command.dup.insert(1, *ruby_args)
      dir = Dir.mktmpdir("excludes-audit")
      reader, writer = IO.pipe
      pid = Process.spawn(env, *command, in: File::NULL, out: writer, err: writer, pgroup: true, chdir: dir)
      writer.close
      output = +""
      # tests can print anything, so make the output safe to match against
      drain = Thread.new { output << reader.read.scrub }
      drain.report_on_exception = false
      deadline = Process.clock_gettime(Process::CLOCK_MONOTONIC) + @time_limit
      until (_, status = Process.wait2(pid, Process::WNOHANG))
        if Process.clock_gettime(Process::CLOCK_MONOTONIC) > deadline
          Process.kill(:KILL, -pid) rescue Process.kill(:KILL, pid) rescue nil
          Process.wait(pid) rescue nil
          drain.join(1)
          return [:timeout, output]
        end
        sleep 0.1
      end
      drain.join
      [status.exitstatus, output]
    ensure
      reader&.close
      FileUtils.rm_rf(dir) if dir
    end

    def parallel(items)
      queue = Queue.new
      items.each_with_index { |item, i| queue << [item, i] }
      results = Array.new(items.size)
      done = 0
      lock = Mutex.new
      [@jobs, items.size].min.times.map do
        Thread.new do
          while (item, i = (queue.pop(true) rescue nil))
            results[i] = begin
              yield(*item)
            rescue => e
              # report it and keep going with the rest
              @log.puts "audit error for #{item.first}: #{e.class}: #{e.message}"
              item.first.is_a?(Entry) ? Result.new(item.first, :crashed, "audit error: #{e.message}") : [item.first, [1, ""]]
            end
            lock.synchronize { @log.print "\r#{done += 1}/#{items.size}" if @log.tty? }
          end
        end
      end.each(&:join)
      @log.puts if @log.tty? && !items.empty?
      results
    end
  end

  # Removes the lines for the given entries from their exclude files, and deletes files left with no excludes.
  # An exclude that spans several lines is left alone. Returns the entries that were left alone.
  def self.prune(entries)
    skipped = []
    entries.group_by(&:path).each do |path, list|
      lines = File.readlines(path)
      list.sort_by(&:line).reverse_each do |entry|
        line = lines[entry.line - 1]
        if line&.match?(/\A\s*exclude[\s(]/) && Ripper.sexp(line)
          lines.delete_at(entry.line - 1)
        else
          skipped << entry
        end
      end
      if lines.none? { |l| l.match?(/^\s*exclude\b/) }
        File.delete(path)
        dir = File.dirname(path)
        Dir.rmdir(dir) if Dir.empty?(dir)
      else
        File.write(path, lines.join)
      end
    end
    skipped
  end

  def self.report(results, io = $stdout)
    by_status = results.group_by(&:status)
    STATUSES.each do |status|
      list = by_status[status] or next
      io.puts "", "#{status} (#{list.size})"
      list.sort_by { |r| [r.entry.path, r.entry.line] }.each do |r|
        path = r.entry.path.delete_prefix("#{Dir.pwd}/")
        io.puts "  #{r.entry} #{path}:#{r.entry.line}#{" - #{r.detail}" if r.detail}"
      end
    end
    io.puts "", STATUSES.filter_map { |s| "#{by_status[s].size} #{s}" if by_status[s] }.join(", ")
  end
end
