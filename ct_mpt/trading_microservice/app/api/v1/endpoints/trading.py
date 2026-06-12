"""
Trading Simulation Endpoints
============================

FastAPI endpoints for trading simulation (Step 2).
"""

from fastapi import APIRouter, UploadFile, File, Form, HTTPException, Depends
from fastapi.responses import FileResponse
from typing import Optional
import time
import uuid
from pathlib import Path
import json

from app.api.v1.models import (
    TradingSimulationRequest,
    TradingSimulationResponse,
    TradingSimulationJobResponse,
    TradingSimulationStatusResponse,
    FileUploadResponse,
    ValidationResult
)
from app.services.trading_simulator import TradingSimulator
from app.services.data_processor import DataProcessor
from app.core.exceptions import (
    ValidationError,
    TradingSimulationError,
    FileProcessingError,
    ResourceLimitError
)
from app.core.config import settings
from app.core.job_manager import job_manager
from app.utils.logger import get_logger

logger = get_logger(__name__)
router = APIRouter()


async def get_trading_simulator() -> TradingSimulator:
    """Dependency to get trading simulator service."""
    return TradingSimulator()


async def get_data_processor() -> DataProcessor:
    """Dependency to get data processor service."""
    return DataProcessor()


@router.post("/upload-backtest-results", response_model=FileUploadResponse)
async def upload_backtest_results(
    file: UploadFile = File(...),
    data_processor: DataProcessor = Depends(get_data_processor)
):
    """
    Upload backtest results file for trading simulation.

    This endpoint accepts the output from Step 1 (portfolio optimization)
    and prepares it for Step 2 (trading simulation).

    Args:
        file: Backtest results file from portfolio optimization

    Returns:
        Upload status and validation results
    """
    request_id = str(uuid.uuid4())
    logger.info(f"Uploading backtest results file: {file.filename}", request_id=request_id)

    try:
        # Validate file type
        if not any(file.filename.endswith(ext) for ext in settings.ALLOWED_FILE_TYPES):
            raise FileProcessingError(
                f"Unsupported file type: {file.filename}. "
                f"Allowed types: {settings.ALLOWED_FILE_TYPES}"
            )

        # Check file size
        content = await file.read()
        if len(content) > settings.MAX_FILE_SIZE:
            raise ResourceLimitError(
                f"File too large: {len(content)} bytes (max: {settings.MAX_FILE_SIZE})"
            )

        # Save to temporary location
        temp_path = data_processor.save_temp_file(content, file.filename)

        # Validate backtest results file
        validation_result = data_processor.validate_file(temp_path, "backtest_results")

        logger.info(f"Backtest results file uploaded successfully", request_id=request_id)

        return FileUploadResponse(
            filename=file.filename,
            size=len(content),
            content_type=file.content_type,
            validation_status="valid",
            validation_details=validation_result
        )

    except Exception as e:
        logger.error(f"Backtest results upload failed: {str(e)}", request_id=request_id)
        if isinstance(e, (FileProcessingError, ResourceLimitError)):
            raise
        raise FileProcessingError(f"Upload failed: {str(e)}")


@router.post("/validate-backtest-data", response_model=ValidationResult)
async def validate_backtest_data(
    file: UploadFile = File(...),
    data_processor: DataProcessor = Depends(get_data_processor)
):
    """
    Validate backtest results file without running simulation.

    Args:
        file: Backtest results file to validate

    Returns:
        Validation results
    """
    request_id = str(uuid.uuid4())
    logger.info(f"Validating backtest data: {file.filename}", request_id=request_id)

    try:
        content = await file.read()
        temp_path = data_processor.save_temp_file(content, file.filename)

        try:
            validation_result = data_processor.validate_file(temp_path, "backtest_results")

            # Clean up temporary files
            data_processor.cleanup_temp_files()

            return ValidationResult(
                is_valid=True,
                errors=[],
                warnings=[],
                details=validation_result
            )

        except Exception as e:
            return ValidationResult(
                is_valid=False,
                errors=[str(e)],
                warnings=[],
                details={"filename": file.filename}
            )

    except Exception as e:
        logger.error(f"Backtest data validation failed: {str(e)}", request_id=request_id)
        raise ValidationError(f"Validation failed: {str(e)}")


