# Port of deltablue.py, as documented below, to Ruby.
# Stefan Marr, 2014-04-28
# 
# Was: deltablue.py
# =================
# 
# Ported for the PyPy project.
# Contributed by Daniel Lindsley
# 
# This implementation of the DeltaBlue benchmark was directly ported
# from the `V8's source code`_, which was in turn derived
# from the Smalltalk implementation by John Maloney and Mario
# Wolczko. The original Javascript implementation was licensed under the GPL.
# 
# It's been updated in places to be more idiomatic to Python (for loops over
# collections, a couple magic methods, ``OrderedCollection`` being a list & things
# altering those collections changed to the builtin methods) but largely retains
# the layout & logic from the original. (Ugh.)
# 
# .. _`V8's source code`: (http://code.google.com/p/v8/source/browse/branches/bleeding_edge/benchmarks/deltablue.js)


# HOORAY FOR GLOBALS... Oh wait.
# In spirit of the original, we'll keep it, but ugh.
$planner = nil

class Strength
    attr_accessor :strength

    def initialize(strength, name)
      @strength = strength
      @name     = name
    end

    REQUIRED         = self.new(0, "required")
    STRONG_PREFERRED = self.new(1, "strongPreferred")
    PREFERRED        = self.new(2, "preferred")
    STRONG_DEFAULT   = self.new(3, "strongDefault")
    NORMAL           = self.new(4, "normal")
    WEAK_DEFAULT     = self.new(5, "weakDefault")
    WEAKEST          = self.new(6, "weakest")

    def self.stronger(s1, s2)
        s1.strength < s2.strength
    end

    def self.weaker(s1, s2)
        s1.strength > s2.strength
    end

    def self.weakest_of(s1, s2)
        if self.weaker(s1, s2)
            return s1
        end
        return s2
    end

    def self.strongest(s1, s2)
        if self.stronger(s1, s2)
            return s1
        end
        return s2
    end

    def next_weaker
      case @strength
        when 0
          WEAKEST
        when 1
          WEAK_DEFAULT
        when 2
          NORMAL
        when 3
          STRONG_DEFAULT
        when 4
          PREFERRED
        when 5
          # TODO: This looks like a bug in the original code. Shouldn't this be
          #       ``STRONG_PREFERRED? Keeping for porting sake...
          REQUIRED
      end
    end
end


class Constraint
    attr_accessor :strength

    def initialize(strength)
        @strength = strength
    end

    def add_constraint
        add_to_graph
        $planner.incremental_add(self)
    end

    def satisfy(mark)
        choose_method(mark)

        if not is_satisfied
            if @strength == Strength::REQUIRED
                puts 'Could not satisfy a required constraint!'
            end
            return nil
        end

        mark_inputs(mark)
        out = output
        overridden = out.determined_by

        unless overridden.nil?
            overridden.mark_unsatisfied
        end

        out.determined_by = self

        unless $planner.add_propagate(self, mark)
            puts 'Cycle encountered'
        end

        out.mark = mark
        overridden
    end

    def destroy_constraint
        if is_satisfied
            $planner.incremental_remove(self)
        else
            remove_from_graph
        end
    end

    def is_input
        false
    end
end

class UnaryConstraint < Constraint
    def initialize(v, strength)
        super(strength)
        @my_output = v
        @satisfied = false
        add_constraint
    end

    def add_to_graph
        @my_output.add_constraint(self)
        @satisfied = false
    end

    def choose_method(mark)
        if @my_output.mark != mark and Strength.stronger(@strength, @my_output.walk_strength)
            @satisfied = true
        else
            @satisfied = false
        end
    end

    def is_satisfied
        @satisfied
    end

    def mark_inputs(mark)
        # No-ops.
    end

    def output
        # Ugh. Keeping it for consistency with the original. So much for
        # "we're all adults here"...
        @my_output
    end

    def recalculate
        @my_output.walk_strength = @strength
        @my_output.stay          = !is_input

        if @my_output.stay
            execute
        end
    end

    def mark_unsatisfied
        @satisfied = false
    end

    def inputs_known(mark)
        true
    end

    def remove_from_graph
        unless @my_output.nil?
            @my_output.remove_constraint(self)
            @satisfied = false
        end
    end
end

class StayConstraint < UnaryConstraint
    def execute
        # The methods, THEY DO NOTHING.
    end
end


class EditConstraint < UnaryConstraint
    def is_input
        true
    end

    def execute
        # This constraint also does nothing.
    end
end


class Direction
    # Hooray for things that ought to be structs!
    NONE     = 0
    FORWARD  = 1
    BACKWARD = -1
end


