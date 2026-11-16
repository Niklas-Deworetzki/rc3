#define RSSA_HEAPSIZE (102400)
#define RSSA_HEAPTOP (memory[20])
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <sys/time.h>
#include <time.h>
#if !defined(RSSA_HEAPTOP) || !defined(RSSA_HEAPSIZE)
#error "Required intrinsic values have not been defined."

#else
#define RSSA_AVAILABLE_HEAP (RSSA_HEAPSIZE - RSSA_HEAPTOP)
#endif

int *memory;

void __bootstrap_janus_fw();
void __bootstrap_janus_bw();

void __check_alloc(int allocated_heap, int size);
void __check_free(int allocated_heap, int size);
void __check_index(int base, int size, int index);

void memorySwap(int x, int y);

#define _PRINT_MAIN_MESSAGE "Variables at the end of 'main' procedure:\n"
void __print_int(const char* name, int value);
void __print_stack(const char* name, int ref, int top);
void __print_array(const char* name, int ref, int size);

int main(int argc, char *argv[]) {
 clock_t begin,end;
	begin = clock();
    if (argv[1] == NULL) {
        __bootstrap_janus_fw();
    } else if ((strcmp(argv[1],"--bw")) == 0) {
        __bootstrap_janus_bw();
    } else {
        printf("use --bw as an option to execute backwards, use no option to execute forwards\n");
        return 1;
    }
 end= clock();
	printf("%ld\n",end-begin);
    if (memory != NULL) {
        free(memory);
    }
}


void __check_alloc(int allocated_heap, int size) {
    if (RSSA_AVAILABLE_HEAP < size || size < 0) {
        fprintf(stderr, "Allocation failed. Tried to allocate %d elements, but only %d are available.\n",
            size, RSSA_AVAILABLE_HEAP);
        exit(1);
    }
}

void __check_free(int allocated_heap, int size) {
    if (RSSA_HEAPTOP < size || size < 0) {
        fprintf(stderr, "Free failed. Tried to free up %d elements, but only %d are allocated.\n",
            size, RSSA_HEAPTOP);
        exit(1);
    }
}

void __check_index(int base, int size, int index) {
    if (index < base || base + size <= index) {
        fprintf(stderr, "Index %d is out of bounds for structure of size %d.\n",
            index - base, size);
        exit(1);
    }
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



void nexttry_fw(int *_arg0);
void Lnexttry_1_fw(int *_arg0, int Ta_1, int try_2);
void Lnexttry_2_fw(int *_arg0, int Ta_2, int try_4);
void Lnexttry_3_fw(int *_arg0, int Tb_2, int try_5);
void Lnexttry_4_fw(int *_arg0, int Tb_2, int try_5);
void nexttry_bw(int *_arg0);
void Lnexttry_4_bw(int *_arg0, int Tb_1, int try_4);
void Lnexttry_3_bw(int *_arg0, int Tb_0, int try_3);
void Lnexttry_1_bw(int *_arg0, int Ta_0, int try_1);
void Lnexttry_2_bw(int *_arg0, int Ta_0, int try_1);
void main_fw(int *_arg0, int *_arg1, int *_arg2);
void Lmain_0_1_fw(int *_arg0, int *_arg1, int *_arg2, int Ta_1, int num_1, int index_1, int fact_1);
void Lmain_0_3_fw(int *_arg0, int *_arg1, int *_arg2, int Ta_1, int num_1, int index_1, int fact_1);
void Lmain_0_2_fw(int *_arg0, int *_arg1, int *_arg2, int Tb_1, int index_2, int fact_2, int num_3);
void Lmain_0_4_fw(int *_arg0, int *_arg1, int *_arg2, int Tb_2, int index_3, int fact_3, int num_4);
void main_bw(int *_arg0, int *_arg1, int *_arg2);
void Lmain_0_3_bw(int *_arg0, int *_arg1, int *_arg2, int Ta_2, int num_3, int index_2, int fact_2);
void Lmain_0_4_bw(int *_arg0, int *_arg1, int *_arg2, int Tb_0, int index_1, int fact_1, int num_2);
void Lmain_0_2_bw(int *_arg0, int *_arg1, int *_arg2, int Tb_0, int index_1, int fact_1, int num_2);
void Lmain_0_1_bw(int *_arg0, int *_arg1, int *_arg2, int Ta_0, int num_0, int index_0, int fact_0);
void factor_fw(int *_arg0, int *_arg1);
void Lfactor_1_fw(int *_arg0, int *_arg1, int Ta_1, int try_1, int num_1, int fact_1, int i_1);
void Lfactor_3_fw(int *_arg0, int *_arg1, int Ta_1, int try_1, int num_1, int fact_1, int i_1);
void Lfactor_2_fw(int *_arg0, int *_arg1, int Tb_1, int fact_2, int try_2, int i_2, int num_2);
void Lfactor_5_fw(int *_arg0, int *_arg1, int Ti_1, int fact_3, int try_4, int i_3, int num_3);
void Lfactor_7_fw(int *_arg0, int *_arg1, int Ti_1, int fact_3, int try_4, int i_3, int num_3);
void Lfactor_6_fw(int *_arg0, int *_arg1, int Tj_1, int try_5, int num_4, int fact_4, int i_4);
void Lfactor_8_fw(int *_arg0, int *_arg1, int Tj_2, int try_6, int num_6, int fact_5, int i_6);
void Lfactor_4_fw(int *_arg0, int *_arg1, int Tb_2, int fact_6, int try_7, int i_7, int num_7);
void Lfactor_9_fw(int *_arg0, int *_arg1, int Tx_1, int fact_7, int i_8, int num_8, int try_8);
void Lfactor_10_fw(int *_arg0, int *_arg1, int Tx_2, int fact_8, int i_10, int num_10, int try_9);
void Lfactor_11_fw(int *_arg0, int *_arg1, int Ty_2, int fact_9, int i_11, int try_10, int num_12);
void Lfactor_12_fw(int *_arg0, int *_arg1, int Ty_2, int fact_9, int i_11, int try_10, int num_12);
void Lfactor_13_fw(int *_arg0, int *_arg1, int Trb_1, int fact_10, int try_11, int i_12, int num_13);
void Lfactor_17_fw(int *_arg0, int *_arg1, int Tlc_1, int fact_11, int try_12, int i_13, int num_14);
void Lfactor_19_fw(int *_arg0, int *_arg1, int Tlc_1, int fact_11, int try_12, int i_13, int num_14);
void Lfactor_18_fw(int *_arg0, int *_arg1, int Tmc_1, int fact_12, int try_13, int i_14, int num_15);
void Lfactor_20_fw(int *_arg0, int *_arg1, int Tmc_2, int fact_13, int try_15, int i_15, int num_16);
void Lfactor_14_fw(int *_arg0, int *_arg1, int Trb_2, int fact_14, int try_16, int i_16, int num_17);
void Lfactor_15_fw(int *_arg0, int *_arg1, int Tsb_2, int fact_15, int try_18, int i_17, int num_18);
void Lfactor_16_fw(int *_arg0, int *_arg1, int Tsb_2, int fact_15, int try_18, int i_17, int num_18);
void factor_bw(int *_arg0, int *_arg1);
void Lfactor_16_bw(int *_arg0, int *_arg1, int Tsb_1, int fact_14, int try_17, int i_16, int num_17);
void Lfactor_15_bw(int *_arg0, int *_arg1, int Tsb_0, int fact_13, int try_15, int i_15, int num_16);
void Lfactor_19_bw(int *_arg0, int *_arg1, int Tlc_2, int fact_12, int try_14, int i_14, int num_15);
void Lfactor_20_bw(int *_arg0, int *_arg1, int Tmc_0, int fact_11, int try_12, int i_13, int num_14);
void Lfactor_18_bw(int *_arg0, int *_arg1, int Tmc_0, int fact_11, int try_12, int i_13, int num_14);
void Lfactor_17_bw(int *_arg0, int *_arg1, int Tlc_0, int fact_10, int try_11, int i_12, int num_13);
void Lfactor_13_bw(int *_arg0, int *_arg1, int Trb_0, int fact_9, int try_10, int i_11, int num_12);
void Lfactor_14_bw(int *_arg0, int *_arg1, int Trb_0, int fact_9, int try_10, int i_11, int num_12);
void Lfactor_12_bw(int *_arg0, int *_arg1, int Ty_1, int fact_8, int i_10, int try_9, int num_11);
void Lfactor_11_bw(int *_arg0, int *_arg1, int Ty_0, int fact_7, int i_9, int try_8, int num_9);
void Lfactor_9_bw(int *_arg0, int *_arg1, int Tx_0, int fact_6, int i_7, int num_7, int try_7);
void Lfactor_10_bw(int *_arg0, int *_arg1, int Tx_0, int fact_6, int i_7, int num_7, int try_7);
void Lfactor_3_bw(int *_arg0, int *_arg1, int Ta_2, int try_6, int num_6, int fact_5, int i_6);
void Lfactor_7_bw(int *_arg0, int *_arg1, int Ti_2, int fact_4, int try_5, int i_5, int num_5);
void Lfactor_8_bw(int *_arg0, int *_arg1, int Tj_0, int try_4, int num_3, int fact_3, int i_3);
void Lfactor_6_bw(int *_arg0, int *_arg1, int Tj_0, int try_4, int num_3, int fact_3, int i_3);
void Lfactor_5_bw(int *_arg0, int *_arg1, int Ti_0, int fact_2, int try_3, int i_2, int num_2);
void Lfactor_4_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int try_1, int i_1, int num_1);
void Lfactor_2_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int try_1, int i_1, int num_1);
void Lfactor_1_bw(int *_arg0, int *_arg1, int Ta_0, int try_0, int num_0, int fact_0, int i_0);
void zeroi_fw(int *_arg0, int *_arg1);
void Lzeroi_1_fw(int *_arg0, int *_arg1, int Ta_1, int fact_1, int i_1);
void Lzeroi_3_fw(int *_arg0, int *_arg1, int Ta_1, int fact_1, int i_1);
void Lzeroi_2_fw(int *_arg0, int *_arg1, int Tb_1, int fact_2, int i_2);
void Lzeroi_4_fw(int *_arg0, int *_arg1, int Tb_2, int fact_3, int i_4);
void zeroi_bw(int *_arg0, int *_arg1);
void Lzeroi_3_bw(int *_arg0, int *_arg1, int Ta_2, int fact_2, int i_3);
void Lzeroi_4_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int i_1);
void Lzeroi_2_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int i_1);
void Lzeroi_1_bw(int *_arg0, int *_arg1, int Ta_0, int fact_0, int i_0);

//49: procedure nexttry(int try)
void nexttry_fw(int *_arg0){
int try_0 = *_arg0;
//50:     try += 2
int try_1 = try_0 + (2 + 0);
//51:     if try = 4 then
int Ta_0 = 0 ^ (try_1 == 4);
if ((Ta_0 != 0)) Lnexttry_1_fw(_arg0, Ta_0, try_1); else Lnexttry_2_fw(_arg0, Ta_0, try_1); 
}

//51:     if try = 4 then
void Lnexttry_1_fw(int *_arg0, int Ta_1, int try_2){
assert(0 == (Ta_1 ^ (try_2 == 4)));
//52:         try -= 1
int try_3 = try_2 - (1 + 0);
int Tb_0 = 0 ^ (try_3 == 3);
Lnexttry_3_fw(_arg0, Tb_0, try_3); 
}

//52:         try -= 1
void Lnexttry_2_fw(int *_arg0, int Ta_2, int try_4){
assert(0 == (Ta_2 ^ (try_4 == 4)));
//51:     if try = 4 then
int Tb_1 = 0 ^ (try_4 == 3);
Lnexttry_4_fw(_arg0, Tb_1, try_4); 
}

//51:     if try = 4 then
void Lnexttry_3_fw(int *_arg0, int Tb_2, int try_5){
assert(0 != ((Tb_2 != 0)));
assert(0 == (Tb_2 ^ (try_5 == 3)));
//49: procedure nexttry(int try)
*_arg0 = try_5;
}

