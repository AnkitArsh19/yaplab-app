import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from '../ui/Avatar';
import '../../styles/ContactSelectionModal.css';

const MODAL_MODES = {
    FORWARD: 'forward',
    CREATE_GROUP: 'createGroup',
    ADD_MEMBERS: 'addMembers'
};

const DEFAULT_TITLES = {
    [MODAL_MODES.FORWARD]: 'Forward messages',
    [MODAL_MODES.CREATE_GROUP]: 'Select contacts',
    [MODAL_MODES.ADD_MEMBERS]: 'Add New Members'
};

const DEFAULT_SUBTITLES = {
    [MODAL_MODES.FORWARD]: (count) => `${count} message${count > 1 ? 's' : ''} selected`,
    [MODAL_MODES.CREATE_GROUP]: 'Choose contacts to add to the group',
    [MODAL_MODES.ADD_MEMBERS]: 'Choose contacts to add to the group'
};

const DEFAULT_ACTION_TEXTS = {
    [MODAL_MODES.FORWARD]: 'Forward',
    [MODAL_MODES.CREATE_GROUP]: 'Create Group',
    [MODAL_MODES.ADD_MEMBERS]: 'Add Members'
};

const ANIMATIONS = {
    overlay: {
        initial: { opacity: 0 },
        animate: { opacity: 1 },
        exit: { opacity: 0 }
    },
    modal: {
        initial: { opacity: 0, scale: 0.9, y: -20 },
        animate: { opacity: 1, scale: 1, y: 0 },
        exit: { opacity: 0, scale: 0.9, y: -20 }
    },
    item: {
        initial: { opacity: 0, x: -20, backgroundColor: 'rgba(244, 239, 227, 0)' },
        animate: { opacity: 1, x: 0, backgroundColor: 'rgba(244, 239, 227, 0)' },
        hover: { backgroundColor: 'rgba(244, 239, 227, 0.3)', scale: 1.01 },
        tap: { scale: 0.98 }
    }
};

