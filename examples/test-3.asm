This is PINS'24 compiler (code generation):
Program:
  FunDef main [1.1:7.7] depth=1 parsSize=4 varsSize=20
      --- Code: ---
      LABEL main
        PUSH -12
        POPN
        REGN.FP
        PUSH -16
        OPER.ADD
        NAME :0
        INIT
        REGN.FP
        PUSH -20
        OPER.ADD
        NAME :1
        INIT
        PUSH 10
        REGN.FP
        LOAD
        NAME inc
        CALL
        PUSH 0
        RETN
    Pars:
    Stmts:
      LetStmt [2.5:7.7]
          --- Code: ---
            REGN.FP
            PUSH -16
            OPER.ADD
            NAME :0
            INIT
            REGN.FP
            PUSH -20
            OPER.ADD
            NAME :1
            INIT
            PUSH 10
            REGN.FP
            LOAD
            NAME inc
            CALL
            PUSH 4
            POPN
        LetDefs:
          VarDef x [3.9:3.17] offset=-16 size=8 depth=1 inits=3,4
              --- Code: ---
                REGN.FP
                PUSH -16
                OPER.ADD
                NAME :0
                INIT
              --- Data: ---
              LABEL :0
                DATA 2
                DATA 1
                DATA 1
                DATA 3
                DATA 1
                DATA 1
                DATA 4
            Inits:
              Init 1* [3.15:3.15]
                AtomExpr INTCONST(3) [3.15:3.15]
              Init 1* [3.17:3.17]
                AtomExpr INTCONST(4) [3.17:3.17]
          VarDef i [4.9:4.15] offset=-20 size=4 depth=1 inits=0
              --- Code: ---
                REGN.FP
                PUSH -20
                OPER.ADD
                NAME :1
                INIT
              --- Data: ---
              LABEL :1
                DATA 1
                DATA 1
                DATA 1
                DATA 0
            Inits:
              Init 1* [4.15:4.15]
                AtomExpr INTCONST(0) [4.15:4.15]
        LetStmts:
          ExprStmt [6.9:6.15]
              --- Code: ---
                PUSH 10
                REGN.FP
                LOAD
                NAME inc
                CALL
                PUSH 4
                POPN
            CallExpr inc [6.9:6.15] def@[9.1:9.14]
                --- Code: ---
                  PUSH 10
                  REGN.FP
                  LOAD
                  NAME inc
                  CALL
              Args:
                AtomExpr INTCONST(10) [6.13:6.14]
                    --- Code: ---
                      PUSH 10
  FunDef inc [9.1:9.14] depth=1 parsSize=8 varsSize=8
      --- Code: ---
      LABEL inc
        PUSH 0
        POPN
        REGN.FP
        PUSH 4
        OPER.ADD
        LOAD
        PUSH 1
        OPER.ADD
        PUSH 4
        RETN
    Pars:
      ParDef n [9.9:9.9] offset=4 size=4 depth=1
    Stmts:
      ExprStmt [9.12:9.14]
          --- Code: ---
            REGN.FP
            PUSH 4
            OPER.ADD
            LOAD
            PUSH 1
            OPER.ADD
            PUSH 4
            POPN
        BinExpr ADD [9.12:9.14]
            --- Code: ---
              REGN.FP
              PUSH 4
              OPER.ADD
              LOAD
              PUSH 1
              OPER.ADD
          NameExpr n [9.12:9.12] def@[9.9:9.9] lval
              --- Code: ---
                REGN.FP
                PUSH 4
                OPER.ADD
                LOAD
          AtomExpr INTCONST(1) [9.14:9.14]
              --- Code: ---
                PUSH 1
  VarDef y [11.1:11.9] size=8 inits=5,5
      --- Code: ---
        NAME y
        NAME :2
        INIT
      --- Data: ---
      LABEL y
        SIZE 8
      LABEL :2
        DATA 1
        DATA 2
        DATA 1
        DATA 5
    Inits:
      Init 2* [11.7:11.9]
        AtomExpr INTCONST(5) [11.9:11.9]
  FunDef putstr [13.1:13.19] depth=1 parsSize=8 varsSize=8
    Pars:
      ParDef straddr [13.12:13.18] offset=4 size=4 depth=1
    Stmts:
  FunDef putint [14.1:14.20] depth=1 parsSize=8 varsSize=8
    Pars:
      ParDef intvalue [14.12:14.19] offset=4 size=4 depth=1
    Stmts:

