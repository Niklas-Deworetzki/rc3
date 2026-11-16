# Changelog

This file holds all notable changes and developments in the R3C research project.
Since the compiler and its related components are part of a research project, changes can occur frequently and may affect many components.

## Upcoming Version
### Added
- More syntactic freedom for RSSA programs.
- Better error reporting for syntax errors in Janus.
- Global Constant Propagation that propagates constants in parameter lists of entry and exit points.
- Optimization to remove unused procedures from a program.
- Loop Unrolling in Janus.
- Elimination of unreachable program paths in RSSA.

### Fixed
- Issue with illegal variable names in generated C code.

### Removed
- On-the-fly Constant Propagation as part of CSE.

## Version 1.5.4 - 2022.02.28
### Added
- Analysis for live and used variables in order to eliminate unnecessary assignments.
- Constant Propagation now detects more cases, where a constant value can be obtained.
- Backup-Strategy for CSE.

### Changed
- Large portions of internal structure, removing unnecessary conversions and increasing performance.
- Updated versions of dependencies.
- Improved output of runtime statistics for C programs.

### Fixed
- RSSA programs without an @Heap annotation can now be translated to C.
- RSSA programs uncalling a procedure with incorrect arguments were not detected as an error.

## Version 1.5.3 - 2021.11.18
### Added
- Static syntax analysis for the RSSA frontend.
- More example programs.
- The invertible Janus self-interpreter from Yokoyama and Glück.

### Changed
- Improved common subexpression elemination.
- Various internal API improvements.

### Fixed
- Various issues with RSSA annotations.
- Issue where implied optimizations were not handled correctly.
- Conflicts in RSSA parser.


## Version 1.5.0 – 2021.05.12
### Added
- Virtual Machine executing RSSA instructions via command `rvm`.
- More Annotations in RSSA Code to support standalone programs.
- Option `--count-instructions` to include profiling information in generated C code.
- Inlining optimization on RSSA instructions. This inlines procedure calls depending on a whitelist, a blacklist and a scoring system based on a procedures instruction count.
- Backend option `--backend-option=vm` to run compiled code on the RSSA VM.
- Testsuite testing RSSA VM against Janus Interpreter via command `rptest`.

### Changed
- Behavior of command line options for graphical debugging. The option `--graph-file` now implies `--graph`.
- Project structure to remove maintaining overhead.
- Maven build script now contains different profiles.
- Name mangling to prevent clashes with C identifiers is now performed when translating RSSA to C.

### Fixed
- Possible infinite loop in RSSA backend during code generation.
- CSE wrongfully eliminating required finalizers or not emitting enough of them.
- Memory accesses in the condition of a conditional entry or exit point leading to all kinds of problems with CSE and RSSA code generation.
- Swap instruction now allows atoms at both sides.
- Graphical debug output of CSE now correctly displays OF edges as well as PC edges for Exit-Nodes if an operand occurs in the parameter list as well as in the condition.


## Version 1.4.0 – 2021.02.25

### Added
- CI/CD integration.
- Constant Propagation optimization in CSE.
- Support for commutative operators in CSE.
- Execution of CSE and CP in both directions.

### Changed
- Behavior of debug optimization flags. They now enable the debugged optimization if it wasn't enabled already.
- Internal representation of SwapInstruction and handling of these instructions during CSE.

### Fixed
- GraphViz output files are no longer created if they were not requested.
- Translation of local-blocks is now symmetric under reversal.

## Version 1.3.0 – 2021.01.21

### Added
- Option to create files with GraphViz output.

### Changed
- Copyright message on Help Page
- Changed the internal interface of main classes

## Version 1.2.2 – 2020.12.17

### Added
- Capability to count the number of executed instructions.

### Changed
- Hierarchy of RSSA classes to better represent structure described in paper.

### Fixed
- Issue with recursive calls in alternating directions that caused the C-Code generator to emit wrong direction calls.


## Version 1.2.1 – 2020.11.20

### Added
- Shorthand options for optimizations and help.
- Option to automatically stop execution time of compiled programs.

### Fixed
- Spelling mistakes in CLI and comments.
- An issue in RSSA code generation where wrong variable names would occur in annotations.


## Version 1.2 – 2020.11.16

### Added
- Graphical output for basic blocks and control graphs via GraphViz.
- Annotations in RSSA to ensure index bounds for memory accesses.
- Additional RSSA backend with improved translation of control flow instructions.

### Changed
- CLI library to picocli.
- Internal structure of RSSA code generator is now using reverse code generation.


## Version 1.1 – 2020.10.28

### Added
- The initial RSSA backend.
- A C-Code generator for RSSA.
- The `--print-main` option showing the result of a computation without the need of IO operations.


## Version 1.0 – 2020.08.17.

The initial version of this project is the result of a Bachelor's Thesis by Niklas Deworetzki. It includes:

- Scanner and Parser for the Janus Programming Language.
- Semantic analysis for the Janus Programming Language.
- The Three Address Code (TAC) Backend.
- A C-Code Generator for the TAC Backend.
- The Interpreter Backend.
