require 'rubygems/package'
require 'fileutils'
project 'JRuby Dist' do
  
  version = ENV['JRUBY_VERSION'] ||
    File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby-dist:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'pom'

  phase 'prepare-package' do

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

    execute :pack200 do |ctx|
      jruby_home = Dir[ File.join( ctx.project.build.directory.to_pathname,
                                   'META-INF/jruby.home/**/*.jar' ) ]
      gem_home = Dir[ File.join( ctx.project.build.directory.to_pathname,
                                  'rubygems-provided/**/*.jar' ) ]
      lib_dir = Dir[ File.join( ctx.basedir.to_pathname,
                                '../../lib/*.jar' ) ]

      (jruby_home + gem_home + lib_dir).each do |f|
        file = f.sub /.jar$/, ''
        unless File.exists?( file + '.pack.gz' )
          puts "pack200 #{f.sub(/.*jruby.home./, '').sub(/.*rubygems-provided./, '')}"
          system('pack200', "#{file}.pack.gz", "#{file}.jar")
        end
      end
    end

    execute :fix_executable_bits do |ctx|
      Dir[ File.join( ctx.project.build.directory.to_pathname,
                      'META-INF/jruby.home/bin/*' ) ].each do |f|
        unless f.match /.(bat|exe|dll)$/
          puts f
          File.chmod( 0755, f ) rescue nil
        end
      end
    end
  end

  phase :package do
    plugin( :assembly, '2.4',
            :recompressZippedFiles => true,
            :tarLongFileMode =>  'gnu' ) do
      execute_goals( :single, :id => 'bin.tar.gz and bin.zip',
                     :descriptors => [ 'src/main/assembly/bin.xml' ] )
      execute_goals( :single, :id => 'bin200.tar.gz',
                     :attach => false,
                     :descriptors => [ 'src/main/assembly/bin200.xml' ] )
    end
  end

  plugin( :invoker )

  # since the source packages are done from the git repository we need
  # to be inside a git controlled directory. for example the source packages
  # itself does not contain the git repository and can not pack
  # the source packages itself !!

  profile 'source dist' do

    activation do
      file( :exists => '../../.git' )
    end

    phase 'package' do
      execute :pack_sources do |ctx|
        require 'fileutils'

        revision = `git log -1 --format="%H"`.chomp

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
      plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
        execute_goal( 'attach-artifact',
                      :id => 'attach-artifacts',
                      :artifacts => [ { :file => '${project.build.directory}/${project.build.finalName}-src.zip',
                                        :type => 'zip',
                                        :classifier => 'src' } ] )

      end
      plugin( 'net.ju-n.maven.plugins:checksum-maven-plugin', '1.2' ) do
        execute_goals( :artifacts, :algorithms => ['SHA256' ] )
      end
    end
  end
end
