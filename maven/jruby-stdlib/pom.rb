require 'fileutils'

project 'JRuby Stdlib' do

  version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id "org.jruby:jruby-stdlib:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'jruby_home' => '${basedir}/../..',
              'gem.home' => '${jruby_home}/lib/ruby/gems/shared',
              'main.basedir' => '${project.parent.parent.basedir}',
              # we copy everything into the target/classes/META-INF
              # so the jar plugin just packs it - see build/resources below
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home',
              'jruby.complete.gems' => '${jruby.complete.home}/lib/ruby/gems/shared' )

  unless version =~ /-SNAPSHOT/
    properties 'jruby.home' => '${basedir}/../..'
  end

  execute( 'fix shebang on gem bin files and add *.bat files',
           'initialize' ) do |ctx|
    
    puts 'fix the gem stub files'
    jruby_home = ctx.project.properties.get_property( 'jruby_home' )
    bindir = File.join( jruby_home, 'lib', 'ruby', 'gems', 'shared', 'bin' )
    Dir[ File.join( bindir, '*' ) ].each do |f|
      content = File.read( f )
      new_content = content.sub( /#!.*/, "#!/usr/bin/env jruby
" )
      File.open( f, "w" ) { |file| file.print( new_content ) }
    end
    
    puts 'generate the missing bat files'
    Dir[File.join( jruby_home, 'bin', '*' )].each do |fn|
      next unless File.file?(fn)
      next if fn =~ /.bat$/
      next if File.exist?("#{fn}.bat")
      next unless File.open(fn, 'r', :internal_encoding => 'ASCII-8BIT') do |io|
        line = io.readline rescue ""
        line =~ /^#!.*ruby/
      end
      puts "Generating #{File.basename(fn)}.bat"
      File.open("#{fn}.bat", "wb") do |f|
        f.print "@ECHO OFF\r\n"
        f.print "@\"%~dp0jruby.exe\" -S #{File.basename(fn)} %*\r\n"
      end
    end
  end
  
  execute( 'copy bin/jruby.bash to bin/jruby',
           'process-resources' ) do |ctx|
    require 'fileutils'
    jruby_complete = ctx.project.properties.get_property( 'jruby.complete.home' )
    FileUtils.cp( File.join( jruby_complete, 'bin', 'jruby.bash' ), 
                  File.join( jruby_complete, 'bin', 'jruby' ) )
  end

  execute 'jrubydir', 'prepare-package' do |ctx|
    require( ctx.project.properties['jruby_home'].to_pathname + '/core/src/main/ruby/jruby/commands.rb' )
    JRuby::Commands.generate_dir_info( ctx.project.build.output_directory.to_pathname + '/META-INF/jruby.home' )
  end

  # we have no sources and attach an empty jar later in the build to
  # satisfy oss.sonatype.org upload

  plugin( :source, 'skipSource' =>  'true' )

  # this plugin is configured to attach empty jars for sources and javadocs
  plugin( 'org.codehaus.mojo:build-helper-maven-plugin' )

  plugin( :invoker )

  build do

    # both resources are includes for the $jruby_home/lib directory

    resource do
      directory '${gem.home}'
      includes 'gems/rake-${rake.version}/bin/r*', 'gems/rdoc-${rdoc.version}/bin/r*', 'specifications/default/*.gemspec'
      target_path '${jruby.complete.gems}'
    end

    resource do
      directory '${jruby_home}'
      includes 'bin/ast*', 'bin/gem*', 'bin/irb*', 'bin/jgem*', 'bin/jirb*', 'bin/jruby*', 'bin/rake*', 'bin/ri*', 'bin/rdoc*', 'bin/testrb*', 'lib/ruby/stdlib/**', 'lib/ruby/truffle/**', 'bin/unpack200.sh'
      excludes 'bin/jruby', 'bin/jruby*_*', 'bin/jruby*-*', '**/.*', 'lib/ruby/stdlib/rubygems/defaults/jruby_native.rb'
      target_path '${jruby.complete.home}'
    end

    resource do
      directory '${basedir}/src/main/resources'
      filtering true
    end
  end
end
