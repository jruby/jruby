/*
 * Copyright (C) 2007 Wayne Meissner
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


#define MEMSET(buf, value, size) do { \
    int i; for (i = 0; i < size; ++i) buf[i] = value; \
} while(0)
#define MEMCPY(dst, src, size) do { \
    int i; for (i = 0; i < size; ++i) dst[i] = src[i]; \
} while(0)

#define FILL(JTYPE, CTYPE) \
void fill##JTYPE##Buffer(CTYPE* buf, CTYPE value, int size) { MEMSET(buf, value, size); }

#define COPY(JTYPE, CTYPE) \
void copy##JTYPE##Buffer(CTYPE* dst, CTYPE* src, int size) { MEMCPY(dst, src, size); }

#define FUNC(JTYPE, CTYPE) \
    FILL(JTYPE, CTYPE); \
    COPY(JTYPE, CTYPE)

FUNC(Byte, char);
FUNC(Short, short);
FUNC(Int, int);
FUNC(Long, long long);
FUNC(Float, float);
FUNC(Double, double);

