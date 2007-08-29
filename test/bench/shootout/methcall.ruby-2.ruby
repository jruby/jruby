#!/usr/bin/tclsh
# $Id: methcall.ruby-2.ruby,v 1.1 2005-04-16 15:11:10 igouy-guest Exp $

# The Great Computer Language Shootout
# http://shootout.alioth.debian.org/
#
# Contributed by Hemang Lavana
# This program is based on object.tcl

package require Itcl

::itcl::class Toggle {
    variable _state
    constructor {start_state} {set _state $start_state}
    public method value {} { return [expr {$_state ? true : false}]}
    public method activate {} { 
        set _state [expr {!$_state}] 
        return $this
    }
}

::itcl::class NthToggle {
    inherit Toggle
    variable _counter
    variable _count_max

    constructor {start_state max_counter} {Toggle::constructor $start_state} {
        set _counter 0
        set _count_max $max_counter
    }
    method activate {} {
        incr _counter 1
        if {$_counter >= $_count_max} {
            set _state [expr {!$_state}]
            set _counter 0
        }
        return $this
    }
}

proc main {n} {
    Toggle toggle TRUE
    for {set i 0} {$i<$n} {incr i} {
        set value [[toggle activate] value]
    }
    puts $value

    NthToggle ntoggle TRUE 3
    for {set i 0} {$i<$n} {incr i} {
        set value [[ntoggle activate] value]
    }
    puts $value
}
main [lindex $argv 0]
