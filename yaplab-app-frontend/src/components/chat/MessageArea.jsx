import React, { useEffect, useRef, useState } from 'react';
import Message from './Message';
import LoadingThreeDots from '../ui/LoadingThreeDots';
import Avatar from '../ui/Avatar';
import { getAvatarColor } from '../../utils/avatarUtils';
import '../../styles/MessageArea.css';
import { format } from 'date-fns';

function formatDateSeparator(dateString) {
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    if (
        date.getFullYear() === today.getFullYear() &&
        date.getMonth() === today.getMonth() &&
        date.getDate() === today.getDate()
    ) {
        return 'Today';
    } else if (
        date.getFullYear() === yesterday.getFullYear() &&
        date.getMonth() === yesterday.getMonth() &&
        date.getDate() === yesterday.getDate()
    ) {
        return 'Yesterday';
    } else {
        return format(date, 'dd/MM/yyyy');
    }
}

function MessageArea({ 
    messages, 
    loading, 
    currentUser, 
    typingUsers = new Set(), 
    selectedChat, 
    isAnyMenuOpen, 
    setIsAnyMenuOpen, 
    onReloadMessages, 
    onReplyToMessage, 
    onEditMessage,
    onForwardMessage,
    isSelectionMode,
    selectedMessages,
    onMessageSelect,
    onEnterSelectionMode
}) {
    const messagesEndRef = useRef(null);
    const messagesContainerRef = useRef(null);
    const previousMessagesLength = useRef(0);
    const isInitialLoad = useRef(true);
    const scrollTimeoutRef = useRef(null);
    const [stickyDate, setStickyDate] = useState('');
    const [showStickyDate, setShowStickyDate] = useState(false);
    const [highlightedMessageId, setHighlightedMessageId] = useState(null);
    const messageRefs = useRef({});

    const handleMessageDeleted = (messageId) => {
        setTimeout(() => {
            const messageStillExists = messages.some(msg => msg.id === messageId);
            if (messageStillExists && onReloadMessages) {
                onReloadMessages();
            }
        }, 500);
    };

    const scrollToBottom = (smooth = true) => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ 
                behavior: smooth ? "smooth" : "instant"
            });
        }
    };

    const scrollToMessage = (messageId) => {
        const messageElement = messageRefs.current[messageId];
        if (messageElement) {
            messageElement.scrollIntoView({ 
                behavior: "smooth", 
                block: "center" 
            });
            
            setHighlightedMessageId(messageId);
            
            setTimeout(() => {
                setHighlightedMessageId(null);
            }, 3000);
        }
    };

    const handleImageLoad = () => {
        const container = messagesContainerRef.current;
        if (container) {
            const isNearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 100;
            if (isNearBottom) {
                requestAnimationFrame(() => {
                    scrollToBottom(true);
                });
            }
        }
    };

    useEffect(() => {
        if (messages.length === 0) {
            isInitialLoad.current = true;
            return;
        }

        if (isInitialLoad.current) {
            scrollToBottom(false);
            isInitialLoad.current = false;
            previousMessagesLength.current = messages.length;
            return;
        }

        if (messages.length > previousMessagesLength.current) {
            requestAnimationFrame(() => {
                setTimeout(() => {
                    scrollToBottom(true);
                }, 10);
            });
        }

        previousMessagesLength.current = messages.length;
    }, [messages]);

    useEffect(() => {
        if (loading) {
            isInitialLoad.current = true;
            previousMessagesLength.current = 0;
        }
    }, [loading]);

    useEffect(() => {
        const container = messagesContainerRef.current;
        if (!container) return;

        const handleScroll = () => {
            setShowStickyDate(true);
            
            if (scrollTimeoutRef.current) {
                clearTimeout(scrollTimeoutRef.current);
            }
            
            scrollTimeoutRef.current = setTimeout(() => {
                setShowStickyDate(false);
            }, 1500);
            
            const dateSeparators = container.querySelectorAll('.date-separator');
            const containerRect = container.getBoundingClientRect();
            const stickyHeaderHeight = 40;
            const threshold = containerRect.top + 20 + stickyHeaderHeight;
            
            let currentVisibleDate = '';
            let shouldHideSticky = false;
            
            for (let i = 0; i < dateSeparators.length; i++) {
                const separator = dateSeparators[i];
                const separatorRect = separator.getBoundingClientRect();
                
                if (separatorRect.top <= threshold + 20 && separatorRect.bottom >= threshold - 20) {
                    shouldHideSticky = true;
                    break;
                }
                
                if (separatorRect.top <= threshold) {
                    currentVisibleDate = separator.textContent;
                } else {
                    break;
                }
            }
            
            const isNearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 50;
            
            if (shouldHideSticky || isNearBottom || !currentVisibleDate) {
                setShowStickyDate(false);
            } else if (currentVisibleDate) {
                setStickyDate(currentVisibleDate);
            }
        };

        container.addEventListener('scroll', handleScroll);
        
        return () => {
            container.removeEventListener('scroll', handleScroll);
            if (scrollTimeoutRef.current) {
                clearTimeout(scrollTimeoutRef.current);
            }
        };
    }, [messages]);

    if (loading) {
        return (
            <div className="message-area">
                <div className="loading-messages">Loading messages...</div>
            </div>
        );
    }

    const groupedMessages = [];
    let lastDate = null;
    messages.forEach((message, idx) => {
        const msgDate = new Date(message.timestamp);
        const dateKey = msgDate.toISOString().split('T')[0];
        if (dateKey !== lastDate) {
            groupedMessages.push({ type: 'date', date: dateKey });
            lastDate = dateKey;
        }
        groupedMessages.push({ type: 'message', message });
    });

    return (
        <div className="message-area">
            <div className={`sticky-date-header ${showStickyDate ? 'visible' : ''}`}>
                {stickyDate}
            </div>
            <div className={`messages-container ${isAnyMenuOpen ? 'scroll-disabled' : ''} ${isSelectionMode ? 'selection-mode' : ''}`} ref={messagesContainerRef}>
                {groupedMessages.length === 0 ? (
                    <div className="no-messages">
                        <p>No messages yet. Start the conversation!</p>
                    </div>
                ) : (
                    groupedMessages.map((item, idx) => {
                        if (item.type === 'date') {
                            return (
                                <div className="date-separator" key={`date-${item.date}-${idx}`}>
                                    {formatDateSeparator(item.date)}
                                </div>
                            );
                        } else if (item.type === 'message') {
                            return (
                                <Message
                                    key={item.message.id || item.message.tempId}
                                    message={item.message}
                                    currentUser={currentUser}
                                    selectedChat={selectedChat}
                                    onImageLoad={handleImageLoad}
                                    isAnyMenuOpen={isAnyMenuOpen}
                                    setIsAnyMenuOpen={setIsAnyMenuOpen}
                                    onMessageDeleted={handleMessageDeleted}
                                    onReplyToMessage={onReplyToMessage}
                                    onEditMessage={onEditMessage}
                                    onForwardMessage={onForwardMessage}
                                    onScrollToMessage={scrollToMessage}
                                    isHighlighted={highlightedMessageId === item.message.id}
                                    isSelectionMode={isSelectionMode}
                                    isSelected={selectedMessages?.has(item.message.id)}
                                    onMessageSelect={onMessageSelect}
                                    onEnterSelectionMode={onEnterSelectionMode}
                                    ref={(el) => {
                                        if (el && item.message.id) {
                                            messageRefs.current[item.message.id] = el;
                                        }
                                    }}
                                />
                            );
                        }
                        return null;
                    })
                )}
                {Array.from(typingUsers).map(userId => {
                    if (userId === currentUser?.id) return null;
                    const user = selectedChat?.participants?.find(p => p.id === userId);
                    const isGroupChat = selectedChat?.chatRoomType === 'GROUP';
                    
                    const getUserColor = (userName) => {
                        return getAvatarColor(userName || 'Unknown');
                    };
                    
                    return (
                        <div key={userId} className={`message typing-indicator ${userId === currentUser?.id ? 'sent' : 'received'} ${isGroupChat ? 'group-chat' : ''}`}>
                            {isGroupChat && (
                                <div className="group-typing-header">
                                    <Avatar
                                        src={user?.profilePictureUrl}
                                        name={user?.userName || 'Unknown'}
                                        size={32}
                                        className="group-typing-avatar"
                                    />
                                    <span 
                                        className="group-typing-name"
                                        style={{ color: getUserColor(user?.userName || 'Unknown') }}
                                    >
                                        {user?.userName || 'Unknown'}
                                    </span>
                                </div>
                            )}
                            <div className="message-content typing-content">
                                <LoadingThreeDots color={userId === currentUser?.id ? undefined : '#a27f68'} />
                            </div>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>
        </div>
    );
}

export default MessageArea;
