import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AuthPage from './components/auth/AuthPage';
import ChatWindow from './components/chat/ChatWindow';
import EmailChangeConfirmation from './components/modals/EmailChangeConfirmation';
import DeveloperProfile from './components/DeveloperProfile';
import apiClient from './utils/apiClient.js';
import websocketService from './utils/websocketService.js';

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState(null);
    const [wsConnectionState, setWsConnectionState] = useState('disconnected');

    useEffect(() => {
        const token = localStorage.getItem('authToken');
        const refreshToken = localStorage.getItem('refreshToken');
        const storedUserId = localStorage.getItem('userId');
        const storedUserName = localStorage.getItem('userName');
        const storedEmailId = localStorage.getItem('emailId');
        
        if (token && refreshToken && storedUserId) {
            setIsAuthenticated(true);
            const userData = {
                id: parseInt(storedUserId),
                userName: storedUserName,
                emailId: storedEmailId
            };
            setUser(userData);
            
            // Set up WebSocket connection listeners
            setupWebSocketListeners();
            
            // Attempt WebSocket connection after page reload
            connectWebSocket(userData);
        }

        // Handle browser/tab close to disconnect websocket
        const handleBeforeUnload = () => {
            try {
                websocketService.disconnect();
            } catch (e) {
            }
        };
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, []);

    const setupWebSocketListeners = () => {
        // Set up connection status listener
        const unsubscribeConnection = websocketService.onConnectionChange((status) => {
            setWsConnectionState(status);
        });

        // Set up disconnection listener for auth errors and max attempts
        const unsubscribeDisconnection = websocketService.onDisconnectionChange((reason) => {
            if (reason === 'auth_error' || reason === 'auth_expired') {
                setWsConnectionState('auth_error');
            } else if (reason === 'max_attempts_reached' || reason === 'no_auth') {
                setWsConnectionState('max_attempts_reached');
            }
        });

        // Store cleanup functions
        window.wsCleanup = () => {
            unsubscribeConnection();
            unsubscribeDisconnection();
        };
    };

    const connectWebSocket = async (userData) => {
        try {
            await websocketService.connect(userData);
        } catch (error) {
            console.error('WebSocket connection failed:', error);
            
            // If authentication failed, logout user
            if (error.message?.includes('Authentication failed')) {
                console.error('WebSocket authentication failed - forcing logout');
                handleLogout();
            }
        }
    };

    const handleLogin = async (loginData) => {
        localStorage.setItem('authToken', loginData.accessToken);
        localStorage.setItem('refreshToken', loginData.refreshToken);
        localStorage.setItem('userId', loginData.id.toString());
        localStorage.setItem('userName', loginData.userName);
        localStorage.setItem('emailId', loginData.emailId);
        
        const userData = {
            id: loginData.id,
            userName: loginData.userName,
            emailId: loginData.emailId
        };
        setUser(userData);
        setIsAuthenticated(true);

        // Set up listeners before connecting
        setupWebSocketListeners();
        
        // Connect WebSocket
        await connectWebSocket(userData);
    };

    const handleLogout = async () => {
        try {
            websocketService.disconnect();
            setWsConnectionState('disconnected');
            
            await apiClient.post('/auth/logout');
        } catch (error) {
            console.error('Logout API call failed:', error);
        } finally {
            // Clean up WebSocket listeners
            if (window.wsCleanup) {
                window.wsCleanup();
                delete window.wsCleanup;
            }
            
            localStorage.removeItem('authToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userId');
            localStorage.removeItem('userName');
            localStorage.removeItem('emailId');
            setIsAuthenticated(false);
            setUser(null);
        }
    };

    return (
        <div className="app">
            <Routes>
                <Route 
                    path="/" 
                    element={isAuthenticated ? <Navigate to="/chat" /> : <AuthPage onLoginSuccess={handleLogin} />} 
                />
                <Route 
                    path="/chat" 
                    element={
                        isAuthenticated ? 
                        <ChatWindow 
                            user={user}
                            onLogout={handleLogout}
                            wsConnectionState={wsConnectionState}
                        /> : 
                        <Navigate to="/" />
                    } 
                />
                <Route 
                    path="/auth/confirm-email-change" 
                    element={<EmailChangeConfirmation />} 
                />
                <Route
                    path="/developer"
                    element={<DeveloperProfile />}
                />
            </Routes>
        </div>
    );
}

export default App;