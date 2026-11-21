# frozen_string_literal: true

# Checks that the JIT can compile every method and block in a set of Ruby
# files, without running any of them.
#
# JRuby's bytecode JIT uses no runtime profile, so whether a method compiles
# doesn't depend on how it has run. This builds IR for each file, then
# compiles each method and block on its own and loads the resulting class,
# the same way the JIT does once one gets hot. Any exception is a method that
# would stay interpreted forever, as in
# https://github.com/jruby/jruby/issues/9814.
#
# It also persists each file's IR, as jrubyc does, and reads it back,
# decoding every method and block. Any exception is a file that jrubyc can
# compile but JRuby can't load, as in
# https://github.com/jruby/jruby/issues/9834.
#
#   bin/jruby -J-XX:-OmitStackTraceInFastThrow tool/jit_compile_check.rb [options] [FILE_OR_DIR ...]
#
# Failures are grouped by exception class and message, so pass
# -XX:-OmitStackTraceInFastThrow: without it, the JVM drops the message from
# exceptions it has thrown many times, which splits one cause into two
# groups.

require 'jruby'
require 'optparse'
require 'yaml'

module JITCompileCheck
  java_import org.jruby.ir.IRClosure
  java_import org.jruby.ir.IRMethod
  java_import org.jruby.ir.persistence.IRReader
  java_import org.jruby.ir.persistence.IRReaderStream
  java_import org.jruby.ir.persistence.IRWriter
  java_import org.jruby.ir.persistence.IRWriterStream
  java_import org.jruby.ir.targets.JVMVisitor
  java_import org.jruby.ir.targets.JVMVisitorMethodContext
  java_import org.jruby.util.OneShotClassLoader
  java_import org.jruby.util.cli.Options

  # A scope the JIT failed to compile.
  Failure = Struct.new(:file, :line, :scope, :error, keyword_init: true) do
    def location = "#{file}:#{line}"

    # The exception class and the first line of its message, used to group
    # failures with the same cause and to match the known causes.
    def cause
      error_class = error.is_a?(java.lang.Throwable) ? error.java_class.name : error.class.name
      "#{error_class}: #{error.message.to_s.lines.first&.strip}"
    end
  end

  Stats = Struct.new(:files, :unparsable, :scopes, :too_large, keyword_init: true)

  module_function

  def runtime = JRuby.runtime

  # Every method and block under `scope`, building lazily built methods so
  # that their nested scopes exist.
  def each_scope(scope, &block)
    scope.builtInterpreterContext if scope.is_a?(IRMethod)
    yield scope if scope.is_a?(IRMethod) || scope.is_a?(IRClosure)
    scope.getLexicalScopes.each { |child| each_scope(child, &block) }
  end

  # Compiles one method or block as the JIT would (see MethodJITTask and
  # BlockJITTask), including loading the class, which is where the JVM
  # rejects invalid bytecode. Returns false if the scope is too large to JIT.
  # JIT max size hardcoded to 3000 to admit nearly all bodies. JRuby's default is lower.
  def compile(scope)
    return false if scope.builtInterpreterContext.getInstructions.length > 3000

    visitor = JVMVisitor.newForJIT(runtime)
    bytes = visitor.compileToBytecode(scope, JVMVisitorMethodContext.new)
    visitor.defineFromBytecode(scope, bytes, OneShotClassLoader.new(runtime.getJRubyClassLoader))
    true
  end

  # Persists the IR of a file and reads it back as JRuby loads a file that
  # jrubyc compiled. The reader decodes each scope's instructions only when
  # they are first used, so this decodes them all.
  def round_trip(script, path)
    bytes = java.io.ByteArrayOutputStream.new
    IRWriter.persist(IRWriterStream.new(bytes), script)
    manager = runtime.getIRManager
    restored = IRReader.load(manager, IRReaderStream.new(manager, bytes.toByteArray, path))
    each_scope(restored) { |scope| scope.getInterpreterContext.getInstructions }
  end

  def check_file(path, stats, failures)
    source = File.binread(path)
    begin
      verbose, $VERBOSE = $VERBOSE, nil # the corpus's parse warnings aren't ours
      script = JRuby.compile_ir(source, path)
    rescue SyntaxError, StandardError, java.lang.Throwable
      stats.unparsable += 1 # deliberately invalid files, or syntax JRuby doesn't support
      return
    ensure
      $VERBOSE = verbose
    end

    stats.files += 1
    scopes = []
    begin
      round_trip(script, path)
    rescue java.lang.Throwable, StandardError => e
      failures << Failure.new(file: path, line: 1, scope: 'IR persistence', error: e)
    end

    begin
      each_scope(script) { |scope| scopes << scope }
    rescue java.lang.Throwable, StandardError => e
      failures << Failure.new(file: path, line: 1, scope: 'IR build', error: e)
      return
    end

    scopes.each do |scope|
      stats.scopes += 1
      stats.too_large += 1 unless compile(scope)
    rescue java.lang.Throwable, StandardError => e
      kind = scope.is_a?(IRMethod) ? "method #{scope.getId}" : 'block'
      failures << Failure.new(file: path, line: scope.getLine + 1, scope: kind, error: e)
    end
  end

  def ruby_files(paths)
    paths.flat_map do |path|
      File.directory?(path) ? Dir.glob(File.join(path, '**', '*.rb')).sort : [path]
    end
  end

  # Known causes of failure, from a YAML file with two keys: `expected`, a
  # list of causes the JIT deliberately doesn't compile, and `bugs`, a map
  # from each cause that should compile to the URLs of the issues known to
  # produce it. A failure whose cause starts with a listed cause doesn't fail
  # the run.
  Known = Struct.new(:expected, :bugs) do
    def self.load(path)
      data = (path && File.exist?(path) && YAML.safe_load_file(path)) || {}
      new(Array(data['expected']), data['bugs'] || {})
    end

    # [:expected, []], [:bug, issue URLs], or nil for a new cause.
    def classify(cause)
      return [:expected, []] if expected.any? { |e| cause.start_with?(e) }

      bug = bugs.find { |c, _| cause.start_with?(c) }
      bug && [:bug, Array(bug.last)]
    end
  end

  def report(failures, known, verbose:)
    failures.group_by(&:cause).sort_by { |_, fs| -fs.size }.each do |cause, fs|
      kind, issues = known.classify(cause)
      label = { expected: 'EXPECTED', bug: 'KNOWN BUG', nil => 'FAIL' }.fetch(kind)
      puts "#{label} #{fs.size} scope(s): #{cause}"
      issues&.each { |issue| puts "        #{issue}" }
      (verbose ? fs : fs.first(3)).each { |f| puts "        #{f.location} (#{f.scope})" }
      puts "        ... and #{fs.size - 3} more" if !verbose && fs.size > 3
    end
  end

  def main(argv)
    known_file = File.expand_path('jit_compile_check.known.yml', __dir__)
    verbose = false
    OptionParser.new do |o|
      o.banner = 'Usage: bin/jruby tool/jit_compile_check.rb [options] [FILE_OR_DIR ...]'
      o.on('--known=FILE', '--excludes=FILE', "known causes of failure (default: #{known_file})") { known_file = _1 }
      o.on('-v', '--verbose', 'list every failing scope') { verbose = true }
    end.parse!(argv)

    files = ruby_files(argv)
    stats = Stats.new(files: 0, unparsable: 0, scopes: 0, too_large: 0)
    failures = []
    started = Process.clock_gettime(Process::CLOCK_MONOTONIC)
    files.each { |f| check_file(f, stats, failures) }

    known = Known.load(known_file)
    report(failures, known, verbose: verbose)
    kinds = failures.map { |f| known.classify(f.cause)&.first }.tally

    puts format("\n%d file(s), %d method(s) and block(s): %d failed (%d expected, %d known bugs, %d new), " \
                "%d too large to JIT, %d file(s) unparsable (%.1fs)",
                stats.files, stats.scopes, failures.size, kinds.fetch(:expected, 0), kinds.fetch(:bug, 0),
                kinds.fetch(nil, 0), stats.too_large, stats.unparsable,
                Process.clock_gettime(Process::CLOCK_MONOTONIC) - started)
    kinds.fetch(nil, 0).zero?
  end
end

exit(JITCompileCheck.main(ARGV.dup)) if $PROGRAM_NAME == __FILE__
