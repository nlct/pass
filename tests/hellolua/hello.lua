#!/usr/bin/lua

if #arg < 1
then
  error("Syntax error: argument missing")
end

print("Hello ".. arg[1] .."!\n")

