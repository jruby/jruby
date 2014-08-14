require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__),'..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Stdlib Complete' do

  model_version '4.0.0'
  id "org.jruby:jruby-stdlib-complete:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom-generated.xml',
              'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}',
              'jruby.complete.home' => '${project.build.outputDirectory}/META-INF/jruby.home' )

  jar( 'org.jruby:jruby-stdlib:${project.version}',
       :exclusions => [ 'org.bouncycastle:bcpkix-jdk15on',
                        'org.bouncycastle:bcprov-jdk15on' ] )

  plugin( :deploy,
          'skip' =>  'true' )
  plugin :shade, '2.1' do
    execute_goals( 'shade',
                   :id => 'pack artifact',
                   :phase => 'package' )
  end


  build do

    resource do
      directory '${jruby.basedir}/lib'
      includes 'lib/**/bc*.jar'
      excludes 
      target_path '${jruby.complete.home}/lib'
    end
  end

end
