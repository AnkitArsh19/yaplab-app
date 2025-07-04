import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import ChatHeader from './ChatHeader';
import MessageArea from './MessageArea';
import MessageInput from './MessageInput';
import Notification from '../ui/Notification';
import MessageSearch from './MessageSearch';
import ContactSelectionModal from '../modals/ContactSelectionModal';
import ConfirmationModal from '../modals/ConfirmationModal';
import AboutComponent from '../ui/AboutComponent';
import GroupSettingsModal from '../groups/GroupSettingsModal';
import '../../styles/ChatArea.css';
import apiClient from '../../utils/apiClient';
import websocketService from '../../utils/websocketService';

const getGroupId = (chat) => chat?.group?.id || chat?.groupId;

const sortMessagesByTimestamp = (messages) => 
    messages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

const updateMessageStatus = (message, status, timestamp) => ({
    ...message,
    messageStatus: status.toUpperCase(),
    ...(status === 'READ' && { readAt: timestamp || new Date().toISOString() }),
    ...(status === 'DELIVERED' && { deliveredAt: timestamp || new Date().toISOString() }),
    isOptimistic: false
});

const formatNotificationMessage = (count, item, action, additionalMsg = '') => {
    const countText = count > 0 ? `${count} ${item}${count > 1 ? 's' : ''}` : 'No messages';
    return `${countText} ${action}${additionalMsg}`.trim();
};

const handleRoomEvents = (roomEvent, messageId, newContent, editTimestamp, currentMessages, setMessagesCallback) => {
    switch (roomEvent.type) {
        case 'MESSAGE_DELETED':
            setMessagesCallback(currentMessages.filter(msg => msg.id !== messageId));
            break;
        case 'MESSAGE_EDITED':
            setMessagesCallback(currentMessages.map(msg =>
                msg.id === messageId 
                    ? { ...msg, content: newContent, edited: true, editTimestamp, isOptimistic: false }
                    : msg
            ).sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp)));
            break;
        case 'MULTIPLE_MESSAGES_DELETED':
            const deletedCount = messageId.length;
            setMessagesCallback(currentMessages.filter(msg => !messageId.includes(msg.id)));
            return formatNotificationMessage(deletedCount, 'message', 'deleted');
    }
};

