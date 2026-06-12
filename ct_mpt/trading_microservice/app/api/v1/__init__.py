"""API v1 router configuration."""

from fastapi import APIRouter
from app.api.v1.endpoints import portfolio, trading

api_router = APIRouter()

# Include endpoint routers
api_router.include_router(
    portfolio.router,
    prefix="/portfolio",
    tags=["Portfolio Optimization"]
)

api_router.include_router(
    trading.router,
    prefix="/trading",
    tags=["Trading Simulation"]
)