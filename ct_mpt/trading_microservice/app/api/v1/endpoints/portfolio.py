"""
Portfolio Optimization Endpoints
================================

FastAPI endpoints for portfolio optimization (Step 1).
"""

from fastapi import APIRouter, UploadFile, File, Form, HTTPException, Depends
from fastapi.responses import FileResponse
from typing import List, Dict, Optional
import time
import uuid
from pathlib import Path
import tempfile
import json

from app.api.v1.models import (
    PortfolioOptimizationRequest,
    PortfolioOptimizationResponse,
    PortfolioOptimizationJobResponse,
    PortfolioOptimizationStatusResponse,
    FileUploadResponse,
    ValidationResult
)
from app.services.portfolio_optimizer import PortfolioOptimizer
from app.services.data_processor import DataProcessor
from app.core.exceptions import (
    ValidationError,
    OptimizationError,
    FileProcessingError,
    ResourceLimitError
)
from app.core.config import settings
from app.core.job_manager import job_manager
from app.utils.logger import get_logger

logger = get_logger(__name__)
router = APIRouter()


async def get_portfolio_optimizer() -> PortfolioOptimizer:
    """Dependency to get portfolio optimizer service."""
    return PortfolioOptimizer()


async def get_data_processor() -> DataProcessor:
    """Dependency to get data processor service."""
    return DataProcessor()


@router.post("/upload-files", response_model=List[FileUploadResponse])
async def upload_prediction_files(
    files: List[UploadFile] = File(...),
    data_processor: DataProcessor = Depends(get_data_processor)
):
    """
    Upload ARIMA prediction files for portfolio optimization.

    Args:
        files: List of prediction files (*.xlsx or *.csv)

    Returns:
        List of upload results with validation status
    """
    request_id = str(uuid.uuid4())
    logger.info(f"Processing {len(files)} file uploads", request_id=request_id)

    if len(files) > 10:
        raise ResourceLimitError("Maximum 10 files allowed per request")

    upload_results = []

    try:
        for file in files:
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

            # Validate file
            try:
                validation_result = data_processor.validate_file(temp_path, "prediction")

                upload_results.append(FileUploadResponse(
                    filename=file.filename,
                    size=len(content),
                    content_type=file.content_type,
                    asset_code=validation_result.get("asset_code"),
                    validation_status="valid",
                    validation_details=validation_result
                ))

            except Exception as e:
                upload_results.append(FileUploadResponse(
                    filename=file.filename,
                    size=len(content),
                    content_type=file.content_type,
                    validation_status="invalid",
                    validation_details={"error": str(e)}
                ))

        logger.info(f"File upload completed", request_id=request_id)
        return upload_results

    except Exception as e:
        logger.error(f"File upload failed: {str(e)}", request_id=request_id)
        raise


@router.post("/validate-data", response_model=ValidationResult)
async def validate_prediction_data(
    files: List[UploadFile] = File(...),
    data_processor: DataProcessor = Depends(get_data_processor)
):
    """
    Validate prediction data files without running optimization.

    Args:
        files: List of prediction files to validate

    Returns:
        Combined validation results
    """
    request_id = str(uuid.uuid4())
    logger.info(f"Validating {len(files)} files", request_id=request_id)

    errors = []
    warnings = []
    details = {"files": {}}

    try:
        for file in files:
            content = await file.read()
            temp_path = data_processor.save_temp_file(content, file.filename)

            try:
                validation_result = data_processor.validate_file(temp_path, "prediction")
                details["files"][file.filename] = validation_result
            except Exception as e:
                errors.append(f"{file.filename}: {str(e)}")

        # Clean up temporary files
        data_processor.cleanup_temp_files()

        return ValidationResult(
            is_valid=len(errors) == 0,
            errors=errors,
            warnings=warnings,
            details=details
        )

    except Exception as e:
        logger.error(f"Data validation failed: {str(e)}", request_id=request_id)
        raise ValidationError(f"Data validation failed: {str(e)}")


