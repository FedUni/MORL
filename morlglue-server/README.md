MORL-Glue Distribution
=========================
This project is licensed under the Apache 2 license. Check LICENSE-2.0.txt for more info. 

MORL-Glue is a Multi-Objective Reinforcement Learning framework, adapted from [RL-Glue][rl-glue] by Brian Tanner and Adam White.


Documentation
----------------------------

Most of the documentation for RL-Glue still applies. A good starting point is the RL-Glue overview and technical manual, available under the `docs` directory.

- `docs/Glue-Overview.pdf`
- `docs/TechnicalManual.pdf`

Please note that the current documents still refer to the server setup for single-objective RL.

Full MORL-Glue Documentation is still being completed.
 
In the mean time please visit RL-Glue's public home:
http://glue.rl-community.org

Super short introduction to MORL-Glue
----------------------------
Each run with MORL-Glue will use four components: the glue, an environment program, an agent program, and an experiment program.  In the most general case, each of these components is a separate process and they all communicate through the glue using network sockets.

To make this work, you're going to need some sort of C compiler.

This code is tested on Linux and Windows.

----------------------------
Getting Started
----------------------------
This project is built with GNU autotools, so if you have a source tarball, you should just need to do:

	$>./configure
	$>make
	$>sudo make install

If you have cloned the repository using Git, you may need to do the following first:

    $>aclocal
	$>autoconf
	$>automake --add-missing
	
This will install the necessary libraries, binaries, and headers to:
	/usr/local/lib
	/usr/local/bin
	/usr/local/includes/rlglue
	
If you don't have sudo privileges or you would like to install them somewhere else (Ex: your home directory):
	$>./configure --prefix=/path/to/your/home/directory         EG: /home/Users/btanner
	$>make
	$>make install



