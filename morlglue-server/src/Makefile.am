
GLUE_RELEASE=3:0:0
GLUE_VERSION=3:0:0

EXTRA_DIST=morl_glue.1
man_MANS = morl_glue.1

EXTRA_C_FLAGS=-O2 -g
lib_LTLIBRARIES = librlutils.la librlglue.la librlgluenetdev.la

#librlutils is just the convenience methods for manipulating the RL structures
#Many people will want to use this library in their own code
librlutils_la_SOURCES = RLStruct_util.c TaskSpec_Parser.c rlglue/utils/C/RLStruct_util.h  rlglue/utils/C/TaskSpec_Parser.h
librlutils_la_LDFLAGS= -version-info $(GLUE_VERSION) -release $(GLUE_RELEASE) -no-missing
librlutils_la_includedir=$(includedir)/rlglue/utils/C
librlutils_la_include_HEADERS = rlglue/utils/C/RLStruct_util.h rlglue/utils/C/TaskSpec_Parser.h
librlutils_la_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99 -g

#librlglue is for direct-compile c/c++ experiments
#It does not use any of the network code
librlglue_la_CFLAGS = -Wall -pedantic -std=c99 $(EXTRA_C_FLAGS) -DDEBUG_GLUE_NETWORK 
librlglue_la_SOURCES = RL_direct_experiment.c RL_direct_environment.c RL_direct_agent.c RL_glue_debug.c RL_glue.c rlglue/RL_common.h rlglue/Agent_common.h rlglue/Environment_common.h rlglue/RL_glue.h
librlglue_la_LDFLAGS= -version-info $(GLUE_VERSION) -release $(GLUE_RELEASE)
librlglue_la_includedir=$(includedir)/rlglue
librlglue_la_include_HEADERS = rlglue/RL_common.h rlglue/Agent_common.h rlglue/Environment_common.h rlglue/RL_glue.h 

NETDEV_RELEASE=0:0:0
NETDEV_VERSION=0:0:0

#librlgluenetdev is the network libraries for RL-Glue.  You could conceivably write your own, but we anticipate
#that the C/C++ Codecs and their derivatives will use this library
librlgluenetdev_la_CFLAGS = -Wall -pedantic -std=c99 $(EXTRA_C_FLAGS)
librlgluenetdev_la_SOURCES = RL_network.c RL_glue_debug.c rlglue/RL_common.h rlglue/network/RL_network.h 
librlgluenetdev_la_LDFLAGS= -version-info $(NETDEV_RELEASE) -release $(NETDEV_VERSION)
librlgluenetdev_la_LIBADD= librlutils.la
librlgluenetdev_la_includedir=$(includedir)/rlglue/network
librlgluenetdev_la_include_HEADERS = rlglue/network/RL_network.h

bin_PROGRAMS = morl_glue

# morl_glue is the executable RL-Glue server that manages the communications between codecs for 
# environments, agents, and experiments
morl_glue_SOURCES = RL_network_experiment.c RL_server_environment.c RL_network_agent.c RL_network_glue.c RL_server_experiment.c RL_glue.c RL_network_environment.c RL_server_agent.c RL_network_glue.h rlglue/Agent_common.h rlglue/Environment_common.h rlglue/RL_common.h rlglue/RL_glue.h
#This is basically a no-op suggested by: http://www.gnu.org/software/libtool/manual/automake/Libtool-Issues.html to solve a problem where because both targets build rlglue.c
morl_glue_CFLAGS = $(AM_CFLAGS) -Wall -pedantic -std=c99 $(EXTRA_C_FLAGS)
#Taking this out because GDB refuses to look it up when debugging rl_glue
morl_glue_LDADD   = librlutils.la librlgluenetdev.la
