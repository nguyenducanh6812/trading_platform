#!/usr/bin/env python3
"""
Environment Setup Script
========================

Handles dependency installation and environment setup with fallback options.
"""

import os
import sys
import subprocess
import platform
from pathlib import Path


def run_command(command, description):
    """Run a command with error handling."""
    print(f"Running: {description}...")
    try:
        result = subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        print(f"[OK] {description} completed successfully")
        return True
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] {description} failed:")
        print(f"  Error: {e.returncode}")
        if e.stdout:
            print(f"  Output: {e.stdout}")
        if e.stderr:
            print(f"  Error: {e.stderr}")
        return False


def upgrade_pip():
    """Upgrade pip to latest version."""
    print("Upgrading pip...")
    return run_command(f"{sys.executable} -m pip install --upgrade pip", "Pip upgrade")


def install_build_tools():
    """Install build tools first."""
    print("Installing build tools...")
    return run_command(
        f"{sys.executable} -m pip install --upgrade setuptools wheel",
        "Build tools installation"
    )


def install_requirements(requirements_file="requirements.txt"):
    """Install requirements with fallback options."""
    print(f"Installing requirements from {requirements_file}...")

    # Try different installation strategies
    strategies = [
        # Strategy 1: Normal installation
        f"{sys.executable} -m pip install -r {requirements_file}",

        # Strategy 2: Force reinstall
        f"{sys.executable} -m pip install -r {requirements_file} --force-reinstall",

        # Strategy 3: No dependencies (let pip handle them)
        f"{sys.executable} -m pip install -r {requirements_file} --no-deps",

        # Strategy 4: User install if in system Python
        f"{sys.executable} -m pip install -r {requirements_file} --user"
    ]

    for i, strategy in enumerate(strategies, 1):
        print(f"\nTrying installation strategy {i}...")
        if run_command(strategy, f"Requirements installation (strategy {i})"):
            return True
        print(f"Strategy {i} failed, trying next...")

    return False


def try_minimal_install():
    """Try installing minimal requirements."""
    print("\n" + "="*60)
    print("Trying minimal installation...")
    print("="*60)

    minimal_packages = [
        "setuptools>=65.0.0",
        "wheel>=0.38.0",
        "fastapi",
        "uvicorn[standard]",
        "python-multipart",
        "pandas",
        "numpy",
        "openpyxl",
        "pydantic",
        "pydantic-settings",
        "python-dotenv"
    ]

    failed_packages = []

    for package in minimal_packages:
        print(f"Installing {package}...")
        if not run_command(f"{sys.executable} -m pip install {package}", f"{package} installation"):
            failed_packages.append(package)

    if failed_packages:
        print(f"\n[WARNING] Failed to install: {failed_packages}")
        return False
    else:
        print("\n[OK] Minimal installation completed successfully!")
        return True


def check_python_version():
    """Check Python version compatibility."""
    version = sys.version_info
    print(f"Python version: {version.major}.{version.minor}.{version.micro}")

    if version.major < 3 or (version.major == 3 and version.minor < 8):
        print("[WARNING] Python 3.8+ is recommended")
        return False

    print("[OK] Python version is compatible")
    return True


def create_directories():
    """Create necessary directories."""
    directories = ['uploads', 'results', 'logs']
    for directory in directories:
        Path(directory).mkdir(exist_ok=True)
        print(f"[OK] Created directory: {directory}")


def create_env_file():
    """Create .env file from example if it doesn't exist."""
    if not Path('.env').exists() and Path('.env.example').exists():
        import shutil
        shutil.copy('.env.example', '.env')
        print("[OK] Created .env file from .env.example")
    elif Path('.env').exists():
        print("[OK] .env file already exists")
    else:
        print("[WARNING] No .env.example found, skipping .env creation")


def main():
    """Main setup process."""
    print("="*60)
    print("Trading Microservice Environment Setup")
    print("="*60)
    print(f"Platform: {platform.system()} {platform.release()}")

    # Check Python version
    if not check_python_version():
        print("Consider upgrading Python for better compatibility")

    # Create directories
    create_directories()

    # Create .env file
    create_env_file()

    # Step 1: Upgrade pip
    upgrade_pip()

    # Step 2: Install build tools
    install_build_tools()

    # Step 3: Try to install requirements
    success = False

    # Try main requirements file first
    if Path('requirements.txt').exists():
        success = install_requirements('requirements.txt')

    # Fallback to minimal requirements
    if not success and Path('requirements-minimal.txt').exists():
        print("\n" + "="*60)
        print("Main installation failed, trying minimal requirements...")
        print("="*60)
        success = install_requirements('requirements-minimal.txt')

    # Last resort: manual minimal install
    if not success:
        success = try_minimal_install()

    print("\n" + "="*60)
    if success:
        print("[OK] Environment setup completed successfully!")
        print("\nYou can now run:")
        print("  python scripts/start_service.py dev")
    else:
        print("[FAILED] Environment setup failed!")
        print("\nTroubleshooting:")
        print("1. Try running in a clean virtual environment")
        print("2. Update your Python version to 3.9+")
        print("3. Install packages individually:")
        print("   pip install fastapi uvicorn pandas numpy openpyxl")
        print("4. Check your internet connection")
        sys.exit(1)

    print("="*60)


if __name__ == "__main__":
    main()