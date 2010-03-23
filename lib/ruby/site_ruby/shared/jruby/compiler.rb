require 'optparse'
require 'jruby'
require 'jruby/compiler/java_class'

module JRuby::Compiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  BytecodeCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = org.jruby.compiler.ASTCompiler
  JavaFile = java.io.File
  MethodSignatureNode = org.jruby.ast.java_signature.MethodSignatureNode
  
  def compile_argv(argv)
    basedir = Dir.pwd
    prefix = ""
    target = Dir.pwd
    java = false
    javac = false
    classpath = []

    opt_parser = OptionParser.new("", 24, '  ') do |opts|
      opts.banner = "jrubyc [options] (FILE|DIRECTORY)"
      opts.separator ""

      opts.on("-d", "--dir DIR", "Use DIR as the root of the compiled package and filename") do |dir|
        basedir = dir
      end

      opts.on("-p", "--prefix PREFIX", "Prepend PREFIX to the file path and package. Default is no prefix.") do |pre|
        prefix = pre
      end

      opts.on("-t", "--target TARGET", "Output files to TARGET directory") do |tgt|
        target = tgt
      end

      opts.on("-j", "--java", "Generate .java classes to accompany the script") do
        java = true
      end

      opts.on("-J", "--javac", "Generate and compile .java classes to accompany the script") do
        javac = true
      end

      opts.on("-c", "--classpath CLASSPATH", "Add a jar to the classpath for building") do |cp|
        classpath.concat cp.split(':')
      end

      opts.parse!(argv)
    end

    if (argv.length == 0)
      raise "No files or directories specified"
    end

    compile_files(argv, basedir, prefix, target, java, javac, classpath)
  end
  module_function :compile_argv

  def compile_files(filenames, basedir = Dir.pwd, prefix = "ruby", target = Dir.pwd, java = false, javac = false, classpath = [])
    runtime = JRuby.runtime

    unless File.exist? target
      raise "Target dir not found: #{target}"
    end

    files = []

    # The compilation code
    compile_proc = proc do |filename|
      begin
        file = File.open(filename)

        pathname = Mangler.mangle_filename_for_classpath(filename, basedir, prefix)

        inspector = org.jruby.compiler.ASTInspector.new

        source = file.read
        node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

        if java || javac
          ruby_script = process_script(node, filename)
          ruby_script.classes.each do |cls|
            puts "Generating Java class #{cls.name} to #{cls.name}.java"
            java_src = cls.name + ".java";
            files << java_src

            File.open(java_src, 'w') do |f|
              f.write(cls.to_s)
            end
          end
        else
          puts "Compiling #{filename} to class #{pathname}"

          inspector.inspect(node)

          asmCompiler = BytecodeCompiler.new(pathname, filename)
          compiler = ASTCompiler.new
          compiler.compile_root(node, asmCompiler, inspector)

          asmCompiler.write_class(JavaFile.new(target))
        end

        0
      rescue Exception
        puts "Failure during compilation of file #{filename}:\n#{$!}"
        puts $!.backtrace
        1
      ensure
        file.close unless file.nil?
      end
    end

    errors = 0
    # Process all the file arguments
    Dir[*filenames].each do |filename|
      unless File.exists? filename
        puts "Error -- file not found: #{filename}"
        errors += 1
        next
      end

      if (File.directory?(filename))
        puts "Compiling all in '#{File.expand_path(filename)}'..."
        Dir.glob(filename + "/**/*.rb").each { |filename|
          errors += compile_proc[filename]
	}
      else
        errors += compile_proc[filename]
      end
    end

    if javac
      files_string = files.join(' ')
      jruby_jar, = ['jruby.jar', 'jruby-complete.jar'].select do |jar|
        File.exist? "#{ENV_JAVA['jruby.home']}/lib/#{jar}"
      end
      classpath_string = classpath.size > 0 ? classpath.join(":") : "."
      compile_string = "javac -d #{target} -cp #{ENV_JAVA['jruby.home']}/lib/#{jruby_jar}:#{classpath_string} #{files_string}"
      puts compile_string
      system compile_string
    end

    errors
  end
  module_function :compile_files
end