function ChatArea({ selectedChat, currentUser, onSendMessage: propOnSendMessage, wsConnectionState, userStatuses, chats = [], onChatRemoved, onChatUpdated }) {
    const [messages, setMessages] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [typingUsers, setTypingUsers] = useState(new Set());
    const [isSending, setIsSending] = useState(false);
    const [resetFocusSignal, setResetFocusSignal] = useState(0);
    const [isAnyMenuOpen, setIsAnyMenuOpen] = useState(false);
    const [replyingToMessage, setReplyingToMessage] = useState(null);
    const [editingMessage, setEditingMessage] = useState(null);
    const [isSelectionMode, setIsSelectionMode] = useState(false);
    const [selectedMessages, setSelectedMessages] = useState(new Set());
    const [notification, setNotification] = useState(null);
    const [isSearchOpen, setIsSearchOpen] = useState(false);
    const [isForwardModalOpen, setIsForwardModalOpen] = useState(false);
    const [messageToForward, setMessageToForward] = useState(null);
    const [confirmationModal, setConfirmationModal] = useState({ 
        isOpen: false, 
        type: '', 
        title: '', 
        message: '', 
        onConfirm: null,
        isLoading: false 
    });
    const [showAboutModal, setShowAboutModal] = useState(false);
    const [aboutModalData, setAboutModalData] = useState(null);
    const [aboutModalType, setAboutModalType] = useState('user');
    const [showGroupSettingsModal, setShowGroupSettingsModal] = useState(false);
    const messageAreaRef = useRef(null);
    const chatAvailable = !!(selectedChat && selectedChat.id);

    useEffect(() => {
        if (chatAvailable) {
            setResetFocusSignal(signal => signal + 1);
        }
    }, [selectedChat?.id]);

    useEffect(() => {
        if (selectedChat?.id) {
            loadMessages();
            if (selectedChat.chatRoomType === 'GROUP') {
                websocketService.joinRoom(selectedChat.id, selectedChat);
            }
        } else {
            setMessages([]);
        }

        return () => {
            if (selectedChat?.id && selectedChat.chatRoomType === 'GROUP') {
                websocketService.leaveRoom(selectedChat.id);
            }
        };
    }, [selectedChat?.id]);

    useEffect(() => {
        if (wsConnectionState !== 'connected' || !selectedChat?.id || !currentUser?.id) return;

        const handleRoomMessageEvent = (data) => {
            if (data.roomId === selectedChat?.id) {
                handleRealTimeMessage(data.message);
            }
        };

        const handleStatusEvent = (update) => {
            if (update.chatroomId === selectedChat?.id || update.userId === currentUser?.id) {
                handleMessageStatusUpdate(update);
            }
        };

        const handleRoomEvent = (event) => {
            if (event.roomId !== selectedChat.id) return;

            if (event.type === 'TYPING' && event.userId !== currentUser.id) {
                setTypingUsers(prev => new Set(prev).add(event.userId));
            } else if (event.type === 'STOP_TYPING' && event.userId !== currentUser.id) {
                setTypingUsers(prev => {
                    const updated = new Set(prev);
                    updated.delete(event.userId);
                    return updated;
                });
            } else {
                const notification = handleRoomEvents(
                    event,
                    event.messageId || event.messageIds,
                    event.newContent,
                    event.editTimestamp,
                    messages,
                    setMessages
                );
                if (notification) {
                    showNotification(notification, 'info');
                }
                if (isSelectionMode) {
                    setSelectedMessages(new Set());
                    setIsSelectionMode(false);
                }
            }
        };

        const handleMemberEvent = (event, type) => {
            if (selectedChat.chatRoomType === 'GROUP' && event.groupId === getGroupId(selectedChat)) {
                showNotification(`${event.username} ${type === 'added' ? 'joined' : 'left'} the group`, 'info');
            }
        };

        const unsubscribers = [
            websocketService.addEventListener('roomMessage', handleRoomMessageEvent),
            websocketService.addEventListener('messageStatus', handleStatusEvent),
            websocketService.addEventListener('roomEvent', handleRoomEvent),
            websocketService.addEventListener('messageDeleted', (event) => {
                if (event.roomId === selectedChat.id) {
                    handleMessageDeleted(event.messageId);
                }
            }),
            websocketService.addEventListener('messageEdited', (event) => {
                if (event.roomId === selectedChat.id || event.chatroomId === selectedChat.id) {
                    handleMessageEdited(event.messageId, event.newContent, event.editTimestamp);
                }
            }),
            websocketService.addEventListener('groupMemberAdded', (event) => handleMemberEvent(event, 'added')),
            websocketService.addEventListener('groupMemberRemoved', (event) => handleMemberEvent(event, 'removed'))
        ];

        return () => unsubscribers.forEach(unsub => unsub());
    }, [wsConnectionState, selectedChat?.id, currentUser?.id]);

    useEffect(() => {
        setTypingUsers(new Set());
    }, [selectedChat?.id]);

    useEffect(() => {
        if (!selectedChat?.id || wsConnectionState !== 'connected' || !currentUser?.id || messages.length === 0) {
            return;
        }

        const readTimer = setTimeout(() => {
            const unreadMessages = messages.filter(msg =>
                msg.senderId !== currentUser.id &&
                msg.messageStatus?.toUpperCase() === 'DELIVERED' &&
                typeof msg.id === 'number' 
            );

            if (unreadMessages.length > 0) {
                const messageIds = unreadMessages.map(msg => msg.id);
                websocketService.markMessagesAsRead(selectedChat.id, messageIds);
               
                setMessages(prev => prev.map(msg =>
                    messageIds.includes(msg.id)
                        ? { ...msg, messageStatus: 'read', readAt: new Date().toISOString() }
                        : msg
                ).sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp)));
            }
        }, 1000);

        return () => clearTimeout(readTimer);
    }, [selectedChat?.id, wsConnectionState, currentUser?.id, messages]);

    const handleRealTimeMessage = (incomingMessage) => {
        if (
            incomingMessage &&
            incomingMessage.id &&
            incomingMessage.senderId !== currentUser?.id &&
            (!incomingMessage.messageStatus ||
                !['DELIVERED', 'READ'].includes(incomingMessage.messageStatus.toUpperCase()))
        ) {
            websocketService.markMessagesAsDelivered(selectedChat.id, [incomingMessage.id]);
        }

        setMessages(prevMessages => {
            const messageWithSenderDetails = {
                ...incomingMessage,
                senderId: incomingMessage.senderId || incomingMessage.sender?.id,
                senderName: incomingMessage.senderName || incomingMessage.sender?.userName,
                isOptimistic: false,
                messageStatus: incomingMessage.messageStatus || 'SENT'
            };

            if (messageWithSenderDetails.senderId === currentUser?.id) {
                const optimisticIndex = prevMessages.findIndex(
                    msg => msg.isOptimistic &&
                           msg.senderId === currentUser.id &&
                           msg.content === messageWithSenderDetails.content
                );

                if (optimisticIndex !== -1) {
                    const updatedMessages = [...prevMessages];
                    updatedMessages[optimisticIndex] = messageWithSenderDetails;
                    return sortMessagesByTimestamp(updatedMessages);
                }
            }

            const existingMsgIndex = prevMessages.findIndex(msg => msg.id === messageWithSenderDetails.id);
            if (existingMsgIndex !== -1) {
                 const updatedMessages = [...prevMessages];
                 updatedMessages[existingMsgIndex] = { ...prevMessages[existingMsgIndex], ...messageWithSenderDetails, isOptimistic: false };
                 return sortMessagesByTimestamp(updatedMessages);
            }

            return sortMessagesByTimestamp([...prevMessages, messageWithSenderDetails]);
        });
    };

    const handleMessageStatusUpdate = (update) => {
        const messageIds = update.messageIds || (update.messageId ? [update.messageId] : []);
        if (messageIds.length === 0 || !update.status) {
            return;
        }

        setMessages(prevMessages => {
            const updatedMessages = prevMessages.map(msg => 
                messageIds.includes(msg.id)
                    ? updateMessageStatus(msg, update.status, update.readAt || update.deliveredAt)
                    : msg
            );
            return sortMessagesByTimestamp(updatedMessages);
        });
    };

    const handleMessageDeleted = (messageId) => {
        setMessages(prevMessages => 
            prevMessages.filter(msg => msg.id !== messageId)
        );
    };

    const handleMultipleMessagesDeleted = (messageIds) => {
        const deletedCount = messageIds.length;
        
        setMessages(prevMessages => 
            prevMessages.filter(msg => !messageIds.includes(msg.id))
        );
        
        showNotification(`${deletedCount} message${deletedCount > 1 ? 's' : ''} deleted`, 'info');
        
        if (isSelectionMode) {
            setSelectedMessages(new Set());
            setIsSelectionMode(false);
        }
    };

    const handleMessageEdited = (messageId, newContent, editTimestamp) => {
        setMessages(prevMessages => {
            const updatedMessages = prevMessages.map(msg =>
                msg.id === messageId 
                    ? { 
                        ...msg, 
                        content: newContent, 
                        edited: true, 
                        editTimestamp: editTimestamp,
                        isOptimistic: false 
                    } 
                    : msg
            ).sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
            
            return updatedMessages;
        });
    };

    const loadMessages = async () => {
        if (!selectedChat?.id) return;
        setIsLoading(true);
        try {
            const response = await apiClient.get(`/chatrooms/${selectedChat.id}/messages?userId=${currentUser.id}`);
            if (response.ok) {
                const data = await response.json();
                setMessages(sortMessagesByTimestamp(data.map(m => ({...m, isOptimistic: false}))));
            } else {
                console.error('Failed to load messages:', response.statusText);
                setMessages([]);
            }
        } catch (error) {
            console.error('Error loading messages:', error);
            setMessages([]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSendMessageOptimistic = async (messageContent) => {
        let messageText = '';
        let fileAttachment = null;
        
        if (typeof messageContent === 'string') {
            messageText = messageContent;
        } else if (typeof messageContent === 'object') {
            messageText = messageContent.content || '';
            
            if (messageContent.fileId) {
                fileAttachment = {
                    fileId: messageContent.fileId,
                    fileName: messageContent.fileName,
                    fileUrl: messageContent.fileUrl,
                    fileSize: messageContent.fileSize,
                    fileType: messageContent.fileType
                };
            } else if (messageContent.audioFile) {
                fileAttachment = {
                    audioFile: messageContent.audioFile,
                    fileName: messageContent.fileName,
                    fileSize: messageContent.fileSize,
                    fileType: messageContent.fileType,
                    audioDuration: messageContent.audioDuration
                };
            }
        }
        const isAudioMessage = fileAttachment && (fileAttachment.audioFile || fileAttachment.fileType?.startsWith('audio/'));
        const hasFileAttachment = !!fileAttachment;
        
        if (!isAudioMessage && !hasFileAttachment && (!messageText.trim() || !chatAvailable || !currentUser?.id || isSending)) return;
        if ((isAudioMessage || hasFileAttachment) && (!chatAvailable || !currentUser?.id || isSending)) return;
        
        const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        const optimisticMessage = {
            id: tempId,
            tempId: tempId,
            content: messageText.trim() || (isAudioMessage ? '🎤 Voice message' : (fileAttachment?.fileType === 'image/gif' ? '🎬 GIF' : (hasFileAttachment ? '📎 File' : ''))),
            senderId: currentUser.id,
            senderName: currentUser.userName,
            timestamp: new Date().toISOString(),
            isOptimistic: true,
            messageStatus: 'SENDING_OPTIMISTIC',
            chatRoomId: selectedChat.id,
            ...(fileAttachment && {
                fileUrl: fileAttachment.fileUrl,
                fileName: fileAttachment.fileName,
                fileSize: fileAttachment.fileSize,
                fileType: fileAttachment.fileType,
                ...(fileAttachment.audioDuration && { audioDuration: fileAttachment.audioDuration })
            })
        };
        
        setMessages(prevMessages => [...prevMessages, optimisticMessage].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp)));
        setIsSending(true);
        
        try {
            if (propOnSendMessage) {
                await propOnSendMessage(selectedChat, messageText, fileAttachment);
            }
        } catch (error) {
            setMessages(prevMessages => prevMessages.map(msg => msg.id === tempId ? { ...msg, isOptimistic: false, messageStatus: 'ERROR' } : msg));
        } finally {
            setIsSending(false);
            setResetFocusSignal(signal => signal + 1);
        }
    };

    const handleReplyToMessage = (messageToReply) => {
        setReplyingToMessage(messageToReply);
        setResetFocusSignal(signal => signal + 1);
    };

    const handleCancelReply = () => {
        setReplyingToMessage(null);
    };

    const handleSendReply = async (content, file = null) => {
        if (!replyingToMessage || !currentUser || !selectedChat) {
            return;
        }

        try {
            setIsSending(true);
            
            const replyData = {
                senderId: currentUser.id,
                content: content,
                groupId: null,
                receiverId: null,
                fileId: file?.id || null,
                fileUrl: file?.url || null,
                fileName: file?.name || null,
                fileSize: file?.size || null,
                edited: false,
                forwarded: false,
                editTimestamp: null
            };

            const response = await apiClient.post(`/messages/${replyingToMessage.id}/reply`, replyData);
            
            if (response.ok) {
                setReplyingToMessage(null);
                if (propOnSendMessage) {
                    propOnSendMessage();
                }
            }
        } catch (error) {
            console.error('Failed to send reply:', error);
        } finally {
            setIsSending(false);
        }
    };

    const handleEditMessage = (messageToEdit) => {
        if (messageToEdit.senderId === currentUser?.id && messageToEdit.content && !messageToEdit.fileUrl) {
            setEditingMessage(messageToEdit);
            setResetFocusSignal(signal => signal + 1);
        }
    };

    const handleCancelEdit = () => {
        setEditingMessage(null);
    };

    const handleSendEdit = async (newContent) => {
        if (!editingMessage || !currentUser || !selectedChat || !newContent.trim()) {
            return;
        }

        setMessages(prevMessages =>
            sortMessagesByTimestamp(prevMessages.map((msg) =>
                msg.id === editingMessage.id 
                    ? { 
                        ...msg, 
                        content: newContent.trim(), 
                        edited: true, 
                        editTimestamp: new Date().toISOString(),
                        isOptimistic: false 
                    } 
                    : msg
            ))
        );

        setIsSending(true);
        try {
            const response = await apiClient.put(
                `/messages/${editingMessage.id}/edit?userId=${currentUser.id}`,
                {
                    messageId: editingMessage.id,
                    newContent: newContent.trim()
                }
            );

            if (response.ok) {
                setEditingMessage(null);
                if (propOnSendMessage) {
                    propOnSendMessage();
                }
            } else {
                const data = await response.json().catch(() => ({}));
                console.error('Edit failed:', data);
                loadMessages();
            }
        } catch (error) {
            console.error('Failed to send edit:', error);
            loadMessages();
        } finally {
            setIsSending(false);
        }
    };

    const handleInputChange = (e) => {
        if (selectedChat?.id && wsConnectionState === 'connected') {
            websocketService.startTyping(selectedChat.id);
        }
    };

    useEffect(() => {
        if (wsConnectionState === 'disconnected' && currentUser && selectedChat?.id) {
            const recoveryTimer = setTimeout(() => {
                websocketService.connect(currentUser)
                    .then(() => {
                        if (selectedChat?.id) {
                           websocketService.joinRoom(selectedChat.id);
                        }
                    })
                    .catch(error => console.error('WebSocket reconnection failed:', error));
            }, 2000);
            return () => clearTimeout(recoveryTimer);
        }
    }, [wsConnectionState, currentUser, selectedChat?.id]);

    const handleEnterSelectionMode = () => {
        setIsSelectionMode(true);
        setSelectedMessages(new Set());
    };

    const handleExitSelectionMode = () => {
        setIsSelectionMode(false);
        setSelectedMessages(new Set());
    };

    const handleMessageSelect = (messageId) => {
        setSelectedMessages(prev => {
            const newSelection = new Set(prev);
            if (newSelection.has(messageId)) {
                newSelection.delete(messageId);
            } else {
                newSelection.add(messageId);
            }
            return newSelection;
        });
    };

    const handleSelectAll = () => {
        setSelectedMessages(new Set(messages.map(msg => msg.id)));
    };

    const handleDeselectAll = () => {
        setSelectedMessages(new Set());
    };

    const showNotification = (message, type = 'info') => {
        setNotification({ message, type });
    };

    const handleOpenSearch = () => {
        setIsSearchOpen(true);
    };

    const handleCloseSearch = () => {
        setIsSearchOpen(false);
    };

    const handleJumpToMessage = (messageId) => {
        const messageElement = document.querySelector(`[data-message-id="${messageId}"]`);
        if (messageElement) {
            messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            
            const messageDiv = messageElement.closest('.message');
            if (messageDiv) {
                messageDiv.classList.add('highlighted');
                setTimeout(() => {
                    messageDiv.classList.remove('highlighted');
                }, 3000);
            }
        }
    };

    const handleBulkDelete = async () => {
        if (selectedMessages.size === 0) return;
        
        try {
            const messageIds = Array.from(selectedMessages);
            
            const selectedMessageObjects = messages.filter(msg => messageIds.includes(msg.id));
            const sentCount = selectedMessageObjects.filter(msg => msg.senderId === currentUser.id).length;
            const receivedCount = selectedMessageObjects.length - sentCount;
            
            const response = await apiClient.request('/messages/multiple', {
                method: 'DELETE',
                body: JSON.stringify({
                    messageIds: messageIds,
                    userId: currentUser.id
                })
            });
            
            if (response.ok) {
                const deletedIds = await response.json();
                
                setMessages(prevMessages => 
                    prevMessages.filter(msg => !deletedIds.includes(msg.id))
                );
                
                let notificationMessage = '';
                if (receivedCount > 0 && sentCount > 0) {
                    notificationMessage = `${sentCount} message${sentCount > 1 ? 's' : ''} deleted. Note: You can only delete your own messages.`;
                } else if (sentCount > 0) {
                    notificationMessage = `${sentCount} message${sentCount > 1 ? 's' : ''} deleted successfully.`;
                } else {
                    notificationMessage = 'No messages deleted. You can only delete your own messages.';
                }
                
                showNotification(notificationMessage, sentCount > 0 ? 'success' : 'warning');
                
                handleExitSelectionMode();
            } else {
                throw new Error(`Delete failed with status: ${response.status}`);
            }
        } catch (error) {
            console.error('Failed to bulk delete messages:', error);
            showNotification('Failed to delete messages. Please try again.', 'error');
            loadMessages();
        }
    };

    const handleBulkForward = () => {
        if (selectedMessages.size > 0) {
            setIsForwardModalOpen(true);
        }
    };

    const handleForwardMessages = async (selectedChatIds) => {
        if (!selectedChatIds.length) return;
        
        const messagesToForward = messageToForward ? 
            [messageToForward] : 
            Array.from(selectedMessages)
                .map(id => messages.find(msg => msg.id === id))
                .filter(Boolean)
                .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

        const messageIds = messagesToForward.map(msg => msg.id);
        if (!messageIds.length) return;

        try {
            await Promise.all(
                selectedChatIds.map(chatId => 
                    messageToForward ?
                        apiClient.forwardMessage(messageToForward.id, chatId, currentUser.id) :
                        apiClient.forwardMultipleMessages(messageIds, chatId, currentUser.id)
                )
            );

            const messageCount = messageIds.length;
            const chatCount = selectedChatIds.length;
            showNotification(
                `Successfully forwarded ${messageCount} message${messageCount > 1 ? 's' : ''} to ${chatCount} chat${chatCount > 1 ? 's' : ''}`,
                'success'
            );

            if (isSelectionMode) {
                setIsSelectionMode(false);
                setSelectedMessages(new Set());
            }
        } catch (error) {
            console.error('Failed to forward messages:', error);
            showNotification('Failed to forward messages. Please try again.', 'error');
        }
    };

    const handleCloseForwardModal = () => {
        setIsForwardModalOpen(false);
        setMessageToForward(null);
    };

    const handleForwardMessage = (message) => {
        setMessageToForward(message);
        setIsForwardModalOpen(true);
    };

    const getContactUser = () => {
        if (!selectedChat || selectedChat.chatRoomType === 'GROUP') return null;
        const otherParticipant = selectedChat.participants?.find(p => p.id !== currentUser?.id);
        return otherParticipant || null;
    };

    const handleChatMenuAction = (action) => {
        switch (action) {
            case 'contactInfo':
                const contactUser = getContactUser();
                if (contactUser) {
                    setAboutModalData(contactUser);
                    setAboutModalType('user');
                    setShowAboutModal(true);
                }
                break;
            case 'groupInfo':
                if (selectedChat && selectedChat.chatRoomType === 'GROUP') {
                    const groupData = selectedChat.group || selectedChat;
                    setAboutModalData(groupData);
                    setAboutModalType('group');
                    setShowAboutModal(true);
                }
                break;
            case 'mediaFiles':
                break;
            case 'searchMessages':
                handleOpenSearch();
                break;
            case 'clearChat':
                setConfirmationModal({
                    isOpen: true,
                    type: 'warning',
                    title: 'Clear Chat',
                    message: 'Are you sure you want to clear this chat? This will hide all messages for you only. This action cannot be undone.',
                    onConfirm: handleClearChat,
                    isLoading: false
                });
                break;
            case 'deleteChat':
                setConfirmationModal({
                    isOpen: true,
                    type: 'danger',
                    title: 'Delete Chat',
                    message: 'Are you sure you want to delete this chat? This will clear all messages for you and remove you from this conversation. This action cannot be undone.',
                    onConfirm: handleDeleteChat,
                    isLoading: false
                });
                break;
            case 'blockContact':
                setConfirmationModal({
                    isOpen: true,
                    type: 'danger',
                    title: 'Block Contact',
                    message: 'Are you sure you want to block this contact? You will no longer receive messages from them.',
                    onConfirm: handleBlockContact,
                    isLoading: false
                });
                break;
            case 'leaveGroup':
                setConfirmationModal({
                    isOpen: true,
                    type: 'warning',
                    title: 'Leave Group',
                    message: 'Are you sure you want to leave this group? You will no longer receive messages from this group.',
                    onConfirm: handleLeaveGroup,
                    isLoading: false
                });
                break;
            case 'deleteGroup':
                setConfirmationModal({
                    isOpen: true,
                    type: 'danger',
                    title: 'Delete Group',
                    message: 'Are you sure you want to delete this group? This will remove all members and delete all messages for everyone. This action cannot be undone.',
                    onConfirm: handleDeleteGroup,
                    isLoading: false
                });
                break;
            case 'groupSettings':
                if (selectedChat && selectedChat.chatRoomType === 'GROUP') {
                    setShowGroupSettingsModal(true);
                }
                break;
            default:
                break;
        }
    };

    const handleClearChat = async () => {
        setConfirmationModal(prev => ({ ...prev, isLoading: true }));
        try {
            const response = await apiClient.post(`/chatrooms/${selectedChat.id}/clear?userId=${currentUser.id}`);
            if (response.ok) {
                setMessages([]);
                await loadMessages();
                showNotification('Chat cleared successfully', 'success');
                setConfirmationModal(prev => ({ ...prev, isOpen: false, isLoading: false }));
            } else {
                showNotification('Failed to clear chat', 'error');
                setConfirmationModal(prev => ({ ...prev, isLoading: false }));
            }
        } catch (error) {
            console.error('Error clearing chat:', error);
            showNotification('Failed to clear chat', 'error');
            setConfirmationModal(prev => ({ ...prev, isLoading: false }));
        }
    };

    const handleDeleteChat = async () => {
        setConfirmationModal(prev => ({ ...prev, isLoading: true }));
        try {
            const response = await apiClient.post(`/chatrooms/${selectedChat.id}/delete?userId=${currentUser.id}`);
            if (response.ok) {
                setMessages([]);
                showNotification('Chat deleted successfully', 'success');
                setConfirmationModal(prev => ({ ...prev, isOpen: false, isLoading: false }));
                if (onChatRemoved) {
                    onChatRemoved(selectedChat.id, 'personal');
                }
            } else {
                showNotification('Failed to delete chat', 'error');
                setConfirmationModal(prev => ({ ...prev, isLoading: false }));
            }
        } catch (error) {
            console.error('Error deleting chat:', error);
            showNotification('Failed to delete chat', 'error');
            setConfirmationModal(prev => ({ ...prev, isLoading: false }));
        }
    };

    const handleLeaveGroup = async () => {
        setConfirmationModal(prev => ({ ...prev, isLoading: true }));
        try {
            await apiClient.delete(`/groups/removeuser?userId=${currentUser.id}&groupId=${selectedChat.groupId}`);
            showNotification('Left group successfully', 'success');
            setConfirmationModal(prev => ({ ...prev, isOpen: false, isLoading: false }));
            
            if (onChatRemoved) {
                onChatRemoved(selectedChat.id, 'group');
            }
        } catch (error) {
            console.error('Error leaving group:', error);
            showNotification('Failed to leave group', 'error');
            setConfirmationModal(prev => ({ ...prev, isLoading: false }));
        }
    };

    const handleDeleteGroup = async () => {
        setConfirmationModal(prev => ({ ...prev, isLoading: true }));
        try {
            await apiClient.delete(`/groups/${selectedChat.groupId}?userId=${currentUser.id}`);
            showNotification('Group deleted successfully', 'success');
            setConfirmationModal(prev => ({ ...prev, isOpen: false, isLoading: false }));
            if (onChatRemoved) {
                onChatRemoved(selectedChat.id, 'group');
            }
        } catch (error) {
            console.error('Error deleting group:', error);
            showNotification('Failed to delete group', 'error');
            setConfirmationModal(prev => ({ ...prev, isLoading: false }));
        }
    };

    const refreshGroupDetails = async (groupId) => {
        try {
            const response = await apiClient.getGroupDetails(groupId);
            if (response.ok) {
                const updatedGroup = await response.json();
                
                if (selectedChat && selectedChat.groupId === groupId) {
                    let participants = [];
                    
                    if (updatedGroup.userNames && updatedGroup.userNames.length > 0) {
                        const existingParticipants = selectedChat.participants || [];
                        
                        participants = updatedGroup.userNames.map((userName, index) => {
                            const existingParticipant = existingParticipants.find(p => p.userName === userName);
                            if (existingParticipant) {
                                return existingParticipant;
                            } else {
                                return {
                                    id: Date.now() + index,
                                    userName: userName,
                                    emailId: '',
                                    profilePictureUrl: ''
                                };
                            }
                        });
                    }
                    
                    const updatedChat = {
                        ...selectedChat,
                        participants: participants,
                        name: updatedGroup.name || selectedChat.name,
                        profilePictureUrl: updatedGroup.profilePictureUrl || selectedChat.profilePictureUrl,
                        profilePicture: updatedGroup.profilePictureUrl || selectedChat.profilePicture
                    };
                    
                    if (onChatUpdated) {
                        onChatUpdated(selectedChat.id, updatedChat);
                    }
                }
            } else {
                console.error('Failed to get group details:', response.statusText);
            }
        } catch (error) {
            console.error('Error refreshing group details:', error);
        }
    };

    const handleUpdateGroup = async (updatedData) => {
        if (!selectedChat?.groupId) {
            showNotification('Group ID not found', 'error');
            return;
        }
        
        try {
            let success = true;
            
            if (updatedData.name && updatedData.name !== selectedChat.group?.name) {
                const nameResponse = await apiClient.updateGroupName(selectedChat.groupId, 
                    { name: updatedData.name }, 
                    currentUser.id
                );
                if (!nameResponse.ok) {
                    const errorData = await nameResponse.json().catch(() => ({}));
                    console.error('Failed to update group name:', errorData);
                    success = false;
                    showNotification(errorData.message || 'Failed to update group name', 'error');
                }
            }
            
            if (updatedData.avatar) {
                const avatarResponse = await apiClient.uploadGroupProfilePicture(selectedChat.groupId, updatedData.avatar);
                if (!avatarResponse.ok) {
                    const errorData = await avatarResponse.json().catch(() => ({}));
                    console.error('Failed to update profile picture:', errorData);
                    success = false;
                    showNotification(errorData.message || 'Failed to update profile picture', 'error');
                }
            }
            
            if (success) {
                showNotification('Group updated successfully', 'success');
                await refreshGroupDetails(selectedChat.groupId);
            }
        } catch (error) {
            console.error('Error updating group:', error);
            showNotification('Failed to update group', 'error');
        }
    };

    const handleGroupOperationError = async (error, operation, refreshGroup = true) => {
        console.error(`Error ${operation}:`, error);
        showNotification(`Failed to ${operation}: ${error.message || 'Unknown error'}`, 'error');
        
        if (refreshGroup && selectedChat?.groupId) {
            try {
                await refreshGroupDetails(selectedChat.groupId);
            } catch (refreshError) {
                console.error('Failed to refresh group details after error:', refreshError);
            }
        }
    };

    const handleMemberOperationSuccess = async (message, refreshGroup = true) => {
        showNotification(message, 'success');
        if (refreshGroup && selectedChat?.groupId) {
            setTimeout(() => refreshGroupDetails(selectedChat.groupId), 100);
        }
    };

    const handleAddMembers = async (selectedUserIds) => {
        if (!selectedChat?.groupId) {
            showNotification('Group ID not found', 'error');
            return;
        }
        
        try {
            const userIds = Array.isArray(selectedUserIds) ? selectedUserIds : [selectedUserIds];
            let addedCount = 0;
            let lastError = null;
            
            for (const userId of userIds) {
                const numericUserId = Number(userId);
                const numericGroupId = Number(selectedChat.groupId);
                
                if (isNaN(numericUserId) || isNaN(numericGroupId)) {
                    lastError = `Invalid ID format for user ${userId}`;
                    console.error('Invalid user ID or group ID:', { userId, groupId: selectedChat.groupId });
                    continue;
                }
                
                try {
                    const response = await apiClient.addUserToGroup(numericUserId, numericGroupId);
                    if (response.ok) {
                        addedCount++;
                    } else {
                        const errorText = await response.text().catch(() => '');
                        lastError = `Failed to add user ${numericUserId}: ${errorText || response.statusText}`;
                    }
                } catch (userError) {
                    lastError = `Failed to add user ${numericUserId}: ${userError.message || ''}`;
                }
            }
            
            if (addedCount === userIds.length) {
                await handleMemberOperationSuccess(formatNotificationMessage(addedCount, 'member', 'added successfully'));
            } else if (addedCount > 0) {
                showNotification(formatNotificationMessage(addedCount, 'member', 'added successfully', ` of ${userIds.length}. ${lastError || ''}`), 'warning');
                await refreshGroupDetails(selectedChat.groupId);
            } else {
                throw new Error(lastError || 'Failed to add members');
            }
        } catch (error) {
            await handleGroupOperationError(error, 'add members');
        }
    };

    const handleRemoveMember = async (memberId) => {
        if (!selectedChat?.groupId) {
            showNotification('Group ID not found', 'error');
            return;
        }
        
        try {
            const response = await apiClient.removeUserFromGroup(memberId, selectedChat.groupId);
            if (response.ok) {
                await handleMemberOperationSuccess('Member removed successfully');
            } else {
                const errorText = await response.text().catch(() => '');
                throw new Error(errorText || 'Failed to remove member');
            }
        } catch (error) {
            await handleGroupOperationError(error, 'remove member');
        }
    };

    if (!selectedChat) {
        return (
            <div className="chat-area no-chat">
                <div className="no-chat-message">
                    <img className="logo-image-chatarea" src="logo.png" alt="YapLab Logo" />
                    <h3>Welcome to YapLab!</h3>
                    <p>Select a conversation or search for contacts to start chatting</p>
                </div>
            </div>
        );
    }
    
    const canSendMessage = selectedChat?.id && currentUser?.id;
    const showDisconnectedPlaceholder = wsConnectionState === 'disconnected' || wsConnectionState === 'reconnecting';

    return (
        <div className="chat-area">
            <ChatHeader
                selectedChat={selectedChat}
                currentUser={currentUser}
                userStatuses={userStatuses}
                isSelectionMode={isSelectionMode}
                selectedMessages={selectedMessages}
                onExitSelectionMode={handleExitSelectionMode}
                onSelectAll={handleSelectAll}
                onDeselectAll={handleDeselectAll}
                onBulkDelete={handleBulkDelete}
                onBulkForward={handleBulkForward}
                onOpenSearch={handleOpenSearch}
                onChatMenuAction={handleChatMenuAction}
            />
            <MessageArea
                messages={messages}
                loading={isLoading}
                currentUser={currentUser}
                typingUsers={typingUsers}
                selectedChat={selectedChat}
                isAnyMenuOpen={isAnyMenuOpen}
                setIsAnyMenuOpen={setIsAnyMenuOpen}
                onReloadMessages={loadMessages}
                onReplyToMessage={handleReplyToMessage}
                onEditMessage={handleEditMessage}
                onForwardMessage={handleForwardMessage}
                isSelectionMode={isSelectionMode}
                selectedMessages={selectedMessages}
                onMessageSelect={handleMessageSelect}
                onEnterSelectionMode={handleEnterSelectionMode}
            />
            <div>
                <MessageInput
                    onSendMessage={editingMessage ? handleSendEdit : (replyingToMessage ? handleSendReply : handleSendMessageOptimistic)}
                    onInputChange={handleInputChange}
                    autoFocus={true}
                    resetFocusSignal={resetFocusSignal}
                    chatAvailable={chatAvailable}
                    wsConnectionState={wsConnectionState}
                    roomId={selectedChat?.id}
                    currentUser={currentUser}
                    disabled={isSending || isSelectionMode}
                    replyingToMessage={replyingToMessage}
                    onCancelReply={handleCancelReply}
                    editingMessage={editingMessage}
                    onCancelEdit={handleCancelEdit}
                />
            </div>
            
            <Notification
                message={notification?.message}
                type={notification?.type}
                onClose={() => setNotification(null)}
            />
            
            <MessageSearch
                isOpen={isSearchOpen}
                onClose={handleCloseSearch}
                messages={messages}
                currentUser={currentUser}
                onJumpToMessage={handleJumpToMessage}
            />
            
            <ContactSelectionModal
                isOpen={isForwardModalOpen}
                onClose={handleCloseForwardModal}
                chats={chats}
                selectedMessages={messageToForward ? [messageToForward] : Array.from(selectedMessages).map(id => messages.find(msg => msg.id === id)).filter(Boolean)}
                onAction={handleForwardMessages}
                currentUser={currentUser}
                currentChatId={selectedChat?.id || selectedChat?.chatroomId}
                mode="forward"
                allowMultiSelect={true}
                showSelectAll={true}
                filterCurrentChat={true}
            />
            
            <ConfirmationModal
                isOpen={confirmationModal.isOpen}
                onClose={() => setConfirmationModal(prev => ({ ...prev, isOpen: false }))}
                onConfirm={confirmationModal.onConfirm}
                title={confirmationModal.title}
                message={confirmationModal.message}
                type={confirmationModal.type}
                isLoading={confirmationModal.isLoading}
                confirmText={confirmationModal.type === 'danger' ? 'Delete' : 'Confirm'}
                cancelText="Cancel"
            />
            
            {showAboutModal && aboutModalData && createPortal(
                <AboutComponent
                    isOpen={showAboutModal}
                    onClose={() => setShowAboutModal(false)}
                    data={aboutModalData}
                    type={aboutModalType}
                    mode="view"
                    currentUser={currentUser}
                    members={aboutModalType === 'group' ? (selectedChat?.participants || []) : []}
                />,
                document.body
            )}

            {showGroupSettingsModal && selectedChat && selectedChat.chatRoomType === 'GROUP' && createPortal(
                <GroupSettingsModal
                    key={`group-settings-${selectedChat.groupId}-${(selectedChat.participants || []).length}`}
                    isOpen={showGroupSettingsModal}
                    onClose={() => setShowGroupSettingsModal(false)}
                    group={selectedChat.group || selectedChat}
                    members={selectedChat.participants || []}
                    currentUser={currentUser}
                    chats={chats}
                    onUpdateGroup={handleUpdateGroup}
                    onAddMembers={handleAddMembers}
                    onRemoveMember={handleRemoveMember}
                    onDeleteGroup={handleDeleteGroup}
                />,
                document.body
            )}
        </div>
    );
}

export default ChatArea;