@router.post("/simulate-job", response_model=TradingSimulationJobResponse)
async def simulate_trading_job(
    # File path - if not provided, uses default latest file
    backtest_results_filename: Optional[str] = Form(
        default="portfolio_optimization_BTC_ETH_20250917_235212.xlsx",
        description="Filename of backtest results in trading_microservice/results/ directory"
    ),

    # Account configuration
    total_capital: float = Form(default=1000.0, gt=0.0),
    trading_portion: float = Form(default=0.5, gt=0.0, lt=1.0),

    # Trading parameters
    trading_fee: float = Form(default=0.0015, ge=0.0, le=0.01),
    leverage_scale: float = Form(default=1.5, gt=0.0, le=10.0),

    # Asset configuration
    btc_decimal_places: int = Form(default=3, ge=0, le=8),
    eth_decimal_places: int = Form(default=2, ge=0, le=8),

    # Rebalancing configuration
    rebalancing_frequency: str = Form(default="monthly"),
    custom_rebalancing_days: Optional[int] = Form(default=None, ge=1, le=365),

    # Output options
    include_summary: bool = Form(default=True),

    # Dependencies
    simulator: TradingSimulator = Depends(get_trading_simulator)
):
    """
    Submit trading simulation job (async).

    Returns immediately with job ID. Use the job status endpoint to check progress.
    """
    request_id = str(uuid.uuid4())
    try:
        logger.info(f"Creating trading simulation job", request_id=request_id)

        # Validate rebalancing configuration
        if rebalancing_frequency == "custom" and custom_rebalancing_days is None:
            raise ValidationError("custom_rebalancing_days required when frequency is 'custom'")

        # Construct file path from results directory
        from pathlib import Path
        results_dir = Path("D:/work/ct_mpt/trading_microservice/results")
        backtest_file_path = results_dir / backtest_results_filename

        # Validate file exists
        if not backtest_file_path.exists():
            raise ValidationError(f"Backtest results file not found: {backtest_results_filename}")

        # Create simulation config
        config = {
            'backtest_results_file': str(backtest_file_path),
            'total_capital': total_capital,
            'trading_portion': trading_portion,
            'trading_fee': trading_fee,
            'leverage_scale': leverage_scale,
            'btc_decimal_places': btc_decimal_places,
            'eth_decimal_places': eth_decimal_places,
            'rebalancing_frequency': rebalancing_frequency,
            'custom_rebalancing_days': custom_rebalancing_days,
            'include_summary': include_summary
        }

        # Construct file path from results directory
        from pathlib import Path
        results_dir = Path("D:/work/ct_mpt/trading_microservice/results")
        backtest_file_path = results_dir / backtest_results_filename

        # Validate file exists
        if not backtest_file_path.exists():
            raise ValidationError(f"Backtest results file not found: {backtest_results_filename}")

        # Create job
        job_id = job_manager.create_job("trading_simulation", config)

        logger.info(f"Trading simulation job created: {job_id}", request_id=request_id)

        # Console notification
        print(f"\n=== TRADING SIMULATION API CALL ===", flush=True)
        print(f"Job created: {job_id}", flush=True)
        print(f"Backtest file: {backtest_file_path}", flush=True)
        print(f"Starting background simulation...", flush=True)

        # Start background task
        job_manager.start_job(
            job_id,
            simulator.simulate_trading_job,
            str(backtest_file_path),
            config
        )

        return TradingSimulationJobResponse(
            job_id=job_id,
            status="submitted",
            message="Trading simulation job submitted and started successfully",
            estimated_processing_time_minutes=2,
            check_status_url=f"/api/v1/trading/job-status/{job_id}"
        )

    except Exception as e:
        logger.error(f"Trading simulation job creation failed: {str(e)}", request_id=request_id)
        if isinstance(e, (ValidationError, FileProcessingError)):
            raise
        raise HTTPException(status_code=500, detail=f"Job creation failed: {str(e)}")


