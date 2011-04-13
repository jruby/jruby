#if !defined(__WIN32__) && !defined(__MINGW32__)
#   include <stdarg.h>
#   include <errno.h>
#endif

#define INIT_SZ 128

#ifndef ENOMEM
#	define ENOMEM 12
#endif

#

#ifndef VA_COPY
#  ifdef HAVE_VA_COPY
#    define VA_COPY(dest, src) va_copy(dest, src)
#  else
#    ifdef HAVE___VA_COPY
#      define VA_COPY(dest, src) __va_copy(dest, src)
#    else
#      define VA_COPY(dest, src) (dest) = (src)
#    endif
#  endif
#endif


static inline int vasprintf(char **str, const char *fmt, va_list ap)
{
        int ret = -1;
        va_list ap2;
        char *string, *newstr;
        size_t len;

        VA_COPY(ap2, ap);
        if ((string = (char*)malloc(INIT_SZ)) == NULL)
                goto fail;

        ret = vsnprintf(string, INIT_SZ, fmt, ap2);
        if (ret >= 0 && ret < INIT_SZ) { /* succeeded with initial alloc */
                *str = string;
        } else if (ret == INT_MAX || ret < 0) { /* Bad length */
                goto fail;
        } else {        /* bigger than initial, realloc allowing for nul */
                len = (size_t)ret + 1;
                if ((newstr = (char*)realloc(string, len)) == NULL) {
                        free(string);
                        goto fail;
                } else {
                        va_end(ap2);
                        VA_COPY(ap2, ap);
                        ret = vsnprintf(newstr, len, fmt, ap2);
                        if (ret >= 0 && (size_t)ret < len) {
                                *str = newstr;
                        } else { /* failed with realloc'ed string, give up */
                                free(newstr);
                                goto fail;
                        }
                }
        }
        va_end(ap2);
        return (ret);

fail:
        *str = NULL;
        errno = ENOMEM;
        va_end(ap2);
        return (-1);
}
static inline int asprintf(char **str, const char *fmt, ...)
{
        va_list ap;
        int ret;

        *str = NULL;
        va_start(ap, fmt);
        ret = vasprintf(str, fmt, ap);
        va_end(ap);

        return ret;
}
