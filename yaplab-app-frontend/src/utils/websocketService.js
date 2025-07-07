import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import config from './config.js';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.connecting = false;
        this.user = null;
        this.subscriptions = new Map();
        this.roomSubscriptions = new Map();
        this.messageQueue = [];
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
        this.connectionCallbacks = new Set();
        this.disconnectionCallbacks = new Set();
        this.eventListeners = new Map();
        this.typingTimeouts = new Map();
        this.currentlyTypingRooms = new Set();
        this.isPageVisible = true;
        this.networkOnline = navigator.onLine;
        this.connectionCheckInterval = null;

        // Initialize monitoring
        this.initializeConnectionMonitoring();
    }

    initializeConnectionMonitoring() {
        document.addEventListener('visibilitychange', () => {
            this.isPageVisible = !document.hidden;

            if (this.isPageVisible && this.user && !this.connected) {
                setTimeout(() => this.forceReconnect(), 1000);
            }
        });

        window.addEventListener('online', () => {
            this.networkOnline = true;
            if (this.user && !this.connected) {
                this.handleNetworkReconnect();
            }
        });

        window.addEventListener('offline', () => {
            this.networkOnline = false;
        });

        window.addEventListener('focus', () => {
            if (this.user && !this.connected) {
                setTimeout(() => this.forceReconnect(), 500);
            }
        });
    }

    isConnectionHealthy() {
        if (!this.connected || !this.client) return false;

        if (!this.client.connected) {
            console.warn('Connection unhealthy: STOMP client not connected');
            return false;
        }

        if (this.lastHeartbeat && Date.now() - this.lastHeartbeat > 30000) {
            console.warn('Connection unhealthy: no heartbeat for 30+ seconds');
            return false;
        }

        return true;
    }

    async forceReconnect() {
        if (this.connecting) return;

        this.connected = false;

        if (this.client) {
            try {
                this.client.deactivate();
            } catch (error) {
                console.error('Error deactivating client:', error);
            }
        }

        this.reconnectAttempts = 0;

        try {
            await this.connect(this.user);
        } catch (error) {
            console.error('Force reconnect failed:', error);
        }
    }

    async handleNetworkReconnect() {
        if (!this.networkOnline || this.connecting) return;

        await new Promise(resolve => setTimeout(resolve, 2000));

        if (this.networkOnline && !this.connected) {
            this.forceReconnect();
        }
    }

    startConnectionMonitoring() {
        this.stopConnectionMonitoring();
        this.connectionCheckInterval = setInterval(() => {
            if (this.user && this.isPageVisible && this.networkOnline) {
                // Check connection health more frequently
                if (!this.connected && !this.connecting) {
                    // Connection lost, attempting reconnect...
                    this.forceReconnect();
                } else if (this.connected && this.messageQueue.length > 0) {
                    // Processing queued messages...
                    this.processMessageQueue();
                }
                // Health check for existing connections
                if (this.connected && !this.isConnectionHealthy()) {
                    // Connection unhealthy, forcing reconnect...
                    this.forceReconnect();
                }
            }
        }, 10000); // Check every 10 seconds instead of 60
    }

    // Stop connection monitoring
    stopConnectionMonitoring() {
        if (this.connectionCheckInterval) {
            clearInterval(this.connectionCheckInterval);
            this.connectionCheckInterval = null;
        }
    }

    // Event listener management
    addEventListener(event, callback) {
        if (!this.eventListeners.has(event)) {
            this.eventListeners.set(event, new Set());
        }
        this.eventListeners.get(event).add(callback);
        return () => this.eventListeners.get(event)?.delete(callback);
    }

    notifyEventListeners(event, data) {
        const listeners = this.eventListeners.get(event);
        if (listeners) {
            listeners.forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`Error in ${event} listener:`, error);
                }
            });
        }
    }

    // Connection status listeners
    onConnectionChange(callback) {
        this.connectionCallbacks.add(callback);
        return () => this.connectionCallbacks.delete(callback);
    }

    onDisconnectionChange(callback) {
        this.disconnectionCallbacks.add(callback);
        return () => this.disconnectionCallbacks.delete(callback);
    }

    notifyConnectionChange(status) {
        this.connectionCallbacks.forEach(callback => {
            try {
                callback(status);
            } catch (error) {
                console.error('Error in connection callback:', error);
            }
        });
    }

    notifyDisconnection(reason) {
        this.disconnectionCallbacks.forEach(callback => {
            try {
                callback(reason);
            } catch (error) {
                console.error('Error in disconnection callback:', error);
            }
        });
    }

    connect(user) {
    if (this.connected || this.connecting) {
        return Promise.resolve();
    }

    this.user = user;
    this.connecting = true;
    this.notifyConnectionChange('connecting');

    return new Promise((resolve, reject) => {
        const authToken = localStorage.getItem('authToken');

        if (!authToken) {
            console.error('No authentication token found');
            this.connecting = false;
            this.notifyConnectionChange('disconnected');
            reject(new Error('No authentication token'));
            return;
        }

        try {
            this.client = new Client({
                webSocketFactory: () => {
                    return new SockJS(config.websocket.url);
                },

                connectHeaders: {
                    'Authorization': `Bearer ${authToken}`,
                    'X-User-ID': user.id.toString(),
                    'X-User-Email': user.emailId
                },

                heartbeatIncoming: config.websocket.heartbeatIncoming,
                heartbeatOutgoing: config.websocket.heartbeatOutgoing,
                reconnectDelay: 0,

                onConnect: (frame) => {
    this.connected = true;
    this.connecting = false;
    this.reconnectAttempts = 0;
    this.setupUserSubscriptions();
    this.processMessageQueue();
    this.startConnectionMonitoring();
    this.notifyConnectionChange('connected');
    resolve();
},

                    onDisconnect: (frame) => {
                        this.connected = false;
                        this.connecting = false;
                        this.stopConnectionMonitoring();
                        this.clearSubscriptions();
                        this.notifyConnectionChange('disconnected');

                        if (this.user && localStorage.getItem('authToken') && this.reconnectAttempts < this.maxReconnectAttempts) {
                            this.handleReconnect();
                        }
                    },

                    onStompError: (frame) => {
                        console.error('WebSocket STOMP Error:', frame.headers.message || 'Connection failed');
                        this.connected = false;
                        this.connecting = false;
                        this.notifyConnectionChange('error');

                        if (frame.headers.message && (
                            frame.headers.message.includes('Access Denied') ||
                            frame.headers.message.includes('Unauthorized') ||
                            frame.headers.message.includes('403')
                        )) {
                            console.error('Authentication error detected');
                            this.notifyDisconnection('auth_error');
                            reject(new Error('Authentication failed'));
                        } else {
                            reject(new Error(`WebSocket Error: ${frame.headers.message || 'Connection failed'}`));
                        }
                    },

                    onWebSocketError: (event) => {
                        console.error('WebSocket connection error:', event);
                        this.connected = false;
                        this.connecting = false;
                        this.notifyConnectionChange('error');

                        if (this.reconnectAttempts === 0) {
                            this.handleReconnect().catch(() => {
                                reject(new Error('WebSocket connection failed'));
                            });
                        } else {
                            reject(new Error('WebSocket connection failed'));
                        }
                    }
                });

                this.client.activate();

                // Connection timeout
                setTimeout(() => {
                    if (this.connecting && !this.connected) {
                        console.warn('WebSocket connection timeout');
                        this.client.deactivate();
                        this.connecting = false;
                        this.notifyConnectionChange('timeout');
                        reject(new Error('Connection timeout'));
                    }
                }, 15000);

            } catch (error) {
                console.error('Failed to create WebSocket connection:', error);
                this.connecting = false;
                this.notifyConnectionChange('error');
                reject(error);
            }
        });
    }

