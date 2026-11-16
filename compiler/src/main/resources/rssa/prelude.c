#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>


#ifdef COUNT_INSTRUCTIONS
#ifndef PROCEDURE_COUNT
#error "Required value PROCEDURE_COUNT has not been defined."
#endif /* PROCEDURE_COUNT */

static unsigned long int __instruction_count[PROCEDURE_COUNT];
static char *__procedure_names[PROCEDURE_COUNT];
static int __unique_procedure_id = 0;

#define __INCREMENT_INSTRUCTION_COUNTER(procedure_id) (__instruction_count[(procedure_id)]++)

#endif /* COUNT_INSTRUCTIONS */


#ifdef STOP_RUNTIME
#include <sys/time.h>
#include <time.h>

#ifndef STOP_RUNTIME_MESSAGE
// Define a default format string for the stopped time. This could be changed in code generator.
#define STOP_RUNTIME_MESSAGE "Program finished in %0.2lf ms\n"
#endif /* STOP_RUNTIME_MESSAGE */

#endif /* STOP_RUNTIME */


#if !defined(RSSA_HEAPSIZE)
#error "Required intrinsic values have not been defined."
#endif

int *memory;

void __bootstrap_janus_fw();
void __bootstrap_janus_bw();

void __check_alloc(int usage, int requested);
void __check_free(int usage, int requested);
int __check_index(int base, int size, int index);

void memorySwap(int x, int y);

#define _PRINT_MAIN_MESSAGE "Variables at the end of 'main' procedure:\n"
void __print_int(const char* name, int value);
void __print_stack(const char* name, int ref, int top);
void __print_array(const char* name, int ref, int size);
void __print_string(const char* name, int ref, int size);

int main(int argc, char *argv[]) {

#ifdef STOP_RUNTIME
     clock_t begin_clock, end_clock;
     begin_clock = clock();
#endif

    if (argv[1] == NULL) {
        __bootstrap_janus_fw();
    } else if (strcmp(argv[1], "--bw") == 0) {
        __bootstrap_janus_bw();
    } else {
        printf("use --bw as an option to execute backwards, use no option to execute forwards\n");
        return 1;
    }

#ifdef STOP_RUNTIME
    end_clock = clock();
    printf(STOP_RUNTIME_MESSAGE, (double) (end_clock - begin_clock) * 1000.0 / CLOCKS_PER_SEC);
#endif

#ifdef COUNT_INSTRUCTIONS
    unsigned long int totalExecutedInstructionCount = 0;
    fprintf(stderr, "Listing called procedures and number of executed instructions:\n");
    for (int i = 0; i < PROCEDURE_COUNT; i++) {
        char *procedure_name = __procedure_names[i];
        if (procedure_name != NULL) {
            fprintf(stderr, " - %s: %ld\n", procedure_name, __instruction_count[i]);
            totalExecutedInstructionCount += __instruction_count[i];
        }
    }
    fprintf(stderr, "Total executed instructions: %ld\n", totalExecutedInstructionCount);
#endif

    if (memory != NULL) {
        free(memory);
    }
}


void __check_alloc(int usage, int requested) {
    if (RSSA_HEAPSIZE - usage < requested || requested < 0) {
        fprintf(stderr, "Allocation failed. Tried to allocate %d elements, but only %d are available.\n",
            requested, RSSA_HEAPSIZE - usage);
        exit(1);
    }
}

void __check_free(int usage, int requested) {
    if (usage < requested || requested < 0) {
        fprintf(stderr, "Free failed. Tried to free up %d elements, but only %d are allocated.\n",
            requested, usage);
        exit(1);
    }
}

int __check_index(int base, int size, int index) {
    if (index < base || base + size <= index) {
        fprintf(stderr, "Index %d is out of bounds for structure of size %d.\n",
            index - base, size);
        exit(1);
    }
    return index;
}


void memorySwap(int x, int y) {
    int tmp = memory[x];
    memory[x] = memory[y];
    memory[y] = tmp;
}

void __print_int(const char* name, int value) {
    printf("%s = %d\n", name, value);
}

void __print_stack(const char* name, int ref, int top) {
    printf("%s = ", name);
    if (top == 0) {
        printf("nil\n");
    } else {
        printf("<");
        for (int i = 1; i <= top; i++) {
            printf("%d", memory[ref + (top - i)]);
            if (i != top) {
                printf(", ");
            }
        }
        printf("]\n");
    }
}

void __print_array(const char* name, int ref, int size) {
    printf("%s = [", name);
    for (int i = 0; i < size; i++) {
        if (i > 0) {
            printf(", ");
        }
        printf("%d", memory[ref + i]);
    }
    printf("]\n");
}

void __print_string(const char* name, int ref, int size) {
    printf("%s = ", name);
    for (int i = 0; i < size && memory[ref + i] != 0; i++) {
        putchar((char) memory[ref + i]);
    }
    printf("\n");
}
