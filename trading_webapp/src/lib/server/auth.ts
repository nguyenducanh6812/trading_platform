import * as jose from 'jose';
import { JWT_SECRET } from '$env/static/private';

const secret = new TextEncoder().encode(JWT_SECRET);

export interface User {
	id: string;
	username: string;
	email: string;
}

export async function signJWT(payload: User | { id: string }, expiresIn: string): Promise<string> {
	return await new jose.SignJWT(payload as any)
		.setProtectedHeader({ alg: 'HS256' })
		.setIssuedAt()
		.setExpirationTime(expiresIn)
		.sign(secret);
}

export async function verifyJWT(token: string): Promise<User> {
	try {
		const { payload } = await jose.jwtVerify(token, secret);
		return payload as unknown as User;
	} catch (e) {
		throw new Error('Invalid or expired token');
	}
}

export async function refreshAccessToken(
	refreshToken: string
): Promise<{ token: string; user: User }> {
	try {
		const payload = await verifyJWT(refreshToken);

		// In production, fetch user from database
		// For now, we'll use the payload data
		const user: User = {
			id: payload.id,
			username: payload.username || 'demo',
			email: payload.email || 'demo@example.com'
		};

		const newAccessToken = await signJWT(user, '15m');

		return {
			token: newAccessToken,
			user
		};
	} catch (e) {
		throw new Error('Invalid refresh token');
	}
}

// Mock user validation - replace with actual database query
export async function validateCredentials(
	username: string,
	password: string
): Promise<User | null> {
	// For demo purposes
	if (username === 'demo' && password === 'demo') {
		return {
			id: 'user-123',
			username: 'demo',
			email: 'demo@tradingplatform.com'
		};
	}

	// In production, query your backend API or database:
	// const response = await fetch(`${API_BASE}/auth/login`, {
	//   method: 'POST',
	//   body: JSON.stringify({ username, password })
	// });
	// return response.ok ? await response.json() : null;

	return null;
}
