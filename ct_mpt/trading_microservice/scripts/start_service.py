#!/usr/bin/env python3
"""
Trading Microservice Startup Script
===================================

Development and production startup script for the trading microservice.
"""

import os
import sys
import argparse
import subprocess
from pathlib import Path

# Fix Windows console encoding issues
os.environ['PYTHONIOENCODING'] = 'ascii:replace'


def check_requirements():
    """Check if all requirements are installed."""
    missing_packages = []
    required_packages = [
        ('fastapi', 'FastAPI'),
        ('uvicorn', 'Uvicorn'),
        ('pandas', 'Pandas'),
        ('numpy', 'NumPy'),
        ('openpyxl', 'OpenPyXL'),
        ('pydantic', 'Pydantic')
    ]

    for package, name in required_packages:
        try:
            __import__(package)
        except ImportError:
            missing_packages.append(name)

    if not missing_packages:
        print("[OK] All required packages are installed")
        return True
    else:
        print(f"[ERROR] Missing required packages: {', '.join(missing_packages)}")
        print("\nTo install dependencies, run one of:")
        print("  python scripts/setup_environment.py  (recommended)")
        print("  pip install -r requirements-minimal.txt  (basic)")
        print("  pip install -r requirements.txt  (full)")
        return False


def setup_environment():
    """Set up environment variables and directories."""
    # Create necessary directories
    directories = ['uploads', 'results', 'logs']
    for dir_name in directories:
        Path(dir_name).mkdir(exist_ok=True)
        print(f"[OK] Created directory: {dir_name}")

    # Check for .env file
    if not Path('.env').exists():
        print("[WARNING] No .env file found. Using defaults from .env.example")
        if Path('.env.example').exists():
            print("You can copy .env.example to .env and customize settings")


def start_development_server():
    """Start development server with auto-reload."""
    print("Starting development server...")
    print("API Documentation will be available at: http://localhost:8000/docs")
    print("Health check: http://localhost:8000/api/v1/health")

    cmd = [
        "uvicorn",
        "app.main:app",
        "--host", "0.0.0.0",
        "--port", "8000",
        "--reload",
        "--log-level", "info"
    ]

    subprocess.run(cmd)


def start_production_server():
    """Start production server."""
    print("Starting production server...")

    cmd = [
        "uvicorn",
        "app.main:app",
        "--host", "0.0.0.0",
        "--port", "8000",
        "--workers", "4",
        "--log-level", "info"
    ]

    subprocess.run(cmd)


def run_docker():
    """Run with Docker Compose."""
    print("Starting with Docker Compose...")
    print("Building and starting containers...")

    subprocess.run(["docker-compose", "up", "--build"])


def run_tests():
    """Run test suite."""
    print("Running test suite...")

    if not Path('tests').exists():
        print("No tests directory found")
        return

    cmd = ["python", "-m", "pytest", "tests/", "-v"]
    subprocess.run(cmd)


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(description="Trading Microservice Startup")
    parser.add_argument(
        "mode",
        choices=["dev", "prod", "docker", "test", "setup"],
        help="Startup mode"
    )
    parser.add_argument(
        "--skip-checks",
        action="store_true",
        help="Skip requirement checks"
    )
    parser.add_argument(
        "--auto-setup",
        action="store_true",
        help="Automatically run setup if dependencies are missing"
    )

    args = parser.parse_args()

    print("=" * 60)
    print("Trading Microservice Startup")
    print("=" * 60)

    # Check requirements unless skipped
    if not args.skip_checks and args.mode not in ["docker", "setup"]:
        if not check_requirements():
            if args.auto_setup:
                print("Auto-setup enabled, running setup...")
                subprocess.run([sys.executable, "scripts/setup_environment.py"])
                # Check again after setup
                if not check_requirements():
                    print("Setup completed but some packages are still missing.")
                    sys.exit(1)
            else:
                sys.exit(1)

    # Setup environment
    setup_environment()

    # Start based on mode
    try:
        if args.mode == "dev":
            start_development_server()
        elif args.mode == "prod":
            start_production_server()
        elif args.mode == "docker":
            run_docker()
        elif args.mode == "test":
            run_tests()
        elif args.mode == "setup":
            print("Running setup...")
            subprocess.run([sys.executable, "scripts/setup_environment.py"])
    except KeyboardInterrupt:
        print("\n\nShutting down...")
    except Exception as e:
        print(f"Error starting service: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()