CODE SEGMENT:
       0 [5]   NAME y       ; Push variable address (where the initial value will get written to)
       5 [5]   NAME :2      ; Push the address of initial value description
      10 [1]   INIT         ; Initialize y
      11 [5]   PUSH 0       ; Push static link for main
      16 [5]   NAME main    ; Push the address of main function
      21 [1]   CALL         ; Call main (execution continues at "LABEL main")
      22 [5]   PUSH 0       ; Pass exit code parameter to exit function
      27 [5]   NAME exit    ; Push the address of exit function
      32 [1]   CALL         ; Call exit (execution stops)
      33 [0] LABEL main     ; Creates a mapping of label -> address for main (before the execution)
      33 [5]   PUSH -12     ; Push constant value -12 (used to set the next 3B on the stack to 0 by the next instruction POPN)
      38 [1]   POPN         ; Set the next 3B (12b) on the stack to 0
      39 [1]   REGN.FP      ; Push value of FP register
      40 [5]   PUSH -16     ; Push constant value (used to calculate the memory address of x[0] by the next instruction)
      45 [1]   OPER.ADD     ; Push the result of FP - 16 (memory address of x[0])
      46 [5]   NAME :0      ; Push the address of initial value descriptor of x
      51 [1]   INIT         ; Initialize the array x
      52 [1]   REGN.FP      ; Push the value of FP
      53 [5]   PUSH -20     ; Push constant value -20 (used to calculate the memory address of i by the next instruction)
      58 [1]   OPER.ADD     ; Push the result of FP - 20 (memory address of i)
      59 [5]   NAME :1      ; Push the address of initial value descriptor of i
      64 [1]   INIT         ; Initialize variable i
      65 [5]   PUSH 10      ; Push constant value 10
      70 [1]   REGN.FP      ; Push value of FP (address of static link)
      71 [1]   LOAD         ; Push value at address of FP (static link)
      72 [5]   NAME inc     ; Push the address of inc
      77 [1]   CALL         ; Call inc (execution continues at "LABEL inc")
      78 [5]   PUSH 0       ; Push the size of params of main (used by RETN for cleanup)
      83 [1]   RETN         ; Restore caller register values, cleanup current function frame and parameters
      84 [0] LABEL inc      ; Creates a mapping of label -> address for inc (before the execution)
      84 [5]   PUSH 0       ; Push constant value 0 (used to set the next 0B on the stack to 0 by the next instruction POPN)
      89 [1]   POPN         ; Push/pop 0 values from the stack (basically do nothing since there are no local variables)
      90 [1]   REGN.FP      ; Push value of FP register
      91 [5]   PUSH 4       ; Push constant 4
      96 [1]   OPER.ADD     ; Push result of FP + 4 (the address of parameter n)
      97 [1]   LOAD         ; Push the value of parameter n
      98 [5]   PUSH 1       ; Push constant 1
     103 [1]   OPER.ADD     ; Push result of n + 1
     104 [5]   PUSH 4       ; Push the size of params of inc (used by RETN for cleanup)
     109 [1]   RETN         ; Restore caller register values, cleanup current function frame and parameters

DATA SEGMENT:
     110 [0] LABEL :0       ; Initial value descriptor of variable x
     110 [4]   DATA 2
     114 [4]   DATA 1
     118 [4]   DATA 1
     122 [4]   DATA 3
     126 [4]   DATA 1
     130 [4]   DATA 1
     134 [4]   DATA 4
     138 [0] LABEL :1       ; Initial value descriptor of variable i
     138 [4]   DATA 1
     142 [4]   DATA 1
     146 [4]   DATA 1
     150 [4]   DATA 0
     154 [0] LABEL y        ; Value of global variable y
     154 [ ]   SIZE 8
     162 [0] LABEL :2       ; Initial value descriptor of variable y
     162 [4]   DATA 1
     166 [4]   DATA 2
     170 [4]   DATA 1
     174 [4]   DATA 5

:-) Done.
