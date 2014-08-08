require 'rexml/document'
require 'rexml/xpath'

doc = REXML::Document.new File.new(File.join(File.dirname(__FILE__), '..', 'pom.xml'))
version = REXML::XPath.first(doc, "//project/version").text

project 'JRuby Artifacts' do

  model_version '4.0.0'
  id "org.jruby:jruby-artifacts:#{version}"
  inherit "org.jruby:jruby-parent:#{version}"
  packaging 'pom'

  properties 'tesla.dump.pom' => 'pom-generated.xml'

  profile 'all' do

    modules [ 'jruby',
            'jruby-noasm',
            'jruby-stdlib',
            'jruby-complete',
            'jruby-rake-plugin',
            'jruby-core-complete',
            'jruby-stdlib-complete',
            'jruby-jars',
            'jruby-dist' ]

  end

  profile 'release' do

    modules [ 'jruby',
            'jruby-noasm',
            'jruby-stdlib',
            'jruby-complete',
            'jruby-rake-plugin',
            'jruby-core-complete',
            'jruby-stdlib-complete',
            'jruby-dist' ]

  end

  profile 'main' do

    modules [ 'jruby',
            'jruby-noasm',
            'jruby-stdlib' ]

  end

  profile 'complete' do

    modules [ 'jruby-stdlib',
            'jruby-complete' ]

  end

  profile 'rake-plugin' do

    modules [ 'jruby-rake-plugin' ]

  end

  profile 'dist' do

    modules [ 'jruby-stdlib',
            'jruby-stdlib-complete',
            'jruby-dist' ]

  end

  profile 'jruby-jars' do

    modules [ 'jruby-stdlib',
            'jruby-core-complete',
            'jruby-stdlib-complete',
            'jruby-jars' ]

  end

end
