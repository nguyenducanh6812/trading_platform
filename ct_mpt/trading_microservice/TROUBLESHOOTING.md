# Troubleshooting Guide

## Installation Issues

### Problem: "Cannot import 'setuptools.build_meta'" Error

This is a common issue with older setuptools versions and some package combinations.

**Solutions (try in order):**

1. **Run the setup script (recommended):**
   ```bash
   python scripts/setup_environment.py
   ```

2. **Upgrade build tools first:**
   ```bash
   python -m pip install --upgrade pip setuptools wheel
   pip install -r requirements-minimal.txt
   ```

3. **Try minimal installation:**
   ```bash
   pip install fastapi uvicorn pandas numpy openpyxl pydantic python-dotenv
   ```

4. **Use a clean virtual environment:**
   ```bash
   # Create new environment
   python -m venv fresh_env

   # Windows
   fresh_env\Scripts\activate

   # Linux/Mac
   source fresh_env/bin/activate

   # Install
   python scripts/setup_environment.py
   ```

### Problem: NumPy Installation Fails

**Solutions:**

1. **Install NumPy separately:**
   ```bash
   pip install --upgrade setuptools wheel
   pip install numpy
   pip install -r requirements-minimal.txt
   ```

2. **Use conda instead of pip:**
   ```bash
   conda install numpy pandas
   pip install fastapi uvicorn pydantic
   ```

### Problem: Python Version Compatibility

**Minimum Requirements:**
- Python 3.8+
- pip 21.0+

**Check your versions:**
```bash
python --version
pip --version
```

## Runtime Issues

### Problem: Port 8000 Already in Use

**Solutions:**

1. **Use a different port:**
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8001
   ```

2. **Find and kill the process using port 8000:**
   ```bash
   # Windows
   netstat -ano | findstr :8000
   taskkill /PID <PID> /F

   # Linux/Mac
   lsof -ti:8000 | xargs kill -9
   ```

### Problem: Import Errors at Runtime

**Check if you're in the right directory:**
```bash
# Should be in trading_microservice/
ls app/
```

**Try absolute imports:**
```bash
# Add to PYTHONPATH
export PYTHONPATH=$PYTHONPATH:$(pwd)
```

## Quick Solutions

### If Nothing Works:

1. **Try Docker instead:**
   ```bash
   docker-compose up --build
   ```

2. **Use auto-setup:**
   ```bash
   python scripts/start_service.py dev --auto-setup
   ```

3. **Manual minimal setup:**
   ```bash
   pip install fastapi[all]
   pip install pandas openpyxl
   python -m uvicorn app.main:app --reload
   ```

## Common Error Messages

### "ModuleNotFoundError: No module named 'app'"

**Solution:**
```bash
# Make sure you're in the right directory
cd trading_microservice
python scripts/start_service.py dev
```

### "ImportError: cannot import name 'Annotated'"

**Solution:**
```bash
# Update typing-extensions
pip install --upgrade typing-extensions
```

### "FileNotFoundError: [Errno 2] No such file or directory: 'uploads'"

**Solution:**
```bash
# Create directories
mkdir uploads results logs
```

## Getting Help

1. **Check the logs:**
   ```bash
   # Look in logs/ directory for error details
   ```

2. **Run with verbose output:**
   ```bash
   python scripts/start_service.py dev --skip-checks
   ```

3. **Test individual components:**
   ```bash
   python -c "import fastapi; print('FastAPI OK')"
   python -c "import pandas; print('Pandas OK')"
   python -c "import numpy; print('NumPy OK')"
   ```

## Development Mode

If you just want to test the API quickly:

```bash
# Minimal requirements only
pip install fastapi uvicorn python-multipart

# Start with basic functionality
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

This will start the API with basic functionality. Some advanced features may not work without all dependencies.