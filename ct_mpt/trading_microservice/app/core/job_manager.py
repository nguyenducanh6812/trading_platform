"""
Job Management System
====================

Manages asynchronous background jobs for long-running operations.
"""

import asyncio
import uuid
from datetime import datetime
from typing import Dict, Optional, Any, Callable
from enum import Enum

from app.utils.logger import get_logger

logger = get_logger(__name__)


def safe_print(message: str):
    """Safe print function that handles encoding issues on Windows."""
    try:
        # Force ASCII encoding to avoid any Unicode issues
        ascii_message = message.encode('ascii', 'replace').decode('ascii')
        print(ascii_message, flush=True)
        # Also log to logger as backup
        logger.info(f"CONSOLE: {ascii_message}")
    except Exception as e:
        # Ultimate fallback
        fallback_msg = f"[LOG] {str(e)[:50]}"
        print(fallback_msg, flush=True)
        logger.error(f"Print failed: {str(e)}")


class JobStatus(str, Enum):
    """Job status enumeration."""
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


class Job:
    """Job information container."""

    def __init__(self, job_id: str, job_type: str, parameters: Dict[str, Any]):
        self.job_id = job_id
        self.job_type = job_type
        self.parameters = parameters
        self.status = JobStatus.PENDING
        self.progress_percentage = 0.0
        self.current_step = "Initializing"
        self.message = "Job created"
        self.created_at = datetime.now()
        self.started_at: Optional[datetime] = None
        self.completed_at: Optional[datetime] = None
        self.result_file_path: Optional[str] = None
        self.error: Optional[str] = None
        self.task: Optional[asyncio.Task] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert job to dictionary representation."""
        return {
            "job_id": self.job_id,
            "job_type": self.job_type,
            "status": self.status,
            "progress_percentage": self.progress_percentage,
            "current_step": self.current_step,
            "message": self.message,
            "created_at": self.created_at.isoformat(),
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "result_file_path": self.result_file_path,
            "error": self.error
        }


class JobManager:
    """Manages background jobs."""

    def __init__(self):
        self.jobs: Dict[str, Job] = {}

    def create_job(self, job_type: str, parameters: Dict[str, Any]) -> str:
        """Create a new job and return job ID."""
        job_id = str(uuid.uuid4())
        job = Job(job_id, job_type, parameters)
        self.jobs[job_id] = job

        logger.info(f"Created job {job_id} of type {job_type}")
        return job_id

    def get_job(self, job_id: str) -> Optional[Job]:
        """Get job by ID."""
        return self.jobs.get(job_id)

    def start_job(self, job_id: str, task_func: Callable, *args, **kwargs) -> bool:
        """Start executing a job."""
        job = self.get_job(job_id)
        if not job:
            return False

        if job.status != JobStatus.PENDING:
            return False

        job.status = JobStatus.PROCESSING
        job.started_at = datetime.now()
        job.current_step = "Starting processing"

        # Create and start the background task
        job.task = asyncio.create_task(
            self._run_job_with_error_handling(job, task_func, *args, **kwargs)
        )

        logger.info(f"Started job {job_id}")
        return True

    async def _run_job_with_error_handling(self, job: Job, task_func: Callable, *args, **kwargs):
        """Run job with error handling."""
        try:
            result = await task_func(job, *args, **kwargs)
            job.status = JobStatus.COMPLETED
            job.completed_at = datetime.now()
            job.progress_percentage = 100.0
            job.current_step = "Completed"
            job.message = "Job completed successfully"

            if isinstance(result, str):  # File path
                job.result_file_path = result

            logger.info(f"Completed job {job.job_id}")

        except Exception as e:
            job.status = JobStatus.FAILED
            job.completed_at = datetime.now()
            job.error = str(e)
            job.message = f"Job failed: {str(e)}"

            logger.error(f"Job {job.job_id} failed: {str(e)}")

    def update_job_progress(self, job_id: str, progress: float, step: str, message: str = None):
        """Update job progress."""
        job = self.get_job(job_id)
        if job:
            job.progress_percentage = progress
            job.current_step = step
            if message:
                job.message = message

            # Console logging for progress
            safe_print(f"[JOB] {job_id}: {progress:.1f}% - {step}")
            if message:
                safe_print(f"   {message}")

    def cleanup_old_jobs(self, max_age_hours: int = 24):
        """Clean up jobs older than specified hours."""
        cutoff_time = datetime.now()
        cutoff_time = cutoff_time.replace(hour=cutoff_time.hour - max_age_hours)

        to_remove = []
        for job_id, job in self.jobs.items():
            if job.created_at < cutoff_time:
                if job.task and not job.task.done():
                    job.task.cancel()
                to_remove.append(job_id)

        for job_id in to_remove:
            del self.jobs[job_id]
            logger.info(f"Cleaned up old job {job_id}")


# Global job manager instance
job_manager = JobManager()