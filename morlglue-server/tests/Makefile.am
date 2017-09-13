check_PROGRAMS= test_sanity test_1 test_message test_rl_episode test_init_cleanup test_empty test_speed 
#test_cparser
TESTS = $(check_PROGRAMS)

INCLUDES = -I$(top_srcdir)/src
LDADD = ../src/librlglue.la ../src/librlutils.la

test_sanity_SOURCES = test_1_environment.c	test_1_agent.c test_sanity_experiment.c useful_functions.c useful_functions.h
test_sanity_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

test_1_SOURCES = test_1_environment.c	test_1_agent.c test_1_experiment.c useful_functions.c useful_functions.h
test_1_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

test_message_SOURCES = test_message_agent.c	test_message_environment.c test_message_experiment.c useful_functions.c useful_functions.h
test_message_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

test_rl_episode_SOURCES = test_1_environment.c	test_1_agent.c test_rl_episode_experiment.c useful_functions.c useful_functions.h
test_rl_episode_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

test_init_cleanup_SOURCES = test_1_environment.c  test_1_agent.c test_init_cleanup_experiment.c useful_functions.c useful_functions.h
test_init_cleanup_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

test_empty_SOURCES = test_empty_environment.c	test_empty_agent.c test_empty_experiment.c useful_functions.c useful_functions.h
test_empty_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

test_speed_SOURCES = test_speed_environment.c	test_1_agent.c test_speed_experiment.c useful_functions.c useful_functions.h
test_speed_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99

#test_cparser_SOURCES = test_cparser.c
#test_cparser_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99



#IF this is uncommented, then we get errors unless check_Programs is commented
#noinst_PROGRAMS = $(TESTS)
