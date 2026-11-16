cmake_minimum_required(VERSION 3.22.1)
project(nexterm)
add_library(nexterm SHARED pty.cpp)
target_compile_options(nexterm PRIVATE -Wall -Wextra -Werror)
