"""
API Dependencies
===============

Common dependencies for API endpoints.
"""

from fastapi import HTTPException, Request
from typing import Optional
import time
import uuid


async def get_request_id(request: Request) -> str:
    """Generate or get request ID for tracking."""
    request_id = getattr(request.state, "request_id", None)
    if not request_id:
        request_id = str(uuid.uuid4())
        request.state.request_id = request_id
    return request_id


class RateLimiter:
    """Simple rate limiter for API endpoints."""

    def __init__(self, max_requests: int = 100, time_window: int = 60):
        self.max_requests = max_requests
        self.time_window = time_window
        self.requests = {}

    def is_allowed(self, client_id: str) -> bool:
        """Check if request is allowed for client."""
        now = time.time()

        # Clean old requests
        if client_id in self.requests:
            self.requests[client_id] = [
                req_time for req_time in self.requests[client_id]
                if now - req_time < self.time_window
            ]

        # Check rate limit
        client_requests = self.requests.get(client_id, [])
        if len(client_requests) >= self.max_requests:
            return False

        # Add current request
        if client_id not in self.requests:
            self.requests[client_id] = []
        self.requests[client_id].append(now)

        return True


# Global rate limiter instance
rate_limiter = RateLimiter()


async def check_rate_limit(request: Request):
    """Check rate limit for client."""
    client_ip = request.client.host

    if not rate_limiter.is_allowed(client_ip):
        raise HTTPException(
            status_code=429,
            detail="Rate limit exceeded. Please try again later."
        )