class BinaryConstraint < Constraint
    def initialize(v1, v2, strength)
        super(strength)
        @v1 = v1
        @v2 = v2
        @direction = Direction::NONE
        add_constraint
    end

    def choose_method(mark)
        if @v1.mark == mark
            if @v2.mark != mark and Strength::stronger(@strength, @v2.walk_strength)
                @direction = Direction::FORWARD
            else
                @direction = Direction::BACKWARD
            end
        end

        if @v2.mark == mark
            if @v1.mark != mark and Strength::stronger(@strength, @v1.walk_strength)
                @direction = Direction::BACKWARD
            else
                @direction = Direction::NONE
            end
        end

        if Strength.weaker(@v1.walk_strength, @v2.walk_strength)
            if Strength.stronger(@strength, @v1.walk_strength)
                @direction = Direction::BACKWARD
            else
                @direction = Direction::NONE
            end
        else
            if Strength.stronger(@strength, @v2.walk_strength)
                @direction = Direction::FORWARD
            else
                @direction = Direction::BACKWARD
            end
        end
    end

    def add_to_graph
        @v1.add_constraint(self)
        @v2.add_constraint(self)
        @direction = Direction::NONE
    end

    def is_satisfied
        @direction != Direction::NONE
    end

    def mark_inputs(mark)
        input.mark = mark
    end

    def input
        if @direction == Direction::FORWARD
            return @v1
        end

        @v2
    end

    def output
        if @direction == Direction::FORWARD
            return @v2
        end

        @v1
    end

    def recalculate
        ihn = input
        out = output
        out.walk_strength = Strength.weakest_of(@strength, ihn.walk_strength)
        out.stay = ihn.stay

        if out.stay
            execute
        end
    end

    def mark_unsatisfied
        @direction = Direction::NONE
    end

    def inputs_known(mark)
        i = input
        i.mark == mark or i.stay or i.determined_by.nil?
    end

    def remove_from_graph
        unless @v1.nil?
            @v1.remove_constraint(self)
        end

        unless @v2.nil?
            @v2.remove_constraint(self)
        end

        @direction = Direction::NONE
    end
end

class ScaleConstraint < BinaryConstraint
    def initialize(src, scale, offset, dest, strength)
        @direction = Direction::NONE
        @scale     = scale
        @offset    = offset
        super(src, dest, strength)
    end

    def add_to_graph
        super
        @scale.add_constraint(self)
        @offset.add_constraint(self)
    end

    def remove_from_graph
        super

        unless @scale.nil?
            @scale.remove_constraint(self)
        end

        unless @offset.nil?
            @offset.remove_constraint(self)
        end
    end

    def mark_inputs(mark)
        super
        @scale.mark  = mark
        @offset.mark = mark
    end

    def execute
        if @direction == Direction::FORWARD
            @v2.value = @v1.value * @scale.value + @offset.value
        else
            @v1.value = (@v2.value - @offset.value) / @scale.value
        end
    end

    def recalculate
        ihn = input
        out = output
        out.walk_strength = Strength.weakest_of(@strength, ihn.walk_strength)
        out.stay = (ihn.stay and @scale.stay and @offset.stay)

        if out.stay
            execute
        end
    end
end


class EqualityConstraint < BinaryConstraint
    def execute
        output.value = input.value
    end
end

class Variable
    attr_accessor :mark
    attr_accessor :walk_strength
    attr_accessor :determined_by
    attr_accessor :stay
    attr_accessor :value
    attr_accessor :constraints

    def initialize(name, initial_value = 0)
        @name          = name
        @value         = initial_value
        @constraints   = []
        @determined_by = nil
        @mark          = 0
        @walk_strength = Strength::WEAKEST
        @stay          = true
    end

    def inspect
        # To make debugging this beast from pdb easier...
        return "<Variable: #{@name} - #{value}>"
    end

    def add_constraint(constraint)
        @constraints << constraint
    end

    def remove_constraint(constraint)
        @constraints.delete(constraint)

        if @determined_by == constraint
            @determined_by = nil
        end
    end
end


