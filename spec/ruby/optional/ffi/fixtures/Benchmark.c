/*
 * Copyright (c) 2007 Wayne Meissner. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the project nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include <sys/types.h>
#include <stdint.h>

void returnVoid() {

}

void returnVoidI(int arg) {

}
int returnInt() {
    return 0;
}

int returnIntI(int arg) {
    return arg;
}

typedef int8_t s8;
typedef uint8_t u8;
typedef int16_t s16;
typedef uint16_t u16;
typedef int32_t s32;
typedef uint32_t u32;
typedef int64_t s64;
typedef uint64_t u64;
typedef float f32;
typedef double f64;
typedef void v;
typedef char* S;
typedef void* P;

#define B6(R, T1, T2, T3, T4, T5, T6) R bench_##T1##T2##T3##T4##T5##T6##_##R(T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6) {}
#define B5(R, T1, T2, T3, T4, T5) R bench_##T1##T2##T3##T4##T5##_##R(T1 a1, T2 a2, T3 a3, T4 a4, T5 a5) {}
#define B4(R, T1, T2, T3, T4) R bench_##T1##T2##T3##T4##_##R(T1 a1, T2 a2, T3 a3, T4 a4) {}
#define B3(R, T1, T2, T3) R bench_##T1##T2##T3##_##R(T1 a1, T2 a2, T3 a3) {}
#define B2(R, T1, T2) R bench_##T1##T2##_##R(T1 a1, T2 a2) {}
#define B1(R, T1) R bench_##T1##_##R(T1 a1) {}
#define BrV(T) B1(v, T); B2(v, T, T); B3(v, T, T, T); B4(v, T, T, T, T); B5(v, T, T, T, T, T); B6(v, T, T, T, T, T, T);
BrV(u32);
BrV(s32);
BrV(s64);
BrV(u64);
BrV(f32);
BrV(f64);
BrV(S);
BrV(P);