setupUserSubscriptions() {
    if (!this.user) return;

    this.subscribe(`/user/${this.user.id}/messages`, (message) => {
        this.notifyEventListeners('personalMessage', message);
    });

    this.subscribe(`/user/${this.user.id}/status`, (update) => {
        this.notifyEventListeners('messageStatus', update);
    });

    this.subscribe('/topic/user-status', (userStatus) => {
        this.notifyEventListeners('userStatusUpdate', userStatus);
    });
}


joinRoom(roomId, chatData = null) {
        // Ensure connection before joining room
        if (!this.connected) {
            // Not connected, attempting reconnect before joining room...
            this.forceReconnect().then(() => {
                this.joinRoom(roomId, chatData);
            });
            return;
        }
    if (this.roomSubscriptions.has(roomId)) {
        return;
    }

    this.send(`/app/chatroom.join/${roomId}`, {
        id: this.user.id,
        userName: this.user.userName,
        emailId: this.user.emailId
    });

    const subscriptions = [];

    const messagesSub = this.subscribe(`/room/${roomId}/messages`, (message) => {
        // 📨 WebSocket: Room message received for room
        this.notifyEventListeners('roomMessage', { roomId, message });
    });
    subscriptions.push(messagesSub);

    const eventsSub = this.subscribe(`/room/${roomId}/events`, (event) => {
        this.handleRoomEvent(roomId, event);
    });
    subscriptions.push(eventsSub);

    const isGroupChat = chatData?.chatRoomType === 'GROUP' || roomId.startsWith('group_');
    if (isGroupChat) {
        const groupId = chatData?.group?.id || roomId.replace('group_', '');
        const groupEventsSub = this.subscribe(`/topic/group/${groupId}`, (event) => {
            this.handleGroupEvent(groupId, event);
        });
        subscriptions.push(groupEventsSub);
    }

    this.roomSubscriptions.set(roomId, subscriptions);
}

    leaveRoom(roomId) {
        const subscriptions = this.roomSubscriptions.get(roomId);
        if (subscriptions) {
            this.send('/app/chatroom.leave', {
                id: this.user.id,
                userName: this.user.userName,
                emailId: this.user.emailId
            });

            subscriptions.forEach(sub => {
                if (sub && sub.unsubscribe) {
                    sub.unsubscribe();
                }
            });
            this.roomSubscriptions.delete(roomId);
        }
    }

    handleRoomEvent(roomId, event) {
        switch (event.type) {
            case 'MESSAGE_STATUS_UPDATE':
            this.notifyEventListeners('messageStatus', {
                roomId,
                messageIds: event.messageIds,
                status: event.status,
                chatroomId: event.chatroomId
            });
            break;
            case 'TYPING':
            case 'STOP_TYPING':
                this.notifyEventListeners('roomEvent', { ...event, roomId });
                break;
            case 'MESSAGE_EDITED':
                this.notifyEventListeners('messageEdited', {
                    roomId,
                    messageId: event.messageId,
                    newContent: event.newContent,
                    edited: event.edited,
                    editTimestamp: event.editTimestamp
                });
                break;
            case 'MESSAGE_DELETED':
                this.notifyEventListeners('messageDeleted', {
                    roomId,
                    messageId: event.messageId,
                    softDeleted: event.softDeleted
                });
                break;
            case 'MULTIPLE_MESSAGES_DELETED':
                this.notifyEventListeners('multipleMessagesDeleted', {
                    roomId,
                    messageIds: event.messageIds,
                    softDeleted: event.softDeleted,
                    userId: event.userId
                });
                break;
            case 'MESSAGES_DELETED':
                this.notifyEventListeners('messagesDeleted', {
                    roomId,
                    messageIds: event.messageIds,
                    deletedBy: event.deletedBy
                });
                break;
            case 'USER_JOINED':
                this.notifyEventListeners('userJoined', {
                    roomId,
                    user: event.user
                });
                break;
            case 'USER_LEFT':
                this.notifyEventListeners('userLeft', {
                    roomId,
                    user: event.user
                });
                break;
            default:
                break;
        }
    }

    handleGroupEvent(groupId, event) {
        switch (event.type) {
            case 'GROUP_UPDATED':
                console.log('Received group update:', event);
                this.notifyEventListeners('groupUpdated', {
                    groupId: parseInt(groupId),
                    ...event.data
                });
                break;
            case 'USER_JOINED':
                this.notifyEventListeners('groupMemberAdded', {
                    groupId: parseInt(groupId),
                    userId: event.userId,
                    username: event.username,
                    timestamp: event.timestamp
                });
                break;
            case 'USER_LEFT':
                this.notifyEventListeners('groupMemberRemoved', {
                    groupId: parseInt(groupId),
                    userId: event.userId,
                    username: event.username,
                    timestamp: event.timestamp
                });
                break;
            default:
                console.log('Unhandled group event:', event);
                break;
        }
    }

    async handleReconnect() {
        // Try token refresh before giving up
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            try {
                console.log('Max attempts reached, trying token refresh...');
                await this.refreshAuthToken();
                this.reconnectAttempts = 0; // Reset attempts after token refresh
            } catch (error) {
                console.error('Token refresh failed:', error);
                this.notifyDisconnection('auth_expired');
                return Promise.reject(new Error('Authentication expired'));
            }
        }
        if (!this.user || !localStorage.getItem('authToken')) {
            this.notifyDisconnection('no_auth');
            return Promise.reject(new Error('No authentication available'));
        }
        this.reconnectAttempts++;
        const delay = Math.min(this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1), 30000);
        this.notifyConnectionChange('reconnecting');
        return new Promise((resolve, reject) => {
            setTimeout(async () => {
                try {
                    await this.connect(this.user);
                    resolve();
                } catch (error) {
                    console.error(`Reconnection attempt ${this.reconnectAttempts} failed:`, error);
                    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                        this.handleReconnect().then(resolve).catch(reject);
                    } else {
                        this.handleReconnect().then(resolve).catch(reject);
                    }
                }
            }, delay);
        });
    }

    async refreshAuthToken() {
        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                    'Content-Type': 'application/json'
                }
            });
            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('authToken', data.token);
                console.log('Token refreshed successfully');
                return data.token;
            } else {
                throw new Error('Token refresh failed');
            }
        } catch (error) {
            console.error('Token refresh error:', error);
            throw error;
        }
    }

    subscribe(destination, callback) {
        if (!this.connected) {
            this.messageQueue.push({ type: 'subscribe', destination, callback });
            return null;
        }

        try {
            const subscription = this.client.subscribe(destination, (message) => {
                try {
                    let parsedMessage;
                    try {
                        parsedMessage = JSON.parse(message.body);
                    } catch (parseError) {
                        parsedMessage = message.body;
                    }

                    callback(parsedMessage);
                } catch (error) {
                    console.error(`Error handling message on ${destination}:`, error);
                    callback(message.body);
                }
            });

            this.subscriptions.set(destination, subscription);
            return subscription;
        } catch (error) {
            console.error('Failed to subscribe to:', destination, error);
            return null;
        }
    }

    send(destination, message, headers = {}) {
        if (!this.connected) {
            this.messageQueue.push({ type: 'send', destination, message, headers });
            // Notify frontend that message is queued, not sent
            this.notifyEventListeners('messageQueued', {
                destination,
                message,
                status: 'queued'
            });
            // Try to reconnect immediately
            if (!this.connecting && this.user) {
                this.forceReconnect();
            }
            return false; // Indicate message wasn't sent
        }
        try {
            this.client.publish({
                destination: destination,
                body: JSON.stringify(message),
                headers: headers
            });
            // Notify successful send
            this.notifyEventListeners('messageSent', {
                destination,
                message,
                status: 'sent'
            });
            return true; // Message sent successfully
        } catch (error) {
            console.error('Failed to send message:', error);
            // Try reconnection on send failure
            if (!this.connecting && this.user) {
                this.forceReconnect();
            }
            return false;
        }
    }    // Enhanced messaging methods
    sendPersonalMessage(message) {
        const personalMessageData = {
            senderId: message.senderId,
            receiverId: message.receiverId,
            content: message.content,
            groupId: null,
            fileId: message.fileId || null,
            fileUrl: message.fileUrl || null,
            fileName: message.fileName || null,
            fileSize: message.fileSize || null,
            repliedToMessageId: message.repliedToMessageId || null,
            edited: message.edited || false,
            forwarded: message.forwarded || false,
            editTimestamp: message.editTimestamp || null
        };
        
        this.send('/app/messages/personal', personalMessageData);
    }

    sendGroupMessage(message) {
        const groupMessageData = {
            senderId: message.senderId,
            receiverId: null,
            content: message.content,
            groupId: message.groupId,
            fileId: message.fileId || null,
            fileUrl: message.fileUrl || null,
            fileName: message.fileName || null,
            fileSize: message.fileSize || null,
            repliedToMessageId: message.repliedToMessageId || null,
            edited: message.edited || false,
            forwarded: message.forwarded || false,
            editTimestamp: message.editTimestamp || null
        };
        
        this.send('/app/messages/group', groupMessageData);
    }

    // Typing indicators with auto-stop
    startTyping(roomId) {
        if (this.currentlyTypingRooms.has(roomId)) {
            return; // Already typing in this room
        }

        this.currentlyTypingRooms.add(roomId);
        this.send(`/app/messages/typing/${roomId}`, {});

        // Clear existing timeout for this room
        if (this.typingTimeouts.has(roomId)) {
            clearTimeout(this.typingTimeouts.get(roomId));
        }

        // Auto-stop typing after 3 seconds of inactivity
        const timeout = setTimeout(() => {
            this.stopTyping(roomId);
        }, 3000);

        this.typingTimeouts.set(roomId, timeout);
    }

    stopTyping(roomId) {
        if (!this.currentlyTypingRooms.has(roomId)) {
            return; // Not typing in this room
        }

        this.currentlyTypingRooms.delete(roomId);
        this.send(`/app/messages/stop-typing/${roomId}`, {});

        // Clear timeout
        if (this.typingTimeouts.has(roomId)) {
            clearTimeout(this.typingTimeouts.get(roomId));
            this.typingTimeouts.delete(roomId);
        }
    }

    markMessagesAsRead(chatRoomId, messageIds) {
    if (!this.connected || !chatRoomId || !Array.isArray(messageIds) || messageIds.length === 0) {
        return;
    }

    this.send('/app/messages.read', {
        chatroomId: chatRoomId,
        messageIds: messageIds,
        userId: this.user?.id
    });
}

