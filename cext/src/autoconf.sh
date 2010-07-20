#!/bin/sh
rm configure
autoconf
autoheader
sh configure $@

