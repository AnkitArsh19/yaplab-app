import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from '../ui/Avatar';
import '../../styles/MessageSearch.css';

function MessageSearch({ isOpen, onClose, messages, currentUser, onJumpToMessage }) {
    const [searchQuery, setSearchQuery] = useState('');
    const [filteredMessages, setFilteredMessages] = useState([]);
    const searchInputRef = useRef(null);

    useEffect(() => {
        if (isOpen && searchInputRef.current) {
            setTimeout(() => {
                searchInputRef.current.focus();
            }, 100);
        }
    }, [isOpen]);

    useEffect(() => {
        if (searchQuery.trim().length > 0) {
            const filtered = messages.filter(message => {
                const contentMatch = message.content?.toLowerCase().includes(searchQuery.toLowerCase());
                const senderMatch = message.senderName?.toLowerCase().includes(searchQuery.toLowerCase());
                return contentMatch || senderMatch;
            });
            setFilteredMessages(filtered);
        } else {
            setFilteredMessages([]);
        }
    }, [searchQuery, messages]);

    const handleClose = () => {
        setSearchQuery('');
        setFilteredMessages([]);
        onClose();
    };

    const handleMessageClick = (message) => {
        onJumpToMessage(message.id);
        handleClose();
    };

    const highlightText = (text, query) => {
        if (!query.trim()) return text;
        
        const parts = text.split(new RegExp(`(${query})`, 'gi'));
        return parts.map((part, index) => 
            part.toLowerCase() === query.toLowerCase() ? 
                <span key={index} className="highlight">{part}</span> : part
        );
    };

    const formatMessageTime = (timestamp) => {
        const date = new Date(timestamp);
        const now = new Date();
        
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const messageDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        const diffInDays = Math.floor((today - messageDate) / (1000 * 60 * 60 * 24));

        if (diffInDays === 0) {
            return date.toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit',
                hour12: true 
            });
        } else if (diffInDays === 1) {
            const time = date.toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit',
                hour12: true 
            });
            return `Yesterday ${time}`;
        } else if (diffInDays < 7) {
            const dayName = date.toLocaleDateString([], { weekday: 'long' });
            const time = date.toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit',
                hour12: true 
            });
            return `${dayName} ${time}`;
        } else {
            const dateStr = date.toLocaleDateString([], { 
                month: 'short', 
                day: 'numeric' 
            });
            const time = date.toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit',
                hour12: true 
            });
            return `${dateStr} ${time}`;
        }
    };

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            <motion.div 
                className="message-search-overlay"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={handleClose}
            >
                <motion.div 
                    className="message-search-modal"
                    initial={{ opacity: 0, scale: 0.9, y: -20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9, y: -20 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="message-search-header">
                        <div className="search-input-container">
                            <img src="search-icon.png" alt="search" className="search-icon" />
                            <input
                                ref={searchInputRef}
                                type="text"
                                placeholder="Search messages..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="search-input"
                            />
                            {searchQuery && (
                                <button
                                    className="clear-search-btn"
                                    onClick={() => setSearchQuery('')}
                                >
                                    <img src="cross-icon.png" alt="clear" />
                                </button>
                            )}
                        </div>
                        <button className="close-search-btn" onClick={handleClose}>
                            <img src="cross-icon.png" alt="close" />
                        </button>
                    </div>

                    <div className="message-search-results">
                        {searchQuery.trim().length === 0 ? (
                            <div className="search-placeholder">
                                <img src="search-icon.png" alt="search" className="placeholder-icon" />
                                <p>Search for messages, names, or keywords</p>
                            </div>
                        ) : filteredMessages.length === 0 ? (
                            <div className="no-results">
                                <p>No messages found for "{searchQuery}"</p>
                            </div>
                        ) : (
                            <div className="results-list">
                                <div className="results-header">
                                    {filteredMessages.length} message{filteredMessages.length !== 1 ? 's' : ''} found
                                </div>
                                {filteredMessages.map((message) => (
                                    <div
                                        key={message.id}
                                        className="search-result-item"
                                        onClick={() => handleMessageClick(message)}
                                    >
                                        <div className="result-avatar">
                                            <Avatar
                                                src={message.profilePictureUrl}
                                                name={message.senderName}
                                                size={40}
                                            />
                                        </div>
                                        <div className="result-content">
                                            <div className="result-header">
                                                <span className="sender-name">
                                                    {message.senderId === currentUser?.id ? 'You' : message.senderName}
                                                </span>
                                                <span className="message-time">
                                                    {formatMessageTime(message.timestamp)}
                                                </span>
                                            </div>
                                            <div className="result-message">
                                                {message.fileUrl ? (
                                                    <span className="file-message">
                                                        📎 {message.fileName || 'File attachment'}
                                                    </span>
                                                ) : (
                                                    highlightText(message.content || '', searchQuery)
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}

export default MessageSearch;
