AC_INIT(src/RL_glue.c)
AC_CONFIG_AUX_DIR(config)
AM_INIT_AUTOMAKE(rlglue,3.04)
AC_PROG_CC
AC_PROG_INSTALL
AC_PROG_LIBTOOL
AM_PROG_LIBTOOL

AM_MAINTAINER_MODE

# Host filesystem options
case "${host}" in
  *mingw*)
    AC_DEFINE(HAVE_MINGW, 1,
      [Define if we should use the Windows socket headers. ])
    ;;
esac

AC_ARG_ENABLE(mingw,
[  --enable-mingw    Add libs to cross compile for mingw],
[case "${enableval}" in
  yes) mingw=true ;;
  no)  mingw=false ;;
  *) AC_MSG_ERROR(bad value ${enableval} for --enable-mingw) ;;
esac],[mingw=false])

AM_CONDITIONAL(MINGW, test x$mingw = xtrue)

AC_CHECK_HEADERS([sys/time.h])
AC_CHECK_FUNCS([gettimeofday])

AC_CONFIG_HEADERS(config/config.h)

AC_OUTPUT(Makefile src/Makefile tests/Makefile)