//51:     if try = 4 then
void Lnexttry_4_fw(int *_arg0, int Tb_2, int try_5){
assert(0 == ((Tb_2 != 0)));
assert(0 == (Tb_2 ^ (try_5 == 3)));
//49: procedure nexttry(int try)
*_arg0 = try_5;
}

//49: procedure nexttry(int try)
void nexttry_bw(int *_arg0){
int try_5 = *_arg0;
//51:     if try = 4 then
int Tb_2 = 0 ^ (try_5 == 3);
if ((Tb_2 != 0)) Lnexttry_3_bw(_arg0, Tb_2, try_5); else Lnexttry_4_bw(_arg0, Tb_2, try_5); 
}

//51:     if try = 4 then
void Lnexttry_4_bw(int *_arg0, int Tb_1, int try_4){
assert(0 == (Tb_1 ^ (try_4 == 3)));
//52:         try -= 1
int Ta_2 = 0 ^ (try_4 == 4);
Lnexttry_2_bw(_arg0, Ta_2, try_4); 
}

//52:         try -= 1
void Lnexttry_3_bw(int *_arg0, int Tb_0, int try_3){
assert(0 == (Tb_0 ^ (try_3 == 3)));
int try_2 = try_3 + (1 + 0);
//51:     if try = 4 then
int Ta_1 = 0 ^ (try_2 == 4);
Lnexttry_1_bw(_arg0, Ta_1, try_2); 
}

//51:     if try = 4 then
void Lnexttry_1_bw(int *_arg0, int Ta_0, int try_1){
assert(0 != ((Ta_0 != 0)));
assert(0 == (Ta_0 ^ (try_1 == 4)));
//50:     try += 2
int try_0 = try_1 - (2 + 0);
//49: procedure nexttry(int try)
*_arg0 = try_0;
}

//51:     if try = 4 then
void Lnexttry_2_bw(int *_arg0, int Ta_0, int try_1){
assert(0 == ((Ta_0 != 0)));
assert(0 == (Ta_0 ^ (try_1 == 4)));
//50:     try += 2
int try_0 = try_1 - (2 + 0);
//49: procedure nexttry(int try)
*_arg0 = try_0;
}

//55: procedure main()
void main_fw(int *_arg0, int *_arg1, int *_arg2){
int index_0 = *_arg0;
int num_0 = *_arg1;
int fact_0 = *_arg2;
//69:     from num=0 do
int Ta_0 = 0 ^ (num_0 == 0);
Lmain_0_1_fw(_arg0, _arg1, _arg2, Ta_0, num_0, index_0, fact_0); 
}

//69:     from num=0 do
void Lmain_0_1_fw(int *_arg0, int *_arg1, int *_arg2, int Ta_1, int num_1, int index_1, int fact_1){
assert(0 != ((Ta_1 != 0)));
assert(0 == (Ta_1 ^ (num_1 == 0)));
//70:     num +=1
int num_2 = num_1 + (1 + 0);
int Tb_0 = 0 ^ (num_2 == 43500);
if ((Tb_0 != 0)) Lmain_0_4_fw(_arg0, _arg1, _arg2, Tb_0, index_1, fact_1, num_2); else Lmain_0_2_fw(_arg0, _arg1, _arg2, Tb_0, index_1, fact_1, num_2); 
}

//69:     from num=0 do
void Lmain_0_3_fw(int *_arg0, int *_arg1, int *_arg2, int Ta_1, int num_1, int index_1, int fact_1){
assert(0 == ((Ta_1 != 0)));
assert(0 == (Ta_1 ^ (num_1 == 0)));
//70:     num +=1
int num_2 = num_1 + (1 + 0);
int Tb_0 = 0 ^ (num_2 == 43500);
if ((Tb_0 != 0)) Lmain_0_4_fw(_arg0, _arg1, _arg2, Tb_0, index_1, fact_1, num_2); else Lmain_0_2_fw(_arg0, _arg1, _arg2, Tb_0, index_1, fact_1, num_2); 
}

//70:     num +=1
void Lmain_0_2_fw(int *_arg0, int *_arg1, int *_arg2, int Tb_1, int index_2, int fact_2, int num_3){
assert(0 == (Tb_1 ^ (num_3 == 43500)));
//69:     from num=0 do
int Ta_2 = 0 ^ (num_3 == 0);
Lmain_0_3_fw(_arg0, _arg1, _arg2, Ta_2, num_3, index_2, fact_2); 
}

//69:     from num=0 do
void Lmain_0_4_fw(int *_arg0, int *_arg1, int *_arg2, int Tb_2, int index_3, int fact_3, int num_4){
assert(0 == (Tb_2 ^ (num_4 == 43500)));
//72:     call factor(num, fact)
int _tmp1 = num_4;
int _tmp2 = fact_3;
factor_fw(&_tmp1, &_tmp2);
int num_5 = _tmp1;
int fact_4 = _tmp2;
//55: procedure main()
*_arg0 = index_3;
*_arg1 = num_5;
*_arg2 = fact_4;
}

//55: procedure main()
void main_bw(int *_arg0, int *_arg1, int *_arg2){
int index_3 = *_arg0;
int num_5 = *_arg1;
int fact_4 = *_arg2;
//72:     call factor(num, fact)
int _tmp1 = num_5;
int _tmp2 = fact_4;
factor_fw(&_tmp1, &_tmp2);
int num_4 = _tmp1;
int fact_3 = _tmp2;
//69:     from num=0 do
int Tb_2 = 0 ^ (num_4 == 43500);
Lmain_0_4_bw(_arg0, _arg1, _arg2, Tb_2, index_3, fact_3, num_4); 
}

//69:     from num=0 do
void Lmain_0_3_bw(int *_arg0, int *_arg1, int *_arg2, int Ta_2, int num_3, int index_2, int fact_2){
assert(0 == (Ta_2 ^ (num_3 == 0)));
//70:     num +=1
int Tb_1 = 0 ^ (num_3 == 43500);
Lmain_0_2_bw(_arg0, _arg1, _arg2, Tb_1, index_2, fact_2, num_3); 
}

//70:     num +=1
void Lmain_0_4_bw(int *_arg0, int *_arg1, int *_arg2, int Tb_0, int index_1, int fact_1, int num_2){
assert(0 != ((Tb_0 != 0)));
assert(0 == (Tb_0 ^ (num_2 == 43500)));
int num_1 = num_2 - (1 + 0);
//69:     from num=0 do
int Ta_1 = 0 ^ (num_1 == 0);
if ((Ta_1 != 0)) Lmain_0_1_bw(_arg0, _arg1, _arg2, Ta_1, num_1, index_1, fact_1); else Lmain_0_3_bw(_arg0, _arg1, _arg2, Ta_1, num_1, index_1, fact_1); 
}

//70:     num +=1
void Lmain_0_2_bw(int *_arg0, int *_arg1, int *_arg2, int Tb_0, int index_1, int fact_1, int num_2){
assert(0 == ((Tb_0 != 0)));
assert(0 == (Tb_0 ^ (num_2 == 43500)));
int num_1 = num_2 - (1 + 0);
//69:     from num=0 do
int Ta_1 = 0 ^ (num_1 == 0);
if ((Ta_1 != 0)) Lmain_0_1_bw(_arg0, _arg1, _arg2, Ta_1, num_1, index_1, fact_1); else Lmain_0_3_bw(_arg0, _arg1, _arg2, Ta_1, num_1, index_1, fact_1); 
}

//69:     from num=0 do
void Lmain_0_1_bw(int *_arg0, int *_arg1, int *_arg2, int Ta_0, int num_0, int index_0, int fact_0){
assert(0 == (Ta_0 ^ (num_0 == 0)));
//55: procedure main()
*_arg0 = index_0;
*_arg1 = num_0;
*_arg2 = fact_0;
}

//9: procedure factor(int num, int fact[20])
void factor_fw(int *_arg0, int *_arg1){
int num_0 = *_arg0;
int fact_0 = *_arg1;
//10:     local int try = 0   // Attempted factor.
int try_0 = 0 ^ (0 + 0);
//11:     local int i = 0     // Pointer to last factor in factor table.
int i_0 = 0 ^ (0 + 0);
//12:     from (try = 0) && (num > 1) loop
int Tc_0 = 0 ^ (try_0 == 0);
int Td_0 = 0 ^ (num_0 > 1);
int Ta_0 = 0 ^ (Tc_0 & Td_0);
assert(0 == (Td_0 ^ (num_0 > 1)));
assert(0 == (Tc_0 ^ (try_0 == 0)));
Lfactor_1_fw(_arg0, _arg1, Ta_0, try_0, num_0, fact_0, i_0); 
}

//12:     from (try = 0) && (num > 1) loop
void Lfactor_1_fw(int *_arg0, int *_arg1, int Ta_1, int try_1, int num_1, int fact_1, int i_1){
assert(0 != ((Ta_1 != 0)));
int Te_0 = 0 ^ (try_1 == 0);
int Tf_0 = 0 ^ (num_1 > 1);
assert(0 == (Ta_1 ^ (Te_0 & Tf_0)));
assert(0 == (Tf_0 ^ (num_1 > 1)));
assert(0 == (Te_0 ^ (try_1 == 0)));
//12:     from (try = 0) && (num > 1) loop
int Tg_0 = 0 ^ (try_1 * try_1);
int Tb_0 = 0 ^ (Tg_0 > num_1);
assert(0 == (Tg_0 ^ (try_1 * try_1)));
if ((Tb_0 != 0)) Lfactor_4_fw(_arg0, _arg1, Tb_0, fact_1, try_1, i_1, num_1); else Lfactor_2_fw(_arg0, _arg1, Tb_0, fact_1, try_1, i_1, num_1); 
}

//12:     from (try = 0) && (num > 1) loop
void Lfactor_3_fw(int *_arg0, int *_arg1, int Ta_1, int try_1, int num_1, int fact_1, int i_1){
assert(0 == ((Ta_1 != 0)));
int Te_0 = 0 ^ (try_1 == 0);
int Tf_0 = 0 ^ (num_1 > 1);
assert(0 == (Ta_1 ^ (Te_0 & Tf_0)));
assert(0 == (Tf_0 ^ (num_1 > 1)));
assert(0 == (Te_0 ^ (try_1 == 0)));
//12:     from (try = 0) && (num > 1) loop
int Tg_0 = 0 ^ (try_1 * try_1);
int Tb_0 = 0 ^ (Tg_0 > num_1);
assert(0 == (Tg_0 ^ (try_1 * try_1)));
if ((Tb_0 != 0)) Lfactor_4_fw(_arg0, _arg1, Tb_0, fact_1, try_1, i_1, num_1); else Lfactor_2_fw(_arg0, _arg1, Tb_0, fact_1, try_1, i_1, num_1); 
}

//12:     from (try = 0) && (num > 1) loop
void Lfactor_2_fw(int *_arg0, int *_arg1, int Tb_1, int fact_2, int try_2, int i_2, int num_2){
int Th_0 = 0 ^ (try_2 * try_2);
assert(0 == (Tb_1 ^ (Th_0 > num_2)));
assert(0 == (Th_0 ^ (try_2 * try_2)));
//13:         call nexttry(try)
int _tmp1 = try_2;
nexttry_fw(&_tmp1);
int try_3 = _tmp1;
//14:         from fact[i] != try loop               // Divide out all occurrences of this
__check_index(fact_2, 20, 0 ^ (fact_2 + i_2));int Tk_0 = 0 ^ (fact_2 + i_2);
int Tl_0 = 0 ^ (memory[Tk_0] + 0);
int Ti_0 = 0 ^ (Tl_0 != try_3);
assert(0 == (Tl_0 ^ (memory[Tk_0] + 0)));
assert(0 == (Tk_0 - (i_2 + fact_2)));
Lfactor_5_fw(_arg0, _arg1, Ti_0, fact_2, try_3, i_2, num_2); 
}

