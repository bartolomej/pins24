CODE SEGMENT:
       0 [5]   PUSH 0
       5 [5]   NAME main
      10 [1]   CALL
      11 [5]   PUSH 0
      16 [5]   NAME exit
      21 [1]   CALL
      22 [0] LABEL main
      22 [5]   PUSH 0
      27 [1]   POPN
      28 [5]   PUSH 5
      33 [1]   REGN.FP
      34 [1]   LOAD
      35 [5]   NAME inc
      40 [1]   CALL
      41 [5]   PUSH 0
      46 [1]   RETN
      47 [0] LABEL inc
      47 [5]   PUSH -4
      52 [1]   POPN
      53 [1]   REGN.FP
      54 [5]   PUSH -12
      59 [1]   OPER.ADD
      60 [5]   NAME 0
      65 [1]   INIT
      66 [1]   REGN.FP      ; Load the argument n
      67 [5]   PUSH 4       ; ...
      72 [1]   OPER.ADD     ; ...
      73 [1]   LOAD         ; ...
      74 [1]   REGN.FP      ; Load the address of m
      75 [5]   PUSH -12     ; ...
      80 [1]   OPER.ADD     ;
      81 [1]   SAVE
      82 [1]   REGN.FP
      83 [5]   PUSH -12
      88 [1]   OPER.ADD
      89 [1]   LOAD
      90 [5]   PUSH 1
      95 [1]   OPER.ADD
      96 [5]   PUSH 4
     101 [1]   RETN

DATA SEGMENT:
     102 [0] LABEL 0
     102 [4]   DATA 1
     106 [4]   DATA 1
     110 [4]   DATA 1
     114 [4]   DATA 0

CODE LABELS:
LABEL main = 22
LABEL inc = 47

DATA LABELS:
LABEL 0 = 102