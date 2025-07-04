import React from 'react';
import Avatar from '../ui/Avatar';
import ChatMenu from './ChatMenu';
import '../../styles/ChatHeader.css';
import { formatLastSeen } from '../../utils/formatLastSeen.js';
import { motion } from 'framer-motion';

function ChatHeader({ 
    selectedChat, 
    userStatuses, 
    currentUser, 
    isSelectionMode, 
    selectedMessages, 
    onExitSelectionMode, 
    onBulkDelete, 
    onBulkForward,
    onChatMenuAction
}) {
    if (!selectedChat) return null;

    const isPersonalChat = selectedChat.chatRoomType !== 'GROUP';
    let statusText = '';
    
    if (isPersonalChat && selectedChat.participants?.length > 0) {
        const otherParticipant = selectedChat.participants.find(p => 
            p?.id && p.id !== currentUser?.id
        );
        
        if (otherParticipant?.id) {
            const userStatusData = userStatuses?.[otherParticipant.id];
            
            if (userStatusData?.userStatus === 'ONLINE') {
                statusText = 'Online';
            } else if (userStatusData?.userStatus === 'OFFLINE') {
                if (userStatusData.lastSeen) {
                    try {
                        statusText = formatLastSeen(userStatusData.lastSeen);
                    } catch (error) {
                        console.warn('Error formatting lastSeen:', error);
                        statusText = 'Offline';
                    }
                } else {
                    statusText = 'Offline';
                }
            } else {
                statusText = 'Offline';
            }
        }
    }

    if (isSelectionMode) {
        const selectedCount = selectedMessages?.size || 0;
        return (
            <div className="chat-header selection-mode">
                <div className="chat-header-info">
                    <motion.button 
                        className="selection-back-btn"
                        onClick={onExitSelectionMode}
                    >
                        <img src="cross-icon.png" alt="Close" />
                    </motion.button>
                    <div className="selection-info">
                        <span className="selection-count">
                            {selectedCount} selected
                        </span>
                    </div>
                    <div className="selection-actions">
                        {selectedCount > 0 && (
                            <>
                                <motion.button 
                                    className="action-btn forward-btn"
                                    onClick={onBulkForward}
                                    title="Forward"
                                >
                                    <img src="share-solid.svg" alt="Forward" />
                                </motion.button>
                                <motion.button 
                                    className="action-btn delete-btn"
                                    onClick={onBulkDelete}
                                    title="Delete"
                                >
                                    <img src="delete-icon.svg" alt="Delete" />
                                </motion.button>
                            </>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="chat-header">
            <div className="chat-header-info">
                <Avatar 
                    src={selectedChat.profilePicture}
                    name={selectedChat.name}
                    size={55}
                    className="chat-header-avatar"
                />
                <div className="chat-header-details">
                    <h3 className="chat-header-name">{selectedChat.name}</h3>
                    {isPersonalChat && (
                        <p className={`chat-header-status ${statusText === 'Online' ? 'online' : 'offline'}`}>
                            {statusText || 'Offline'}
                        </p>
                    )}
                </div>
                <div className='chat-menu-container'>
                    <ChatMenu 
                        onAction={onChatMenuAction}
                        chatType={selectedChat.chatRoomType === 'GROUP' ? 'group' : 'personal'}
                        isGroupCreator={selectedChat.chatRoomType === 'GROUP' && 
                                      selectedChat.group?.createdById === currentUser?.id}
                    />
                </div>
            </div>
        </div>
    );
}

export default ChatHeader;
