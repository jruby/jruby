
     
class JarInstaller
  
  def self.install( installer )
    spec = installer.spec
    return if spec.requirements.empty?

    jars_file = File.join( spec.gem_dir, spec.require_path, 
                           "#{spec.name}_jars.rb" )

    return if File.exists?( jars_file ) && 
      File.mtime( spec.spec_file ) < File.mtime( jars_file )

    deps = File.join( spec.gem_dir, 'deps.lst' )

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
    maven.exec 'dependency:list', "-DoutputFile=#{deps}", '-DincludeScope=runtime', '-DoutputAbsoluteArtifactFilename=true', '-DincludeTypes=jar', '-DoutputScope=false', '-f', spec.spec_file, '--quiet'

    FileUtils.mkdir_p( File.dirname( jars_file ) )
    File.open( jars_file, 'w' ) do |f|
      f.puts "require 'jar-dependencies'"
      f.puts
      File.read( deps ).each_line do |line|
        if line.match /:jar:/
          vendored = line.sub( /:jar:/, '-' )
          vendored.sub!( /:[^:]+$/, '' )
          vendored.sub!( /:[^:]+$/, '.jar' )
          vendored.sub!( /:/, File::SEPARATOR )
          vendored.sub!( /^\s+/, '' )
          vendored = File.join( spec.gem_dir, spec.require_path, vendored )

          line.gsub!( /:jar:|:compile:|:runtime:/, ':' )
          line.sub!( /^\s+/, '' )
          path = line.sub( /^.*:/, '' ).strip

          FileUtils.mkdir_p( File.dirname( vendored ) )
          FileUtils.cp( path, vendored )

          args = line.sub( /:[^:]+$/, '' ).gsub( /:/, "', '" )
          f.puts( "require_jarfile( '#{args}' )" )
        end
      end
    end
  end
end

