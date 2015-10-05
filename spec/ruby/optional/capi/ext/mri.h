#ifndef RUBYSPEC_CAPI_MRI_H
#define RUBYSPEC_CAPI_MRI_H

/* #undef any HAVE_ defines that MRI does not have. */
#undef HAVE_RB_STR_PTR
#undef HAVE_RB_STR_PTR_READONLY

#undef HAVE_RB_PROTECT_INSPECT

/* RubySpec assumes following are public API */
#ifndef rb_str_len
int rb_str_len(VALUE);
#endif

#endif
