class ApiClient {
    constructor() {
        this.baseURL = 'http://localhost:8080';
        this.isRefreshing = false;
        this.failedQueue = [];
    }

    processQueue(error, token = null) {
        this.failedQueue.forEach(({ resolve, reject }) => {
            if (error) {
                reject(error);
            } else {
                resolve(token);
            }
        });
        
        this.failedQueue = [];
        }

    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        const token = localStorage.getItem('authToken');
        const refreshToken = localStorage.getItem('refreshToken');

        // Prepare headers - only set Content-Type if not explicitly overridden
        const headers = {};
        
        // Only set Content-Type for non-FormData requests
        if (!options.body || !(options.body instanceof FormData)) {
            headers['Content-Type'] = 'application/json';
        }
        
        // Add custom headers from options
        if (options.headers) {
            Object.assign(headers, options.headers);
        }

        const config = {
            headers,
            ...options
        };
        if (token && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        if (refreshToken && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
            config.headers['X-Refresh-Token'] = refreshToken;
        }

        try {
            const response = await fetch(url, config);
            
            // Check for new access token from backend refresh
            const newAccessToken = response.headers.get('Authorization');
            if (newAccessToken) {
                const newToken = newAccessToken.replace('Bearer ', '');
                localStorage.setItem('authToken', newToken);
            }

            // Handle token expiration (but not for auth endpoints)
            if (response.status === 401 && !endpoint.includes('/auth/')) {
                const errorData = await response.json().catch(() => ({}));
                
                if (errorData.code === 'TOKEN_EXPIRED' && refreshToken && !this.isRefreshing) {
                    return this.handleTokenRefresh(endpoint, options);
                } else {
                    this.handleUnauthorized();
                    throw new Error('Session expired. Please login again.');
                }
            }

            // Handle other unauthorized responses (but not for auth endpoints)
            if (response.status === 403 && !endpoint.includes('/auth/')) {
                this.handleUnauthorized();
                throw new Error('Access denied.');
            }

            return response;
        } catch (error) {
            // If it's a network error and we have tokens, try refresh (but not for auth endpoints)
            if (error.name === 'TypeError' && refreshToken && !this.isRefreshing && !endpoint.includes('/auth/')) {
                try {
                    return await this.handleTokenRefresh(endpoint, options);
                } catch (refreshError) {
                    console.error('API Request failed:', error);
                    throw error;
                }
            }
            
            console.error('API Request failed:', error);
            throw error;
        }
    }

    async handleTokenRefresh(originalEndpoint, originalOptions) {
        if (this.isRefreshing) {
            // If already refreshing, queue this request
            return new Promise((resolve, reject) => {
                this.failedQueue.push({ resolve, reject });
            }).then(() => {
                // Retry original request with new token
                return this.request(originalEndpoint, originalOptions);
            }).catch(err => {
                throw err;
            });
        }

        this.isRefreshing = true;
        const refreshToken = localStorage.getItem('refreshToken');

        try {
            const refreshResponse = await fetch(`${this.baseURL}/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ refreshToken })
            });

            if (refreshResponse.ok) {
                const refreshData = await refreshResponse.json();
                const newAccessToken = refreshData.accessToken;
                
                localStorage.setItem('authToken', newAccessToken);
                this.isRefreshing = false;
                this.processQueue(null, newAccessToken);

                // Retry original request with new token
                return this.request(originalEndpoint, originalOptions);
            } else {
                throw new Error('Token refresh failed');
            }
        } catch (error) {
            this.isRefreshing = false;
            this.processQueue(error, null);
            this.handleUnauthorized();
            throw new Error('Session expired. Please login again.');
        }
    }

    async get(endpoint, options = {}) {
        return this.request(endpoint, { ...options, method: 'GET' });
    }

    async post(endpoint, data, options = {}) {
        return this.request(endpoint, {
            ...options,
            method: 'POST',
            body: data ? JSON.stringify(data) : undefined
        });
    }

    async put(endpoint, data, options = {}) {
        return this.request(endpoint, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async delete(endpoint, options = {}) {
        return this.request(endpoint, { ...options, method: 'DELETE' });
    }

    // Forward a single message to a target chat room
    async forwardMessage(messageId, targetRoomId, senderId) {
        return this.post(`/messages/${messageId}/forward`, {
            targetRoomId,
            senderId
        });
    }

    // Forward multiple messages to a target chat room
    async forwardMultipleMessages(messageIds, targetRoomId, senderId) {
        return this.post('/messages/forward/multiple', {
            messageIds,
            targetRoomId,
            userId: senderId
        });
    }

    // Group-related API methods
    async createGroup(groupData, createdById) {
        return this.post(`/groups/create?createdById=${createdById}`, groupData);
    }

    async getGroupDetails(groupId) {
        return this.get(`/groups/${groupId}`);
    }

    async addUserToGroup(userId, groupId) {
        return this.post(`/groups/adduser?userId=${userId}&groupId=${groupId}`);
    }

    async removeUserFromGroup(userId, groupId) {
        return this.delete(`/groups/removeuser?userId=${userId}&groupId=${groupId}`);
    }

    async updateGroupName(groupId, groupData, userId) {
        return this.put(`/groups/${groupId}?userId=${userId}`, groupData);
    }

    async deleteGroup(groupId, userId) {
        return this.delete(`/groups/${groupId}?userId=${userId}`);
    }

    async uploadGroupProfilePicture(groupId, file) {
        const formData = new FormData();
        formData.append('file', file);
        
        return this.request(`/groups/${groupId}/profile-picture`, {
            method: 'POST',
            body: formData
        });
    }

    async uploadUserProfilePicture(userId, file) {
        const formData = new FormData();
        formData.append('file', file);
        
        return this.request(`/users/${userId}/profile-picture`, {
            method: 'POST',
            body: formData
        });
    }

    async getOrCreateGroupChatRoom(groupId) {
        return this.post('/chatrooms/group', {
            groupId: groupId,
            chatroomType: 'GROUP'
        });
    }

    handleUnauthorized() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userId');
        if (typeof window !== 'undefined') {
            window.location.href = '/';
        }
    }

    isAuthenticated() {
        const token = localStorage.getItem('authToken');
        const refreshToken = localStorage.getItem('refreshToken');
        return !!(token && refreshToken);
    }
}

const apiClient = new ApiClient();
export default apiClient;