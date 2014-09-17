require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__), '..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Stdlib' do

  model_version '4.0.0'
  id "org.jruby:jruby-stdlib:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readOnly' => true,
              'jruby.basedir' => '${basedir}/../../',
              'jruby.home' => '${basedir}/../../',
              'bundle.name' => 'JRuby Stdlib',
              'bundle.symbolic_name' => 'org.jruby.jruby-stdlib',
              'jruby.complete.gems' => '${jruby.complete.home}/lib/ruby/gems/shared',
              'main.basedir' => '${project.parent.parent.basedir}',
              'gem.home' => '${jruby.basedir}/lib/ruby/gems/shared',
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home' )

  plugin( :jar, :archive => { :manifestFile => '${project.build.outputDirectory}/META-INF/MANIFEST.MF' } )

  plugin( :source,
          'skipSource' =>  'true' )
  plugin 'de.saumya.mojo:gem-maven-plugin:${jruby.plugins.version}' do
    execute_goals( 'exec',
                   :id => 'fix shebang on gem bin files and add *.bat files',
                   :phase => 'initialize',
                   'script' =>  'puts \'fix the gem stub files\'
		Dir[ \'../../lib/ruby/gems/shared/bin/*\' ].each do |f|
                  if File.file?( f )
		    content = File.read( f )
		    new_content = content.sub(/#!.*/, "#!/usr/bin/env jruby
")
		     File.open( f, "w" ) { |file| file.print( new_content ) }
                  end
		end
	
		puts \'generate the missing bat files\'
		Dir[File.join( \'${jruby.basedir}\', \'bin\', \'*\' )].each do |fn|
                  next unless File.file?(fn)
		  next if fn =~ /.bat$/
		  next if File.exist?("#{fn}.bat")
		  next unless File.open(fn, \'r\', :internal_encoding => \'ASCII-8BIT\') do |io|
		    line = io.readline rescue ""
		    line =~ /^#!.*ruby/
		  end
		  puts "Generating #{File.basename(fn)}.bat"
		  File.open("#{fn}.bat", "wb") do |f|
                    f.print "@ECHO OFF\r\n"
		    f.print "@\"%~dp0jruby.exe\" -S #{File.basename(fn)} %*\r\n"
                  end
		end' )
    execute_goals( 'exec',
                   :id => 'copy bin/jruby.bash to bin/jruby',
                   :phase => 'process-resources',
                   'script' =>  'puts \'copy jruby.bash to jruby\'
		require \'fileutils\'
		FileUtils.cp( File.join( \'${jruby.complete.home}\', \'bin\', \'jruby.bash\' ), File.join( \'${jruby.complete.home}\', \'bin\', \'jruby\' ) )' )
  end

  plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
    execute_goals( 'attach-artifact',
                   :id => 'attach-artifacts',
                   :phase => 'package',
                   'artifacts' => [ { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'sources' },
                                    { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'javadoc' } ] )
  end

  execute 'jrubydir', 'prepare-package' do |ctx|
    require( ctx.project.properties['jruby.home'].to_pathname + 'core/src/main/ruby/jruby/commands.rb' )
    JRuby::Commands.generate_dir_info( ctx.project.build.output_directory.to_pathname + '/META-INF/jruby.home' )
  end

  build do

    resource do
      directory '${gem.home}'
      includes 'gems/rake-${rake.version}/bin/r*', 'gems/rdoc-${rdoc.version}/bin/r*', 'specifications/default/*.gemspec'
      excludes 
      target_path '${jruby.complete.gems}'
    end

    resource do
      directory '${jruby.basedir}'
      includes 'bin/ast*', 'bin/gem*', 'bin/irb*', 'bin/jgem*', 'bin/jirb*', 'bin/jruby*', 'bin/rake*', 'bin/ri*', 'bin/rdoc*', 'bin/testrb*', 'lib/ruby/1.8/**', 'lib/ruby/1.9/**', 'lib/ruby/2.0/**', 'lib/ruby/shared/**'
      excludes 'bin/jruby', 'bin/jruby*_*', 'bin/jruby*-*', '**/.*', 'lib/ruby/shared/rubygems/defaults/jruby_native.rb'
      target_path '${jruby.complete.home}'
    end

    resource do
      directory '${basedir}/src/main/resources'
      filtering true
    end
  end

end
