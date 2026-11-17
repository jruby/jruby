# frozen_string_literal: true
#
# .rb part for JRuby's native Set impl (taken from set.rb)

class Set

  def pretty_print(pp)  # :nodoc:
    pp.text sprintf('#<%s: {', self.class.name)
    pp.nest(1) {
      pp.seplist(self) { |o|
        pp.pp o
      }
    }
    pp.text '}>'
  end

  def pretty_print_cycle(pp)    # :nodoc:
    pp.text sprintf('#<%s: {%s}>', self.class.name, empty? ? '' : '...')
  end

end