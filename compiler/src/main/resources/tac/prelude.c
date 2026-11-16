/* CCode Janus Prelude - 10.06.2020 */
#include <stdlib.h>
#include <stdio.h>


#ifdef COUNT_INSTRUCTIONS
#ifndef PROCEDURE_COUNT
#error "Required value PROCEDURE_COUNT has not been defined."
#endif /* PROCEDURE_COUNT */

static unsigned long int __instruction_count[PROCEDURE_COUNT];
static char *__procedure_names[PROCEDURE_COUNT];
static int __unique_procedure_id = 0;

#define __INCREMENT_INSTRUCTION_COUNTER(procedure_id) (__instruction_count[(procedure_id)]++)

/* Disable implicit error checking, since counting won't work with forks. */
#undef IMPLICIT_NIL
#undef IMPLICIT_ARITHMETIC

#endif /* COUNT_INSTRUCTIONS */


/* Code required for implicit checks */
#if defined(IMPLICIT_NIL) || defined(IMPLICIT_ARITHMETIC)

#include <signal.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <errno.h>

#endif /* defined(IMPLICIT_NIL) || defined(IMPLICIT_ARITHMETIC) */

static void error(const char *message) {
    printf("%s\n", message);
    exit(1);
}

#define _ILLEGAL_STACK_OPERATION_MSG "Illegal stack operation."
#define _ILLEGAL_ARITHMETIC_MSG "Illegal arithmetic operation."
#define _ERROR_NO_MEMORY "Error: Failed to allocate memory!"

/* Assertion jump targets are implemented as macros, that translate to error-calls. */
#define _ASSERTION_PREFIX "Assertion failed: "
#define _ASSERTION_INITIALIZER_MISMATCH() \
    error(_ASSERTION_PREFIX"Variable does not equal expected value.")
#define _ASSERTION_IF_CONDITION_MISMATCH() \
    error(_ASSERTION_PREFIX"Condition at the end of branch has the wrong value.")
#define _ASSERTION_ENTRY_CONDITION_MUST_BE_FALSE() \
    error(_ASSERTION_PREFIX"The entry condition must evaluate to false upon repeated evaluation.")
#define _ASSERTION_ENTRY_CONDITION_MUST_BE_TRUE() \
    error(_ASSERTION_PREFIX"The entry condition must evaluate to true upon first evaluation.")
#define _ASSERTION_POP_NONZERO() \
    error(_ASSERTION_PREFIX"Cannot pop elements into a non-zero variable.")
#define _ASSERTION_POP_EMPTY_STACK() \
    error(_ASSERTION_PREFIX _ILLEGAL_STACK_OPERATION_MSG)
#define _ASSERTION_TOP_EMPTY_STACK() \
    error(_ASSERTION_PREFIX _ILLEGAL_STACK_OPERATION_MSG)
#define _ASSERTION_ARRAY_INDEX() \
    error(_ASSERTION_PREFIX"Array index is out of bounds.")
#define _ASSERTION_DIVISION_BY_ZERO() \
    error(_ASSERTION_PREFIX _ILLEGAL_ARITHMETIC_MSG)

/* Stack operations */
#define nil NULL
typedef struct s_cell {
    struct s_cell *predecessor;
    int value;
    int refcount;
} stack_cell;
typedef stack_cell *stack;

void __builtin_salloc(stack *toInit) {
    *toInit = malloc(sizeof(stack_cell));
    if (*toInit == NULL) {
        error(_ERROR_NO_MEMORY);
    }
    (*toInit)->refcount = 0;
}

#define __builtin_sfree(stack) free(stack)

/* Functions used to print variable names and values after execution. */
void printVariable_int(const char* name, int variable) {
    printf("%s = %d\n", name, variable);
}

void printVariable_stack(const char* name, stack variable) {
    printf("%s = ", name);
    if (variable == nil) {
        printf("nil\n");
    } else {
        printf("<%d", variable->value);
        variable = variable->predecessor;

        while (variable != nil) {
            printf(", %d", variable->value);
            variable = variable->predecessor;
        }
        printf("]\n");
    }
}

/* Delegate printVariable_intArray to another function that accepts arrays of any size. */
#define printVariable_intArray(name, array) \
    printVariable_intArrayAllSizes(name, array, (sizeof(array) / sizeof(array[0])))

void printVariable_intArrayAllSizes(const char* name, int *array, int length) {
    printf("%s = [", name);
    for (int i = 0; i < length; i++) {
        if (i > 0) {
            printf(", ");
        }
        printf("%d", array[i]);
    }
    printf("]\n");
}

/* =========================================================== *
 *                 Builtin library procedures.                 *
 * =========================================================== */
void builtin_read(int * variable);
void builtin_write(int *variable);
void builtin_readc(int *variable);
void builtin_writec(int *variable);

/* procedure read(int) */
void builtin_read(int *variable) {
    builtin_write(variable); // Write current value to user (part of specification)

    stack input = nil;
    stack tmp = nil;
    int amount_read = 0;
    int char_read = 0;

    char_read = getchar();
    while (char_read != '\n' && char_read != EOF) {
        amount_read++;

        // Push character to stack.
        __builtin_salloc(&tmp);
        tmp->value = char_read;
        tmp->predecessor = input;
        input = tmp;

        char_read = getchar();
    }

    // Allocate continuous region of memory to hold input characters.
    char *input_buffer = malloc(amount_read + 1);
    if (input_buffer == NULL) {
        error(_ERROR_NO_MEMORY);
    }
    input_buffer[amount_read] = '\0';

    while (input != nil) {
        amount_read--;
        input_buffer[amount_read] = (char) input->value;

        // Clear top of stack.
        tmp = input;
        input = input->predecessor;
        __builtin_sfree(tmp);
    }

    *variable = strtol(input_buffer, NULL, 10);
    free(input_buffer);
}
/* procedure write(int) */
void builtin_write(int *variable) {
    printf("%d\n", *variable);
}
/* procedure readc(int) */
void builtin_readc(int *variable) {
    builtin_writec(variable);
    if ((*variable = getchar()) == EOF) {
        *variable = -1;
    }
}
/* procedure writec(int) */
void builtin_writec(int *variable) {
    putchar(*variable);
}


/* Setup code. */

void main_fw(void); /* main_fw(void) must always exist. */

#if defined(IMPLICIT_NIL) || defined(IMPLICIT_ARITHMETIC)
static void handle_sig(int signal) {
    switch (signal) {

#ifdef IMPLICIT_NIL
        case SIGSEGV:
        case SIGBUS:
            error(_ILLEGAL_STACK_OPERATION_MSG);
#endif /* IMPLICIT_NIL */

#ifdef IMPLICIT_ARITHMETIC
        case SIGFPE:
            error(_ILLEGAL_ARITHMETIC_MSG);
#endif /* IMPLICIT_ARITHMETIC */

        default:
            error("Terminated by unknown signal.");
    }
}
#endif /* defined(IMPLICIT_NIL) || defined(IMPLICIT_ARITHMETIC) */

int main(int argc, char *argv[]) {

#if defined(IMPLICIT_NIL) || defined(IMPLICIT_ARITHMETIC)
    pid_t  pid;
    int status;

    pid = fork();
    if (pid == 0 /* in child */) {
        main_fw();
        return 0;

    } else if(pid > 0 /* in parent */) {
        waitpid(pid, &status, 0);

        /* Janus was terminated by a signal. */
        if (WIFSIGNALED(status)) {
            handle_sig(WTERMSIG(status));
        }
    } else {
        error("Startup failed!");
    }
    return 0;
#else
    main_fw();
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
}
/* END OF PRELUDE */
