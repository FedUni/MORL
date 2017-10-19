AC_INIT([morlglue], [3.0.0], [c.foale@federation.edu.au])
AM_INIT_AUTOMAKE([foreign -Wall -Werror])
AC_PROG_CC
AC_PROG_INSTALL
AM_PROG_AR

AC_CONFIG_MACRO_DIR([m4])

LT_INIT


# AM_MAINTAINER_MODE

AC_CHECK_HEADERS([sys/time.h])
AC_CHECK_FUNCS([gettimeofday])

AC_CONFIG_HEADERS([config.h])
AC_CONFIG_FILES([
  Makefile
  src/Makefile
])

AC_OUTPUT
