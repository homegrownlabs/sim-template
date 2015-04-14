# My Simulant Suite

## Running the Sample Service

The only pre-requisite for running the [sample service](https://github.com/homegrownlabs/sim-sample-service) is a working
installation of [Docker](https://docs.docker.com/).

After that, just run `run-service.sh` to invoke the proper Docker incantations.

```sh
$ ./run-service.sh
```

If you're on Linux, just add `sudo` in front of that command.

## Interacting with the Simulation

Before attempting to interact with your simulation, you will need to start the
target system, and (optionally) start a Datomic transactor.

For the generate sample service, you can launch an instance with the command:

```sh
$ ./run-service.sh
```

### At the REPL

To interact with the simulation codebase, open your editor to
`src/simulation/repl.clj` and evaluate regions or the entire file.

This file runs Datomic in in-memory mode, so running a Datomic transactor is
not necessary.

### At the Command-Line

This project comes bundled with an active installation of
[boot-sim](https://github.com/homegrownlabs/boot-sim), which provides a number
of command-line tasks for interacting with a simulation.

To start a Datomic transactor for persistent sims, [get
Datomic](http://www.datomic.com/get-datomic.html), then launch a transactor
from the downloaded artifacts. For a simple, dev transactor, use this command:

```sh
# In datomic-free-X.Y.ZZZZ/
$ bin/transactor config/samples/free-transactor-template.properties
Launching with Java options -server -Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=50
Starting datomic:free://localhost:4334/<DB-NAME>, storing data in: data ...
System started datomic:free://localhost:4334/<DB-NAME>, storing data in: data
```

## License

Copyright © [year] [name]
