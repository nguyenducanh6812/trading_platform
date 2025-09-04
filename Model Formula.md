# Model Formula

[CML.py](CML.py)

Find the **weights of each asset** that has maximum Sharpe Ratio with constraint

**Phase 1**

**Constraint :**

$$
⁍
$$

Strategy Long:

$$
 \overrightarrow{w^T}i = 1
$$

Strategy Short:

$$
\overrightarrow{w^T}i = -1
$$

Strategy Full Hedging:

$$
\overrightarrow{w^T}i = 0
$$

Expected Return of each asset:

$$
ARIMA
$$

Expected Portfolio Return:

$$
E(R_p)=w_p^T \bar{ARIMA}  
$$

Population Covariance Matrix:

$$
⁍
$$

$$
\Sigma = \frac{1}{n}[(R^T-\bar{R})(R-\bar{R}^T)]
$$

Volatility of Portfolio:

$$
\sigma_p = \sqrt{w_p^T \Sigma w_p}
$$

Sharpe Ratio:

$$
SR = ⁍
$$

where:

$w$ : Column vector of asset weights (m x 1)

$R$ : matrix of asset return (n x m)

$i$ : vector i ( n x 1)

$\Sigma$ : Covariance Matrix ( m x m)

$n$ : time periods = 7

$m :$  number of assets = 2 (BTC,ETH)

$\lambda$: Rebalancing frequency (Daily = 1 / Weekly = 7 / monthly = 30)

$R_f$ : risk free rate = $(1+ R_{f(annual)} )^(\frac{\lambda}{365})-1$

- Daily rebalancing = 0,0001075
- =((1,04)*(1/365))-1

**Phase 2:**

**Risk Averse: Level 1(minimum risk)**

Steps:

1. find the  has the smallest volatility that belongs to efficient frontier.

$$
E(R_{GMVP})=  w_{GMVP}^T\bar{R}  
$$

Where:

GMVP : Global minimum variance portfolio

1. find the weight of risky asset portfolio

$$
w_{risky}= \frac{R_{GMVP} - R_f}{E(R_p)-R_f}
$$

1. Calculate the Return and Volatility of each strategy ( Long/Short/Hedging):

$$
E(R_{stg})= w_{risky}*E(R_p) + (1-w_{risky})*R_f = E(R_{GMVP})
$$

$$
\sigma_{stg} = \frac{E(R_{stg})\sigma_p-R_f\sigma_p}{E(R_p)-Rf}
$$

**Risk neutral:**

$w_{max}$ : weights of assets in portfolio have the greatest return.

$E(R_{max})$ for each strategy: The greatest expected return belongs to the efficient frontier.

1. Find expected max return:

 

$$
E(R_{Max})=  w_{max}^T\bar{R}  
$$

1. find weight of risky portfolio:

$$
w_{risky}= \frac{E(R_{max}) - R_f}{E(R_p)-R_f}
$$

1. Calculate the Return and Volatility of each strategy ( Long/Short/Hedging):

$$
E(R_{stg})= w_{risky}*E(R_p) + (1-w_{risky})*R_f = E(R_{max})
$$

$$
\sigma_{stg} = \frac{E(R_{stg})\sigma_p-R_f\sigma_p}{E(R_p)-Rf}
$$

***Risk Lover:***

1. Find volatility at $E(R_{max})$:

$$
\sigma_{max} = \sqrt{w_{max}^T \Sigma w_{max}}
$$

1. find expected return:

$E(R_{Risk Lover})=R_f + SR *\sigma_{max}$

1. find weight of risky portfolio:

$$
w_{risky}= \frac{R_{risklover} - R_f}{E(R_p)-R_f}
$$

1. Calculate the Return and Volatility of each strategy ( Long/Short/Hedging):

$$
E(R_{stg})= w_{risky}*E(R_p) + (1-w_{risky})*R_f = E(R_{risklover})
$$

$$
\sigma_{stg} = \frac{E(R_{stg})\sigma_p-R_f\sigma_p}{E(R_p)-R_f}
$$

where:

E(R_p) : Expected Portfolio Return Max Sharp ratio of each strategy

\sigma_p: Volatility Max Sharp Ratio from phase 1 of each strategy

***Decision making***

Compare Risk and Return:

if:

$$
E(R_{Long}) - E(R_{Short}) > \sigma_{Long}-\sigma_{Short} 
$$

and Max Sharp Ratio Phase 1 Long > 0

⇒ choosing Long Strategy 

else:

⇒ choosing Short Strategy

