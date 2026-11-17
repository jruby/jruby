/*
 * Copyright (c) 2007 Wayne Meissner. All rights reserved.
 *
 * For licensing, see LICENSE.SPECS
 */

#include <string.h>

int
buffer_equals(const void* s1, const void* s2, size_t size)
{
    return memcmp(s1, s2, size) == 0;
}

void 
string_set(char* s1, const char* s2)
{
    strcpy(s1, s2);
}
void
string_concat(char* dst, const char* src)
{
    strcat(dst, src);
}
void
string_dummy(char* dummy)
{
}
const char*
string_null(void)
{
    return NULL;
}

