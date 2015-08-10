class TargetBuilder
  attr_reader :targets
  def initialize( version, basedir )
    @version = version
    @basedir = basedir
    @names = ""
    @targets = ""
    @rake = 'lib/ruby/gems/shared/gems/rake-10.1.0/lib/rake/rake_test_loader.rb'
    # TODO
    #Dir[File.join(@basedir, "../lib/ruby/gems/shared/gems/rake-*/lib/rake/rake_test_loader.rb")]).first.sub( /.*\/..\/lib\//, 'lib/' )
  end
  def names
    @names[0..-2]
  end
  def create_target( name, complete )
    files = ""
    File.open(File.join(@basedir, name + '.index')) do |f|
      f.each_line.each do |line|
        filename = "#{@basedir}/#{line.chomp}.rb"
        next unless File.exist? filename
        next if filename =~ /externals\/ruby1.9\/ruby\/test_class/
        next if filename =~ /externals\/ruby1.9\/ruby\/test_io/
        next if filename =~ /externals\/ruby1.9\/ruby\/test_econv/
        next if filename =~ /externals\/ruby1.9\/test_open3/
        filename.sub!( /.*\/test\//, 'test/' )
        files << "<arg value='#{filename}'/>\n"
      end
    end

    if complete
      name = "test-jruby-complete-#{name}"
      jars = "maven/jruby-complete/target/jruby-complete-#{@version}.jar"
    else
      name = "test-jruby-jars-#{name}"
      jars = "lib/jruby.jar:maven/jruby-stdlib/target/jruby-stdlib-#{@version}.jar"
    end
    @names << name + ","
    @targets << "<target name='#{name}'>\n<exec executable='java' failonerror='true' vmlauncher='false'>\n<env key='GEM_PATH' value='lib/ruby/gems/shared'/>\n<arg value='-Djruby.home=uri:classloader://META-INF/jruby.home'/>\n<arg value='-cp'/>\n<arg value='core/target/test-classes:test/target/test-classes:#{jars}'/>\n<arg value='org.jruby.Main'/>\n<arg value='-I.:test/externals/ruby1.9:test/externals/ruby1.9/ruby'/>\n<arg value='-r./test/ruby19_env.rb'/>\n<arg value='-rminitest/excludes'/>\n<arg value='#{@rake}'/>\n#{files}<!--arg value='-v'/-->\n</exec>\n</target>\n"
  end
end

project 'JRuby Integration Tests' do
  
  version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip

  model_version '4.0.0'
  id 'jruby-tests'
  inherit 'org.jruby:jruby-parent', version
  packaging 'jar'

  repository( :id => 'rubygems-releases',
              :url => 'https://otto.takari.io/content/repositories/rubygems/maven/releases' )

  plugin_repository( :id => 'sonatype',
                     :url => 'https://oss.sonatype.org/content/repositories/snapshots/' ) do
    releases false
    snapshots true
  end
  plugin_repository( :id => 'rubygems-releases',
                     :url => 'https://otto.takari.io/content/repositories/rubygems/maven/releases' )

  properties( 'jruby.basedir' => '${basedir}/..',
              'gem.home' => '${jruby.basedir}/lib/ruby/gems/shared' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar( 'junit:junit:4.11',
       :scope => 'test' )
  jar( 'org.apache.ant:ant:${ant.version}',
       :scope => 'provided' )
  jar( 'bsf:bsf:2.4.0',
       :scope => 'provided' )
  jar( 'commons-logging:commons-logging:1.1.3',
       :scope => 'test' )
  jar( 'org.livetribe:livetribe-jsr223:2.0.7',
       :scope => 'test' )
  jar( 'org.jruby:requireTest:1.0',
       :systemPath => '${project.basedir}/requireTest-1.0.jar',
       :scope => 'system' )
  gem 'rubygems:jruby-launcher:${jruby-launcher.version}'
  gem 'rubygems:rake:${rake.version}'
  gem 'rubygems:rspec:${rspec.version}'
  gem 'rubygems:minitest:${minitest.version}'
  gem 'rubygems:minitest-excludes:${minitest-excludes.version}'
  gem 'rubygems:json:${json.version}'
  gem 'rubygems:rdoc:${rdoc.version}'

  plugin 'de.saumya.mojo:gem-maven-plugin:${jruby.plugins.version}' do
    execute_goals( 'initialize',
                   :phase => 'initialize',
                   'gemPath' =>  '${gem.home}',
                   'gemHome' =>  '${gem.home}',
                   'binDirectory' =>  '${jruby.basedir}/bin',
                   'includeRubygemsInTestResources' =>  'false',
                   'libDirectory' =>  '${jruby.basedir}/lib',
                   'jrubyJvmArgs' =>  '-Djruby.home=${jruby.basedir}' )
  end

  plugin( :compiler,
          'encoding' =>  'utf-8',
          'debug' =>  'true',
          'verbose' =>  'true',
          'fork' =>  'true',
          'showWarnings' =>  'true',
          'showDeprecation' =>  'true',
          'source' =>  '${base.java.version}',
          'target' =>  '${base.java.version}' )
  plugin :dependency do
    execute_goals( 'copy',
                   :id => 'copy jars for testing',
                   :phase => 'process-classes',
                   'artifactItems' => [ { 'groupId' =>  'junit',
                                          'artifactId' =>  'junit',
                                          'version' =>  '4.11',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'junit.jar' },
                                        { 'groupId' =>  'com.googlecode.jarjar',
                                          'artifactId' =>  'jarjar',
                                          'version' =>  '1.1',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'jarjar.jar' },
                                        { 'groupId' =>  'bsf',
                                          'artifactId' =>  'bsf',
                                          'version' =>  '2.4.0',
                                          'type' =>  'jar',
                                          'overWrite' =>  'false',
                                          'outputDirectory' =>  'target',
                                          'destFileName' =>  'bsf.jar' } ] )
  end

  plugin( :deploy,
          'skip' =>  'true' )
  plugin( :site,
          'skip' =>  'true',
          'skipDeploy' =>  'true' )

  build do
    default_goal 'test'
    test_source_directory '.'
  end

  builder = TargetBuilder.new( version, basedir )
  builder.create_target( 'jruby.1.9', false )
  builder.create_target( 'slow', true )
  builder.create_target( 'objectspace', false )
  builder.create_target( 'mri.1.9', false )
  builder.create_target( 'rubicon.1.9', true )
        
  File.write(File.join(basedir, '..', 'antlib', 'extra.xml'), "<project basedir='..'>\n<target name='mvn'>\n<exec executable='mvn' vmlauncher='false'>\n<arg line='-q'/>\n<arg line='-Ptest,bootstrap'/>\n<arg line='-DskipTests'/>\n</exec>\n<echo>\nbuild jruby maven artifact\n</echo>\n<exec executable='mvn' vmlauncher='false'>\n<arg line='-q'/>\n<arg line='-Pmain'/>\n</exec>\n<echo>\nbuild jruby-complete.jar\n</echo>\n<exec executable='mvn' vmlauncher='false'>\n<arg line='-q'/>\n<arg line='-Pcomplete'/>\n</exec>\n</target>\n#{builder.targets}<target description='test using jruby-complete or jruby-core/jruby-stdlib jars' name='test-jruby-jars' depends='mvn,#{builder.names}'/></project>")

end