markMessagesAsDelivered(chatRoomId, messageIds) {
    if (!this.connected || !chatRoomId || !Array.isArray(messageIds) || messageIds.length === 0) {
        return;
    }

    this.send('/app/messages.delivered', {
        chatroomId: chatRoomId,
        messageIds: messageIds,
        userId: this.user?.id
    });
}

    editMessage(messageId, newContent) {
        if (!this.connected || !messageId || !newContent) {
            return;
        }

        this.send('/app/messages/edit', {
            messageId: messageId,
            newContent: newContent
        });
    }

    forwardMessage(messageId, targetRoomId) {
        if (!this.connected || !messageId || !targetRoomId) {
            return;
        }

        this.send('/app/messages/forward', {
            messageId: messageId,
            targetRoomId: targetRoomId,
            senderId: this.user?.id
        });
    }

    performMultipleOperation(operation, messageIds, targetRoomId = null) {
        if (!this.connected || !operation || !Array.isArray(messageIds) || messageIds.length === 0) {
            return;
        }

        this.send('/app/messages/multiple-operations', {
            operation: operation,
            messageIds: messageIds,
            targetRoomId: targetRoomId,
            userId: this.user?.id
        });
    }

    deleteMultipleMessages(messageIds) {
        this.performMultipleOperation('DELETE', messageIds);
    }

    forwardMultipleMessages(messageIds, targetRoomId) {
        this.performMultipleOperation('FORWARD', messageIds, targetRoomId);
    }

    markMultipleMessagesAsRead(messageIds, roomId) {
        this.performMultipleOperation('MARK_READ', messageIds, roomId);
    }

    async deleteMessage(messageId, userId) {
        // This is handled by the Message component using REST API
        // WebSocket events will handle real-time updates
    }

    processMessageQueue() {
        while (this.messageQueue.length > 0) {
            const item = this.messageQueue.shift();
            
            if (item.type === 'send') {
                this.send(item.destination, item.message, item.headers);
            } else if (item.type === 'subscribe') {
                this.subscribe(item.destination, item.callback);
            }
        }
    }

    disconnect() {
        this.stopConnectionMonitoring();
        
        this.currentlyTypingRooms.forEach(roomId => {
            this.stopTyping(roomId);
        });
        
        this.typingTimeouts.forEach(timeout => clearTimeout(timeout));
        this.typingTimeouts.clear();
        
        this.user = null;
        this.connected = false;
        this.connecting = false;
        this.reconnectAttempts = 0;
        
        this.clearSubscriptions();
        this.messageQueue = [];
        
        if (this.client) {
            this.client.deactivate();
            this.client = null;
        }
        
        this.notifyConnectionChange('disconnected');
    }

    clearSubscriptions() {
        this.subscriptions.forEach((subscription, destination) => {
            try {
                subscription.unsubscribe();
            } catch (error) {
                console.error('Error unsubscribing from:', destination, error);
            }
        });
        this.subscriptions.clear();
        
        this.roomSubscriptions.forEach((subscriptions, roomId) => {
            subscriptions.forEach(sub => {
                try {
                    if (sub && sub.unsubscribe) {
                        sub.unsubscribe();
                    }
                } catch (error) {
                    console.error('Error unsubscribing from room:', roomId, error);
                }
            });
        });
        this.roomSubscriptions.clear();
    }

    isConnected() {
        return this.connected;
    }

    getCurrentUser() {
        return this.user;
    }

    getConnectionStatus() {
        if (this.connected) return 'connected';
        if (this.connecting) return 'connecting';
        return 'disconnected';
    }

    removeEventListener(event, callback) {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
        listeners.delete(callback);
        if (listeners.size === 0) {
            this.eventListeners.delete(event);
        }
    }
}

    subscribeToChatRoom(chatRoomId, callback) {
        this.joinRoom(chatRoomId);
        return this.addEventListener('roomMessage', (data) => {
            if (data.roomId === chatRoomId) {
                callback(data.message);
            }
        });
    }

    subscribeToUserQueue(userId, callback) {
        return this.addEventListener('personalMessage', callback);
    }

    subscribeToPersonalMessages(userId, callback) {
        return this.addEventListener('personalMessage', callback);
    }

    subscribeToMessageStatus(callback) {
        return this.addEventListener('messageStatus', callback);
    }

    subscribeToUserStatus(callback) {
    return this.addEventListener('userStatusUpdate', callback);
}

