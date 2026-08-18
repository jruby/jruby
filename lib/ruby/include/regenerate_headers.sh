#!/usr/bin/env bash

svn export --force https://github.com/jruby/ruby/trunk/include .

header_symbols() {
    grep \
        --recursive \
        --extended-regexp \
        --only-matching \
        --ignore-case \
        --no-filename \
        $1 **/*.h | sort --unique
}

all_symbols=$(
    header_symbols 'rb_\w+'
)

symbols_with_parameters=$(
    header_symbols 'rb_\w+\s*\(' | grep -Eoi 'rb_\w+'
)

symbols_without_parameters=$(
    comm -23 <(echo "$all_symbols") <(echo "$symbols_with_parameters")
)

symbol_with_parameter_errors=$(
    echo "$symbols_with_parameters" |
        awk '{print "#define " $0 "(...) compile_time_ERROR(\"JRuby does not support Ruby C Extensions\");"}'
)

symbol_without_parameter_errors=$(
    echo "$symbols_without_parameters" |
        awk '{print "#define " $0 " compile_time_ERROR(\"JRuby does not support Ruby C Extensions\");"}'
)

echo "#pragma once

#include <assert.h>

#if defined(static_assert) || __cplusplus >= 201103L
#define compile_time_ERROR(MSG) static_assert(0, MSG)
#else
 static int compile_time_ERROR(void) { return (99); }
#endif

$symbol_with_parameter_errors
$symbol_without_parameter_errors" > ruby.h

find ruby -type f -name '*.h' -print0 | while IFS= read -r -d $'\0' file
do
    # Replace every directory name in $file with ".."
    # and then replace the file name with "ruby.h"
    # e.g. "some/path/some_file.h" becomes "../../ruby.h"
    main_header_path=$(
        echo $file | sed -e 's/[^\/]\+\//..\//g' -e 's/\/[^\/]\+$/\/ruby.h/g'
    )

    echo "#include \"$main_header_path\"" > $file
done