//14:         from fact[i] != try loop               // Divide out all occurrences of this
void Lfactor_5_fw(int *_arg0, int *_arg1, int Ti_1, int fact_3, int try_4, int i_3, int num_3){
assert(0 != ((Ti_1 != 0)));
__check_index(fact_3, 20, 0 ^ (fact_3 + i_3));int Tm_0 = 0 ^ (fact_3 + i_3);
int Tn_0 = 0 ^ (memory[Tm_0] + 0);
assert(0 == (Ti_1 ^ (Tn_0 != try_4)));
assert(0 == (Tn_0 ^ (memory[Tm_0] + 0)));
assert(0 == (Tm_0 - (i_3 + fact_3)));
//14:         from fact[i] != try loop               // Divide out all occurrences of this
int To_0 = 0 ^ (num_3 % try_4);
int Tj_0 = 0 ^ (To_0 != 0);
assert(0 == (To_0 ^ (num_3 % try_4)));
if ((Tj_0 != 0)) Lfactor_8_fw(_arg0, _arg1, Tj_0, try_4, num_3, fact_3, i_3); else Lfactor_6_fw(_arg0, _arg1, Tj_0, try_4, num_3, fact_3, i_3); 
}

//14:         from fact[i] != try loop               // Divide out all occurrences of this
void Lfactor_7_fw(int *_arg0, int *_arg1, int Ti_1, int fact_3, int try_4, int i_3, int num_3){
assert(0 == ((Ti_1 != 0)));
__check_index(fact_3, 20, 0 ^ (fact_3 + i_3));int Tm_0 = 0 ^ (fact_3 + i_3);
int Tn_0 = 0 ^ (memory[Tm_0] + 0);
assert(0 == (Ti_1 ^ (Tn_0 != try_4)));
assert(0 == (Tn_0 ^ (memory[Tm_0] + 0)));
assert(0 == (Tm_0 - (i_3 + fact_3)));
//14:         from fact[i] != try loop               // Divide out all occurrences of this
int To_0 = 0 ^ (num_3 % try_4);
int Tj_0 = 0 ^ (To_0 != 0);
assert(0 == (To_0 ^ (num_3 % try_4)));
if ((Tj_0 != 0)) Lfactor_8_fw(_arg0, _arg1, Tj_0, try_4, num_3, fact_3, i_3); else Lfactor_6_fw(_arg0, _arg1, Tj_0, try_4, num_3, fact_3, i_3); 
}

//14:         from fact[i] != try loop               // Divide out all occurrences of this
void Lfactor_6_fw(int *_arg0, int *_arg1, int Tj_1, int try_5, int num_4, int fact_4, int i_4){
int Tp_0 = 0 ^ (num_4 % try_5);
assert(0 == (Tj_1 ^ (Tp_0 != 0)));
assert(0 == (Tp_0 ^ (num_4 % try_5)));
//15:             i += 1                             // factor
int i_5 = i_4 + (1 + 0);
//16:             fact[i] += try
__check_index(fact_4, 20, 0 ^ (fact_4 + i_5));int Tq_0 = 0 ^ (fact_4 + i_5);
memory[Tq_0] += (try_5 + 0);
assert(0 == (Tq_0 - (i_5 + fact_4)));
//17:             local int z = num / try
int z_0 = 0 ^ (num_4 / try_5);
//18:             z <=> num
int z_1 = num_4; int num_5 = z_0;
assert(0 == (z_1 - (num_5 * try_5)));
__check_index(fact_4, 20, 0 ^ (fact_4 + i_5));int Tr_0 = 0 ^ (fact_4 + i_5);
int Ts_0 = 0 ^ (memory[Tr_0] + 0);
int Ti_2 = 0 ^ (Ts_0 != try_5);
assert(0 == (Ts_0 ^ (memory[Tr_0] + 0)));
assert(0 == (Tr_0 - (i_5 + fact_4)));
Lfactor_7_fw(_arg0, _arg1, Ti_2, fact_4, try_5, i_5, num_5); 
}

//18:             z <=> num
void Lfactor_8_fw(int *_arg0, int *_arg1, int Tj_2, int try_6, int num_6, int fact_5, int i_6){
int Tt_0 = 0 ^ (num_6 % try_6);
assert(0 == (Tj_2 ^ (Tt_0 != 0)));
assert(0 == (Tt_0 ^ (num_6 % try_6)));
int Tu_0 = 0 ^ (try_6 == 0);
int Tv_0 = 0 ^ (num_6 > 1);
int Ta_2 = 0 ^ (Tu_0 & Tv_0);
assert(0 == (Tv_0 ^ (num_6 > 1)));
assert(0 == (Tu_0 ^ (try_6 == 0)));
Lfactor_3_fw(_arg0, _arg1, Ta_2, try_6, num_6, fact_5, i_6); 
}

//18:             z <=> num
void Lfactor_4_fw(int *_arg0, int *_arg1, int Tb_2, int fact_6, int try_7, int i_7, int num_7){
int Tw_0 = 0 ^ (try_7 * try_7);
assert(0 == (Tb_2 ^ (Tw_0 > num_7)));
assert(0 == (Tw_0 ^ (try_7 * try_7)));
//23:     if num != 1 then
int Tx_0 = 0 ^ (num_7 != 1);
if ((Tx_0 != 0)) Lfactor_9_fw(_arg0, _arg1, Tx_0, fact_6, i_7, num_7, try_7); else Lfactor_10_fw(_arg0, _arg1, Tx_0, fact_6, i_7, num_7, try_7); 
}

//23:     if num != 1 then
void Lfactor_9_fw(int *_arg0, int *_arg1, int Tx_1, int fact_7, int i_8, int num_8, int try_8){
assert(0 == (Tx_1 ^ (num_8 != 1)));
//24:         i += 1                                 // Put last prime away, if not done
int i_9 = i_8 + (1 + 0);
//25:         fact[i] ^= num                         // and zero num
__check_index(fact_7, 20, 0 ^ (fact_7 + i_9));int Tz_0 = 0 ^ (fact_7 + i_9);
memory[Tz_0] ^= (num_8 + 0);
assert(0 == (Tz_0 - (i_9 + fact_7)));
//26:         num     ^= fact[i]
__check_index(fact_7, 20, 0 ^ (fact_7 + i_9));int Tab_0 = 0 ^ (fact_7 + i_9);
int num_9 = num_8 ^ (memory[Tab_0] + 0);
assert(0 == (Tab_0 - (i_9 + fact_7)));
//27:         fact[i] ^= num
__check_index(fact_7, 20, 0 ^ (fact_7 + i_9));int Tbb_0 = 0 ^ (fact_7 + i_9);
memory[Tbb_0] ^= (num_9 + 0);
assert(0 == (Tbb_0 - (i_9 + fact_7)));
__check_index(fact_7, 20, 0 ^ (fact_7 + i_9));int Tcb_0 = 0 ^ (fact_7 + i_9);
int Teb_0 = 0 ^ (i_9 - 1);
__check_index(fact_7, 20, 0 ^ (fact_7 + Teb_0));int Tdb_0 = 0 ^ (fact_7 + Teb_0);
int Tfb_0 = 0 ^ (memory[Tcb_0] + 0);
int Tgb_0 = 0 ^ (memory[Tdb_0] + 0);
int Ty_0 = 0 ^ (Tfb_0 != Tgb_0);
assert(0 == (Tgb_0 ^ (memory[Tdb_0] + 0)));
assert(0 == (Tfb_0 ^ (memory[Tcb_0] + 0)));
assert(0 == (Tdb_0 - (Teb_0 + fact_7)));
assert(0 == (Teb_0 ^ (i_9 - 1)));
assert(0 == (Tcb_0 - (i_9 + fact_7)));
Lfactor_11_fw(_arg0, _arg1, Ty_0, fact_7, i_9, try_8, num_9); 
}

//27:         fact[i] ^= num
void Lfactor_10_fw(int *_arg0, int *_arg1, int Tx_2, int fact_8, int i_10, int num_10, int try_9){
assert(0 == (Tx_2 ^ (num_10 != 1)));
//29:         num -= 1
int num_11 = num_10 - (1 + 0);
__check_index(fact_8, 20, 0 ^ (fact_8 + i_10));int Thb_0 = 0 ^ (fact_8 + i_10);
int Tjb_0 = 0 ^ (i_10 - 1);
__check_index(fact_8, 20, 0 ^ (fact_8 + Tjb_0));int Tib_0 = 0 ^ (fact_8 + Tjb_0);
int Tkb_0 = 0 ^ (memory[Thb_0] + 0);
int Tlb_0 = 0 ^ (memory[Tib_0] + 0);
int Ty_1 = 0 ^ (Tkb_0 != Tlb_0);
assert(0 == (Tlb_0 ^ (memory[Tib_0] + 0)));
assert(0 == (Tkb_0 ^ (memory[Thb_0] + 0)));
assert(0 == (Tib_0 - (Tjb_0 + fact_8)));
assert(0 == (Tjb_0 ^ (i_10 - 1)));
assert(0 == (Thb_0 - (i_10 + fact_8)));
Lfactor_12_fw(_arg0, _arg1, Ty_1, fact_8, i_10, try_9, num_11); 
}

//29:         num -= 1
void Lfactor_11_fw(int *_arg0, int *_arg1, int Ty_2, int fact_9, int i_11, int try_10, int num_12){
assert(0 != ((Ty_2 != 0)));
__check_index(fact_9, 20, 0 ^ (fact_9 + i_11));int Tmb_0 = 0 ^ (fact_9 + i_11);
int Tob_0 = 0 ^ (i_11 - 1);
__check_index(fact_9, 20, 0 ^ (fact_9 + Tob_0));int Tnb_0 = 0 ^ (fact_9 + Tob_0);
int Tpb_0 = 0 ^ (memory[Tmb_0] + 0);
int Tqb_0 = 0 ^ (memory[Tnb_0] + 0);
assert(0 == (Ty_2 ^ (Tpb_0 != Tqb_0)));
assert(0 == (Tqb_0 ^ (memory[Tnb_0] + 0)));
assert(0 == (Tpb_0 ^ (memory[Tmb_0] + 0)));
assert(0 == (Tnb_0 - (Tob_0 + fact_9)));
assert(0 == (Tob_0 ^ (i_11 - 1)));
assert(0 == (Tmb_0 - (i_11 + fact_9)));
//32:     if (fact[i-1] * fact[i-1]) < fact[i] then  // Zero try
int Tub_0 = 0 ^ (i_11 - 1);
__check_index(fact_9, 20, 0 ^ (fact_9 + Tub_0));int Ttb_0 = 0 ^ (fact_9 + Tub_0);
int Twb_0 = 0 ^ (i_11 - 1);
__check_index(fact_9, 20, 0 ^ (fact_9 + Twb_0));int Tvb_0 = 0 ^ (fact_9 + Twb_0);
int Txb_0 = 0 ^ (memory[Ttb_0] + 0);
int Tyb_0 = 0 ^ (memory[Tvb_0] + 0);
__check_index(fact_9, 20, 0 ^ (fact_9 + i_11));int Tzb_0 = 0 ^ (fact_9 + i_11);
int Tac_0 = 0 ^ (Txb_0 * Tyb_0);
int Tbc_0 = 0 ^ (memory[Tzb_0] + 0);
int Trb_0 = 0 ^ (Tac_0 < Tbc_0);
assert(0 == (Tbc_0 ^ (memory[Tzb_0] + 0)));
assert(0 == (Tac_0 ^ (Txb_0 * Tyb_0)));
assert(0 == (Tzb_0 - (i_11 + fact_9)));
assert(0 == (Tyb_0 ^ (memory[Tvb_0] + 0)));
assert(0 == (Txb_0 ^ (memory[Ttb_0] + 0)));
assert(0 == (Tvb_0 - (Twb_0 + fact_9)));
assert(0 == (Twb_0 ^ (i_11 - 1)));
assert(0 == (Ttb_0 - (Tub_0 + fact_9)));
assert(0 == (Tub_0 ^ (i_11 - 1)));
if ((Trb_0 != 0)) Lfactor_13_fw(_arg0, _arg1, Trb_0, fact_9, try_10, i_11, num_12); else Lfactor_14_fw(_arg0, _arg1, Trb_0, fact_9, try_10, i_11, num_12); 
}