subscribeToUserActivity(callback) {
    return this.addEventListener('userStatusUpdate', callback);
}

/**
     * Subscribe to group events (updates, member changes, etc)
     */
    subscribeToGroupEvents(groupId, callback) {
        if (!this.client?.connected) {
            console.warn('WebSocket not connected when trying to subscribe to group events');
            return () => {};
        }
        
        const groupEventsTopic = `/topic/group/${groupId}`;
        
        // Use addEventListener for consistent event handling
        const unsubscribe = this.addEventListener('groupUpdated', (data) => {
            if (data.groupId === parseInt(groupId)) {
                callback({
                    type: 'GROUP_UPDATED',
                    data: data
                });
            }
        });

        const memberUnsubscribe = this.addEventListener('groupMemberAdded', (data) => {
            if (data.groupId === parseInt(groupId)) {
                callback({
                    type: 'USER_JOINED',
                    ...data
                });
            }
        });

        const memberRemoveUnsubscribe = this.addEventListener('groupMemberRemoved', (data) => {
            if (data.groupId === parseInt(groupId)) {
                callback({
                    type: 'USER_LEFT',
                    ...data
                });
            }
        });

        // Subscribe to the actual WebSocket topic
        const subscription = this.client.subscribe(groupEventsTopic, (message) => {
            try {
                const event = JSON.parse(message.body);
                this.handleGroupEvent(groupId, event);
            } catch (error) {
                console.error('Error parsing group event:', error);
            }
        });
        
        this.subscriptions.set(groupEventsTopic, subscription);
        
        return () => {
            unsubscribe();
            memberUnsubscribe();
            memberRemoveUnsubscribe();
            subscription.unsubscribe();
            this.subscriptions.delete(groupEventsTopic);
        };
    }

}

const websocketService = new WebSocketService();
export default websocketService;