class Planner
    def initialize
        @current_mark = 0
    end

    def incremental_add(constraint)
        mark = new_mark
        overridden = constraint.satisfy(mark)

        until overridden.nil?
            overridden = overridden.satisfy(mark)
        end
    end

    def incremental_remove(constraint)
        out = constraint.output
        constraint.mark_unsatisfied
        constraint.remove_from_graph
        unsatisfied = remove_propagate_from(out)
        strength    = Strength::REQUIRED

        loop do
            for u in unsatisfied
                if u.strength == strength
                    incremental_add(u)
                end

                strength = strength.next_weaker
            end
            break if strength != Strength::WEAKEST
        end
    end

    def new_mark
        @current_mark += 1
        @current_mark
    end

    def make_plan(sources)
        mark = new_mark
        plan = Plan.new()
        todo = sources

        until todo.empty?
            c = todo.pop

            if c.output.mark != mark and c.inputs_known(mark)
                plan.add_constraint(c)
                c.output.mark = mark
                add_constraints_consuming_to(c.output, todo)
            end
        end

        plan
    end

    def extract_plan_from_constraints(constraints)
        sources = []

        for c in constraints
            if c.is_input and c.is_satisfied
                sources << c
            end
        end

        make_plan(sources)
    end

    def add_propagate(c, mark)
        todo = []
        todo << c

        until todo.empty?
            d = todo.pop

            if d.output.mark == mark
                incremental_remove(c)
                return false
            end

            d.recalculate
            add_constraints_consuming_to(d.output, todo)
        end

        true
    end

    def remove_propagate_from(out)
        out.determined_by = nil
        out.walk_strength = Strength::WEAKEST
        out.stay = true
        unsatisfied = []
        todo = []
        todo << out

        until todo.empty?
            v = todo.pop

            for c in v.constraints
                unless c.is_satisfied
                    unsatisfied << c
                end
            end

            determining = v.determined_by

            for c in v.constraints
                if c != determining and c.is_satisfied
                    c.recalculate
                    todo << c.output
                end
            end
        end

        unsatisfied
    end

    def add_constraints_consuming_to(v, coll)
        determining = v.determined_by
        cc = v.constraints

        for c in cc
            if c != determining and c.is_satisfied
                # I guess we're just updating a reference (``coll``)? Seems
                # inconsistent with the rest of the implementation, where they
                # return the lists...
                coll << c
            end
        end
    end
end


class Plan
    def initialize
        @v = []
    end

    def add_constraint(c)
        @v << c
    end

    def size
        @v.size
    end

    def [](index)
        @v[index]
    end

    def execute
        for c in @v
            c.execute
        end
    end
end


# Main

def chain_test(n)

    # This is the standard DeltaBlue benchmark. A long chain of equality
    # constraints is constructed with a stay constraint on one end. An
    # edit constraint is then added to the opposite end and the time is
    # measured for adding and removing this constraint, and extracting
    # and executing a constraint satisfaction plan. There are two cases.
    # In case 1, the added constraint is stronger than the stay
    # constraint and values must propagate down the entire length of the
    # chain. In case 2, the added constraint is weaker than the stay
    # constraint so it cannot be accomodated. The cost in this case is,
    # of course, very low. Typical situations lie somewhere between these
    # two extremes.

    $planner = Planner.new
    prev, first, last = nil, nil, nil

    # We need to go up to n inclusively.
    for i in 0..n
        name = "v%s" % i
        v = Variable.new(name)

        unless prev.nil?
            EqualityConstraint.new(prev, v, Strength::REQUIRED)
        end

        if i == 0
            first = v
        end

        if i == n
            last = v
        end

        prev = v
    end

    StayConstraint.new(last, Strength::STRONG_DEFAULT)
    edit  = EditConstraint.new(first, Strength::PREFERRED)
    edits = []
    edits << edit
    plan = $planner.extract_plan_from_constraints(edits)

    for i in 0..99
        first.value = i
        plan.execute

        if last.value != i
            puts 'Chain test failed.'
        end
    end
end

def projection_test(n)
    # This test constructs a two sets of variables related to each
    # other by a simple linear transformation (scale and offset). The
    # time is measured to change a variable on either side of the
    # mapping and to change the scale and offset factors.

    $planner = Planner.new
    scale   = Variable.new("scale", 10)
    offset  = Variable.new("offset", 1000)
    src, dst = nil, nil

    dests = []

    for i in 0..(n - 1)
        src = Variable.new("src%s" % i, i)
        dst = Variable.new("dst%s" % i, i)
        dests << dst
        StayConstraint.new(src, Strength::NORMAL)
        ScaleConstraint.new(src, scale, offset, dst, Strength::REQUIRED)
    end

    change(src, 17)

    if dst.value != 1170
        puts 'Projection 1 failed'
    end

    change(dst, 1050)

    if src.value != 5
        puts 'Projection 2 failed'
    end

    change(scale, 5)

    for i in 0..(n - 2)
        if dests[i].value != (i * 5 + 1000)
            puts 'Projection 3 failed'
        end
    end

    change(offset, 2000)

    for i in 0..(n - 2)
        if dests[i].value != (i * 5 + 2000)
            puts 'Projection 4 failed'
        end
    end
end


def change(v, new_value)
    edit = EditConstraint.new(v, Strength::PREFERRED)
    edits = []
    edits << edit

    plan = $planner.extract_plan_from_constraints(edits)

    for i in 0..9
        v.value = new_value
        plan.execute
    end

    edit.destroy_constraint
end


def run(iterations)
    chain_test(iterations)
    projection_test(iterations)
end


def warmup
  1_000.times do
    run(100)
  end
end

def sample
  run(10_000)
end

def name
  return "deltablue"
end

