# Expected Return Prediction Module

## Overview

The Expected Return Prediction module implements a multi-step calculation process to predict expected returns based on OC analysis results and ARIMA coefficients. This module follows the modular monolith architecture and integrates seamlessly with the existing crypto trading model.

## Features

### ✅ **Complete Implementation**
- **Data Preparation**: AR.L value allocation from Demean_Diff_OC data
- **Multi-step Calculations**: Prd_Diff_OC → Prd_OC → Prd_Return_Arima  
- **File Integration**: Writes results back to existing OC analysis files
- **Comprehensive Logging**: Detailed progress and validation logging
- **Error Handling**: Robust error handling with descriptive messages

### 🔧 **Core Components**

1. **ExpectedReturnPredictor** - Main orchestrator
2. **DataPreparer** - AR.L allocation and data preparation
3. **ReturnCalculator** - Multi-step prediction calculations
4. **ResultsWriter** - Excel file writing with formatting

## Calculation Process

The module implements the exact calculation process specified in the requirements:

### Step 1: AR.L Allocation
```
Ar.L1 (T) = Demean_Diff_OC (T-1)
Ar.L2 (T) = Demean_Diff_OC (T-2)
...
Ar.L30 (T) = Demean_Diff_OC (T-30)
```

### Step 2: Prd_Diff_OC Calculation  
```
Prd_Diff_OC = Sigma(Ar.L(i) * Arima.Ar.L(i)) + Mean_Diff_OC
```

### Step 3: Prd_OC Calculation
```
Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1)
```

### Step 4: Prd_Return_Arima Calculation
```
Prd_Return_Arima(T) = Prd_OC(T) / Open(T)
```

## Usage

### Command Line Interface

```bash
# Basic usage with default file locations
python run_expected_return_prediction.py --asset BTC

# Custom file paths
python run_expected_return_prediction.py --asset ETH \
  --oc-file "oc_analysis_results/ETH_oc_analysis.xlsx" \
  --arima-file "oc_analysis_results/ETH_arima_results.json"

# Custom output file
python run_expected_return_prediction.py --asset BTC \
  --output "results/BTC_expected_returns.xlsx"
```

### Python API

```python
from crypto_trading_model.expected_return_prediction import ExpectedReturnPredictor, PredictionConfig

# Create configuration
config = PredictionConfig(
    oc_file_path="oc_analysis_results/BTC_oc_analysis.xlsx",
    arima_file_path="oc_analysis_results/BTC_arima_results.json", 
    asset_code="BTC"
)

# Run prediction
predictor = ExpectedReturnPredictor()
results = predictor.predict_returns(config)

print(f"Generated {results.valid_predictions} predictions")
print(f"Results saved to: {results.output_file_path}")
```

## Input Requirements

### OC Analysis File (Excel)
Required columns:
- `Demean_Diff_OC` - Demeaned difference of OC values
- `OC` - Open-Close ratio values  
- `Open_Price` - Opening prices
- `Mean_Diff_OC` - Mean of Diff_OC (optional, will be calculated if missing)

### ARIMA Results File (JSON)
Required structure:
```json
{
  "coefficients": {
    "ar.L1": 0.3,
    "ar.L2": 0.2, 
    "ar.L3": 0.1
  },
  "asset_code": "BTC",
  "p": 3,
  "d": 0,
  "q": 0
}
```

## Output Format

The module enhances the original OC analysis file with:

### New Columns Added
- `Ar.L1`, `Ar.L2`, ..., `Ar.LN` - AR lag allocations
- `Prd_Diff_OC` - Predicted difference OC
- `Prd_OC` - Predicted OC values
- `Prd_Return_Arima` - Final predicted returns

### Excel Sheets Created
1. **Main Data Sheet** - Original data + AR.L columns + predictions
2. **Summary Sheet** - Statistics and data quality metrics

## Data Quality & Validation

### Automatic Validation
- File existence and format validation
- Required column presence checking
- ARIMA coefficient availability verification
- Division by zero protection
- Infinite value handling

### Data Quality Metrics
- Valid prediction count vs total rows
- Missing data percentage by column
- Statistical summaries for all calculated fields
- Data range validation

## Error Handling

### Comprehensive Error Types
- `ExpectedReturnPredictorError` - Main workflow errors
- `ARLAllocationError` - Data preparation errors  
- `CalculationError` - Mathematical calculation errors
- `WriterError` - File writing errors

### Graceful Degradation
- Missing coefficients default to 0.0 with warnings
- Invalid data points are skipped with logging
- Partial results are saved even if some calculations fail

## Testing

Run the test suite:
```bash
cd src/crypto_trading_model/expected_return_prediction
python test_expected_return_prediction.py
```

The test creates synthetic data and validates the complete workflow.

## Integration Points

### With ARIMA Module
- Reads JSON results from `run_oc_arima.py`
- Uses AR lag coefficients (ar.L1, ar.L2, etc.)
- Supports any AR model order (p parameter)

### With OC Analysis Module  
- Reads Excel files from OC analysis
- Preserves original data structure
- Adds new columns to existing sheets

## Performance Considerations

### Optimization Features
- Vectorized calculations using pandas/numpy
- Efficient memory usage with in-place operations
- Minimal file I/O with single read/write operations
- Progress logging for long-running operations

### Scalability
- Handles datasets with thousands of rows
- Memory-efficient AR lag allocation
- Configurable chunk processing for large files

## Configuration

### Default File Locations
```
oc_analysis_results/
├── {ASSET}_oc_analysis.xlsx     # Input OC analysis
├── {ASSET}_arima_results.json   # Input ARIMA results  
└── {ASSET}_oc_analysis.xlsx     # Output (enhanced)
```

### Logging Configuration
- INFO level: Progress and summaries
- DEBUG level: Detailed calculation steps
- WARNING level: Data quality issues
- ERROR level: Critical failures

## Future Enhancements

### Potential Extensions
- Support for multiple prediction horizons
- Confidence intervals for predictions
- Model validation metrics (MAE, RMSE)
- Real-time prediction updates
- Backtesting framework integration

### Additional Features
- CSV output format support
- Custom column naming schemes
- Prediction visualization charts
- Performance benchmarking tools

## Dependencies

### Required Packages
- `pandas` - Data manipulation and analysis
- `numpy` - Numerical computing
- `openpyxl` - Excel file handling
- `pathlib` - Path operations

### Optional Packages
- `matplotlib` - Visualization (future feature)
- `pytest` - Enhanced testing (development)

## Architecture

The module follows the modular monolith pattern with clear separation of concerns:

```
ExpectedReturnPredictor (Orchestrator)
├── DataPreparer (AR.L allocation)
├── ReturnCalculator (Mathematical operations)  
└── ResultsWriter (File operations)
```

Each component is independently testable and follows SOLID principles for maintainability and extensibility.