//29:         num -= 1
void Lfactor_12_fw(int *_arg0, int *_arg1, int Ty_2, int fact_9, int i_11, int try_10, int num_12){
assert(0 == ((Ty_2 != 0)));
__check_index(fact_9, 20, 0 ^ (fact_9 + i_11));int Tmb_0 = 0 ^ (fact_9 + i_11);
int Tob_0 = 0 ^ (i_11 - 1);
__check_index(fact_9, 20, 0 ^ (fact_9 + Tob_0));int Tnb_0 = 0 ^ (fact_9 + Tob_0);
int Tpb_0 = 0 ^ (memory[Tmb_0] + 0);
int Tqb_0 = 0 ^ (memory[Tnb_0] + 0);
assert(0 == (Ty_2 ^ (Tpb_0 != Tqb_0)));
assert(0 == (Tqb_0 ^ (memory[Tnb_0] + 0)));
assert(0 == (Tpb_0 ^ (memory[Tmb_0] + 0)));
assert(0 == (Tnb_0 - (Tob_0 + fact_9)));
assert(0 == (Tob_0 ^ (i_11 - 1)));
assert(0 == (Tmb_0 - (i_11 + fact_9)));
//32:     if (fact[i-1] * fact[i-1]) < fact[i] then  // Zero try
int Tub_0 = 0 ^ (i_11 - 1);
__check_index(fact_9, 20, 0 ^ (fact_9 + Tub_0));int Ttb_0 = 0 ^ (fact_9 + Tub_0);
int Twb_0 = 0 ^ (i_11 - 1);
__check_index(fact_9, 20, 0 ^ (fact_9 + Twb_0));int Tvb_0 = 0 ^ (fact_9 + Twb_0);
int Txb_0 = 0 ^ (memory[Ttb_0] + 0);
int Tyb_0 = 0 ^ (memory[Tvb_0] + 0);
__check_index(fact_9, 20, 0 ^ (fact_9 + i_11));int Tzb_0 = 0 ^ (fact_9 + i_11);
int Tac_0 = 0 ^ (Txb_0 * Tyb_0);
int Tbc_0 = 0 ^ (memory[Tzb_0] + 0);
int Trb_0 = 0 ^ (Tac_0 < Tbc_0);
assert(0 == (Tbc_0 ^ (memory[Tzb_0] + 0)));
assert(0 == (Tac_0 ^ (Txb_0 * Tyb_0)));
assert(0 == (Tzb_0 - (i_11 + fact_9)));
assert(0 == (Tyb_0 ^ (memory[Tvb_0] + 0)));
assert(0 == (Txb_0 ^ (memory[Ttb_0] + 0)));
assert(0 == (Tvb_0 - (Twb_0 + fact_9)));
assert(0 == (Twb_0 ^ (i_11 - 1)));
assert(0 == (Ttb_0 - (Tub_0 + fact_9)));
assert(0 == (Tub_0 ^ (i_11 - 1)));
if ((Trb_0 != 0)) Lfactor_13_fw(_arg0, _arg1, Trb_0, fact_9, try_10, i_11, num_12); else Lfactor_14_fw(_arg0, _arg1, Trb_0, fact_9, try_10, i_11, num_12); 
}

//32:     if (fact[i-1] * fact[i-1]) < fact[i] then  // Zero try
void Lfactor_13_fw(int *_arg0, int *_arg1, int Trb_1, int fact_10, int try_11, int i_12, int num_13){
int Tdc_0 = 0 ^ (i_12 - 1);
__check_index(fact_10, 20, 0 ^ (fact_10 + Tdc_0));int Tcc_0 = 0 ^ (fact_10 + Tdc_0);
int Tfc_0 = 0 ^ (i_12 - 1);
__check_index(fact_10, 20, 0 ^ (fact_10 + Tfc_0));int Tec_0 = 0 ^ (fact_10 + Tfc_0);
int Tgc_0 = 0 ^ (memory[Tcc_0] + 0);
int Thc_0 = 0 ^ (memory[Tec_0] + 0);
__check_index(fact_10, 20, 0 ^ (fact_10 + i_12));int Tic_0 = 0 ^ (fact_10 + i_12);
int Tjc_0 = 0 ^ (Tgc_0 * Thc_0);
int Tkc_0 = 0 ^ (memory[Tic_0] + 0);
assert(0 == (Trb_1 ^ (Tjc_0 < Tkc_0)));
assert(0 == (Tkc_0 ^ (memory[Tic_0] + 0)));
assert(0 == (Tjc_0 ^ (Tgc_0 * Thc_0)));
assert(0 == (Tic_0 - (i_12 + fact_10)));
assert(0 == (Thc_0 ^ (memory[Tec_0] + 0)));
assert(0 == (Tgc_0 ^ (memory[Tcc_0] + 0)));
assert(0 == (Tec_0 - (Tfc_0 + fact_10)));
assert(0 == (Tfc_0 ^ (i_12 - 1)));
assert(0 == (Tcc_0 - (Tdc_0 + fact_10)));
assert(0 == (Tdc_0 ^ (i_12 - 1)));
//33:         from (try * try) > fact[i] loop
__check_index(fact_10, 20, 0 ^ (fact_10 + i_12));int Tnc_0 = 0 ^ (fact_10 + i_12);
int Toc_0 = 0 ^ (try_11 * try_11);
int Tpc_0 = 0 ^ (memory[Tnc_0] + 0);
int Tlc_0 = 0 ^ (Toc_0 > Tpc_0);
assert(0 == (Tpc_0 ^ (memory[Tnc_0] + 0)));
assert(0 == (Toc_0 ^ (try_11 * try_11)));
assert(0 == (Tnc_0 - (i_12 + fact_10)));
Lfactor_17_fw(_arg0, _arg1, Tlc_0, fact_10, try_11, i_12, num_13); 
}

//33:         from (try * try) > fact[i] loop
void Lfactor_17_fw(int *_arg0, int *_arg1, int Tlc_1, int fact_11, int try_12, int i_13, int num_14){
assert(0 != ((Tlc_1 != 0)));
__check_index(fact_11, 20, 0 ^ (fact_11 + i_13));int Tqc_0 = 0 ^ (fact_11 + i_13);
int Trc_0 = 0 ^ (try_12 * try_12);
int Tsc_0 = 0 ^ (memory[Tqc_0] + 0);
assert(0 == (Tlc_1 ^ (Trc_0 > Tsc_0)));
assert(0 == (Tsc_0 ^ (memory[Tqc_0] + 0)));
assert(0 == (Trc_0 ^ (try_12 * try_12)));
assert(0 == (Tqc_0 - (i_13 + fact_11)));
//33:         from (try * try) > fact[i] loop
int Tmc_0 = 0 ^ (try_12 == 0);
if ((Tmc_0 != 0)) Lfactor_20_fw(_arg0, _arg1, Tmc_0, fact_11, try_12, i_13, num_14); else Lfactor_18_fw(_arg0, _arg1, Tmc_0, fact_11, try_12, i_13, num_14); 
}

//33:         from (try * try) > fact[i] loop
void Lfactor_19_fw(int *_arg0, int *_arg1, int Tlc_1, int fact_11, int try_12, int i_13, int num_14){
assert(0 == ((Tlc_1 != 0)));
__check_index(fact_11, 20, 0 ^ (fact_11 + i_13));int Tqc_0 = 0 ^ (fact_11 + i_13);
int Trc_0 = 0 ^ (try_12 * try_12);
int Tsc_0 = 0 ^ (memory[Tqc_0] + 0);
assert(0 == (Tlc_1 ^ (Trc_0 > Tsc_0)));
assert(0 == (Tsc_0 ^ (memory[Tqc_0] + 0)));
assert(0 == (Trc_0 ^ (try_12 * try_12)));
assert(0 == (Tqc_0 - (i_13 + fact_11)));
//33:         from (try * try) > fact[i] loop
int Tmc_0 = 0 ^ (try_12 == 0);
if ((Tmc_0 != 0)) Lfactor_20_fw(_arg0, _arg1, Tmc_0, fact_11, try_12, i_13, num_14); else Lfactor_18_fw(_arg0, _arg1, Tmc_0, fact_11, try_12, i_13, num_14); 
}

//33:         from (try * try) > fact[i] loop
void Lfactor_18_fw(int *_arg0, int *_arg1, int Tmc_1, int fact_12, int try_13, int i_14, int num_15){
assert(0 == (Tmc_1 ^ (try_13 == 0)));
//34:             uncall nexttry(try)
int _tmp1 = try_13;
nexttry_bw(&_tmp1);
int try_14 = _tmp1;
__check_index(fact_12, 20, 0 ^ (fact_12 + i_14));int Ttc_0 = 0 ^ (fact_12 + i_14);
int Tuc_0 = 0 ^ (try_14 * try_14);
int Tvc_0 = 0 ^ (memory[Ttc_0] + 0);
int Tlc_2 = 0 ^ (Tuc_0 > Tvc_0);
assert(0 == (Tvc_0 ^ (memory[Ttc_0] + 0)));
assert(0 == (Tuc_0 ^ (try_14 * try_14)));
assert(0 == (Ttc_0 - (i_14 + fact_12)));
Lfactor_19_fw(_arg0, _arg1, Tlc_2, fact_12, try_14, i_14, num_15); 
}

//34:             uncall nexttry(try)
void Lfactor_20_fw(int *_arg0, int *_arg1, int Tmc_2, int fact_13, int try_15, int i_15, int num_16){
assert(0 == (Tmc_2 ^ (try_15 == 0)));
int Txc_0 = 0 ^ (i_15 - 1);
__check_index(fact_13, 20, 0 ^ (fact_13 + Txc_0));int Twc_0 = 0 ^ (fact_13 + Txc_0);
int Tzc_0 = 0 ^ (i_15 - 1);
__check_index(fact_13, 20, 0 ^ (fact_13 + Tzc_0));int Tyc_0 = 0 ^ (fact_13 + Tzc_0);
int Tad_0 = 0 ^ (memory[Twc_0] + 0);
int Tbd_0 = 0 ^ (memory[Tyc_0] + 0);
__check_index(fact_13, 20, 0 ^ (fact_13 + i_15));int Tcd_0 = 0 ^ (fact_13 + i_15);
int Tdd_0 = 0 ^ (Tad_0 * Tbd_0);
int Ted_0 = 0 ^ (memory[Tcd_0] + 0);
int Tsb_0 = 0 ^ (Tdd_0 < Ted_0);
assert(0 == (Ted_0 ^ (memory[Tcd_0] + 0)));
assert(0 == (Tdd_0 ^ (Tad_0 * Tbd_0)));
assert(0 == (Tcd_0 - (i_15 + fact_13)));
assert(0 == (Tbd_0 ^ (memory[Tyc_0] + 0)));
assert(0 == (Tad_0 ^ (memory[Twc_0] + 0)));
assert(0 == (Tyc_0 - (Tzc_0 + fact_13)));
assert(0 == (Tzc_0 ^ (i_15 - 1)));
assert(0 == (Twc_0 - (Txc_0 + fact_13)));
assert(0 == (Txc_0 ^ (i_15 - 1)));
Lfactor_15_fw(_arg0, _arg1, Tsb_0, fact_13, try_15, i_15, num_16); 
}

