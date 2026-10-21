/*
 * Copyright (c) 2007 Wayne Meissner. All rights reserved.
 *
 * For licensing, see LICENSE.SPECS
 */

#include <stdio.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>

typedef char s8;
typedef short s16;
typedef int s32;
typedef long long s64;
typedef float f32;
typedef double f64;

typedef union union_test {
    char b;
    short s;
    int i;
    long long j;
    long l;
    float f;
    double d;
    s8 a[10];
} union_test_t;

#define T(uni, x, type) \
  type uni##_ptr_align_##type(uni* u) { return u->x; } \
  uni* uni##_ptr_make_union_with_##type(type value) { static uni u; u.x = value; return &u; } \
  type uni##_val_align_##type(uni u) { return u.x; } \
  uni uni##_val_make_union_with_##type(type value) { static uni u; u.x = value; return u; }

T(union_test_t, b, s8);
T(union_test_t, s, s16);
T(union_test_t, i, s32);
T(union_test_t, j, s64);
T(union_test_t, f, f32);
T(union_test_t, d, f64);
T(union_test_t, l, long);

unsigned int union_size() { return sizeof(union_test_t); }

typedef union union_small_test {
    char c;
    short s;
} union_small_test_t;

T(union_small_test_t, c, s8);
T(union_small_test_t, s, s16);

unsigned int union_small_size() { return sizeof(union_small_test_t); }

typedef union union_mixed_test {
    float f;
    char c;
} union_mixed_test_t;

T(union_mixed_test_t, f, f32);
T(union_mixed_test_t, c, s8);

unsigned int union_mixed_size() { return sizeof(union_mixed_test_t); }

typedef union union_float_test {
    float f;
    double d;
} union_float_test_t;

T(union_float_test_t, f, f32);
T(union_float_test_t, d, f64);

unsigned int union_float_size() { return sizeof(union_float_test_t); }


/*
 * Union by-value tests.
 *
 * On ARM64, unions composed entirely of doubles are Homogeneous Floating-point
 * Aggregates (HFA) and must be passed/returned in floating-point registers.
 * If libffi describes the union with integer filler types instead of double,
 * values end up in the wrong registers and come back as garbage.
 */
typedef union {
    double v[4];
    struct { double x, y, z, t; } coord;
} union_double_t;

union_double_t union_double_coord(double x, double y, double z, double t) {
    union_double_t u;
    u.coord.x = x;
    u.coord.y = y;
    u.coord.z = z;
    u.coord.t = t;
    return u;
}

double union_double_get_x(union_double_t u) { return u.coord.x; }
double union_double_get_y(union_double_t u) { return u.coord.y; }
double union_double_get_z(union_double_t u) { return u.coord.z; }
double union_double_get_t(union_double_t u) { return u.coord.t; }

union_double_t union_double_add(union_double_t a, union_double_t b) {
    union_double_t r;
    r.coord.x = a.coord.x + b.coord.x;
    r.coord.y = a.coord.y + b.coord.y;
    r.coord.z = a.coord.z + b.coord.z;
    r.coord.t = a.coord.t + b.coord.t;
    return r;
}
