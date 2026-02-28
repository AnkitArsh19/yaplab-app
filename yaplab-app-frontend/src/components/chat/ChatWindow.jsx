import React, { useState, useEffect } from 'react';
import Sidebar from './Sidebar';
import ChatArea from './ChatArea';
import ContactSelectionModal from '../modals/ContactSelectionModal';
import GroupDetailsModal from '../groups/GroupDetailsModal';
import Notification from '../ui/Notification';
import '../../styles/ChatWindow.css';
import apiClient from '../../utils/apiClient';
import websocketService from '../../utils/websocketService';
import { formatMessageForList } from '../../utils/messageUtils';

function ChatWindow({ user, onLogout, wsConnectionState }) {
    const [chats, setChats] = useState([]);
    const [selectedChat, setSelectedChat] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [userStatuses, setUserStatuses] = useState({});
    const [showCreateGroupModal, setShowCreateGroupModal] = useState(false);
    const [showGroupDetailsModal, setShowGroupDetailsModal] = useState(false);
    const [selectedContactsForGroup, setSelectedContactsForGroup] = useState([]);
    const [notification, setNotification] = useState(null);

    // Effect 1: Load chats when user becomes available (runs once per user)
    useEffect(() => {
        if (user?.id) {
            loadUserChats();
        } else {
            setLoading(false);
            setChats([]);
        }
    }, [user?.id]);

    // Effect 2: Set up WebSocket event subscriptions when connection is ready
    useEffect(() => {
        if (user?.id && wsConnectionState === 'connected') {
            setupWebSocketSubscriptions();
        }

        return () => {
            if (window.wsSubscriptionCleanup) {
                window.wsSubscriptionCleanup();
                delete window.wsSubscriptionCleanup;
            }
        };
    }, [wsConnectionState]);

    // Effect 3: Join group rooms when both chats are loaded and WS is connected
    useEffect(() => {
        if (wsConnectionState === 'connected' && chats.length > 0) {
            chats.forEach(chat => {
                if (chat.chatRoomType === 'GROUP') {
                    websocketService.joinRoom(chat.id, chat);
                }
            });
        }
    }, [wsConnectionState, chats.length]);

    useEffect(() => {
        if (wsConnectionState === 'max_attempts_reached' || wsConnectionState === 'auth_error') {
            onLogout();
        }
    }, [wsConnectionState, onLogout]);

    const setupWebSocketSubscriptions = async () => {
        const unsubscribePersonal = websocketService.addEventListener('personalMessage', handleIncomingMessage);
        const unsubscribeRoomMessage = websocketService.addEventListener('roomMessage', (data) => {
            handleIncomingMessage(data.message);
        });
        const unsubscribeStatus = websocketService.addEventListener('messageStatus', handleMessageStatusUpdate);
        const unsubscribeUserStatus = websocketService.addEventListener('userStatusUpdate', handleUserStatusUpdate);

        try {
            const response = await apiClient.get(`/chatrooms/user/${user.id}`);
            if (response.ok) {
                const chatrooms = await response.json();
                chatrooms.forEach(chatRoom => {
                    if (chatRoom.chatRoomType === 'PERSONAL') {
                        websocketService.joinRoom(chatRoom.chatroomId);
                    }
                });
            }
        } catch (err) {
            console.error('Failed to subscribe to all personal chatrooms for DELIVERED status:', err);
        }

        try {
            const response = await apiClient.get('/users/status/comprehensive');
            if (response.ok) {
                const comprehensiveStatuses = await response.json();
                
                // Convert lastSeen from ISO string to timestamp if needed
                const processedStatuses = {};
                Object.keys(comprehensiveStatuses).forEach(userId => {
                    const status = comprehensiveStatuses[userId];
                    processedStatuses[userId] = {
                        ...status,
                        lastSeen: status.lastSeen ? (typeof status.lastSeen === 'string' ? new Date(status.lastSeen).getTime() : status.lastSeen) : null
                    };
                });
                
                setUserStatuses(processedStatuses);
            } else {
                const onlineResponse = await apiClient.get('/users/list/ONLINE');
                if (onlineResponse.ok) {
                    const onlineUsers = await onlineResponse.json();
                    const initialStatuses = {};
                    onlineUsers.forEach(onlineUser => {
                        initialStatuses[onlineUser.id] = {
                            userStatus: 'ONLINE',
                            lastSeen: null,
                            userName: onlineUser.userName
                        };
                    });
                    setUserStatuses(initialStatuses);
                }
            }
        } catch (error) {
            console.error('Failed to fetch initial user statuses:', error);
            try {
                const onlineResponse = await apiClient.get('/users/list/ONLINE');
                if (onlineResponse.ok) {
                    const onlineUsers = await onlineResponse.json();
                    const initialStatuses = {};
                    onlineUsers.forEach(onlineUser => {
                        initialStatuses[onlineUser.id] = {
                            userStatus: 'ONLINE',
                            lastSeen: null,
                            userName: onlineUser.userName
                        };
                    });
                    setUserStatuses(initialStatuses);
                }
            } catch (fallbackError) {
                console.error('Fallback status fetch failed:', fallbackError);
            }
        }

        window.wsSubscriptionCleanup = () => {
            unsubscribePersonal();
            unsubscribeRoomMessage();
            unsubscribeStatus();
            unsubscribeUserStatus();
        };
    };

const handleIncomingMessage = (messageData) => {
    if (messageData.chatRoomId) {
        updateChatWithNewMessage(messageData);
    } else {
        console.error('Message received without chatRoomId:', messageData);
    }
    };

    const handleMessageStatusUpdate = (statusUpdate) => {
        if (statusUpdate.chatroomId) {
        updateChatStatus(statusUpdate.chatroomId, statusUpdate);
    }
};

    const updateChatStatus = (chatroomId, statusUpdate) => {
        if (statusUpdate.status === 'READ' && statusUpdate.messageIds) {
         
        }
    };

const handleUserStatusUpdate = (userUpdate) => {
    setUserStatuses(prevStatuses => {
        if (!userUpdate?.id) {
            console.error('Invalid user update received (no ID):', userUpdate);
            return prevStatuses;
        }
        const newStatusForUser = {
            userStatus: userUpdate.userStatus || 'OFFLINE',
            lastSeen: userUpdate.lastSeen ? (typeof userUpdate.lastSeen === 'string' ? new Date(userUpdate.lastSeen).getTime() : userUpdate.lastSeen) : null,
            userName: userUpdate.userName || prevStatuses[userUpdate.id]?.userName
        };
        const newStatuses = {
            ...prevStatuses,
            [userUpdate.id]: newStatusForUser
        };
        if (newStatusForUser.userStatus === 'ONLINE') {
            // For each personal chat where this user is a participant (not the current user)
            chats.forEach(chat => {
                if (chat.chatRoomType === 'PERSONAL') {
                    const otherParticipant = chat.participants?.find(p => p.id === userUpdate.id);
                    if (otherParticipant) {
                        if (chat.messages && Array.isArray(chat.messages)) {
                            const sentMessages = chat.messages.filter(m => m.senderId !== userUpdate.id && (!m.messageStatus || m.messageStatus === 'SENT'));
                            if (sentMessages.length > 0) {
                                const messageIds = sentMessages.map(m => m.id);
                                websocketService.markMessagesAsDelivered(chat.id, messageIds);
                            }
                        }
                    }
                }
            });
        }
        return newStatuses;
    });
};

    const updateChatWithNewMessage = (message) => {
        const chatRoomId = message.chatRoomId;
        setChats(prevChats => {
            const updatedChats = prevChats.map(chat => {
                if (chat.id === chatRoomId) {
                    const chatRoomData = {
                        chatRoomType: chat.chatRoomType,
                        participants: chat.participants
                    };
                    
                    const updatedChat = {
                        ...chat,
                        lastMessage: formatMessageForList(message, user, chatRoomData),
                        lastActivity: message.timestamp,
                        timestamp: formatTimestamp(message.timestamp)
                    };
                    

                    return updatedChat;
                }
                return chat;
            });
            
            return updatedChats.sort((a, b) => {
                const timeA = new Date(a.lastActivity || a.timestamp || 0);
                const timeB = new Date(b.lastActivity || b.timestamp || 0);
                return timeB - timeA;
            });
        });
    };

    const handleSelectChat = (chatId) => {
        const chat = chats.find(c => c.id === chatId);
        setSelectedChat(chat);
    };    const handleSendMessage = async (chatRoom, messageContent, fileAttachment = null) => {
        try {
            if (wsConnectionState !== 'connected') {
                throw new Error('WebSocket not connected. Cannot send message.');
            }
            if (!chatRoom || !chatRoom.id) {
                throw new Error('Chat not found');
            }
            
            const messageData = {
                content: messageContent,
                senderId: user.id,
                timestamp: new Date().toISOString(),
                ...(fileAttachment && {
                    fileId: fileAttachment.fileId,
                    fileUrl: fileAttachment.fileUrl,
                    fileName: fileAttachment.fileName,
                    fileSize: fileAttachment.fileSize
                })
            };
            
            if (chatRoom.chatRoomType === 'GROUP') {
                messageData.groupId = chatRoom.groupId;
                messageData.receiverId = null;
                websocketService.sendGroupMessage(messageData);
            } else {
                messageData.receiverId = chatRoom.participants.find(p => p.id !== user.id)?.id;
                websocketService.sendPersonalMessage(messageData, chatRoom.id);
            }
            
            updateChatActivity(chatRoom.id, messageContent, new Date().toISOString());
            return { success: true, message: messageData };
        } catch (error) {
            console.error('Error sending message:', error);
            throw error;
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

    const loadUserChats = async () => {
        if (!user?.id) return;
        
        setLoading(true);
        try {
            const response = await apiClient.get(`/chatrooms/user/${user.id}`);
            if (response.ok) {
                const data = await response.json();
                
                const chatPromises = data
                    .filter(chatRoom => {
                        if (chatRoom.chatRoomType === 'GROUP') {
                            return chatRoom.group && chatRoom.group.id;
                        } else {
                            const otherParticipant = chatRoom.participants?.find(p => p.id !== user.id);
                            return otherParticipant && otherParticipant.id && otherParticipant.userName;
                        }
                    })
                    .map(async (chatRoom) => {
                    const otherParticipant = chatRoom.participants?.find(p => p.id !== user.id);
                    
                    let lastMessage = "No messages yet";
                    try {
                        const messagesResponse = await apiClient.get(`/messages/${chatRoom.chatroomId}`);
                        if (messagesResponse.ok) {
                            const messages = await messagesResponse.json();
                            if (messages.length > 0) {
                                const lastMsg = messages[messages.length - 1];
                                lastMessage = formatMessageForList(lastMsg, user, chatRoom);
                            }
                        }
                    } catch (error) {
                        console.error(`Error fetching messages for chat ${chatRoom.chatroomId}:`, error);
                    }
                    
                    return {
                        id: chatRoom.chatroomId,
                        name: chatRoom.group?.name || otherParticipant?.userName || 'Unknown',
                        profilePicture: chatRoom.group?.profilePictureUrl || otherParticipant?.profilePictureUrl,
                        lastMessage: lastMessage,
                        timestamp: formatTimestamp(chatRoom.lastActivity),
                        lastActivity: chatRoom.lastActivity, 
                        chatRoomType: chatRoom.chatRoomType,
                        participants: chatRoom.participants,
                        groupId: chatRoom.group?.id || null,
                        group: chatRoom.group // Include full group data for group chats
                    };
                });

                const mappedChats = await Promise.all(chatPromises);
                
                const sortedChats = mappedChats.sort((a, b) => {
                    const timeA = a.lastActivity ? new Date(a.lastActivity) : new Date(0);
                    const timeB = b.lastActivity ? new Date(b.lastActivity) : new Date(0);
                    return timeB - timeA;
                });
                
                setChats(sortedChats);
                
                // Join all group chat rooms for real-time updates
                if (wsConnectionState === 'connected') {
                    sortedChats.forEach(chat => {
                        if (chat.chatRoomType === 'GROUP') {
                            websocketService.joinRoom(chat.id);
                        }
                    });
                }
                
                setError(null);
            } else {
                setError('Failed to load chats');
                setChats([]);
            }
        } catch (error) {
            console.error('Error loading chats:', error);
            setError('Error loading chats');
            setChats([]);
        } finally {
            setLoading(false);
        }
    };

    const handleStartNewChat = async (contact, existingChatId = null) => {
        try {
            if (!user || !user.id) {
                console.error('User not found for starting new chat');
                return;
            }

            if (existingChatId) {
    
                const existingChat = chats.find(chat => chat.id === existingChatId);
                if (existingChat) {
                    setSelectedChat(existingChat);
                    return;
                }
            }

            const contactId = contact.id;
            
            if (!contactId) {
                console.error('Contact ID not found');
                return;
            }

            const chatRoomDTO = {
                participantIds: [user.id, contactId]
            };

            const response = await apiClient.post('/chatrooms/personal', chatRoomDTO);
            
            if (response.ok) {
                const chatRoom = await response.json();
                
                const existingChat = chats.find(chat => chat.id === chatRoom.chatroomId);
                
                if (!existingChat) {
                    const otherParticipant = chatRoom.participants.find(p => p.id !== user.id);
                    
                    const newChat = {
                        id: chatRoom.chatroomId,
                        name: otherParticipant?.userName || contact.userName,
                        profilePicture: otherParticipant?.profilePictureUrl || contact.profilePictureUrl,
                        lastMessage: "Start a conversation...",
                        timestamp: formatTimestamp(chatRoom.lastActivity),
                        lastActivity: chatRoom.lastActivity,
                        chatRoomType: chatRoom.chatRoomType,
                        participants: chatRoom.participants,
                        groupId: chatRoom.group?.id || null
                    };
                    
                    setChats(prevChats => [newChat, ...prevChats]);
                    setSelectedChat(newChat);
                } else {
                    setSelectedChat(existingChat);
                }
            } else {
                console.error('Failed to create chat room');
            }
        } catch (error) {
            console.error('Error starting new chat:', error);
        }
    };

    const handleCreateGroup = () => {
        setShowCreateGroupModal(true);
    };

    const handleContactSelection = (selectedContactIds) => {
        const selectedContacts = chats.filter(chat => {
            const chatId = chat.id || chat.chatroomId;
            return selectedContactIds.includes(chatId);
        });
        
        setShowCreateGroupModal(false);
        setTimeout(() => {
            setSelectedContactsForGroup(selectedContacts);
            setShowGroupDetailsModal(true);
        }, 50);
    };

    const handleGroupCreation = async (groupData) => {
        try {
            const response = await apiClient.createGroup(
                {
                    name: groupData.name,
                    userId: groupData.userIds
                },
                user.id
            );

            if (response.ok) {
                const newGroup = await response.json();

                // Create or get the group chat room first
                const chatRoomResponse = await apiClient.getOrCreateGroupChatRoom(newGroup.id);
                if (chatRoomResponse.ok) {
                    const chatRoomData = await chatRoomResponse.json();
                    if (groupData.profilePicture) {
                        try {
                            const uploadResponse = await apiClient.uploadGroupProfilePicture(newGroup.id, groupData.profilePicture);
                            if (uploadResponse.ok) {
                            } else {
                                console.error('Failed to upload group profile picture - HTTP status:', uploadResponse.status);
                            }
                        } catch (uploadError) {
                            console.error('Failed to upload group profile picture:', uploadError);
                        }
                    }
                    await loadUserChats();
                    
                    // Subscribe to the new group chat room
                    if (wsConnectionState === 'connected') {
                        websocketService.joinRoom(chatRoomData.chatroomId);
                    }
                    
                    setTimeout(() => {
                        setSelectedChat(prevSelected => {
                            const newChat = chats.find(chat => 
                                chat.chatRoomType === 'GROUP' && 
                                chat.groupId === newGroup.id
                            );
                            return newChat || prevSelected;
                        });
                    }, 1000);
                }

                setNotification({
                    message: 'Group created successfully!',
                    type: 'success'
                });
            } else {
                console.error('Group creation failed with status:', response.status);
                let errorMessage = 'Failed to create group';
                try {
                    const errorData = await response.json();
                    console.error('Error response data:', errorData);
                    errorMessage = errorData.message || errorMessage;
                } catch (e) {
                    console.error('Could not parse error response:', e);
                    errorMessage = `HTTP ${response.status} - ${response.statusText}`;
                }
                throw new Error(errorMessage);
            }
        } catch (error) {
            console.error('Error creating group:', error);
            console.error('Error details:', {
                name: error.name,
                message: error.message,
                stack: error.stack
            });
            setNotification({
                message: `Failed to create group: ${error.message}`,
                type: 'error'
            });
        } finally {
            setShowGroupDetailsModal(false);
            setSelectedContactsForGroup([]);
        }
    };

    const handleCloseGroupModals = () => {
        setShowCreateGroupModal(false);
        setShowGroupDetailsModal(false);
        setSelectedContactsForGroup([]);
    };

    const updateChatActivity = (chatRoomId, lastMessage, timestamp) => {
        setChats(prevChats => 
            prevChats.map(chat => 
                chat.id === chatRoomId
                    ? {
                        ...chat,
                        lastMessage: lastMessage,
                        lastActivity: timestamp,
                        timestamp: formatTimestamp(timestamp)
                    }
                    : chat
            ).sort((a, b) => {
                const timeA = new Date(a.lastActivity || a.timestamp || 0);
                const timeB = new Date(b.lastActivity || b.timestamp || 0);
                return timeB - timeA;
            })
        );
    };

    const showNotification = (message, type = 'info') => {
        setNotification({ message, type });
        setTimeout(() => setNotification(null), 5000);
    };

    const handleChatUpdated = (chatId, updatedData) => {
        setChats(prevChats => 
            prevChats.map(chat => 
                chat.id === chatId 
                    ? { ...chat, ...updatedData }
                    : chat
            )
        );
        
        if (selectedChat?.id === chatId) {
            setSelectedChat(prevSelected => ({ ...prevSelected, ...updatedData }));
        }
    };

    const handleChatRemoved = (chatId, chatType) => {
        setChats(prevChats => prevChats.filter(chat => chat.id !== chatId));
        
        if (selectedChat?.id === chatId) {
            setSelectedChat(null);
        }
        
        showNotification(`${chatType === 'group' ? 'Group' : 'Chat'} removed successfully`, 'success');
    };

    const handleUserUpdate = (updatedUser) => {
        showNotification('Profile updated successfully!', 'success');
    };

    // Subscribe to group updates via WebSocket
    useEffect(() => {
        if (selectedChat?.chatRoomType === 'GROUP' && selectedChat?.groupId) {
            const handleGroupUpdate = (event) => {
                switch (event.type) {
                    case 'GROUP_UPDATED':

                        // Update both name and profile picture if present
                        const updates = {};
                        if (event.data.name) updates.name = event.data.name;
                        if (event.data.profilePictureUrl) {
                            // Add timestamp to force cache refresh
                            const profilePictureWithTimestamp = `${event.data.profilePictureUrl}?t=${Date.now()}`;
                            updates.profilePictureUrl = profilePictureWithTimestamp;
                            updates.profilePicture = profilePictureWithTimestamp; // Also update profilePicture field for consistency
                        }
                        
                        handleChatUpdated(selectedChat.id, updates);
                        
                        if (event.data.name && event.data.profilePictureUrl) {
                            showNotification('Group updated', 'success');
                        } else if (event.data.name) {
                            showNotification('Group name updated', 'success');
                        } else if (event.data.profilePictureUrl) {
                            showNotification('Group picture updated', 'success');
                        }
                        break;
                    case 'USER_JOINED':
                    case 'USER_LEFT':
                        // TODO: Refresh group details to get updated member list

                        break;
                    default:

                }
            };

            const unsubscribe = websocketService.subscribeToGroupEvents(
                selectedChat.groupId,
                handleGroupUpdate
            );
            
            return () => {
                unsubscribe();
            };
        }
    }, [selectedChat?.id, selectedChat?.chatRoomType, selectedChat?.groupId]);

    useEffect(() => {
        return () => {
            if (window.wsSubscriptionCleanup) {
                window.wsSubscriptionCleanup();
                delete window.wsSubscriptionCleanup;
            }
        };
    }, []);

    return (
        <div className="chat-window">            
            <div className="sidebar-window">
                <Sidebar 
                    chats={chats}
                    selectedChatId={selectedChat?.id}
                    onSelectChat={handleSelectChat}
                    loading={loading}
                    error={error}
                    onLogout={onLogout}
                    onStartNewChat={handleStartNewChat}
                    wsConnectionState={wsConnectionState}
                    currentUser={user}
                    onCreateGroup={handleCreateGroup}
                    onChatRemoved={handleChatRemoved}
                    onUserUpdate={handleUserUpdate}
                />
            </div>
            <div className="chatarea-window">
                <ChatArea
                    selectedChat={selectedChat}
                    currentUser={user}
                    onSendMessage={handleSendMessage}
                    wsConnectionState={wsConnectionState}
                    userStatuses={userStatuses}
                    chats={chats}
                    onChatRemoved={handleChatRemoved}
                    onChatUpdated={handleChatUpdated}
                />
            </div>
            
            <ContactSelectionModal
                isOpen={showCreateGroupModal}
                onClose={handleCloseGroupModals}
                chats={chats}
                onAction={handleContactSelection}
                currentUser={user}
                mode="createGroup"
                allowMultiSelect={true}
                showSelectAll={true}
                filterCurrentChat={false}
            />

            <GroupDetailsModal
                isOpen={showGroupDetailsModal}
                onClose={handleCloseGroupModals}
                selectedContacts={selectedContactsForGroup}
                currentUser={user}
                onCreateGroup={handleGroupCreation}
            />
            
            {notification && (
                <Notification
                    message={notification.message}
                    type={notification.type}
                    onClose={() => setNotification(null)}
                />
            )}
        </div>
    );
}

export default ChatWindow;
