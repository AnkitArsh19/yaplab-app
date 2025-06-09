import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AuthPage from './Components/AuthPage.jsx';
import ContactCard from './Components/ContactCard.jsx';
import Sidebar from './Components/Sidebar.jsx';

function App() {
    const [chats, setChats] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [userId, setUserId] = useState(null);

    useEffect(() => {
        const token = localStorage.getItem('authToken');
        const storedUserId = localStorage.getItem('userId');
        if (token && storedUserId) {
            setIsAuthenticated(true);
            setUserId(parseInt(storedUserId));
            fetchChats(token, parseInt(storedUserId));
        }
    }, []);

    const fetchChats = async (token, userId) => {
        setLoading(true);
        try {
            const response = await fetch(`http://localhost:8080/chatrooms/user/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                setChats(data);
            } else {
                throw new Error('Failed to fetch chats');
            }
        } catch (error) {
            console.error('Error fetching chats:', error);
            setError('Failed to load chats. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleLogin = (loginData) => {
        localStorage.setItem('authToken', loginData.accessToken);
        localStorage.setItem('userId', loginData.id.toString());
        setIsAuthenticated(true);
        setUserId(loginData.id);
        fetchChats(loginData.accessToken, loginData.id);
    };

    const handleLogout = () => {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userId');
        setIsAuthenticated(false);
        setUserId(null);
        setChats([]);
    };

    const handleSelectChat = (chatId) => {
        console.log("Selected chat ID:", chatId);
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
                        <Sidebar 
                            chats={chats} 
                            onSelectChat={handleSelectChat} 
                            loading={loading}
                            error={error}
                            onLogout={handleLogout}
                        /> : 
                        <Navigate to="/" />
                    } 
                />
            </Routes>
        </div>
    );
}

export default App;