function ContactSelectionModal({ 
    isOpen, 
    onClose, 
    chats = [], 
    selectedMessages = [], 
    onAction, 
    currentUser, 
    currentChatId,
    mode = MODAL_MODES.FORWARD,
    title,
    subtitle,
    actionButtonText = 'Select',
    allowMultiSelect = true,
    showSelectAll = true,
    filterCurrentChat = true
}) {
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedChats, setSelectedChats] = useState(new Set());
    const [filteredChats, setFilteredChats] = useState([]);
    const searchInputRef = useRef(null);

    useEffect(() => {
        if (isOpen && searchInputRef.current) {
            setTimeout(() => {
                searchInputRef.current.focus();
            }, 100);
        }
    }, [isOpen]);

    useEffect(() => {
        let availableChats = [...chats];
        
        if (mode === 'forward' && filterCurrentChat) {
            availableChats = availableChats.filter(chat => {
                const chatId = chat.id || chat.chatroomId;
                return chatId !== currentChatId;
            });
        }
        
        // For group creation, only show personal chats
        if (mode === 'createGroup' || mode === 'addMembers') {
            availableChats = availableChats.filter(chat => 
                chat.chatRoomType !== 'GROUP'
            );
        }
        
        const uniqueChats = [];
        const seenIds = new Set();
        
        availableChats.forEach(chat => {
            const chatId = chat.id || chat.chatroomId;
            if (chatId && !seenIds.has(chatId)) {
                seenIds.add(chatId);
                uniqueChats.push(chat);
            }
        });
        
        if (searchQuery.trim().length > 0) {
            const filtered = uniqueChats.filter(chat => {
                const nameMatch = chat.name?.toLowerCase().includes(searchQuery.toLowerCase());
                // For personal chats, also search participant names
                if (chat.chatRoomType !== 'GROUP' && chat.participants) {
                    const participantMatch = chat.participants.some(participant => 
                        participant.userName?.toLowerCase().includes(searchQuery.toLowerCase())
                    );
                    return nameMatch || participantMatch;
                }
                return nameMatch;
            });
            setFilteredChats(filtered);
        } else {
            setFilteredChats(uniqueChats);
        }
    }, [searchQuery, chats, currentChatId, mode, filterCurrentChat]);

    const handleClose = () => {
        setSearchQuery('');
        setSelectedChats(new Set());
        onClose();
    };

    const handleChatSelect = (chatId) => {
        if (!allowMultiSelect) {
            // Single select mode
            setSelectedChats(new Set([chatId]));
            return;
        }

        // Multi select mode
        const newSelectedChats = new Set(selectedChats);
        if (newSelectedChats.has(chatId)) {
            newSelectedChats.delete(chatId);
        } else {
            newSelectedChats.add(chatId);
        }
        setSelectedChats(newSelectedChats);
    };

    const handleSelectAll = () => {
        if (!allowMultiSelect || !showSelectAll) return;
        
        if (selectedChats.size === filteredChats.length) {
            // Deselect all
            setSelectedChats(new Set());
        } else {
            // Select all visible chats
            const allChatIds = new Set(filteredChats.map(chat => chat.id || chat.chatroomId));
            setSelectedChats(allChatIds);
        }
    };

    const handleAction = () => {
        if (selectedChats.size > 0) {
            const selectedIds = Array.from(selectedChats);
            onAction(selectedIds);
            handleClose();
        }
    };

    const formatChatName = (chat) => {
        if (chat.chatRoomType === 'GROUP') {
            return chat.name || 'Group Chat';
        } else if (chat.participants && chat.participants.length > 0) {
            // For personal chats, show the other participant's name
            const otherParticipant = chat.participants.find(p => p.id !== currentUser?.id);
            return otherParticipant?.userName || chat.name || 'Contact';
        }
        return chat.name || 'Contact';
    };

    const getChatAvatar = (chat) => {
        if (chat.chatRoomType === 'GROUP') {
            return chat.profilePicture || '/default-group.png';
        } else if (chat.participants && chat.participants.length > 0) {
            const otherParticipant = chat.participants.find(p => p.id !== currentUser?.id);
            return otherParticipant?.profilePictureUrl || chat.profilePicture || '/default-avatar.png';
        }
        return chat.profilePicture || '/default-avatar.png';
    };

    const getChatType = (chat) => {
        if (mode === 'createGroup' || mode === 'addMembers') {
            return 'Contact';
        }
        return chat.chatRoomType === 'GROUP' ? 'Group' : 'Personal';
    };

    const getDefaultTitle = () => DEFAULT_TITLES[mode] || 'Select chats';
    
    const getDefaultSubtitle = () => {
        const subtitleFn = DEFAULT_SUBTITLES[mode];
        return typeof subtitleFn === 'function' 
            ? subtitleFn(selectedMessages.length)
            : subtitleFn || '';
    };
    
    const getDefaultActionText = () => DEFAULT_ACTION_TEXTS[mode] || actionButtonText;

    const getEmptyStateMessage = () => {
        const itemType = (mode === MODAL_MODES.CREATE_GROUP || mode === MODAL_MODES.ADD_MEMBERS) ? 'contacts' : 'chats';
        return searchQuery.trim().length > 0
            ? `No ${itemType} found for "${searchQuery}"`
            : `No ${itemType} available`;
    };

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            <motion.div 
                className="contact-modal-overlay"
                initial={ANIMATIONS.overlay.initial}
                animate={ANIMATIONS.overlay.animate}
                exit={ANIMATIONS.overlay.exit}
                onClick={handleClose}
            >
                <motion.div 
                    className="contact-modal"
                    initial={ANIMATIONS.modal.initial}
                    animate={ANIMATIONS.modal.animate}
                    exit={ANIMATIONS.modal.exit}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="contact-modal-header">
                        <div className="contact-header-left">
                            <button className="contact-close-btn" onClick={handleClose}>
                                <img src="/cross-icon.png" alt="Close" />
                            </button>
                            <div className="contact-header-info">
                                <h3 className="contact-title">{title || getDefaultTitle()}</h3>
                                {(subtitle || getDefaultSubtitle()) && (
                                    <p className="contact-subtitle">
                                        {subtitle || getDefaultSubtitle()}
                                    </p>
                                )}
                            </div>
                        </div>
                        {filteredChats.length > 0 && allowMultiSelect && showSelectAll && (
                            <button 
                                className="contact-select-all-btn"
                                onClick={handleSelectAll}
                            >
                                {selectedChats.size === filteredChats.length ? 'Deselect All' : 'Select All'}
                            </button>
                        )}
                    </div>

                    <div className="contact-search-container">
                        <div className="contact-search-input-wrapper">
                            <img src="/search-icon.png" alt="search" className="contact-search-icon" />
                            <input
                                ref={searchInputRef}
                                type="text"
                                placeholder={`Search ${(mode === 'createGroup' || mode === 'addMembers') ? 'contacts' : 'chats'}...`}
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="contact-search-input"
                            />
                            {searchQuery && (
                                <button
                                    className="contact-clear-search-btn"
                                    onClick={() => setSearchQuery('')}
                                >
                                    <img src="/cross-icon.png" alt="clear" />
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="contact-chat-list">
                        {filteredChats.length === 0 ? (
                            <div className="contact-no-chats">
                                <p>{getEmptyStateMessage()}</p>
                            </div>
                        ) : (
                            filteredChats.map((chat) => {
                                const chatId = chat.id || chat.chatroomId;
                                const isSelected = selectedChats.has(chatId);
                                
                                return (
                                    <motion.div
                                        key={chatId}
                                        className={`contact-chat-item ${isSelected ? 'selected' : ''}`}
                                        initial={ANIMATIONS.item.initial}
                                        animate={ANIMATIONS.item.animate}
                                        transition={{ 
                                            duration: 0.3, 
                                            delay: filteredChats.indexOf(chat) * 0.05,
                                            ease: "easeOut"
                                        }}
                                        whileHover={ANIMATIONS.item.hover}
                                        whileTap={ANIMATIONS.item.tap}
                                        onClick={() => handleChatSelect(chatId)}
                                    >
                                        {allowMultiSelect ? (
                                            <div className="contact-checkbox">
                                                <input
                                                    type="checkbox"
                                                    checked={isSelected}
                                                    onChange={() => handleChatSelect(chatId)}
                                                    onClick={(e) => e.stopPropagation()}
                                                />
                                                <div className={`contact-checkbox-custom ${isSelected ? 'checked' : ''}`}>
                                                    
                                                </div>
                                            </div>
                                        ) : (
                                            <div className="contact-radio">
                                                <input
                                                    type="radio"
                                                    checked={isSelected}
                                                    onChange={() => handleChatSelect(chatId)}
                                                    onClick={(e) => e.stopPropagation()}
                                                />
                                                <div className={`contact-radio-custom ${isSelected ? 'checked' : ''}`}>
                                                    
                                                </div>
                                            </div>
                                        )}

                                        <div className="contact-chat-avatar">
                                            <Avatar
                                                src={getChatAvatar(chat)}
                                                name={formatChatName(chat)}
                                                size={45}
                                            />
                                        </div>
                                        <div className="contact-chat-info">
                                            <div className="contact-chat-name">
                                                {formatChatName(chat)}
                                            </div>
                                            <div className="contact-chat-type">
                                                {getChatType(chat)}
                                            </div>
                                        </div>
                                    </motion.div>
                                );
                            })
                        )}
                    </div>

                    <div className="contact-modal-footer">
                        <div className="contact-selected-count">
                            {selectedChats.size > 0 && (
                                <span>
                                    {selectedChats.size} {(mode === 'createGroup' || mode === 'addMembers') ? 'contact' : 'chat'}{selectedChats.size > 1 ? 's' : ''} selected
                                </span>
                            )}
                        </div>
                        <div className="contact-actions">
                            <button 
                                className="contact-cancel-btn" 
                                onClick={handleClose}
                            >
                                Cancel
                            </button>
                            <button 
                                className={`contact-action-btn ${selectedChats.size === 0 ? 'disabled' : ''}`}
                                onClick={handleAction}
                                disabled={selectedChats.size === 0}
                            >
                                {getDefaultActionText()}
                            </button>
                        </div>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}

export default ContactSelectionModal;