//34:             uncall nexttry(try)
void Lfactor_14_fw(int *_arg0, int *_arg1, int Trb_2, int fact_14, int try_16, int i_16, int num_17){
int Tgd_0 = 0 ^ (i_16 - 1);
__check_index(fact_14, 20, 0 ^ (fact_14 + Tgd_0));int Tfd_0 = 0 ^ (fact_14 + Tgd_0);
int Tid_0 = 0 ^ (i_16 - 1);
__check_index(fact_14, 20, 0 ^ (fact_14 + Tid_0));int Thd_0 = 0 ^ (fact_14 + Tid_0);
int Tjd_0 = 0 ^ (memory[Tfd_0] + 0);
int Tkd_0 = 0 ^ (memory[Thd_0] + 0);
__check_index(fact_14, 20, 0 ^ (fact_14 + i_16));int Tld_0 = 0 ^ (fact_14 + i_16);
int Tmd_0 = 0 ^ (Tjd_0 * Tkd_0);
int Tnd_0 = 0 ^ (memory[Tld_0] + 0);
assert(0 == (Trb_2 ^ (Tmd_0 < Tnd_0)));
assert(0 == (Tnd_0 ^ (memory[Tld_0] + 0)));
assert(0 == (Tmd_0 ^ (Tjd_0 * Tkd_0)));
assert(0 == (Tld_0 - (i_16 + fact_14)));
assert(0 == (Tkd_0 ^ (memory[Thd_0] + 0)));
assert(0 == (Tjd_0 ^ (memory[Tfd_0] + 0)));
assert(0 == (Thd_0 - (Tid_0 + fact_14)));
assert(0 == (Tid_0 ^ (i_16 - 1)));
assert(0 == (Tfd_0 - (Tgd_0 + fact_14)));
assert(0 == (Tgd_0 ^ (i_16 - 1)));
//37:         try -= fact[i-1]
int Tpd_0 = 0 ^ (i_16 - 1);
__check_index(fact_14, 20, 0 ^ (fact_14 + Tpd_0));int Tod_0 = 0 ^ (fact_14 + Tpd_0);
int try_17 = try_16 - (memory[Tod_0] + 0);
assert(0 == (Tod_0 - (Tpd_0 + fact_14)));
assert(0 == (Tpd_0 ^ (i_16 - 1)));
int Trd_0 = 0 ^ (i_16 - 1);
__check_index(fact_14, 20, 0 ^ (fact_14 + Trd_0));int Tqd_0 = 0 ^ (fact_14 + Trd_0);
int Ttd_0 = 0 ^ (i_16 - 1);
__check_index(fact_14, 20, 0 ^ (fact_14 + Ttd_0));int Tsd_0 = 0 ^ (fact_14 + Ttd_0);
int Tud_0 = 0 ^ (memory[Tqd_0] + 0);
int Tvd_0 = 0 ^ (memory[Tsd_0] + 0);
__check_index(fact_14, 20, 0 ^ (fact_14 + i_16));int Twd_0 = 0 ^ (fact_14 + i_16);
int Txd_0 = 0 ^ (Tud_0 * Tvd_0);
int Tyd_0 = 0 ^ (memory[Twd_0] + 0);
int Tsb_1 = 0 ^ (Txd_0 < Tyd_0);
assert(0 == (Tyd_0 ^ (memory[Twd_0] + 0)));
assert(0 == (Txd_0 ^ (Tud_0 * Tvd_0)));
assert(0 == (Twd_0 - (i_16 + fact_14)));
assert(0 == (Tvd_0 ^ (memory[Tsd_0] + 0)));
assert(0 == (Tud_0 ^ (memory[Tqd_0] + 0)));
assert(0 == (Tsd_0 - (Ttd_0 + fact_14)));
assert(0 == (Ttd_0 ^ (i_16 - 1)));
assert(0 == (Tqd_0 - (Trd_0 + fact_14)));
assert(0 == (Trd_0 ^ (i_16 - 1)));
Lfactor_16_fw(_arg0, _arg1, Tsb_1, fact_14, try_17, i_16, num_17); 
}

//37:         try -= fact[i-1]
void Lfactor_15_fw(int *_arg0, int *_arg1, int Tsb_2, int fact_15, int try_18, int i_17, int num_18){
assert(0 != ((Tsb_2 != 0)));
int Tae_0 = 0 ^ (i_17 - 1);
__check_index(fact_15, 20, 0 ^ (fact_15 + Tae_0));int Tzd_0 = 0 ^ (fact_15 + Tae_0);
int Tce_0 = 0 ^ (i_17 - 1);
__check_index(fact_15, 20, 0 ^ (fact_15 + Tce_0));int Tbe_0 = 0 ^ (fact_15 + Tce_0);
int Tde_0 = 0 ^ (memory[Tzd_0] + 0);
int Tee_0 = 0 ^ (memory[Tbe_0] + 0);
__check_index(fact_15, 20, 0 ^ (fact_15 + i_17));int Tfe_0 = 0 ^ (fact_15 + i_17);
int Tge_0 = 0 ^ (Tde_0 * Tee_0);
int The_0 = 0 ^ (memory[Tfe_0] + 0);
assert(0 == (Tsb_2 ^ (Tge_0 < The_0)));
assert(0 == (The_0 ^ (memory[Tfe_0] + 0)));
assert(0 == (Tge_0 ^ (Tde_0 * Tee_0)));
assert(0 == (Tfe_0 - (i_17 + fact_15)));
assert(0 == (Tee_0 ^ (memory[Tbe_0] + 0)));
assert(0 == (Tde_0 ^ (memory[Tzd_0] + 0)));
assert(0 == (Tbe_0 - (Tce_0 + fact_15)));
assert(0 == (Tce_0 ^ (i_17 - 1)));
assert(0 == (Tzd_0 - (Tae_0 + fact_15)));
assert(0 == (Tae_0 ^ (i_17 - 1)));
//40:     call zeroi(i, fact)                        // Zero i
int _tmp1 = i_17;
int _tmp2 = fact_15;
zeroi_fw(&_tmp1, &_tmp2);
int i_18 = _tmp1;
int fact_16 = _tmp2;
assert(0 == (i_18 - (0 + 0)));
assert(0 == (try_18 - (0 + 0)));
//9: procedure factor(int num, int fact[20])
*_arg0 = num_18;
*_arg1 = fact_16;
}

//37:         try -= fact[i-1]
void Lfactor_16_fw(int *_arg0, int *_arg1, int Tsb_2, int fact_15, int try_18, int i_17, int num_18){
assert(0 == ((Tsb_2 != 0)));
int Tae_0 = 0 ^ (i_17 - 1);
__check_index(fact_15, 20, 0 ^ (fact_15 + Tae_0));int Tzd_0 = 0 ^ (fact_15 + Tae_0);
int Tce_0 = 0 ^ (i_17 - 1);
__check_index(fact_15, 20, 0 ^ (fact_15 + Tce_0));int Tbe_0 = 0 ^ (fact_15 + Tce_0);
int Tde_0 = 0 ^ (memory[Tzd_0] + 0);
int Tee_0 = 0 ^ (memory[Tbe_0] + 0);
__check_index(fact_15, 20, 0 ^ (fact_15 + i_17));int Tfe_0 = 0 ^ (fact_15 + i_17);
int Tge_0 = 0 ^ (Tde_0 * Tee_0);
int The_0 = 0 ^ (memory[Tfe_0] + 0);
assert(0 == (Tsb_2 ^ (Tge_0 < The_0)));
assert(0 == (The_0 ^ (memory[Tfe_0] + 0)));
assert(0 == (Tge_0 ^ (Tde_0 * Tee_0)));
assert(0 == (Tfe_0 - (i_17 + fact_15)));
assert(0 == (Tee_0 ^ (memory[Tbe_0] + 0)));
assert(0 == (Tde_0 ^ (memory[Tzd_0] + 0)));
assert(0 == (Tbe_0 - (Tce_0 + fact_15)));
assert(0 == (Tce_0 ^ (i_17 - 1)));
assert(0 == (Tzd_0 - (Tae_0 + fact_15)));
assert(0 == (Tae_0 ^ (i_17 - 1)));
//40:     call zeroi(i, fact)                        // Zero i
int _tmp1 = i_17;
int _tmp2 = fact_15;
zeroi_fw(&_tmp1, &_tmp2);
int i_18 = _tmp1;
int fact_16 = _tmp2;
assert(0 == (i_18 - (0 + 0)));
assert(0 == (try_18 - (0 + 0)));
//9: procedure factor(int num, int fact[20])
*_arg0 = num_18;
*_arg1 = fact_16;
}

//9: procedure factor(int num, int fact[20])
void factor_bw(int *_arg0, int *_arg1){
int num_18 = *_arg0;
int fact_16 = *_arg1;
//40:     call zeroi(i, fact)                        // Zero i
int try_18 = 0 + (0 + 0);
int i_18 = 0 + (0 + 0);
int _tmp1 = i_18;
int _tmp2 = fact_16;
zeroi_fw(&_tmp1, &_tmp2);
int i_17 = _tmp1;
int fact_15 = _tmp2;
//37:         try -= fact[i-1]
int Tae_0 = 0 ^ (i_17 - 1);
int Tzd_0 = 0 + (Tae_0 + fact_15);
int Tce_0 = 0 ^ (i_17 - 1);
int Tbe_0 = 0 + (Tce_0 + fact_15);
int Tde_0 = 0 ^ (memory[Tzd_0] + 0);
int Tee_0 = 0 ^ (memory[Tbe_0] + 0);
int Tfe_0 = 0 + (i_17 + fact_15);
int Tge_0 = 0 ^ (Tde_0 * Tee_0);
int The_0 = 0 ^ (memory[Tfe_0] + 0);
int Tsb_2 = 0 ^ (Tge_0 < The_0);
assert(0 == (The_0 ^ (memory[Tfe_0] + 0)));
assert(0 == (Tge_0 ^ (Tde_0 * Tee_0)));
assert(0 == (Tfe_0 ^ (fact_15 + i_17)));
assert(0 == (Tee_0 ^ (memory[Tbe_0] + 0)));
assert(0 == (Tde_0 ^ (memory[Tzd_0] + 0)));
assert(0 == (Tbe_0 ^ (fact_15 + Tce_0)));
assert(0 == (Tce_0 ^ (i_17 - 1)));
assert(0 == (Tzd_0 ^ (fact_15 + Tae_0)));
assert(0 == (Tae_0 ^ (i_17 - 1)));
if ((Tsb_2 != 0)) Lfactor_15_bw(_arg0, _arg1, Tsb_2, fact_15, try_18, i_17, num_18); else Lfactor_16_bw(_arg0, _arg1, Tsb_2, fact_15, try_18, i_17, num_18); 
}

//37:         try -= fact[i-1]
void Lfactor_16_bw(int *_arg0, int *_arg1, int Tsb_1, int fact_14, int try_17, int i_16, int num_17){
int Trd_0 = 0 ^ (i_16 - 1);
int Tqd_0 = 0 + (Trd_0 + fact_14);
int Ttd_0 = 0 ^ (i_16 - 1);
int Tsd_0 = 0 + (Ttd_0 + fact_14);
int Tud_0 = 0 ^ (memory[Tqd_0] + 0);
int Tvd_0 = 0 ^ (memory[Tsd_0] + 0);
int Twd_0 = 0 + (i_16 + fact_14);
int Txd_0 = 0 ^ (Tud_0 * Tvd_0);
int Tyd_0 = 0 ^ (memory[Twd_0] + 0);
assert(0 == (Tsb_1 ^ (Txd_0 < Tyd_0)));
assert(0 == (Tyd_0 ^ (memory[Twd_0] + 0)));
assert(0 == (Txd_0 ^ (Tud_0 * Tvd_0)));
assert(0 == (Twd_0 ^ (fact_14 + i_16)));
assert(0 == (Tvd_0 ^ (memory[Tsd_0] + 0)));
assert(0 == (Tud_0 ^ (memory[Tqd_0] + 0)));
assert(0 == (Tsd_0 ^ (fact_14 + Ttd_0)));
assert(0 == (Ttd_0 ^ (i_16 - 1)));
assert(0 == (Tqd_0 ^ (fact_14 + Trd_0)));
assert(0 == (Trd_0 ^ (i_16 - 1)));
int Tpd_0 = 0 ^ (i_16 - 1);
int Tod_0 = 0 + (Tpd_0 + fact_14);
int try_16 = try_17 + (memory[Tod_0] + 0);
assert(0 == (Tod_0 ^ (fact_14 + Tpd_0)));
assert(0 == (Tpd_0 ^ (i_16 - 1)));
//34:             uncall nexttry(try)
int Tgd_0 = 0 ^ (i_16 - 1);
int Tfd_0 = 0 + (Tgd_0 + fact_14);
int Tid_0 = 0 ^ (i_16 - 1);
int Thd_0 = 0 + (Tid_0 + fact_14);
int Tjd_0 = 0 ^ (memory[Tfd_0] + 0);
int Tkd_0 = 0 ^ (memory[Thd_0] + 0);
int Tld_0 = 0 + (i_16 + fact_14);
int Tmd_0 = 0 ^ (Tjd_0 * Tkd_0);
int Tnd_0 = 0 ^ (memory[Tld_0] + 0);
int Trb_2 = 0 ^ (Tmd_0 < Tnd_0);
assert(0 == (Tnd_0 ^ (memory[Tld_0] + 0)));
assert(0 == (Tmd_0 ^ (Tjd_0 * Tkd_0)));
assert(0 == (Tld_0 ^ (fact_14 + i_16)));
assert(0 == (Tkd_0 ^ (memory[Thd_0] + 0)));
assert(0 == (Tjd_0 ^ (memory[Tfd_0] + 0)));
assert(0 == (Thd_0 ^ (fact_14 + Tid_0)));
assert(0 == (Tid_0 ^ (i_16 - 1)));
assert(0 == (Tfd_0 ^ (fact_14 + Tgd_0)));
assert(0 == (Tgd_0 ^ (i_16 - 1)));
Lfactor_14_bw(_arg0, _arg1, Trb_2, fact_14, try_16, i_16, num_17); 
}

