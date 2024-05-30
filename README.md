# PINS22

This is a compiler for PINS22 programming language as defined in [this specification](./pins24.pdf).

## Usage

To compile and run a pins24 program, you must clone this repository and run:

```bash
./pins24 ./path-to-my-source-program.pins24
```

You can check out the [./examples](./examples) directory for some example pins24 programs.

## Standard library

### `exit(exitcode)`

Exits the program with the specified exit code.

### `getint()`

Returns the integer read from the standard input.

### `putint(value)`

Prints the integer to standard input.

### `getstr(straddr)`

Writes the string from standard input to the specified address.

### `putstr(straddr)`

Writes the string from specified address to standard output.

### `new(size)`

Reserves the specified amount of space on the heap.

### `del(straddr)`

Frees up reserved memory space at the specified address on the heap.

Note: This technically does nothing, due to our simplified heap implementation.

### `append(dst_ptr, src_str)`

Appends the string at the source address to the string at the destination address.

The destination must have enough memory allocated for both strings.

### `repeat(dst_ptr, char, count)`

Writes `count` repetitions of character `char` to the destination string memory address.

### `strcat(dst_ptr, src_ptr)`

Concatenates provided strings at the destination string memory address.

### `strlen(src_ptr)`

Returns the length of the string at the specified memory address.

### `strcpy(dst_ptr, src_ptr)`

Copies the string from source memory address to the destination memory address.

The destination memory address must have enough allocated memory for the whole source string length.