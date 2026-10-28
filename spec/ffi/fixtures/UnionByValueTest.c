/*
 * For licensing, see LICENSE.SPECS
 */

#include <stdint.h>

/*
 * Union shapes whose by-value calling convention differs between register
 * classes: integer/float merges, homogeneous float aggregates, per-eightbyte
 * classification on SysV x86_64, nesting, oversize unions and callbacks.
 */

typedef union {
    int64_t i;
    double d;
} union_i64_f64_t;

union_i64_f64_t union_i64_f64_make(double d) { union_i64_f64_t u; u.d = d; return u; }
double union_i64_f64_get(union_i64_f64_t u) { return u.d; }
union_i64_f64_t union_i64_f64_make_i(int64_t i) { union_i64_f64_t u; u.i = i; return u; }
int64_t union_i64_f64_get_i(union_i64_f64_t u) { return u.i; }

typedef union {
    float f;
    float v[2];
} union_f32x2_t;

union_f32x2_t union_f32x2_make(float a, float b) { union_f32x2_t u; u.v[0] = a; u.v[1] = b; return u; }
float union_f32x2_get_0(union_f32x2_t u) { return u.v[0]; }
float union_f32x2_get_1(union_f32x2_t u) { return u.v[1]; }

typedef union {
    struct { int32_t tag; double value; } s;
    float f;
} union_tag_f64_t;

union_tag_f64_t union_tag_f64_make(int32_t tag, double value) { union_tag_f64_t u; u.s.tag = tag; u.s.value = value; return u; }
int32_t union_tag_f64_get_tag(union_tag_f64_t u) { return u.s.tag; }
double union_tag_f64_get_value(union_tag_f64_t u) { return u.s.value; }

typedef union {
    double d;
    float v[2];
} union_f64_f32x2_t;

typedef struct {
    double x;
    union_f64_f32x2_t u;
} struct_with_union_t;

struct_with_union_t struct_with_union_make(double x, double d) { struct_with_union_t s; s.x = x; s.u.d = d; return s; }
double struct_with_union_get_x(struct_with_union_t s) { return s.x; }
double struct_with_union_get_d(struct_with_union_t s) { return s.u.d; }

typedef union {
    double v[3];
    int64_t i;
} union_f64x3_i64_t;

union_f64x3_i64_t union_f64x3_i64_make(double a, double b, double c) { union_f64x3_i64_t u; u.v[0] = a; u.v[1] = b; u.v[2] = c; return u; }
double union_f64x3_i64_sum(union_f64x3_i64_t u) { return u.v[0] + u.v[1] + u.v[2]; }

typedef union {
    double v[5];
} union_f64x5_t;

union_f64x5_t union_f64x5_make(double a, double b, double c, double d, double e) {
    union_f64x5_t u; u.v[0] = a; u.v[1] = b; u.v[2] = c; u.v[3] = d; u.v[4] = e; return u;
}
double union_f64x5_sum(union_f64x5_t u) { return u.v[0] + u.v[1] + u.v[2] + u.v[3] + u.v[4]; }

typedef union {
    double v[4];
    struct { double x, y, z, t; } coord;
} union_f64x4_t;

typedef union {
    float f;
    double d;
} union_f32_f64_t;

double union_f64x4_callback(double x, double y, double z, double t, double (*cb)(union_f64x4_t)) {
    union_f64x4_t u; u.coord.x = x; u.coord.y = y; u.coord.z = z; u.coord.t = t;
    return cb(u);
}

double union_f32_f64_callback(double d, double (*cb)(union_f32_f64_t)) {
    union_f32_f64_t u; u.d = d;
    return cb(u);
}

double union_f64x4_callback_ret_t(union_f64x4_t (*cb)(double, double, double, double)) {
    return cb(1.5, 2.5, 3.5, 4.5).coord.t;
}

/*
 * A 4-aligned union whose cells differ in class, placed at offset 4 in a struct:
 * the outer eightbytes are (INTEGER, SSE) on SysV x86_64, so the union's own
 * eightbyte boundaries do not apply.
 */
typedef union {
    float f[2];
    int32_t i;
} union_f32x2_i32_t;

typedef struct {
    int32_t a;
    union_f32x2_i32_t u;
} struct_union_at4_t;

typedef union {
    float f[3];
    int32_t i;
} union_f32x3_i32_t;

typedef struct {
    int32_t a;
    union_f32x3_i32_t u;
} struct_union12_at4_t;

struct_union_at4_t struct_union_at4_make(int32_t a, float f0, float f1) {
    struct_union_at4_t s; s.a = a; s.u.f[0] = f0; s.u.f[1] = f1; return s;
}
int32_t struct_union_at4_get_a(struct_union_at4_t s) { return s.a; }
float struct_union_at4_get_f1(struct_union_at4_t s) { return s.u.f[1]; }

struct_union12_at4_t struct_union12_at4_make(int32_t a, float f0, float f1, float f2) {
    struct_union12_at4_t s; s.a = a; s.u.f[0] = f0; s.u.f[1] = f1; s.u.f[2] = f2; return s;
}
float struct_union12_at4_get_f2(struct_union12_at4_t s) { return s.u.f[2]; }

float struct_union_at4_callback(int32_t a, float f0, float f1, float (*cb)(struct_union_at4_t)) {
    struct_union_at4_t s; s.a = a; s.u.f[0] = f0; s.u.f[1] = f1;
    return cb(s);
}

float struct_union_at4_callback_ret_f1(struct_union_at4_t (*cb)(int32_t, float, float)) {
    return cb(7, 1.5f, 2.5f).u.f[1];
}