//34:             uncall nexttry(try)
void Lfactor_15_bw(int *_arg0, int *_arg1, int Tsb_0, int fact_13, int try_15, int i_15, int num_16){
int Txc_0 = 0 ^ (i_15 - 1);
int Twc_0 = 0 + (Txc_0 + fact_13);
int Tzc_0 = 0 ^ (i_15 - 1);
int Tyc_0 = 0 + (Tzc_0 + fact_13);
int Tad_0 = 0 ^ (memory[Twc_0] + 0);
int Tbd_0 = 0 ^ (memory[Tyc_0] + 0);
int Tcd_0 = 0 + (i_15 + fact_13);
int Tdd_0 = 0 ^ (Tad_0 * Tbd_0);
int Ted_0 = 0 ^ (memory[Tcd_0] + 0);
assert(0 == (Tsb_0 ^ (Tdd_0 < Ted_0)));
assert(0 == (Ted_0 ^ (memory[Tcd_0] + 0)));
assert(0 == (Tdd_0 ^ (Tad_0 * Tbd_0)));
assert(0 == (Tcd_0 ^ (fact_13 + i_15)));
assert(0 == (Tbd_0 ^ (memory[Tyc_0] + 0)));
assert(0 == (Tad_0 ^ (memory[Twc_0] + 0)));
assert(0 == (Tyc_0 ^ (fact_13 + Tzc_0)));
assert(0 == (Tzc_0 ^ (i_15 - 1)));
assert(0 == (Twc_0 ^ (fact_13 + Txc_0)));
assert(0 == (Txc_0 ^ (i_15 - 1)));
int Tmc_2 = 0 ^ (try_15 == 0);
Lfactor_20_bw(_arg0, _arg1, Tmc_2, fact_13, try_15, i_15, num_16); 
}

//34:             uncall nexttry(try)
void Lfactor_19_bw(int *_arg0, int *_arg1, int Tlc_2, int fact_12, int try_14, int i_14, int num_15){
int Ttc_0 = 0 + (i_14 + fact_12);
int Tuc_0 = 0 ^ (try_14 * try_14);
int Tvc_0 = 0 ^ (memory[Ttc_0] + 0);
assert(0 == (Tlc_2 ^ (Tuc_0 > Tvc_0)));
assert(0 == (Tvc_0 ^ (memory[Ttc_0] + 0)));
assert(0 == (Tuc_0 ^ (try_14 * try_14)));
assert(0 == (Ttc_0 ^ (fact_12 + i_14)));
int _tmp1 = try_14;
nexttry_bw(&_tmp1);
int try_13 = _tmp1;
//33:         from (try * try) > fact[i] loop
int Tmc_1 = 0 ^ (try_13 == 0);
Lfactor_18_bw(_arg0, _arg1, Tmc_1, fact_12, try_13, i_14, num_15); 
}

//33:         from (try * try) > fact[i] loop
void Lfactor_20_bw(int *_arg0, int *_arg1, int Tmc_0, int fact_11, int try_12, int i_13, int num_14){
assert(0 != ((Tmc_0 != 0)));
assert(0 == (Tmc_0 ^ (try_12 == 0)));
//33:         from (try * try) > fact[i] loop
int Tqc_0 = 0 + (i_13 + fact_11);
int Trc_0 = 0 ^ (try_12 * try_12);
int Tsc_0 = 0 ^ (memory[Tqc_0] + 0);
int Tlc_1 = 0 ^ (Trc_0 > Tsc_0);
assert(0 == (Tsc_0 ^ (memory[Tqc_0] + 0)));
assert(0 == (Trc_0 ^ (try_12 * try_12)));
assert(0 == (Tqc_0 ^ (fact_11 + i_13)));
if ((Tlc_1 != 0)) Lfactor_17_bw(_arg0, _arg1, Tlc_1, fact_11, try_12, i_13, num_14); else Lfactor_19_bw(_arg0, _arg1, Tlc_1, fact_11, try_12, i_13, num_14); 
}

//33:         from (try * try) > fact[i] loop
void Lfactor_18_bw(int *_arg0, int *_arg1, int Tmc_0, int fact_11, int try_12, int i_13, int num_14){
assert(0 == ((Tmc_0 != 0)));
assert(0 == (Tmc_0 ^ (try_12 == 0)));
//33:         from (try * try) > fact[i] loop
int Tqc_0 = 0 + (i_13 + fact_11);
int Trc_0 = 0 ^ (try_12 * try_12);
int Tsc_0 = 0 ^ (memory[Tqc_0] + 0);
int Tlc_1 = 0 ^ (Trc_0 > Tsc_0);
assert(0 == (Tsc_0 ^ (memory[Tqc_0] + 0)));
assert(0 == (Trc_0 ^ (try_12 * try_12)));
assert(0 == (Tqc_0 ^ (fact_11 + i_13)));
if ((Tlc_1 != 0)) Lfactor_17_bw(_arg0, _arg1, Tlc_1, fact_11, try_12, i_13, num_14); else Lfactor_19_bw(_arg0, _arg1, Tlc_1, fact_11, try_12, i_13, num_14); 
}

//33:         from (try * try) > fact[i] loop
void Lfactor_17_bw(int *_arg0, int *_arg1, int Tlc_0, int fact_10, int try_11, int i_12, int num_13){
int Tnc_0 = 0 + (i_12 + fact_10);
int Toc_0 = 0 ^ (try_11 * try_11);
int Tpc_0 = 0 ^ (memory[Tnc_0] + 0);
assert(0 == (Tlc_0 ^ (Toc_0 > Tpc_0)));
assert(0 == (Tpc_0 ^ (memory[Tnc_0] + 0)));
assert(0 == (Toc_0 ^ (try_11 * try_11)));
assert(0 == (Tnc_0 ^ (fact_10 + i_12)));
//32:     if (fact[i-1] * fact[i-1]) < fact[i] then  // Zero try
int Tdc_0 = 0 ^ (i_12 - 1);
int Tcc_0 = 0 + (Tdc_0 + fact_10);
int Tfc_0 = 0 ^ (i_12 - 1);
int Tec_0 = 0 + (Tfc_0 + fact_10);
int Tgc_0 = 0 ^ (memory[Tcc_0] + 0);
int Thc_0 = 0 ^ (memory[Tec_0] + 0);
int Tic_0 = 0 + (i_12 + fact_10);
int Tjc_0 = 0 ^ (Tgc_0 * Thc_0);
int Tkc_0 = 0 ^ (memory[Tic_0] + 0);
int Trb_1 = 0 ^ (Tjc_0 < Tkc_0);
assert(0 == (Tkc_0 ^ (memory[Tic_0] + 0)));
assert(0 == (Tjc_0 ^ (Tgc_0 * Thc_0)));
assert(0 == (Tic_0 ^ (fact_10 + i_12)));
assert(0 == (Thc_0 ^ (memory[Tec_0] + 0)));
assert(0 == (Tgc_0 ^ (memory[Tcc_0] + 0)));
assert(0 == (Tec_0 ^ (fact_10 + Tfc_0)));
assert(0 == (Tfc_0 ^ (i_12 - 1)));
assert(0 == (Tcc_0 ^ (fact_10 + Tdc_0)));
assert(0 == (Tdc_0 ^ (i_12 - 1)));
Lfactor_13_bw(_arg0, _arg1, Trb_1, fact_10, try_11, i_12, num_13); 
}

//32:     if (fact[i-1] * fact[i-1]) < fact[i] then  // Zero try
void Lfactor_13_bw(int *_arg0, int *_arg1, int Trb_0, int fact_9, int try_10, int i_11, int num_12){
assert(0 != ((Trb_0 != 0)));
int Tub_0 = 0 ^ (i_11 - 1);
int Ttb_0 = 0 + (Tub_0 + fact_9);
int Twb_0 = 0 ^ (i_11 - 1);
int Tvb_0 = 0 + (Twb_0 + fact_9);
int Txb_0 = 0 ^ (memory[Ttb_0] + 0);
int Tyb_0 = 0 ^ (memory[Tvb_0] + 0);
int Tzb_0 = 0 + (i_11 + fact_9);
int Tac_0 = 0 ^ (Txb_0 * Tyb_0);
int Tbc_0 = 0 ^ (memory[Tzb_0] + 0);
assert(0 == (Trb_0 ^ (Tac_0 < Tbc_0)));
assert(0 == (Tbc_0 ^ (memory[Tzb_0] + 0)));
assert(0 == (Tac_0 ^ (Txb_0 * Tyb_0)));
assert(0 == (Tzb_0 ^ (fact_9 + i_11)));
assert(0 == (Tyb_0 ^ (memory[Tvb_0] + 0)));
assert(0 == (Txb_0 ^ (memory[Ttb_0] + 0)));
assert(0 == (Tvb_0 ^ (fact_9 + Twb_0)));
assert(0 == (Twb_0 ^ (i_11 - 1)));
assert(0 == (Ttb_0 ^ (fact_9 + Tub_0)));
assert(0 == (Tub_0 ^ (i_11 - 1)));
//29:         num -= 1
int Tmb_0 = 0 + (i_11 + fact_9);
int Tob_0 = 0 ^ (i_11 - 1);
int Tnb_0 = 0 + (Tob_0 + fact_9);
int Tpb_0 = 0 ^ (memory[Tmb_0] + 0);
int Tqb_0 = 0 ^ (memory[Tnb_0] + 0);
int Ty_2 = 0 ^ (Tpb_0 != Tqb_0);
assert(0 == (Tqb_0 ^ (memory[Tnb_0] + 0)));
assert(0 == (Tpb_0 ^ (memory[Tmb_0] + 0)));
assert(0 == (Tnb_0 ^ (fact_9 + Tob_0)));
assert(0 == (Tob_0 ^ (i_11 - 1)));
assert(0 == (Tmb_0 ^ (fact_9 + i_11)));
if ((Ty_2 != 0)) Lfactor_11_bw(_arg0, _arg1, Ty_2, fact_9, i_11, try_10, num_12); else Lfactor_12_bw(_arg0, _arg1, Ty_2, fact_9, i_11, try_10, num_12); 
}

