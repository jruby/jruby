require 'jruby'

module JRubyCompiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  BytecodeCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = org.jruby.compiler.ASTCompiler
  JavaFile = java.io.File

  module_function
  def compile_args  
    runtime = JRuby.runtime

    if ARGV.size < 1
      puts "Usage: jrubyc <filename.rb> [<filename.rb> ...]"
      exit
    end

    # The compilation code
    compile_proc = proc do |filename|
      begin
        file = File.open(filename)
        destdir = Dir.pwd

        classpath = Mangler.mangle_filename_for_classpath(filename)
        puts " Compiling #{filename} to class #{classpath}"

        inspector = org.jruby.compiler.ASTInspector.new

        source = file.read
        node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

        inspector.inspect(node)

        asmCompiler = BytecodeCompiler.new(classpath, filename)
        compiler = ASTCompiler.new
        compiler.compile_root(node, asmCompiler, inspector)

        asmCompiler.write_class(JavaFile.new(destdir))
      rescue Exception
        puts "Failure during compilation of file #{filename}:\n#{$!}"
      ensure
        file.close unless file.nil?
      end
    end

    # Process all the file arguments
    ARGV.each do |filename|
      unless File.exists? filename
        puts "Error -- file not found: #{filename}"
        next
      end

      if (File.directory?(filename))
        puts "Compiling all in '#{File.expand_path(filename)}'..."
        Dir.glob(filename + "/**/*.rb").each(&compile_proc)
      else
        compile_proc[filename]
      end
    end
  end
end