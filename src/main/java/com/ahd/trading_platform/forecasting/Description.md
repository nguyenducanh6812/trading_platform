# Initial Master Data
File eth_arima_model.json and btc_arima_model.json store master data,
which need to used in the step prepare data and Step 2(Predicted Difference Calculation ) of ## Mathematical Process

# Worker
Recieve input from process variable:
instrumentsCode,
startDate
endDate

# Service Flow

## Load Data
Read master data from instrumentscode_arima_model.json file.
For example: BTC master data stored in btc_arima_model.json

## Mathematical Process

The usecase implements a proven five-step financial prediction process:
### Step 0. Prepare Data to apply ARIMA
OC: Open - Close
Diff_OC: OC(T) - OC(T-1)
Demean_Diff_OC: Diff_OC - Mean_Diff_OC
Where Mean_Diff_OC load from master data config file(instrumentscode_arima_model.json)

### Step 1: AR Lag Data Preparation
```
Ar.L1 (T) = Demean_Diff_OC (T-1)
Ar.L2 (T) = Demean_Diff_OC (T-2)
...
Ar.LN (T) = Demean_Diff_OC (T-N)
```
**Purpose**: Creates autoregressive lag variables from historical price differences

### Step 2: Predicted Difference Calculation  
```
Prd_Diff_OC(T) = Σ[Ar.L(i) × ARIMA.Coefficient(i)] + Mean_Diff_OC
```
Where:
1. Mean_Diff_OC from master data config file(instrumentscode_arima_model.json)
2. ARIMA.Coefficient(i) from master data config file(instrumentscode_arima_model.json)
**Purpose**: Applies ARIMA model to predict future price differences

### Step 3: Predicted OC Calculation
```
Prd_OC(T) = Prd_Diff_OC(T) + OC(T-1)  
```
**Purpose**: Converts predicted differences back to absolute OC values

### Step 4: Final Return Prediction
```
Prd_Return_Arima(T) = Prd_OC(T) / Open_Price(T)
```
**Purpose**: Generates the final expected return ratio for portfolio optimization


So at the end we need to store this values Calculated to the database, to store for the next time use.
So for easy monitoring, I think we should store all value calculated, it help to tracing and trouble shoot.
Like OC, Diff_OC, Date, Prd_Diff_Oc....