//32:     if (fact[i-1] * fact[i-1]) < fact[i] then  // Zero try
void Lfactor_14_bw(int *_arg0, int *_arg1, int Trb_0, int fact_9, int try_10, int i_11, int num_12){
assert(0 == ((Trb_0 != 0)));
int Tub_0 = 0 ^ (i_11 - 1);
int Ttb_0 = 0 + (Tub_0 + fact_9);
int Twb_0 = 0 ^ (i_11 - 1);
int Tvb_0 = 0 + (Twb_0 + fact_9);
int Txb_0 = 0 ^ (memory[Ttb_0] + 0);
int Tyb_0 = 0 ^ (memory[Tvb_0] + 0);
int Tzb_0 = 0 + (i_11 + fact_9);
int Tac_0 = 0 ^ (Txb_0 * Tyb_0);
int Tbc_0 = 0 ^ (memory[Tzb_0] + 0);
assert(0 == (Trb_0 ^ (Tac_0 < Tbc_0)));
assert(0 == (Tbc_0 ^ (memory[Tzb_0] + 0)));
assert(0 == (Tac_0 ^ (Txb_0 * Tyb_0)));
assert(0 == (Tzb_0 ^ (fact_9 + i_11)));
assert(0 == (Tyb_0 ^ (memory[Tvb_0] + 0)));
assert(0 == (Txb_0 ^ (memory[Ttb_0] + 0)));
assert(0 == (Tvb_0 ^ (fact_9 + Twb_0)));
assert(0 == (Twb_0 ^ (i_11 - 1)));
assert(0 == (Ttb_0 ^ (fact_9 + Tub_0)));
assert(0 == (Tub_0 ^ (i_11 - 1)));
//29:         num -= 1
int Tmb_0 = 0 + (i_11 + fact_9);
int Tob_0 = 0 ^ (i_11 - 1);
int Tnb_0 = 0 + (Tob_0 + fact_9);
int Tpb_0 = 0 ^ (memory[Tmb_0] + 0);
int Tqb_0 = 0 ^ (memory[Tnb_0] + 0);
int Ty_2 = 0 ^ (Tpb_0 != Tqb_0);
assert(0 == (Tqb_0 ^ (memory[Tnb_0] + 0)));
assert(0 == (Tpb_0 ^ (memory[Tmb_0] + 0)));
assert(0 == (Tnb_0 ^ (fact_9 + Tob_0)));
assert(0 == (Tob_0 ^ (i_11 - 1)));
assert(0 == (Tmb_0 ^ (fact_9 + i_11)));
if ((Ty_2 != 0)) Lfactor_11_bw(_arg0, _arg1, Ty_2, fact_9, i_11, try_10, num_12); else Lfactor_12_bw(_arg0, _arg1, Ty_2, fact_9, i_11, try_10, num_12); 
}

//29:         num -= 1
void Lfactor_12_bw(int *_arg0, int *_arg1, int Ty_1, int fact_8, int i_10, int try_9, int num_11){
int Thb_0 = 0 + (i_10 + fact_8);
int Tjb_0 = 0 ^ (i_10 - 1);
int Tib_0 = 0 + (Tjb_0 + fact_8);
int Tkb_0 = 0 ^ (memory[Thb_0] + 0);
int Tlb_0 = 0 ^ (memory[Tib_0] + 0);
assert(0 == (Ty_1 ^ (Tkb_0 != Tlb_0)));
assert(0 == (Tlb_0 ^ (memory[Tib_0] + 0)));
assert(0 == (Tkb_0 ^ (memory[Thb_0] + 0)));
assert(0 == (Tib_0 ^ (fact_8 + Tjb_0)));
assert(0 == (Tjb_0 ^ (i_10 - 1)));
assert(0 == (Thb_0 ^ (fact_8 + i_10)));
int num_10 = num_11 + (1 + 0);
//27:         fact[i] ^= num
int Tx_2 = 0 ^ (num_10 != 1);
Lfactor_10_bw(_arg0, _arg1, Tx_2, fact_8, i_10, num_10, try_9); 
}

//27:         fact[i] ^= num
void Lfactor_11_bw(int *_arg0, int *_arg1, int Ty_0, int fact_7, int i_9, int try_8, int num_9){
int Tcb_0 = 0 + (i_9 + fact_7);
int Teb_0 = 0 ^ (i_9 - 1);
int Tdb_0 = 0 + (Teb_0 + fact_7);
int Tfb_0 = 0 ^ (memory[Tcb_0] + 0);
int Tgb_0 = 0 ^ (memory[Tdb_0] + 0);
assert(0 == (Ty_0 ^ (Tfb_0 != Tgb_0)));
assert(0 == (Tgb_0 ^ (memory[Tdb_0] + 0)));
assert(0 == (Tfb_0 ^ (memory[Tcb_0] + 0)));
assert(0 == (Tdb_0 ^ (fact_7 + Teb_0)));
assert(0 == (Teb_0 ^ (i_9 - 1)));
assert(0 == (Tcb_0 ^ (fact_7 + i_9)));
int Tbb_0 = 0 + (i_9 + fact_7);
memory[Tbb_0] ^= (num_9 + 0);
assert(0 == (Tbb_0 ^ (fact_7 + i_9)));
//26:         num     ^= fact[i]
int Tab_0 = 0 + (i_9 + fact_7);
int num_8 = num_9 ^ (memory[Tab_0] + 0);
assert(0 == (Tab_0 ^ (fact_7 + i_9)));
//25:         fact[i] ^= num                         // and zero num
int Tz_0 = 0 + (i_9 + fact_7);
memory[Tz_0] ^= (num_8 + 0);
assert(0 == (Tz_0 ^ (fact_7 + i_9)));
//24:         i += 1                                 // Put last prime away, if not done
int i_8 = i_9 - (1 + 0);
//23:     if num != 1 then
int Tx_1 = 0 ^ (num_8 != 1);
Lfactor_9_bw(_arg0, _arg1, Tx_1, fact_7, i_8, num_8, try_8); 
}

//23:     if num != 1 then
void Lfactor_9_bw(int *_arg0, int *_arg1, int Tx_0, int fact_6, int i_7, int num_7, int try_7){
assert(0 != ((Tx_0 != 0)));
assert(0 == (Tx_0 ^ (num_7 != 1)));
//18:             z <=> num
int Tw_0 = 0 ^ (try_7 * try_7);
int Tb_2 = 0 ^ (Tw_0 > num_7);
assert(0 == (Tw_0 ^ (try_7 * try_7)));
Lfactor_4_bw(_arg0, _arg1, Tb_2, fact_6, try_7, i_7, num_7); 
}

//23:     if num != 1 then
void Lfactor_10_bw(int *_arg0, int *_arg1, int Tx_0, int fact_6, int i_7, int num_7, int try_7){
assert(0 == ((Tx_0 != 0)));
assert(0 == (Tx_0 ^ (num_7 != 1)));
//18:             z <=> num
int Tw_0 = 0 ^ (try_7 * try_7);
int Tb_2 = 0 ^ (Tw_0 > num_7);
assert(0 == (Tw_0 ^ (try_7 * try_7)));
Lfactor_4_bw(_arg0, _arg1, Tb_2, fact_6, try_7, i_7, num_7); 
}

//18:             z <=> num
void Lfactor_3_bw(int *_arg0, int *_arg1, int Ta_2, int try_6, int num_6, int fact_5, int i_6){
int Tu_0 = 0 ^ (try_6 == 0);
int Tv_0 = 0 ^ (num_6 > 1);
assert(0 == (Ta_2 ^ (Tu_0 & Tv_0)));
assert(0 == (Tv_0 ^ (num_6 > 1)));
assert(0 == (Tu_0 ^ (try_6 == 0)));
int Tt_0 = 0 ^ (num_6 % try_6);
int Tj_2 = 0 ^ (Tt_0 != 0);
assert(0 == (Tt_0 ^ (num_6 % try_6)));
Lfactor_8_bw(_arg0, _arg1, Tj_2, try_6, num_6, fact_5, i_6); 
}

//18:             z <=> num
void Lfactor_7_bw(int *_arg0, int *_arg1, int Ti_2, int fact_4, int try_5, int i_5, int num_5){
int Tr_0 = 0 + (i_5 + fact_4);
int Ts_0 = 0 ^ (memory[Tr_0] + 0);
assert(0 == (Ti_2 ^ (Ts_0 != try_5)));
assert(0 == (Ts_0 ^ (memory[Tr_0] + 0)));
assert(0 == (Tr_0 ^ (fact_4 + i_5)));
int z_1 = 0 + (num_5 * try_5);
int num_4 = z_1; int z_0 = num_5;
//17:             local int z = num / try
assert(0 == (z_0 ^ (num_4 / try_5)));
//16:             fact[i] += try
int Tq_0 = 0 + (i_5 + fact_4);
memory[Tq_0] -= (try_5 + 0);
assert(0 == (Tq_0 ^ (fact_4 + i_5)));
//15:             i += 1                             // factor
int i_4 = i_5 - (1 + 0);
//14:         from fact[i] != try loop               // Divide out all occurrences of this
int Tp_0 = 0 ^ (num_4 % try_5);
int Tj_1 = 0 ^ (Tp_0 != 0);
assert(0 == (Tp_0 ^ (num_4 % try_5)));
Lfactor_6_bw(_arg0, _arg1, Tj_1, try_5, num_4, fact_4, i_4); 
}

//14:         from fact[i] != try loop               // Divide out all occurrences of this
void Lfactor_8_bw(int *_arg0, int *_arg1, int Tj_0, int try_4, int num_3, int fact_3, int i_3){
assert(0 != ((Tj_0 != 0)));
int To_0 = 0 ^ (num_3 % try_4);
assert(0 == (Tj_0 ^ (To_0 != 0)));
assert(0 == (To_0 ^ (num_3 % try_4)));
//14:         from fact[i] != try loop               // Divide out all occurrences of this
int Tm_0 = 0 + (i_3 + fact_3);
int Tn_0 = 0 ^ (memory[Tm_0] + 0);
int Ti_1 = 0 ^ (Tn_0 != try_4);
assert(0 == (Tn_0 ^ (memory[Tm_0] + 0)));
assert(0 == (Tm_0 ^ (fact_3 + i_3)));
if ((Ti_1 != 0)) Lfactor_5_bw(_arg0, _arg1, Ti_1, fact_3, try_4, i_3, num_3); else Lfactor_7_bw(_arg0, _arg1, Ti_1, fact_3, try_4, i_3, num_3); 
}

//14:         from fact[i] != try loop               // Divide out all occurrences of this
void Lfactor_6_bw(int *_arg0, int *_arg1, int Tj_0, int try_4, int num_3, int fact_3, int i_3){
assert(0 == ((Tj_0 != 0)));
int To_0 = 0 ^ (num_3 % try_4);
assert(0 == (Tj_0 ^ (To_0 != 0)));
assert(0 == (To_0 ^ (num_3 % try_4)));
//14:         from fact[i] != try loop               // Divide out all occurrences of this
int Tm_0 = 0 + (i_3 + fact_3);
int Tn_0 = 0 ^ (memory[Tm_0] + 0);
int Ti_1 = 0 ^ (Tn_0 != try_4);
assert(0 == (Tn_0 ^ (memory[Tm_0] + 0)));
assert(0 == (Tm_0 ^ (fact_3 + i_3)));
if ((Ti_1 != 0)) Lfactor_5_bw(_arg0, _arg1, Ti_1, fact_3, try_4, i_3, num_3); else Lfactor_7_bw(_arg0, _arg1, Ti_1, fact_3, try_4, i_3, num_3); 
}

//14:         from fact[i] != try loop               // Divide out all occurrences of this
void Lfactor_5_bw(int *_arg0, int *_arg1, int Ti_0, int fact_2, int try_3, int i_2, int num_2){
int Tk_0 = 0 + (i_2 + fact_2);
int Tl_0 = 0 ^ (memory[Tk_0] + 0);
assert(0 == (Ti_0 ^ (Tl_0 != try_3)));
assert(0 == (Tl_0 ^ (memory[Tk_0] + 0)));
assert(0 == (Tk_0 ^ (fact_2 + i_2)));
//13:         call nexttry(try)
int _tmp1 = try_3;
nexttry_fw(&_tmp1);
int try_2 = _tmp1;
//12:     from (try = 0) && (num > 1) loop
int Th_0 = 0 ^ (try_2 * try_2);
int Tb_1 = 0 ^ (Th_0 > num_2);
assert(0 == (Th_0 ^ (try_2 * try_2)));
Lfactor_2_bw(_arg0, _arg1, Tb_1, fact_2, try_2, i_2, num_2); 
}

