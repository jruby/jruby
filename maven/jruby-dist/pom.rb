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

    execute :fix_executable_bits do |ctx|
      Dir[ File.join( ctx.project.build.directory.to_pathname,
                      'META-INF/jruby.home/bin/*' ) ].each do |f|
        unless f.match /.(bat|exe|dll)$/
          puts f
          File.chmod( 0755, f ) rescue nil
        end
      end
    end

    execute :copy_windows_exe do |ctx|
      FileUtils.cp(File.join(ctx.project.build.directory.to_pathname,
                             'META-INF/jruby.home/bin/jruby.exe'),
                   File.join(ctx.project.build.directory.to_pathname,
                             ctx.project.build.finalName + '-windows.exe'))
    end

    plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
      execute_goal( 'attach-artifact',
                    :id => 'attach-windows-artifacts',
                    :artifacts => [ { :file => '${project.build.directory}/${project.build.finalName}-windows.exe',
                                      :type => 'exe',
                                      :classifier => 'windows' } ] )
    end

  end

  phase :package do
    plugin( :assembly, '2.4',
            :recompressZippedFiles => true,
            :tarLongFileMode =>  'gnu' ) do
      execute_goals( :single, :id => 'bin.tar.gz and bin.zip',
                     :descriptors => [ 'src/main/assembly/bin.xml' ] )
    end
  end

  plugin( :invoker )

  plugin( 'net.ju-n.maven.plugins:checksum-maven-plugin' )

  profile 'sonatype-oss-release' do

    plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
      execute_goals( 'attach-artifact',
                     id: 'attach-checksums',
                     phase: :package,
                     artifacts: [ { file: '${project.build.directory}/jruby-dist-${project.version}-bin.tar.gz.sha256',
                                    classifier: :bin,
                                    type: 'tar.gz.sha256'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-bin.tar.gz.sha512',
                                    classifier: :bin,
                                    type: 'tar.gz.sha512'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-bin.zip.sha256',
                                    classifier: :bin,
                                    type: 'zip.sha256'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-bin.zip.sha512',
                                    classifier: :bin,
                                    type: 'zip.sha512'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-src.zip.sha256',
                                    classifier: :src,
                                    type: 'zip.sha256'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-src.zip.sha512',
                                    classifier: :src,
                                    type: 'zip.sha512'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-windows.exe.sha256',
                                    classifier: :windows,
                                    type: 'exe.sha256'},
                                  { file: '${project.build.directory}/jruby-dist-${project.version}-windows.exe.sha512',
                                    classifier: :windows,
                                    type: 'exe.sha512'} ] )

    end
  end

  # since the source packages are done from the git repository we need
  # to be inside a git controlled directory. for example the source packages
  # itself does not contain the git repository and can not pack
  # the source packages itself !!

  profile 'source dist' do

    activation do
      file( :exists => '../../.git' )
    end

    phase 'prepare-package' do
      execute :pack_sources do |ctx|
        require 'fileutils'

        revision = `git log -1 --format="%H"`.chomp

        basefile = "#{ctx.project.build.directory}/#{ctx.project.artifactId}-#{ctx.project.version}-src"

        FileUtils.cd( File.join( ctx.project.basedir.to_s, '..', '..' ) ) do
          [ 'tar.gz', 'zip' ].each do |format|
            puts "create #{basefile}.#{format}"
            system( "git archive --prefix 'jruby-#{ctx.project.version}/' --format #{format} #{revision} . -o #{basefile}.#{format}" ) || raise( "error creating #{format}-file" )
          end
        end
      end
      plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
        execute_goal( 'attach-artifact',
                      :id => 'attach-artifacts',
                      :artifacts => [ { :file => '${project.build.directory}/${project.build.finalName}-src.zip',
                                        :type => 'zip',
                                        :classifier => 'src' } ] )

      end
    end
  end
end
