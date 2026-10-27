#!/bin/sh
# Khóa Groq thứ hai (tài khoản Groq khác) là tùy chọn: bỏ trống thì dùng lại GROQ_API_KEY.
if [ -z "$GROQ_API_KEY_2" ]; then
    export GROQ_API_KEY_2="$GROQ_API_KEY"
fi
exec docker/prod_entrypoint.sh "$@"
