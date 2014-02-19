project 'JRuby Dist' do

  version = '9000.dev' #File.read( File.join( basedir, '..', '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-dist:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.0.9',
              'jruby.home' => '${basedir}/../..',
              'main.basedir' => '${project.parent.parent.basedir}' )

  phase 'package' do
    plugin :dependency do
      execute_goals( 'unpack',
                     :id => 'unpack jruby-stdlib',
                     'stripVersion' =>  'true',
                     'artifactItems' => [ { 'groupId' =>  'org.jruby',
                                            'artifactId' =>  'jruby-stdlib',
                                            'version' =>  '${project.version}',
                                            'type' =>  'jar',
                                            'overWrite' =>  'false',
                                            'outputDirectory' =>  '${project.build.directory}' } ] )
    end    
  end

  execute :fix_executable_bits, 'package' do |ctx|
    Dir[ File.join( ctx.project.build.directory,
                    'META-INF', 
                    'jruby.home', 
                    'bin', 
                    '*' ) ].each do |f|
      unless f.match /.(bat|exe|dll)$/
        puts f
        File.chmod( 0755, f ) rescue nil
      end
    end
  end

  phase :package do
    plugin( :assembly, '2.4',
            'tarLongFileMode' =>  'gnu',
            'descriptors' => [ 'src/main/assembly/jruby.xml' ] ) do
      execute_goals( 'single' )
    end
  end

  plugin( :invoker )

  # for the release we do some extra IT by unzipping a source dist file
  # and run the $ mvn -Pall from there and check a few things to be in place.
  # add check for SNAPSHOT version in any of the modules !!!!

  profile 'release' do

    plugin( :invoker, :pomIncludes => [ '*' ] )

    phase 'pre-integration-test' do
      plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
        execute_goal( 'attach-artifact',
                      :artifacts => [ { :file => '${project.build.directory}/jruby-dist-${project.version}-src.zip',
                                        :type => 'zip',
                                        :classifier => 'src' } ] )
        
      end

      plugin :dependency do
        execute_goals( 'unpack',
                       :id => 'unpack jruby-dist',
                       'stripVersion' =>  'true',
                       'artifactItems' => [ { 'groupId' =>  'org.jruby',
                                              'artifactId' =>  'jruby-dist',
                                              'version' =>  '${project.version}',
                                              'type' =>  'zip',
                                              'classifier' => 'src',
                                              'overWrite' =>  'false',
                                              'outputDirectory' =>  '${project.build.directory}' } ] )
      end
    end

    execute :prepare_sources_for_it, 'pre-integration-test' do |ctx|
      require 'fileutils'
      dir = File.join( "#{ctx.project.build.directory}", 'it' )
      FileUtils.mkdir_p dir
      dir = File.join( dir, 'sources')
      FileUtils.mv( "#{ctx.project.build.directory}/jruby-#{ctx.project.version}", "#{dir}" ) unless File.exists? "#{dir}"
      File.open( "#{dir}/test.properties", 'w' ) do |f|
        f.puts File.join( "outputFile=#{File.expand_path( dir )}", 'tree.txt' )
        f.puts 'appendOutput=true'
      end
    end
  end

  # since the source packages are done from the git repository we need
  # to inside git controlled directory. for example the source packages
  # itself does not contain the git repository !!

  profile 'source dist' do

    activation do
      file( :exists => '../../.git' )
    end

    execute :pack_sources, 'package' do |ctx|
      require 'fileutils'

      revision = `git show`.gsub( /\n.*|commit /, '' )
      
      basefile = "#{ctx.project.build.directory}/#{ctx.project.artifactId}-#{ctx.project.version}-src"

      FileUtils.cd( File.join( ctx.project.basedir.to_s, '..', '..' ) ) do
        [ 'tar', 'zip' ].each do |format|
          puts "create #{basefile}.#{format}"
          system( "git archive --prefix 'jruby-#{ctx.project.version}/' --format #{format} #{revision} . -o #{basefile}.#{format}" ) || raise( "error creating #{format}-file" )
        end
      end
      puts "zipping #{basefile}.tar"
      system( "gzip #{basefile}.tar -f" ) || raise( "error zipping #{basefile}.tar" )
    end
  end

end
