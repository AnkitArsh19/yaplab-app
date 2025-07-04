import React, { useMemo } from 'react';
import '../../styles/Sidebar.css';
import ContactCard from '../ui/ContactCard';
import SettingsMenu from '../ui/SettingsMenu';
import SearchBar from './SearchBar';
import apiClient from '../../utils/apiClient';

function Sidebar({ chats = [], selectedChatId, onSelectChat, loading, error, onLogout, onStartNewChat, currentUser, onCreateGroup, onUserUpdate }) {

    const handleContactSelect = async (contact, existingChatId = null) => {
        try {
            await onStartNewChat(contact, existingChatId);
        } catch (error) {
            console.error('Error starting new chat:', error);
        }
    };

    const handleChatSelect = (chatId) => {
        onSelectChat(chatId);
    };

    const handleUserUpdate = async (updatedUserData) => {
        try {
            const userUpdatePayload = {
                id: currentUser.id,
                userName: updatedUserData.userName || currentUser.userName,
                emailId: updatedUserData.emailId || currentUser.emailId,
                mobileNumber: updatedUserData.mobileNumber || currentUser.mobileNumber,
                password: 'dummy123456'
            };

            const response = await apiClient.put('/users/update', userUpdatePayload);
            
            if (response.ok) {
                const updatedUser = await response.json();
                if (onUserUpdate) {
                    onUserUpdate(updatedUser);
                }
                return updatedUser;
            } else {
                throw new Error('Failed to update user');
            }
        } catch (error) {
            console.error('Error updating user:', error);
            throw error;
        }
    };

    const handleCreateGroup = () => {
        if (onCreateGroup) {
            onCreateGroup();
        }
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) return '';
        
        const messageDate = new Date(timestamp);
        if (isNaN(messageDate.getTime())) return '';
        
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
        const messageDateOnly = new Date(messageDate.getFullYear(), messageDate.getMonth(), messageDate.getDate());
        
        if (messageDateOnly.getTime() === today.getTime()) {
            return messageDate.toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
        } else if (messageDateOnly.getTime() === yesterday.getTime()) {
            return 'Yesterday';
        } else {
            return messageDate.toLocaleDateString([], { 
                month: 'short', 
                day: 'numeric' 
            });
        }
    };

    const sortedChats = useMemo(() => {
        return [...chats].sort((a, b) => {
            const getTimestamp = (chat) => {
                return chat.lastActivity || chat.timestamp || chat.updatedAt || chat.createdAt || 0;
            };
            
            const timeA = new Date(getTimestamp(a));
            const timeB = new Date(getTimestamp(b));
            
            const validTimeA = isNaN(timeA.getTime()) ? new Date(0) : timeA;
            const validTimeB = isNaN(timeB.getTime()) ? new Date(0) : timeB;
            
            return validTimeB - validTimeA;
        });
    }, [chats]);

    return (
        <div className='sidebar'>
            <div className='sidebar-header'>
                <div className="logo">
                    <img className="logo-image" src="logo-white.png" alt="logo-icon" />
                    <img className="logo-name" src="logoname-white.png" alt="logo-name" />
                </div>
                <div className='searchbar-container'>
                    <SettingsMenu 
                        onLogout={onLogout} 
                        onCreateGroup={handleCreateGroup} 
                        currentUser={currentUser}
                        onUserUpdate={handleUserUpdate}
                    /> 
                    <SearchBar 
                        onSelectContact={handleContactSelect} 
                        existingChats={chats} 
                    />
                </div>
            </div>        
            <div className='chatHistory'>
                {loading ? (
                    <div className="loading-message">Loading your chats...</div>
                ) : error ? (
                    <div className="error-message">{error}</div>
                ) : sortedChats.length === 0 ? (
                    <div className="empty-chats-message">
                        <p>No chat history found.</p>
                        <p>Search for contacts above to start a new chat!</p>
                    </div>
                ) : (
                    sortedChats.map((chat, index) => (
                        <ContactCard
                            key={chat.id || chat.chatroomId}
                            profilePicture={chat.profilePicture}
                            name={chat.name}
                            lastMessage={chat.lastMessage || "No messages yet"}
                            timestamp={chat.timestamp || formatTimestamp(chat.lastActivity)}
                            isSelected={selectedChatId === (chat.id || chat.chatroomId)}
                            onClick={() => handleChatSelect(chat.id || chat.chatroomId)}
                            animationDelay={index * 0.1}
                        />
                    ))
                )}
            </div>
        </div>
    );
}

export default Sidebar;