#!/usr/bin/env python3
"""
Simple test to verify console logging works
"""
import sys
import os

print("=== CONSOLE LOG TEST ===")
print("Python version:", sys.version)
print("Current working directory:", os.getcwd())
print("stdout encoding:", sys.stdout.encoding)
print("stderr encoding:", sys.stderr.encoding)

# Test different print methods
print("TEST 1: Basic print")
print("TEST 2: Print with flush", flush=True)

# Test logging
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logger.info("TEST 3: Logger info")

# Test sys.stdout directly
sys.stdout.write("TEST 4: Direct stdout write\n")
sys.stdout.flush()

print("=== END TEST ===")