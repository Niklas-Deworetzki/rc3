# RC3 - Reversible Computing Compiler Collection

This repository is an archived copy of the official RC3 research project.
The official project has been hosted at https://git.thm.de but is currently no longer reachable.

## Usage

This repository provides two executables: A compiler from Janus to RSSA and a virtual machine to directly execute RSSA programs.

To run the compiler, type `./rc3`. 
To run the RSSA VM with an RSSA frontend, type `./rvm`. 
The shell script will then automatically locate or build a JAR with the latest project version and execute this JAR with the given program parameters.

Without any program parameters, a help page is displayed, providing an overview of relevant commands.
To explicitly show this help page, use `./rc3 --help` or `./rvm --help`.
Alternatively `./rc3 --help --backend=BACKEND` can be used to show a help page for a given backend.

Currently supported are four backends:
 - `tac` this backend generates a non-reversible intermediate code from Janus, a so-called *Three Address Code*.
 This code is then translated to C instructions, in order to create an executable program on regular hardware.
 - `interpreter` this backend directly executes a Janus program by traversing the abstract syntax tree.
 No translation is performed.
 - `rssa` this backend translates a Janus program into a reversible intermediate representation (RSSA).
 RSSA instructions can either be translated to C or executed directly using a virtual machine for RSSA.
 - `rrssa` this backend is essentially the same as `rssa`, but uses a different approach to translate conditions.


When a backend generating RSSA is chosen, optimizations may be applied to the generated code.
The selection of optimizations is similar to GCC, using `-f[OPTIMIZATION NAME]` to enable and `-f-no-[OPTIMIZATION]` to disable them.
These backends also support the execution of the generated code via the RSSA VM with the backend option `--backend-options=vm` or short `-bvm`.

**All backends and the vm provide a command line switch to show the results of a program after execution.**
Using `--print-main` the variables declared within the *main* procedure of a Janus program are printed after execution of the program has finished.
This way, an output can be seen after executing RSSA programs, which otherwise don't have any capabilities of producing an output (except for an abnormal stop).



## Build

Before building this repository, make sure our version of the *CUP Maven Plugin* is installed.
This plugin makes the parser generator available in the build process, which is required to build the frontend of our compiler and virtual machine.


```
cd cup-maven-plugin/cup-maven-plugin ; mvn --quiet install
```

Afterwards the compiler can either forcefully be build using `build.sh` or automatically using either the `rc3` or `rvm` scripts.
Both scripts check the current Maven project version to decide, whether or not a rebuild of the project is necessary and rebuild (or initially build) it if required.


## Examples

The example program `ackermann.ja` implements the [Ackermann function](https://en.wikipedia.org/wiki/Ackermann_function) in Janus and calls it with small input parameters.
Following commands can be used to compile the Janus code, optimize it using Common Subexpression Elimination as well as Constant Propagation and create an executable from the resulting code.
The command line flag `--print-main` will let the compiler generate an executable, that prints the results of the main procedure after execution.

```bash
./rc3 --backend=rssa -O --print-main -bcount-instructions programs/examples-without-io/ackermann.ja out.c ; gcc out.c -o ackermann
```

If you wish to execute programs that require IO operations, the interpreter backend is recommended.
Following commands run the `caesar.ja` example program, which implements the caesar chipher, using the interpreter.
It will ask you for a key and an encryption mode (encrypt or decrypt) and will transform all text provided afterwards.
To exit the program, type an *End Of File* character (Usually achieved by pressing Control + D).

```bash
./rc3 --backend=interpreter programs/examples/caesar.ja
```

## Contribution

We are happy to hear feedback, suggestions and even bug reports.
Improvements or changes can be provided using GitLab (only THM internal users).
For external users or contact requests, you can reach out to us at [rc3@mni.thm.de](mailto:rc3@mni.thm.de).

## Copyright Notice

Copyright (C) 2021 Technische Hochschule Mittelhessen, Wiesenstr. 14, 35390 Gießen

Contributors:
- Uwe Meyer
- Niklas Deworetzki
- Pia-Doreen Ritzke
- Marc Schuster
