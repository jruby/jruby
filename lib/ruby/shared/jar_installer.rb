class JarInstaller

  def self.install_jars
    new.install_jars
  end
  
  def initialize( spec = nil )
    if spec.nil?
      specs = Dir[ '*.gemspec' ]
      raise 'more then one gemspec found. please specify one in the constructor' if specs.size > 1
      raise 'no gemspec found' if specs.size == 0
      spec = specs.first
    end
    if spec.is_a? String
      @basedir = File.dirname( File.expand_path( spec ) )
      @specfile = spec
      spec =  eval( File.read( spec ) )
    else
      @basedir = spec.gem_dir
      @specfile = spec.spec_file
    end
    @spec = spec
  end

  def vendor_jars
    return if @spec.requirements.empty?
    _install( true )
  end

  def install_jars
    _install( false )
  end

  private

  def _install( vendor )
    jars_file = File.join( @basedir, @spec.require_path, 
                           "#{@spec.name}_jars.rb" )

    return if File.exists?( jars_file ) && 
      File.mtime( @specfile ) < File.mtime( jars_file )

    deps = File.join( @basedir, 'deps.lst' )

    # lazy load ruby-maven
    begin
      require 'maven/ruby/maven'

    rescue LoadError
      raise 'please install ruby-maven gem so the jar dependencies can be installed'
    end
   
    # monkey patch to NOT include gem dependencies
    require 'maven/tools/gemspec_dependencies'
    eval <<EOF
      class ::Maven::Tools::GemspecDependencies
        def runtime; []; end
        def development; []; end
      end
EOF

    maven = Maven::Ruby::Maven.new
    maven.exec 'dependency:list', "-DoutputFile=#{deps}", '-DincludeScope=runtime', '-DoutputAbsoluteArtifactFilename=true', '-DincludeTypes=jar', '-DoutputScope=false', '-f', @specfile, '--quiet'

    FileUtils.mkdir_p( File.dirname( jars_file ) )
    File.open( jars_file, 'w' ) do |f|
      f.puts "require 'jar-dependencies'"
      f.puts
      File.read( deps ).each_line do |line|
        if line.match /:jar:/
          if vendor
            vendored = line.sub( /:jar:/, '-' )
            vendored.sub!( /:[^:]+$/, '' )
            vendored.sub!( /:[^:]+$/, '.jar' )
            vendored.sub!( /:/, File::SEPARATOR )
            vendored.sub!( /^\s+/, '' )
            vendored = File.join( @basedir, @spec.require_path, vendored )
          end

          line.gsub!( /:jar:|:compile:|:runtime:/, ':' )
          line.sub!( /^\s+/, '' )
          path = line.sub( /^.*:/, '' ).strip
          args = line.sub( /:[^:]+$/, '' ).gsub( /:/, "', '" )

          if vendor
            FileUtils.mkdir_p( File.dirname( vendored ) )
            FileUtils.cp( path, vendored )

            f.puts( "require_jarfile( '#{args}' )" )
          else
            f.puts( "require_jarfile( '#{path}', '#{args}' )" )
          end
        end
      end
    end
  ensure
    FileUtils.rm_f( deps ) if deps
  end
end

