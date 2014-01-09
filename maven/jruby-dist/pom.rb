project 'JRuby Dist' do

  version = '1.7.10' #File.read( File.join( basedir, '..', '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-dist:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'jruby.basedir' => '${basedir}/../..',
              'main.basedir' => '${project.parent.parent.basedir}' )

  phase 'package' do
    plugin :dependency do
      execute_goals( 'unpack',
                     :id => 'unpack jruby-stdlib',
                     'stripVersion' =>  'true',
                     'artifactItems' => [ { 'groupId' =>  'org.jruby',
                                            'artifactId' =>  'jruby-stdlib-complete',
                                            'version' =>  '${project.version}',
                                            'type' =>  'jar',
                                            'overWrite' =>  'false',
                                            'outputDirectory' =>  '${project.build.directory}' } ] )
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

    plugin( :assembly, '2.4',
            'tarLongFileMode' =>  'gnu',
            'descriptors' => [ 'src/main/assembly/jruby.xml' ] ) do
      execute_goals( 'single' )
    end
  end

  # TODO move into plugin-management of root pom
  plugin( :invoker,
          'projectsDirectory' =>  'src/it',
          'cloneProjectsTo' =>  '${project.build.directory}/it',
          'preBuildHookScript' =>  'setup.bsh',
          'postBuildHookScript' =>  'verify.bsh',
          'streamLogs' =>  'true' ) do
    execute_goals( 'install', 'run',
                   :id => 'integration-test',
                   'settingsFile' =>  '${basedir}/src/it/settings.xml',
                   'localRepositoryPath' =>  '${project.build.directory}/local-repo' )
  end

  profile 'source dist' do

    activation do
      file( :exists => '../../.git' )
    end

    phase :package do
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

end
