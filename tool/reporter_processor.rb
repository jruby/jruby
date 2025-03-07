require 'optparse'
require 'errors'                      # Generated from revapi
require_relative 'acceptable_errors'  # Our list of removals and unimportant errors

new_name_search = old_name_search = filter = version = nil

opts = OptionParser.new do |o|
  o.banner = 'Usage: #$0 [options]'
  o.separator <<~EOS
Processes our generate revapi report

This will take any revapi report generated and prune it down
until it only contains interesting changes.  When we find
entries that are acceptable we put them into ./tool/acceptable_errors.rb.


EOS

  o.on('-a', '--acceptable {version}', 'version where the report line is ok') do |arg|
    version = arg
  end
  o.on('-t', '--type {type}', 'match parameter in revapi ala `java.class.removed`') do |arg|
    filter = arg
  end
  o.on('-o', '--old-name {str}', 'regexp match against old_name') do |arg|
    old_name_search = Regexp.new(arg)
  end
  o.on('-n', '--new-name {str}', 'regexp match against new_name') do |arg|
    new_name_search = Regexp.new(arg)
  end
end

opts.parse!(ARGV)

# Generic Rules
NOT = ->(f1) { ->(a) {!f1.call(a) } }
AND = ->(*fns) { ->(a) { fns.all? { |f| f.call(a) } } }
OR = ->(*fns) { ->(a) { fns.any? { |f| f.call(a) } } }
FALSE = ->(a) { false }
TRUE = ->(a) { true }
NOOP = TRUE

# FIXME: to make this a generic tool move specific rules and skip list
#  to its own require so this script can be used by other Java projects.
INVOKER = ->(a) { !!a.old_name.match(/\$INVOKER/) }
POPULATOR = ->(a) { !!a.old_name.match(/\$POPULATOR/) }
INTERP_ANNO = ->(a) { !!a.desc.match(/org\.jruby\.ir\.Interp/) }
JIT_ANNO = ->(a) { !!a.desc.match(/org\.jruby\.ir\.JIT/) }
JRUBY_API_ANNO = ->(a) { !!a.desc.match(/org\.jruby\.api\.JRubyAPI/) }
JRUBY_METHOD_ANNO = ->(a) { !!a.desc.match(/org\.jruby\.anno\.JRubyMethod/) }
IGNORABLE_METHODS = OR.call(JRUBY_METHOD_ANNO, INVOKER, POPULATOR)
IGNORABLE_ANNOS = OR.call(JRUBY_METHOD_ANNO, INTERP_ANNO, JIT_ANNO, JRUBY_API_ANNO)

SKIPS = {
 'java.missing.oldClass' => NOOP,          # Some third party missing class data
 'java.missing.newClass' => NOOP,          # Same as old class but not found in new
 'java.field.constantValueChanged' => NOOP,      # 13.0 -> 15.0 value change
 'java.field.enumConstantOrderChanged' => NOOP,  # enum was not added to end
 'java.class.removed' => IGNORABLE_METHODS,      #
 'java.class.nonFinalClassInheritsFromNewClass' => INVOKER,
 'java.class.noLongerInheritsFromClass' => INVOKER,
 'java.method.movedToSuperClass' => INVOKER,
 'java.annotation.removed' => IGNORABLE_ANNOS,
 'java.annotation.added' => IGNORABLE_ANNOS,
 'java.class.nonPublicPartOfAPI' => NOOP,
 'java.element.nowDeprecated' => NOOP,
 'java.annotation.attributeAdded' => NOOP
}

SUBSTITUTIONS = {
  'org.jruby.runtime.builtin.IRubyObject' => 'IRubyObject',
  'org.jruby.runtime.ThreadContext' => 'ThreadContext',
  'org.jruby.Ruby' => 'Ruby',   # This is doing a lot of heavy lifting
  'org.jruby.RubySymbol' => 'RubySymbol',
  'java.lang.String' => 'String',
  'org.jruby.runtime.Block' => 'Block',
  'org.jruby.parser.StaticScope' => 'StaticScope',
}

class Action
  attr_reader :old_name, :new_name, :type, :desc, :categories
  
  def initialize(old_name, new_name, type, desc, categories)
    @old_name, @new_name, @type, @desc, @categories = old_name, new_name, type, desc, categories
  end

  def annotation_change?
    type.start_with?("java.annotation")
  end

  def breaking?
    categories.any? do |kind, effect|
      effect == "BREAKING" || effect == "POTENTIALLY_BREAKING"
    end
  end

  def sanitize(str)
    SUBSTITUTIONS.each do |pattern, replace|
      str.gsub!(pattern, replace)
    end
    str
  end

  def acceptable_key
    if removed?
      "#{old_name}|#{type}"
    else
      "#{new_name}|#{type}"
    end
  end

  def removed?
    !!type.match(/java\.(method|class|field)\.removed/)
  end

  def added?
    !!type.match(/java\.(method|class|field)\.[^.]*[Aa]dded/)
  end

  def to_s
    str = "type: #{type} - #{desc}\n"
    if !added?
      str += "\n    #{sanitize(old_name)}"
    end
    str += " #{!added? ? "->" : "  "}\n    #{sanitize(new_name)}" if !removed? && !annotation_change?
    str += "\n    #{categories_to_s}"
    str
  end

  private def categories_to_s
    categories.map {|kind, effect| "#{kind.downcase}: #{effect.downcase}"}.join(", ")
  end
end

puts "Filtering for '#{filter}'" if filter
count = 0
processed_count = 0
REPORT[:report].each do |item|
  count += 1
  processed = false
  item[:diff].each do |type, desc, categories|
    next if filter && type != filter
    action = Action.new(item[:old], item[:new], type, desc, categories)
    next if old_name_search && !action.old_name.match?(old_name_search)
    next if new_name_search && !action.new_name.match?(new_name_search)
    fn = SKIPS[type] || FALSE
    next if fn.call(action) || ACCEPTABLE[action.acceptable_key]
    unless action.breaking?
#      puts "'SKIPPING #{action}"
      next
    end
    
    if version
      puts "'#{action.acceptable_key}' => '#{version}',"
    else
      puts action
      puts
    end

    processed = true
  end
  processed_count += 1 if processed
end

puts "Finished looking at #{count} items"
puts "Finished processing at #{processed_count} items"
