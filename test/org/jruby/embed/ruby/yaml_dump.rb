# yaml_dump.rb [embed]

require 'yaml'

def dump
  content = YAML::load @text
  content.each { |k, v| puts "#{k}: #{v.join(", ")}" }
end