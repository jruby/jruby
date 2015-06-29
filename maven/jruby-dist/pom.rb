require 'rubygems/package'
require 'fileutils'
project 'JRuby Dist' do

  version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby-dist:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readOnly' => true,
              'jruby.home' => '${basedir}/../..',
              'main.basedir' => '${project.parent.parent.basedir}',
              'ruby.maven.version' => '3.3.3',
              'ruby.maven.libs.version' => '3.3.3' )

  # pre-installed gems - not default gems !
  gem 'ruby-maven', '${ruby.maven.version}', :scope => 'provided'

  # add torquebox repo only when building from filesystem
  # not when using the pom as "dependency" in some other projects
  profile 'gem proxy' do

    activation do
      file( :exists => '../jruby' )
    end

    repository( :url => 'http://rubygems-proxy.torquebox.org/releases',
                :id => 'rubygems-releases' )
  end

  jruby_plugin :gem do
    execute_goal :initialize
  end

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

    execute :fix_permissions do |ctx|
      gems = File.join( ctx.project.build.directory.to_pathname, 'rubygems-provided' )
      ( Dir[ File.join( gems, '**/*' ) ] + Dir[ File.join( gems, '**/.*' ) ] ).each do |f|
        File.chmod( 0644, f ) rescue nil if File.file?( f )
      end
    end

    execute :dump_full_specs_of_ruby_maven do |ctx|
      rubygems =  File.join( ctx.project.build.directory.to_pathname, 'rubygems-provided' )
      gems =  File.join( rubygems, 'cache/*gem' )
      default_specs = File.join( rubygems, 'specifications/default' )
      FileUtils.mkdir_p( default_specs )
      Dir[gems].each do |gem|
        spec = Gem::Package.new( gem ).spec
        File.open( File.join( default_specs, File.basename( gem ) + 'spec' ), 'w' ) do |f|
          f.print( spec.to_ruby )
        end
      end
    end
  end

  phase :package do
    plugin( :assembly, '2.4',
            'finalName' => "#{model.artifact_id}-#{version.sub(/-SNAPSHOT/, '')}",
            'tarLongFileMode' =>  'gnu',
            'descriptors' => [ 'src/main/assembly/jruby.xml' ] ) do
      execute_goals( 'single' )
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

    execute :pack_sources, 'package' do |ctx|
      require 'fileutils'

      revision = `git show`.gsub( /\n.*|commit /, '' )

      basefile = "#{ctx.project.build.directory}/#{ctx.project.artifactId}-#{ctx.project.version}-src".sub(/-SNAPSHOT/, '')

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