@router.post("/simulate", response_model=TradingSimulationResponse)
async def simulate_trading(
    # File path - if not provided, uses default latest file
    backtest_results_filename: Optional[str] = Form(
        default="portfolio_optimization_BTC_ETH_20250917_235212.xlsx",
        description="Filename of backtest results in trading_microservice/results/ directory"
    ),

    # Account configuration
    total_capital: float = Form(default=1000.0, gt=0.0),
    trading_portion: float = Form(default=0.5, gt=0.0, lt=1.0),

    # Trading parameters
    trading_fee: float = Form(default=0.0015, ge=0.0, le=0.01),
    leverage_scale: float = Form(default=1.5, gt=0.0, le=10.0),

    # Asset configuration
    btc_decimal_places: int = Form(default=3, ge=0, le=8),
    eth_decimal_places: int = Form(default=2, ge=0, le=8),

    # Rebalancing configuration
    rebalancing_frequency: str = Form(default="monthly"),
    custom_rebalancing_days: Optional[int] = Form(default=None, ge=1, le=365),

    # Output options
    include_summary: bool = Form(default=True),

    # Dependencies
    simulator: TradingSimulator = Depends(get_trading_simulator)
):
    """
    Run trading simulation with realistic constraints.

    This endpoint performs Step 2 of the trading pipeline:
    - Takes portfolio optimization results as input
    - Applies realistic trading constraints (fees, account management)
    - Simulates actual trading with rebalancing

    Args:
        backtest_results_filename: Filename of backtest results in trading_microservice/results/ directory
        total_capital: Total capital available for trading
        trading_portion: Portion allocated to trading (0-1)
        trading_fee: Trading fee percentage (0.0015 = 0.15%)
        leverage_scale: Leverage multiplier for positions
        btc_decimal_places: Decimal places for BTC quantities
        eth_decimal_places: Decimal places for ETH quantities
        rebalancing_frequency: Account rebalancing frequency
        custom_rebalancing_days: Custom rebalancing period (if frequency is 'custom')
        include_summary: Include summary statistics in output

    Returns:
        Trading simulation results with realistic performance metrics
    """
    start_time = time.time()
    request_id = str(uuid.uuid4())

    try:
        logger.info(
            f"Starting trading simulation with capital: {total_capital}",
            request_id=request_id
        )

        # Validate rebalancing configuration
        if rebalancing_frequency == "custom" and custom_rebalancing_days is None:
            raise ValidationError("custom_rebalancing_days required when frequency is 'custom'")

        # Construct file path from results directory
        from pathlib import Path
        results_dir = Path("D:/work/ct_mpt/trading_microservice/results")
        backtest_file_path = results_dir / backtest_results_filename

        # Validate file exists
        if not backtest_file_path.exists():
            raise ValidationError(f"Backtest results file not found: {backtest_results_filename}")

        logger.info(f"Using backtest results file: {backtest_file_path}", request_id=request_id)

        # Create simulation config
        config = {
            'total_capital': total_capital,
            'trading_portion': trading_portion,
            'trading_fee': trading_fee,
            'leverage_scale': leverage_scale,
            'btc_decimal_places': btc_decimal_places,
            'eth_decimal_places': eth_decimal_places,
            'rebalancing_frequency': rebalancing_frequency,
            'custom_rebalancing_days': custom_rebalancing_days,
            'include_summary': include_summary
        }

        # Run simulation
        results = await simulator.simulate_trading(str(backtest_file_path), config)

        processing_time = time.time() - start_time
        logger.info(
            f"Trading simulation completed in {processing_time:.2f}s",
            request_id=request_id
        )

        return TradingSimulationResponse(
            **results,
            processing_time_seconds=round(processing_time, 2)
        )

    except Exception as e:
        logger.error(f"Trading simulation failed: {str(e)}", request_id=request_id)
        if isinstance(e, (ValidationError, TradingSimulationError, FileProcessingError)):
            raise
        raise TradingSimulationError(f"Simulation failed: {str(e)}")


@router.get("/download-results/{filename}")
async def download_simulation_results(filename: str):
    """
    Download trading simulation results file.

    Args:
        filename: Name of the results file to download

    Returns:
        Excel file with simulation results
    """
    file_path = Path(settings.RESULTS_DIR) / filename

    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Results file not found")

    return FileResponse(
        path=file_path,
        filename=filename,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )


