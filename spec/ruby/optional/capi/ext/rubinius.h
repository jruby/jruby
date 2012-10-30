#ifndef RUBYSPEC_CAPI_RUBINIUS_H
#define RUBYSPEC_CAPI_RUBINIUS_H

/* #undef any HAVE_ defines that Rubinius does not have. */
#undef HAVE_RB_DEFINE_HOOKED_VARIABLE
#undef HAVE_RB_DEFINE_VARIABLE

#ifdef RUBY_VERSION_IS_1_8_EX_1_9
#undef HAVE_RB_EMATHDOMAINERROR
#undef HAVE_RB_PATH_TO_CLASS
#endif

#endif
