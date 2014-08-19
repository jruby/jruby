require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__),'..', '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Main Maven Artifact With ASM Relocated' do

  model_version '4.0.0'
  id "org.jruby:jruby-noasm:#{version}"
  inherit "org.jruby:jruby-artifacts:#{version}"
  packaging 'jar'

  properties( 'tesla.dump.pom' => 'pom-generated.xml',
              'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}' )

  jar( 'org.jruby:jruby-core:${project.version}:noasm',
       :exclusions => [ 'com.github.jnr:jnr-ffi',
                        'org.ow2.asm:asm',
                        'org.ow2.asm:asm-commons',
                        'org.ow2.asm:asm-analysis',
                        'org.ow2.asm:asm-util' ] )
  jar 'org.jruby:jruby-stdlib:${project.version}'

  plugin( :source,
          'skipSource' =>  'true' )
  plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
    execute_goals( 'attach-artifact',
                   :id => 'attach-artifacts',
                   :phase => 'package',
                   'artifacts' => [ { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'sources' },
                                    { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'javadoc' } ] )
  end

  plugin( :invoker,
          'projectsDirectory' =>  'src/it',
          'cloneProjectsTo' =>  '${project.build.directory}/it',
          'preBuildHookScript' =>  'setup.bsh',
          'postBuildHookScript' =>  'verify.bsh' ) do
    execute_goals( 'install', 'run',
                   :id => 'integration-test',
                   'settingsFile' =>  '${basedir}/src/it/settings.xml',
                   'localRepositoryPath' =>  '${project.build.directory}/local-repo' )
  end

end
