require 'optparse'
require 'fileutils'
require 'jruby'
require 'jruby/compiler/java_class'

module JRuby::Compiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  BytecodeCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = org.jruby.compiler.ASTCompiler
  JavaFile = java.io.File
  MethodSignatureNode = org.jruby.ast.java_signature.MethodSignatureNode
  DEFAULT_PREFIX = ""
  
  def compile_argv(argv)
    basedir = Dir.pwd
    prefix = DEFAULT_PREFIX
    target = Dir.pwd
    java = false
    javac = false
    classpath = []
    javac_options = []

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

      opts.on("-J OPTION", "Pass OPTION to javac for javac compiles") do |tgt|
        javac_options << tgt
      end

      opts.on("--java", "Generate .java classes to accompany the script") do
        java = true
      end

      opts.on("--javac", "Generate and compile .java classes to accompany the script") do
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

    compile_files(argv, basedir, prefix, target, java, javac, javac_options, classpath)
  end
  module_function :compile_argv

  def compile_files(filenames, basedir = Dir.pwd, prefix = DEFAULT_PREFIX, target = Dir.pwd, java = false, javac = false, javac_options = [], classpath = [])
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
          ruby_script = JavaGenerator.generate_java(node, filename)
          ruby_script.classes.each do |cls|
            java_dir = File.join(target, cls.package.gsub('.', '/'))

            FileUtils.mkdir_p java_dir

            java_src = File.join(java_dir, cls.name + ".java")
            puts "Generating Java class #{cls.name} to #{java_src}"
            
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
      javac_string = JavaGenerator.generate_javac(files, javac_options, classpath, target)
      puts javac_string
      system javac_string
    end

    errors
  end
  module_function :compile_files
end
