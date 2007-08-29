#########################################
#     The Computer Language Shootout    #
#   http://shootout.alioth.debian.org/  #
#                                       #
#      Contributed by Jesse Millikan    #
#    Based on version by Gordon Innes   #
#########################################

require 'thread'

creature_meetings = Queue.new
meeting_point = Mutex.new
wait_signal = ConditionVariable.new
meetings_left = ARGV[0].to_i
waiting_colour, incoming_colour = nil, nil

# Each chameneo is represented here by a thread
# and its colour variable, rather than explicitly
# by an object
#
# This is all packed into one place for speed and
# clarity (It's clear to *me* :)
[:blue, :red, :yellow, :blue].each { |colour|
  Thread.new {
    met = 0
    while true
      # The form meeting_point.synchronize { } is slow
      meeting_point.lock

      if meetings_left <= 0
        meeting_point.unlock
	# colour = :faded
	break 
      end

      # Both threads emerge with variable other_colour set
      if waiting_colour
        other_colour = waiting_colour
        incoming_colour = colour
        wait_signal.signal
        meetings_left-=1
        waiting_colour = nil
      else
        waiting_colour = colour
        wait_signal.wait(meeting_point)
        other_colour = incoming_colour
      end
      meeting_point.unlock

      met += 1

      # Take the complement colour
      colour = 
        case other_colour
          when :blue
           colour == :red ? :yellow : :red
          when :red
           colour == :blue ? :yellow : :blue
          when :yellow
           colour == :blue ? :red : :blue
        end
    end

    # Leave the total on the queue for the main thread
    creature_meetings.push(met)
  }
}

total = 0
4.times { total += creature_meetings.pop }
puts total
