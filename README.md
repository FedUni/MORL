# MORL-Glue

MORL-Glue is a Multi-Objective Reinforcement Learning framework, adapted from [RL-Glue][rl-glue] by Brian Tanner and Adam White.

MORL-Glue logically separates the three main components of a reinforcement learning system, allowing the environment, experiment and agent to be implemented in different programming languages, on different systems.

Communication between the three is orchestrated by the MORL-Glue server, which is available for Linux and Windows systems. A Windows x64 build is available under *Releases*.

Most of the documentation for RL-Glue still applies. A good starting point is the RL-Glue overview, available under the `docs` directory.

## Changes from RL-Glue
The main difference between RL-Glue and MORL-Glue is that MORL supports *multiobjective reinforcement learning*. That is, where RL-Glue is built around the assumption that agents should learn to optimize for a single  scalar-valued reward, MORL-Glue allows representing vector-valued rewards. Agents can then combine these rewards into a single scalar value and apply standard single-objective RL techniques, or use explicitly multiobjective approaches.


### Included MORL Agents

Several example MORL agents are included in the source distribution:

- `SkeletonAgent` - A starting-point agent that selects a random action
- `UserControlledAgent` - An example of an agent that uses user input to select an action
- `TLO_Agent` - a Q-Learning agent that uses Thresholded Lexicographic Ordering to determing how to optimize objectives.
  

### Included MORL Environments

MORL-Glue includes a large number of standard RL and MORL benchmark problems:

- `DeepSeaTreasureEnv` - The Deep Sea Treasure episodic multiobjective benchmark problem, with two objectives - a positive treasure and a negative time objective. The rewards in Deep Sea Treasure are such that the pareto front is concave. See [Vamplew et al (2011)][vamplew2011] for details.
- `DeepSeaTreasureMixed` - A variant of the above Deep Sea Treasure environment with a convex pareto front.
- `GeneralisedDeepSeaTreasureEnv` - A generalised version of the Deep Sea Treasure benchmark, with configurable environment size, stochasticity and pareto shape.
- `LinkedRings` and `NonRecurrentRings` - The Linked Rings benchmarks as described in [Vamplew et al (2017a)][vamplew2017a]
- `MOMountainCarDiscretised` - A multiobjective version of the Mountain Car environment, as described in [Vamplew et al (2011)][vamplew2011].
- `SpaceExploration` - The Space Exploration benchmark as described in [Vamplew et al (2017b)][vamplew2017b]
- `BonusWorld` - An episodic 2D grid-world with 3 objectives - 2 are terminal-only, and the other is the time objective. As described in [Vamplew et al (2017b)][vamplew2017b]
- `ResourceGathering` - The resource collection problem from [Barrett & Narayanan (2008)][barrett2008]

### Experiments

- `SkeletonExperiment` - A starting-point experiment for MORL-Glue.
- `DemoExperiment` - Designed for use in demonstrating the capabilities of MORL-Glue, in particular using the Generalised Deep Sea Treasure environment.

## Installation

### Linux

MORL-Glue, like RL-Glue, uses the GNU autotools build framework.

From the `morlglue` directory, run

```
configure && make && make install
```

Depending on your distribution, you may need to `sudo make install`.

You should now be able to run `morlglue` from the command prompt.

### Windows

The MORL-Glue server is available as a statically-linked stand-alone 64-bit executable for Windows (Vista or higher). 
Simply extract the executable into a directory and run it.

Visual Studio 2015 solutions are available if you wish to build your own version.

[rl-glue]: http://glue.rl-community.org/wiki/Main_Page
[aligned-ai]: https://www.researchgate.net/publication/319020316_Human-Aligned_Artificial_Intelligence_is_a_Multiobjective_Problem
[vamplew2011]: https://www.researchgate.net/publication/220343783_Empirical_evaluation_methods_for_multiobjective_reinforcement_learning_algorithms
[vamplew2017a]: https://doi.org/10.1007/s10994-010-5232-5
[vamplew2017b]: https://doi.org/10.1016/j.neucom.2016.09.141 "Softmax exploration strategies for multiobjective reinforcement learning"
[barrett2008]: https://doi.org/10.1145/1390156.1390162 "Learning all optimal policies with multiple criteria"