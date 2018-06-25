# frozen_string_literal: true
#
# .rb part for JRuby's native Set impl (taken from set.rb)

module SetLike
  def flatten_merge(set, seen = Set.new) # :nodoc:
    set.each { |e|
      if e.is_a?(Set)
        if seen.include?(e_id = e.object_id)
          raise ArgumentError, "tried to flatten recursive Set"
        end

        seen.add(e_id)
        flatten_merge(e, seen)
        seen.delete(e_id)
      else
        add(e)
      end
    }

    self
  end
  protected :flatten_merge

  # Returns true if the set is a superset of the given set.
  def superset?(set)
    case
      when set.instance_of?(self.class) && @hash.respond_to?(:>=)
        @hash >= set.instance_variable_get(:@hash)
      when set.is_a?(Set)
        size >= set.size && set.all? { |o| include?(o) }
      else
        raise ArgumentError, "value must be a set"
    end
  end
  alias >= superset?

  # Returns true if the set is a proper superset of the given set.
  def proper_superset?(set)
    case
      when set.instance_of?(self.class) && @hash.respond_to?(:>)
        @hash > set.instance_variable_get(:@hash)
      when set.is_a?(Set)
        size > set.size && set.all? { |o| include?(o) }
      else
        raise ArgumentError, "value must be a set"
    end
  end
  alias > proper_superset?

  # Returns true if the set is a subset of the given set.
  def subset?(set)
    case
      when set.instance_of?(self.class) && @hash.respond_to?(:<=)
        @hash <= set.instance_variable_get(:@hash)
      when set.is_a?(Set)
        size <= set.size && all? { |o| set.include?(o) }
      else
        raise ArgumentError, "value must be a set"
    end
  end
  alias <= subset?

  # Returns true if the set is a proper subset of the given set.
  def proper_subset?(set)
    case
      when set.instance_of?(self.class) && @hash.respond_to?(:<)
        @hash < set.instance_variable_get(:@hash)
      when set.is_a?(Set)
        size < set.size && all? { |o| set.include?(o) }
      else
        raise ArgumentError, "value must be a set"
    end
  end
  alias < proper_subset?

  # Returns true if the set and the given set have at least one
  # element in common.
  #
  #   Set[1, 2, 3].intersect? Set[4, 5]   #=> false
  #   Set[1, 2, 3].intersect? Set[3, 4]   #=> true
  def intersect?(set)
    set.is_a?(Set) or raise ArgumentError, "value must be a set"
    if size < set.size
      any? { |o| set.include?(o) }
    else
      set.any? { |o| include?(o) }
    end
  end

  # Returns true if two sets are equal.  The equality of each couple
  # of elements is defined according to Object#eql?.
  #
  #     Set[1, 2] == Set[2, 1]                       #=> true
  #     Set[1, 3, 5] == Set[1, 5]                    #=> false
  #     Set['a', 'b', 'c'] == Set['a', 'c', 'b']     #=> true
  #     Set['a', 'b', 'c'] == ['a', 'c', 'b']        #=> false
  def ==(other)
    if self.equal?(other)
      true
    elsif other.instance_of?(self.class)
      @hash == other.instance_variable_get(:@hash)
    elsif other.is_a?(Set) && self.size == other.size
      other.all? { |o| @hash.include?(o) }
    else
      false
    end
  end
end

class Set
  include SetLike

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