Calculate Daily Portfolio Return:

$R_{portfolio} = w_{risky}*R_{actual}*w_{Max SR} + (1-w_{risky})*R_f$

where:

$w_{MaxSR}$:  weight of Max SR matrix ( Strategy selected)

$w_{risky}$ : Weight of risky asset of selected strategy

### Phase 3 ARIMA:

### step 1: Verify stationary of backtest result data.

Test stationary by ADF test, KPSS:

if NOT stationary, do differencing the data:

$\Delta Y_t = Y_{t} -Y_{t-1}$

Example:

```python
from statsmodels.tsa.stattools import adfuller, kpss
from arch.unitroot import PhillipsPerron

def adf_test(series):
    result = adfuller(series)
    print("\nAugmented Dickey-Fuller (ADF) Test")
    print(f"ADF Statistic: {result[0]}")
    print(f"p-value: {result[1]}")
    if result[1] < 0.05:
        print("The series is stationary.")
    else:
        print("The series is non-stationary. Differencing may be needed.")

def kpss_test(series):
    result = kpss(series, regression='c', nlags='auto')
    print("\nKwiatkowski-Phillips-Schmidt-Shin (KPSS) Test")
    print(f"KPSS Statistic: {result[0]}")
    print(f"p-value: {result[1]}")
    if result[1] <= 0.05:
        print("The series is non-stationary.")
    else:
        print("The series is stationary.")
```

### Step2 (Demean data):

For stationary series:

mean = average Y

$\bar{Y_{t}} = Y_t - mean$ 

For non-stationary series:

mean = average ($\Delta Y_y$)

$$
\Delta \bar{Y{t}} = \Delta Y_t -mean
$$

### Step3: using AIC minimization to find ARIMA(p,d,q)

sample code:

train = df['demeaned']

p = range(0, 9)
d = range(0, 1)
q = range(0, 9)
best_aic = float("inf")
best_order = None

for param in itertools.product(p, d, q):
try:
model = ARIMA(train, order=param)
model_fit = model.fit()
if model_fit.aic < best_aic:
best_aic = model_fit.aic
best_order = param
except:
continue

print("Best ARIMA order:", best_order)

model = ARIMA(train, order=best_order)
model_fit = model.fit()

print(model_fit.summary())

### step4: find significant coefficients ( $\phi$ )

- confidence intervals do not contain 0
- p-value < 0.1

### step5 forecasting:

Stationary Series:

error terms:

$$
\epsilon_{t} = \bar{Y_{t}}-(\hat{Y}_t - mean(Y_t))
$$

Starting data point: $\epsilon_t = 0$

$\hat{Y_{t}} = \Sigma (\phi_{AR_i}*Y_{t-i}) + \Sigma(\phi_{MA_i}*\epsilon_{t-i}) + mean(Y_t)$

i $\in N^*$

Denote:

$Y_{t-i}$ :  refers to the value of the time series at time step t-i

$\hat{Y}_t$: predicted output

$\phi:$ significant coefficients of AR and MA

Non - Stationary Series:

Error term:

$$
\epsilon_{t} = \Delta \bar{ Y_{t}}-(\Delta \hat{Y}_t - mean(\Delta Y_t))
$$

Starting data point: $\epsilon_t = 0$

$\Delta\hat{Y_{t}} = \Sigma (\phi_{AR_i}*\Delta Y_{t-i}) + \Sigma(\phi_{MA_i}*\epsilon_{t-i}) + mean(\Delta Y_t)$

i $\in N^*$

$\hat{Y_t} = \Delta \hat{Y_t} + Y_{t-1}$

# **Actual Weight:**

Min Amount:

BTCUSDT: 0.001

ETHUSDT: 0.01

(initial investment x $w_i) / Open _{t0}$ = actual amount

Round up to 3 digits after the decimal point : BTCUSDT

Round up to 2 digits after the decimal point: ETHUSDT

# Expected Return Estimate:

1. $OC_t = close_t - open_t$
2. $\Delta{OC_t} = OC_t -OC_{t-1}$ 
3. Demean $\Delta{OC_t}$
4. Train Demeaned $\Delta OC_t$ with ARIMA(p,0,0) p $\in${N*, [1;30]}
5. $\Delta\hat{OC_{t}} = \Sigma (\phi_{AR_i}*\Delta OC_{t-i})  + mean(\Delta OC)$
6. $\hat{OC_t} = \Delta \hat{OC_t}+ OC_{t-1}$
7. $\hat{E(R)} = \hat{OC_t}/open_t$