@router.get("/example-request")
async def get_example_request():
    """
    Get an example trading simulation request.

    Returns:
        Example request structure for trading simulation
    """
    return {
        "description": "Trading Simulation API Examples",
        "async_endpoint": {
            "endpoint": "POST /api/v1/trading/simulate-job",
            "description": "Submit async job (recommended for production)",
            "workflow": [
                "1. Submit job and get job_id",
                "2. Poll job status using GET /api/v1/trading/job-status/{job_id}",
                "3. Download results when status is 'completed'"
            ]
        },
        "sync_endpoint": {
            "endpoint": "POST /api/v1/trading/simulate",
            "description": "Get immediate results (for testing only)",
            "note": "Returns large payload - not recommended for production"
        },
        "form_data": {
            "backtest_results_filename": "portfolio_optimization_BTC_ETH_20250917_235212.xlsx",
            "total_capital": 1000.0,
            "trading_portion": 0.5,
            "trading_fee": 0.0015,
            "leverage_scale": 1.5,
            "btc_decimal_places": 3,
            "eth_decimal_places": 2,
            "rebalancing_frequency": "monthly",
            "custom_rebalancing_days": None,
            "include_summary": True
        },
        "file_handling": {
            "method": "file_path",
            "description": "Specify filename of backtest results in trading_microservice/results/ directory",
            "example": "portfolio_optimization_BTC_ETH_20250917_235212.xlsx",
            "note": "No file upload required - files are read from results directory"
        },
        "async_workflow": [
            "1. Run portfolio optimization (Step 1) to get backtest results",
            "2. Check available files using GET /api/v1/trading/list-backtest-files",
            "3. Submit job using POST /api/v1/trading/simulate-job",
            "4. Poll status using GET /api/v1/trading/job-status/{job_id}",
            "5. Download results when completed"
        ],
        "notes": [
            "Input file must be output from portfolio optimization endpoint",
            "Trading portion should be between 0.1 and 0.9",
            "Rebalancing frequency options: monthly, quarterly, yearly, custom",
            "Maximum file size: 50MB"
        ]
    }


@router.get("/job-status/{job_id}", response_model=TradingSimulationStatusResponse)
async def get_job_status(job_id: str):
    """Get the status of a trading simulation job."""
    job = job_manager.get_job(job_id)

    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    download_url = None
    if job.status == "completed" and job.result_file_path:
        filename = Path(job.result_file_path).name
        download_url = f"/api/v1/trading/download-results/{filename}"

    return TradingSimulationStatusResponse(
        job_id=job.job_id,
        status=job.status,
        progress_percentage=int(job.progress_percentage) if job.progress_percentage else None,
        message=job.message or f"Trading simulation is {job.status}",
        created_at=job.created_at.isoformat(),
        updated_at=job.created_at.isoformat(),  # Use created_at as updated_at since Job doesn't track updates
        processing_time_seconds=None,  # Job doesn't track this directly
        estimated_completion_time=None,  # Job doesn't provide this
        download_url=download_url,
        error_details=job.error
    )


@router.get("/list-backtest-files")
async def list_backtest_files():
    """
    List available backtest results files in the results directory.

    Returns:
        List of available backtest results filenames
    """
    try:
        from pathlib import Path
        results_dir = Path("D:/work/ct_mpt/trading_microservice/results")

        if not results_dir.exists():
            return {
                "status": "error",
                "message": "Results directory not found",
                "files": []
            }

        # Get all .xlsx files in the results directory
        backtest_files = [
            f.name for f in results_dir.glob("*.xlsx")
            if "portfolio_optimization" in f.name
        ]

        return {
            "status": "success",
            "files": sorted(backtest_files, reverse=True),  # Most recent first
            "count": len(backtest_files),
            "default": "portfolio_optimization_BTC_ETH_20250917_235212.xlsx"
        }

    except Exception as e:
        logger.error(f"Failed to list backtest files: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to list files: {str(e)}")


@router.get("/configuration-templates")
async def get_configuration_templates():
    """
    Get pre-defined configuration templates for trading simulation.

    Returns:
        Common configuration templates for different trading scenarios
    """
    return {
        "conservative": {
            "description": "Conservative trading setup with low risk",
            "config": {
                "total_capital": 1000.0,
                "trading_portion": 0.3,
                "trading_fee": 0.002,
                "leverage_scale": 1.0,
                "rebalancing_frequency": "monthly"
            }
        },
        "moderate": {
            "description": "Balanced trading setup (default)",
            "config": {
                "total_capital": 1000.0,
                "trading_portion": 0.5,
                "trading_fee": 0.0015,
                "leverage_scale": 1.5,
                "rebalancing_frequency": "monthly"
            }
        },
        "aggressive": {
            "description": "Aggressive trading setup with higher leverage",
            "config": {
                "total_capital": 1000.0,
                "trading_portion": 0.7,
                "trading_fee": 0.001,
                "leverage_scale": 2.0,
                "rebalancing_frequency": "quarterly"
            }
        },
        "high_frequency": {
            "description": "High frequency rebalancing setup",
            "config": {
                "total_capital": 1000.0,
                "trading_portion": 0.6,
                "trading_fee": 0.0012,
                "leverage_scale": 1.8,
                "rebalancing_frequency": "custom",
                "custom_rebalancing_days": 7
            }
        }
    }