//12:     from (try = 0) && (num > 1) loop
void Lfactor_4_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int try_1, int i_1, int num_1){
assert(0 != ((Tb_0 != 0)));
int Tg_0 = 0 ^ (try_1 * try_1);
assert(0 == (Tb_0 ^ (Tg_0 > num_1)));
assert(0 == (Tg_0 ^ (try_1 * try_1)));
//12:     from (try = 0) && (num > 1) loop
int Te_0 = 0 ^ (try_1 == 0);
int Tf_0 = 0 ^ (num_1 > 1);
int Ta_1 = 0 ^ (Te_0 & Tf_0);
assert(0 == (Tf_0 ^ (num_1 > 1)));
assert(0 == (Te_0 ^ (try_1 == 0)));
if ((Ta_1 != 0)) Lfactor_1_bw(_arg0, _arg1, Ta_1, try_1, num_1, fact_1, i_1); else Lfactor_3_bw(_arg0, _arg1, Ta_1, try_1, num_1, fact_1, i_1); 
}

//12:     from (try = 0) && (num > 1) loop
void Lfactor_2_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int try_1, int i_1, int num_1){
assert(0 == ((Tb_0 != 0)));
int Tg_0 = 0 ^ (try_1 * try_1);
assert(0 == (Tb_0 ^ (Tg_0 > num_1)));
assert(0 == (Tg_0 ^ (try_1 * try_1)));
//12:     from (try = 0) && (num > 1) loop
int Te_0 = 0 ^ (try_1 == 0);
int Tf_0 = 0 ^ (num_1 > 1);
int Ta_1 = 0 ^ (Te_0 & Tf_0);
assert(0 == (Tf_0 ^ (num_1 > 1)));
assert(0 == (Te_0 ^ (try_1 == 0)));
if ((Ta_1 != 0)) Lfactor_1_bw(_arg0, _arg1, Ta_1, try_1, num_1, fact_1, i_1); else Lfactor_3_bw(_arg0, _arg1, Ta_1, try_1, num_1, fact_1, i_1); 
}

//12:     from (try = 0) && (num > 1) loop
void Lfactor_1_bw(int *_arg0, int *_arg1, int Ta_0, int try_0, int num_0, int fact_0, int i_0){
int Tc_0 = 0 ^ (try_0 == 0);
int Td_0 = 0 ^ (num_0 > 1);
assert(0 == (Ta_0 ^ (Tc_0 & Td_0)));
assert(0 == (Td_0 ^ (num_0 > 1)));
assert(0 == (Tc_0 ^ (try_0 == 0)));
//11:     local int i = 0     // Pointer to last factor in factor table.
assert(0 == (i_0 ^ (0 + 0)));
//10:     local int try = 0   // Attempted factor.
assert(0 == (try_0 ^ (0 + 0)));
//9: procedure factor(int num, int fact[20])
*_arg0 = num_0;
*_arg1 = fact_0;
}

//44: procedure zeroi(int i, int fact[20])
void zeroi_fw(int *_arg0, int *_arg1){
int i_0 = *_arg0;
int fact_0 = *_arg1;
//45:     from fact[i+1] = 0 loop
int Td_0 = 0 ^ (i_0 + 1);
__check_index(fact_0, 20, 0 ^ (fact_0 + Td_0));int Tc_0 = 0 ^ (fact_0 + Td_0);
int Te_0 = 0 ^ (memory[Tc_0] + 0);
int Ta_0 = 0 ^ (Te_0 == 0);
assert(0 == (Te_0 ^ (memory[Tc_0] + 0)));
assert(0 == (Tc_0 - (Td_0 + fact_0)));
assert(0 == (Td_0 ^ (i_0 + 1)));
Lzeroi_1_fw(_arg0, _arg1, Ta_0, fact_0, i_0); 
}

//45:     from fact[i+1] = 0 loop
void Lzeroi_1_fw(int *_arg0, int *_arg1, int Ta_1, int fact_1, int i_1){
assert(0 != ((Ta_1 != 0)));
int Tg_0 = 0 ^ (i_1 + 1);
__check_index(fact_1, 20, 0 ^ (fact_1 + Tg_0));int Tf_0 = 0 ^ (fact_1 + Tg_0);
int Th_0 = 0 ^ (memory[Tf_0] + 0);
assert(0 == (Ta_1 ^ (Th_0 == 0)));
assert(0 == (Th_0 ^ (memory[Tf_0] + 0)));
assert(0 == (Tf_0 - (Tg_0 + fact_1)));
assert(0 == (Tg_0 ^ (i_1 + 1)));
//45:     from fact[i+1] = 0 loop
int Tb_0 = 0 ^ (i_1 == 0);
if ((Tb_0 != 0)) Lzeroi_4_fw(_arg0, _arg1, Tb_0, fact_1, i_1); else Lzeroi_2_fw(_arg0, _arg1, Tb_0, fact_1, i_1); 
}

//45:     from fact[i+1] = 0 loop
void Lzeroi_3_fw(int *_arg0, int *_arg1, int Ta_1, int fact_1, int i_1){
assert(0 == ((Ta_1 != 0)));
int Tg_0 = 0 ^ (i_1 + 1);
__check_index(fact_1, 20, 0 ^ (fact_1 + Tg_0));int Tf_0 = 0 ^ (fact_1 + Tg_0);
int Th_0 = 0 ^ (memory[Tf_0] + 0);
assert(0 == (Ta_1 ^ (Th_0 == 0)));
assert(0 == (Th_0 ^ (memory[Tf_0] + 0)));
assert(0 == (Tf_0 - (Tg_0 + fact_1)));
assert(0 == (Tg_0 ^ (i_1 + 1)));
//45:     from fact[i+1] = 0 loop
int Tb_0 = 0 ^ (i_1 == 0);
if ((Tb_0 != 0)) Lzeroi_4_fw(_arg0, _arg1, Tb_0, fact_1, i_1); else Lzeroi_2_fw(_arg0, _arg1, Tb_0, fact_1, i_1); 
}

//45:     from fact[i+1] = 0 loop
void Lzeroi_2_fw(int *_arg0, int *_arg1, int Tb_1, int fact_2, int i_2){
assert(0 == (Tb_1 ^ (i_2 == 0)));
//46:         i -= 1
int i_3 = i_2 - (1 + 0);
int Tj_0 = 0 ^ (i_3 + 1);
__check_index(fact_2, 20, 0 ^ (fact_2 + Tj_0));int Ti_0 = 0 ^ (fact_2 + Tj_0);
int Tk_0 = 0 ^ (memory[Ti_0] + 0);
int Ta_2 = 0 ^ (Tk_0 == 0);
assert(0 == (Tk_0 ^ (memory[Ti_0] + 0)));
assert(0 == (Ti_0 - (Tj_0 + fact_2)));
assert(0 == (Tj_0 ^ (i_3 + 1)));
Lzeroi_3_fw(_arg0, _arg1, Ta_2, fact_2, i_3); 
}

//46:         i -= 1
void Lzeroi_4_fw(int *_arg0, int *_arg1, int Tb_2, int fact_3, int i_4){
assert(0 == (Tb_2 ^ (i_4 == 0)));
//44: procedure zeroi(int i, int fact[20])
*_arg0 = i_4;
*_arg1 = fact_3;
}

//44: procedure zeroi(int i, int fact[20])
void zeroi_bw(int *_arg0, int *_arg1){
int i_4 = *_arg0;
int fact_3 = *_arg1;
//46:         i -= 1
int Tb_2 = 0 ^ (i_4 == 0);
Lzeroi_4_bw(_arg0, _arg1, Tb_2, fact_3, i_4); 
}

//46:         i -= 1
void Lzeroi_3_bw(int *_arg0, int *_arg1, int Ta_2, int fact_2, int i_3){
int Tj_0 = 0 ^ (i_3 + 1);
int Ti_0 = 0 + (Tj_0 + fact_2);
int Tk_0 = 0 ^ (memory[Ti_0] + 0);
assert(0 == (Ta_2 ^ (Tk_0 == 0)));
assert(0 == (Tk_0 ^ (memory[Ti_0] + 0)));
assert(0 == (Ti_0 ^ (fact_2 + Tj_0)));
assert(0 == (Tj_0 ^ (i_3 + 1)));
int i_2 = i_3 + (1 + 0);
//45:     from fact[i+1] = 0 loop
int Tb_1 = 0 ^ (i_2 == 0);
Lzeroi_2_bw(_arg0, _arg1, Tb_1, fact_2, i_2); 
}

//45:     from fact[i+1] = 0 loop
void Lzeroi_4_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int i_1){
assert(0 != ((Tb_0 != 0)));
assert(0 == (Tb_0 ^ (i_1 == 0)));
//45:     from fact[i+1] = 0 loop
int Tg_0 = 0 ^ (i_1 + 1);
int Tf_0 = 0 + (Tg_0 + fact_1);
int Th_0 = 0 ^ (memory[Tf_0] + 0);
int Ta_1 = 0 ^ (Th_0 == 0);
assert(0 == (Th_0 ^ (memory[Tf_0] + 0)));
assert(0 == (Tf_0 ^ (fact_1 + Tg_0)));
assert(0 == (Tg_0 ^ (i_1 + 1)));
if ((Ta_1 != 0)) Lzeroi_1_bw(_arg0, _arg1, Ta_1, fact_1, i_1); else Lzeroi_3_bw(_arg0, _arg1, Ta_1, fact_1, i_1); 
}

//45:     from fact[i+1] = 0 loop
void Lzeroi_2_bw(int *_arg0, int *_arg1, int Tb_0, int fact_1, int i_1){
assert(0 == ((Tb_0 != 0)));
assert(0 == (Tb_0 ^ (i_1 == 0)));
//45:     from fact[i+1] = 0 loop
int Tg_0 = 0 ^ (i_1 + 1);
int Tf_0 = 0 + (Tg_0 + fact_1);
int Th_0 = 0 ^ (memory[Tf_0] + 0);
int Ta_1 = 0 ^ (Th_0 == 0);
assert(0 == (Th_0 ^ (memory[Tf_0] + 0)));
assert(0 == (Tf_0 ^ (fact_1 + Tg_0)));
assert(0 == (Tg_0 ^ (i_1 + 1)));
if ((Ta_1 != 0)) Lzeroi_1_bw(_arg0, _arg1, Ta_1, fact_1, i_1); else Lzeroi_3_bw(_arg0, _arg1, Ta_1, fact_1, i_1); 
}

//45:     from fact[i+1] = 0 loop
void Lzeroi_1_bw(int *_arg0, int *_arg1, int Ta_0, int fact_0, int i_0){
int Td_0 = 0 ^ (i_0 + 1);
int Tc_0 = 0 + (Td_0 + fact_0);
int Te_0 = 0 ^ (memory[Tc_0] + 0);
assert(0 == (Ta_0 ^ (Te_0 == 0)));
assert(0 == (Te_0 ^ (memory[Tc_0] + 0)));
assert(0 == (Tc_0 ^ (fact_0 + Td_0)));
assert(0 == (Td_0 ^ (i_0 + 1)));
//44: procedure zeroi(int i, int fact[20])
*_arg0 = i_0;
*_arg1 = fact_0;
}


void __bootstrap_janus_fw() {
memory = calloc(102400, sizeof(int));
memory[20] = 100;
int index = 0;
int num = 0;
int fact = memory[20];
memory[20] += 20;
main_fw(&index, &num, &fact);}

void __bootstrap_janus_bw() {
memory = calloc(102400, sizeof(int));
memory[20] = 100;
int index = 0;
int num = 0;
int fact = memory[20];
memory[20] += 20;
main_bw(&index, &num, &fact);}