@router.post("/optimize-job", response_model=PortfolioOptimizationJobResponse)
async def optimize_portfolio_job(
    # Asset configuration (simplified for testing)
    asset1: str = Form(default="BTC", description="First asset code"),
    asset2: str = Form(default="ETH", description="Second asset code"),
    asset3: Optional[str] = Form(default=None, description="Third asset code (optional)"),

    # Optimization parameters
    risk_profile: str = Form(default="neutral", description="Risk profile: averse, neutral, lover"),
    optimization_method: str = Form(default="traditional", description="Method: traditional, smart_grid, compare"),
    rebalance_frequency: int = Form(default=1, ge=1, le=30, description="Rebalance every N days"),
    lookback_period: int = Form(default=7, ge=5, le=30, description="Historical lookback period"),
    smart_grid_precision: int = Form(default=2, ge=1, le=4, description="Smart grid precision"),
    use_custom_precision: bool = Form(default=False, description="Use custom weight precision"),
    weight_precision: int = Form(default=16, ge=0, le=16, description="Weight decimal precision"),

    # Date range (simplified)
    start_date: Optional[str] = Form(default="2021-04-15", description="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = Form(default="2025-08-13", description="End date (YYYY-MM-DD)"),

    # Risk parameters

    # File uploads
    files: List[UploadFile] = File(..., description="ARIMA prediction files (Excel/CSV)"),

    # Dependencies
    optimizer: PortfolioOptimizer = Depends(get_portfolio_optimizer)
):
    """
    Submit portfolio optimization job for async processing.

    Returns immediately with job ID. Use the job status endpoint to check progress.
    """
    request_id = str(uuid.uuid4())
    try:
        # Build asset codes li  st from individual parameters
        asset_codes_list = [asset1, asset2]
        if asset3:
            asset_codes_list.append(asset3)

        # Build date range dictionary
        date_range_dict = {"start": start_date, "end": end_date}

        logger.info(f"Creating optimization job for assets: {asset_codes_list}", request_id=request_id)

        # Validate file count matches asset codes
        if len(files) != len(asset_codes_list):
            raise ValidationError(
                f"Number of files ({len(files)}) must match number of assets "
                f"({len(asset_codes_list)}). Assets: {asset_codes_list}"
            )

        # Save uploaded files and create asset mapping
        asset_files = {}
        data_processor = DataProcessor()

        for file, asset_code in zip(files, asset_codes_list):
            content = await file.read()
            temp_path = data_processor.save_temp_file(content, f"{asset_code}_{file.filename}")
            asset_files[asset_code] = temp_path

        # Create optimization config
        config = {
            'risk_profile': risk_profile,
            'optimization_method': optimization_method,
            'rebalance_frequency': rebalance_frequency,
            'lookback_period': lookback_period,
            'smart_grid_precision': smart_grid_precision,
            'use_custom_precision': use_custom_precision,
            'weight_precision': weight_precision if use_custom_precision else None,
            'date_range': (date_range_dict['start'], date_range_dict['end']),
        }

        # Create job
        job_id = job_manager.create_job("portfolio_optimization", {
            "asset_codes": asset_codes_list,
            "config": config
        })

        # Console notification
        print(f"\n=== API CALL ===", flush=True)
        print(f"Job created: {job_id}", flush=True)
        print(f"Assets: {asset_codes_list}", flush=True)
        print(f"Starting background optimization...", flush=True)

        # Start background task
        job_manager.start_job(
            job_id,
            optimizer.optimize_portfolio_job,
            asset_files,
            config
        )

        # Estimate processing time (rough estimate: 1-5 minutes for typical datasets)
        estimated_time = max(1, len(asset_codes_list) * 2)

        return PortfolioOptimizationJobResponse(
            job_id=job_id,
            status="submitted",
            message="Portfolio optimization job submitted successfully",
            estimated_processing_time_minutes=estimated_time,
            check_status_url=f"/api/v1/portfolio/job-status/{job_id}",
            download_url=None
        )

    except Exception as e:
        logger.error(f"Failed to create optimization job: {str(e)}", request_id=request_id)
        if isinstance(e, (ValidationError, OptimizationError, FileProcessingError)):
            raise
        raise OptimizationError(f"Failed to create optimization job: {str(e)}")


@router.get("/job-status/{job_id}", response_model=PortfolioOptimizationStatusResponse)
async def get_job_status(job_id: str):
    """Get the status of a portfolio optimization job."""
    job = job_manager.get_job(job_id)

    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    download_url = None
    if job.status == "completed" and job.result_file_path:
        filename = Path(job.result_file_path).name
        download_url = f"/api/v1/portfolio/download-results/{filename}"

    return PortfolioOptimizationStatusResponse(
        job_id=job.job_id,
        status=job.status,
        progress_percentage=job.progress_percentage,
        current_step=job.current_step,
        message=job.message,
        started_at=job.started_at,
        completed_at=job.completed_at,
        download_url=download_url,
        error=job.error
    )


@router.post("/optimize", response_model=PortfolioOptimizationResponse)
async def optimize_portfolio(
    # Asset configuration (simplified for testing)
    asset1: str = Form(default="BTC", description="First asset code"),
    asset2: str = Form(default="ETH", description="Second asset code"),
    asset3: Optional[str] = Form(default=None, description="Third asset code (optional)"),

    # Optimization parameters
    risk_profile: str = Form(default="neutral", description="Risk profile: averse, neutral, lover"),
    optimization_method: str = Form(default="traditional", description="Method: traditional, smart_grid, compare"),
    rebalance_frequency: int = Form(default=1, ge=1, le=30, description="Rebalance every N days"),
    lookback_period: int = Form(default=7, ge=5, le=30, description="Historical lookback period"),
    smart_grid_precision: int = Form(default=2, ge=1, le=4, description="Smart grid precision"),
    use_custom_precision: bool = Form(default=False, description="Use custom weight precision"),
    weight_precision: int = Form(default=16, ge=0, le=16, description="Weight decimal precision"),

    # Date range (simplified)
    start_date: Optional[str] = Form(default="2021-04-15", description="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = Form(default="2025-08-13", description="End date (YYYY-MM-DD)"),

    # Risk parameters

    # File uploads
    files: List[UploadFile] = File(..., description="ARIMA prediction files (Excel/CSV)"),

    # Dependencies
    optimizer: PortfolioOptimizer = Depends(get_portfolio_optimizer)
):
    """
    Run portfolio optimization using ARIMA prediction files.

    This endpoint performs Step 1 of the trading pipeline:
    - Loads ARIMA prediction files
    - Optimizes portfolio weights using predicted returns
    - Generates optimal trading decisions over time

    Args:
        asset1: First asset code (e.g., "BTC")
        asset2: Second asset code (e.g., "ETH")
        asset3: Optional third asset code
        risk_profile: Risk profile (averse, neutral, lover)
        optimization_method: Optimization method (traditional, smart_grid, compare)
        rebalance_frequency: Rebalancing frequency in days
        lookback_period: Lookback period for covariance calculation
        smart_grid_precision: Precision for Smart Grid optimization
        use_custom_precision: Override weight precision
        weight_precision: Weight precision override
        start_date: Start date (YYYY-MM-DD), defaults to 2021-04-15
        end_date: End date (YYYY-MM-DD), defaults to 2025-08-13
        files: ARIMA prediction files (order should match asset1, asset2, asset3)

    Returns:
        Portfolio optimization results with weights and performance metrics
    """
    start_time = time.time()
    request_id = str(uuid.uuid4())

    try:
        # Build asset codes list from individual parameters
        asset_codes_list = [asset1, asset2]
        if asset3:
            asset_codes_list.append(asset3)

        # Build date range dictionary
        date_range_dict = {"start": start_date, "end": end_date}

        logger.info(
            f"Starting portfolio optimization for assets: {asset_codes_list}",
            request_id=request_id
        )

        # Validate file count matches asset codes
        if len(files) != len(asset_codes_list):
            raise ValidationError(
                f"Number of files ({len(files)}) must match number of assets "
                f"({len(asset_codes_list)}). Assets: {asset_codes_list}"
            )

        # Save uploaded files and create asset mapping
        asset_files = {}
        data_processor = DataProcessor()

        for file, asset_code in zip(files, asset_codes_list):
            content = await file.read()
            temp_path = data_processor.save_temp_file(content, f"{asset_code}_{file.filename}")
            asset_files[asset_code] = temp_path

        # Create optimization config
        config = {
            'risk_profile': risk_profile,
            'optimization_method': optimization_method,
            'rebalance_frequency': rebalance_frequency,
            'lookback_period': lookback_period,
            'smart_grid_precision': smart_grid_precision,
            'use_custom_precision': use_custom_precision,
            'weight_precision': weight_precision if use_custom_precision else None,
            'date_range': (date_range_dict['start'], date_range_dict['end']) if date_range_dict else None,
        }

        # Run optimization with progress logging
        logger.info(f"Starting portfolio optimization for {len(asset_codes_list)} assets")
        results = await optimizer.optimize_portfolio(asset_files, config)

        # Clean up temporary files
        data_processor.cleanup_temp_files()

        processing_time = time.time() - start_time
        logger.info(
            f"Portfolio optimization completed in {processing_time:.2f}s",
            request_id=request_id
        )

        return PortfolioOptimizationResponse(
            **results,
            processing_time_seconds=round(processing_time, 2)
        )

    except Exception as e:
        logger.error(f"Portfolio optimization failed: {str(e)}", request_id=request_id)
        if isinstance(e, (ValidationError, OptimizationError, FileProcessingError)):
            raise
        raise OptimizationError(f"Optimization failed: {str(e)}")


@router.get("/download-results/{filename}")
async def download_optimization_results(filename: str):
    """
    Download portfolio optimization results file.

    Args:
        filename: Name of the results file to download

    Returns:
        Excel file with optimization results
    """
    # In a real implementation, you would:
    # 1. Validate the filename and user permissions
    # 2. Check file exists in secure storage
    # 3. Return the file

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
    Get an example portfolio optimization request.

    Returns:
        Example request structure for portfolio optimization
    """
    return {
        "description": "Portfolio Optimization API Example",
        "endpoint": "POST /api/v1/portfolio/optimize",
        "form_data": {
            "asset_codes": '["BTC", "ETH"]',
            "risk_profile": "neutral",
            "optimization_method": "traditional",
            "rebalance_frequency": 1,
            "lookback_period": 7,
            "smart_grid_precision": 2,
            "use_custom_precision": False,
            "weight_precision": 16,
            "date_range": '{"start": "2021-04-15", "end": "2023-12-31"}',
            "risk_free_rate": 0.0001075
        },
        "files": [
            {
                "field_name": "files",
                "description": "BTC prediction file (*_with_predictions*.xlsx)",
                "required": True
            },
            {
                "field_name": "files",
                "description": "ETH prediction file (*_with_predictions*.xlsx)",
                "required": True
            }
        ],
        "notes": [
            "Upload files in the same order as asset_codes",
            "Each file must contain ARIMA predictions for the corresponding asset",
            "Files must be in .xlsx or .csv format",
            "Maximum file size: 50MB per file"
        ]
    }