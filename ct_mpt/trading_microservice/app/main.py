"""
FastAPI Trading Microservice Main Application
============================================

Provides portfolio optimization and trading simulation APIs.
"""

import sys
import os

# Add the parent directory to Python path for direct execution
if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import logging
import traceback
import time
import json
from contextlib import asynccontextmanager

from app.api.v1 import api_router
from app.core.config import settings
from app.core.exceptions import TradingServiceException
from app.utils.logger import setup_logging


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    # Startup
    setup_logging()
    logger = logging.getLogger(__name__)
    logger.info("Starting Trading Microservice...")
    logger.info(f"Environment: {settings.ENVIRONMENT}")

    yield

    # Shutdown
    logger.info("Shutting down Trading Microservice...")


# Create FastAPI application
app = FastAPI(
    title="Trading Backtest Microservice",
    description="Portfolio optimization and realistic trading simulation",
    version="1.0.0",
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all API requests and responses."""
    start_time = time.time()

    # Get request details
    method = request.method
    url = str(request.url)
    headers = dict(request.headers)

    # Simple console output that bypasses logging configuration
    try:
        import sys

        # Direct write to stdout to ensure visibility
        sys.stdout.write("\n" + "=" * 60 + "\n")
        sys.stdout.write(f"[API] {method} {url}\n")
        sys.stdout.write(f"[TIME] {time.strftime('%Y-%m-%d %H:%M:%S')}\n")

        # Handle request body for POST requests
        if method in ["POST", "PUT", "PATCH"]:
            try:
                body = await request.body()
                if body:
                    content_type = headers.get('content-type', '')
                    if 'multipart/form-data' in content_type:
                        sys.stdout.write("[BODY] Files uploaded\n")
                    else:
                        try:
                            body_str = body.decode('utf-8', errors='replace')
                            if len(body_str) > 100:
                                body_str = body_str[:100] + "..."
                            # Replace any problematic characters
                            safe_body = ''.join(c if ord(c) < 128 else '?' for c in body_str)
                            sys.stdout.write(f"[BODY] {safe_body}\n")
                        except:
                            sys.stdout.write(f"[BODY] Binary data ({len(body)} bytes)\n")

                # Reset body for downstream processing
                async def receive():
                    return {"type": "http.request", "body": body}
                request._receive = receive

            except Exception as e:
                sys.stdout.write(f"[BODY] Error: {str(e)}\n")

        sys.stdout.flush()

    except Exception as e:
        # Fallback - at least try to show something
        try:
            sys.stdout.write(f"[LOG ERROR] {str(e)}\n")
            sys.stdout.flush()
        except:
            pass

    # Process the request
    response = await call_next(request)

    # Log response
    try:
        process_time = time.time() - start_time
        sys.stdout.write(f"[RESPONSE] {response.status_code} | {process_time:.3f}s\n")
        sys.stdout.write("=" * 60 + "\n\n")
        sys.stdout.flush()
    except:
        pass

    return response


@app.exception_handler(TradingServiceException)
async def trading_exception_handler(request: Request, exc: TradingServiceException):
    """Handle custom trading service exceptions."""
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": exc.error_code,
            "message": exc.message,
            "details": exc.details,
            "request_id": getattr(request.state, "request_id", None)
        }
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """Handle unexpected exceptions."""
    logger = logging.getLogger(__name__)
    logger.error(f"Unhandled exception: {str(exc)}")
    logger.error(traceback.format_exc())

    return JSONResponse(
        status_code=500,
        content={
            "error": "INTERNAL_SERVER_ERROR",
            "message": "An unexpected error occurred",
            "request_id": getattr(request.state, "request_id", None)
        }
    )


# Health check endpoint
@app.get("/api/v1/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "service": "trading-microservice",
        "version": "1.0.0",
        "environment": settings.ENVIRONMENT
    }


# Include API routes
app.include_router(api_router, prefix=settings.API_V1_STR)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.ENVIRONMENT == "development"
    )