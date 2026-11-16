#! /usr/bin/env python3

from sys import argv
from shutil import copyfile

if (len(argv) != 4):
    print("Usage: " + argv[0] + " <njvm> <binary> <output>")
    exit(1)

njvmFile = argv[1]
inputFile = argv[2]
outputFile = argv[3]

copyfile(njvmFile, outputFile)

with open(outputFile, "w") as outStream:
    with open(inputFile, "rb") as inStream:
        outStream.write("// THIS FILE WAS AUTOMATICALLY CREATED FROM " + njvmFile + " PREPENDING THE BINARY " + inputFile)
        outStream.write("\nprocedure loadExecutable(stack stdin)\n")
        outStream.write("local int byte = 0\n")
        for byte in inStream.read()[::-1]: # Iterate in reverse order
            outStream.write("byte += " + str(byte) + "\tpush(byte, stdin)\n")
        outStream.write("delocal int byte = 0\n")

    with open(njvmFile, "r") as njvm:
        outStream.write(njvm.read())
