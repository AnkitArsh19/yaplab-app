import React, { useEffect, useState, forwardRef } from 'react';
import '../../styles/Message.css';
import apiClient from '../../utils/apiClient';
import { getAvatarColor } from '../../utils/avatarUtils';
import AudioPlayer from '../media/AudioPlayer';
import MessageVideoModal from '../media/MessageVideoModal';
import MessageMenu from './MessageMenu';
import ConfirmationModal from '../modals/ConfirmationModal';
import Avatar from '../ui/Avatar';

const Message = forwardRef(({ 
    message, 
    currentUser, 
    selectedChat,
    onImageLoad, 
    isAnyMenuOpen, 
    setIsAnyMenuOpen, 
    onMessageDeleted, 
    onReplyToMessage, 
    onScrollToMessage, 
    onEditMessage, 
    onForwardMessage,
    isHighlighted, 
    isSelectionMode, 
    isSelected, 
    onMessageSelect, 
    onEnterSelectionMode 
}, ref) => {
    const [imageUrl, setImageUrl] = useState(null);
    const [imageLoading, setImageLoading] = useState(false);
    const [showVideoModal, setShowVideoModal] = useState(false);
  
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    
    useEffect(() => {
    }, [showDeleteConfirm]);

    const isOwnMessage = React.useMemo(() => {
        if (message.senderId && currentUser?.id) {
            return message.senderId === currentUser.id;
        }
        
        if (message.senderName && currentUser?.userName) {
            return message.senderName === currentUser.userName;
        }
        
        return false;
    }, [message.senderId, message.senderName, currentUser?.id, currentUser?.userName]);

    const isGroupChat = selectedChat?.chatRoomType === 'GROUP';
    
    const getSenderInfo = () => {
        if (!isGroupChat || isOwnMessage) return null;
        
        const sender = selectedChat?.participants?.find(p => p.id === message.senderId);
        
        return {
            name: message.senderName || sender?.userName || 'Unknown',
            avatar: sender?.profilePictureUrl,
            color: getAvatarColor(message.senderName || sender?.userName || 'Unknown')
        };
    };

    const senderInfo = getSenderInfo();

    useEffect(() => {
    }, [message.id, message.senderId, message.senderName, currentUser?.id, isOwnMessage]);

    useEffect(() => {
        const loadAuthenticatedImage = async () => {
            if (!message.fileUrl || !message.fileType) return;
            
            const fileType = message.fileType;
            if (!fileType.startsWith('image/') && !fileType.startsWith('video/')) return;
            
            setImageLoading(true);
            try {
                const response = await apiClient.request(message.fileUrl, {
                    method: 'GET',
                    headers: {}
                });

                if (response.ok) {
                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);
                    setImageUrl(url);
                    
                    if (onImageLoad) {
                        setTimeout(onImageLoad, 100);
                    }
                } else {
                    console.error('Failed to load media:', response.status, response.statusText);
                }
            } catch (error) {
                console.error('Error loading authenticated media:', error);
            } finally {
                setImageLoading(false);
            }
        };

        loadAuthenticatedImage();

        return () => {
            if (imageUrl) {
                URL.revokeObjectURL(imageUrl);
            }
        };
    }, [message.fileUrl, message.fileType]);

    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        if (isNaN(date.getTime())) return '';
        return date.toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
        }).replace(/^0/, '').replace('am', 'AM').replace('pm', 'PM');
    };

    const formatFileSize = (bytes) => {
        if (!bytes) return '';
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return `${size.toFixed(size < 10 && unitIndex > 0 ? 1 : 0)}${units[unitIndex]}`;
    };

    const handleReply = () => {
        if (onReplyToMessage) {
            onReplyToMessage(message);
        }
    };

    const handleCopy = () => {
        if (message.content) {
            navigator.clipboard.writeText(message.content);
        }
    };

    const handleForward = () => {
        if (onForwardMessage) {
            onForwardMessage(message);
        }
    };

    const handleEdit = () => {
        if (onEditMessage) {
            onEditMessage(message);
        }
    };

    const handleMessageClick = (e) => {
        if (isSelectionMode) {
            e.preventDefault();
            e.stopPropagation();
            onMessageSelect && onMessageSelect(message.id);
        }
    };

    const handleMessageLongPress = (e) => {
        if (!isSelectionMode && onEnterSelectionMode) {
            e.preventDefault();
            e.stopPropagation();
            onEnterSelectionMode();
            setTimeout(() => {
                onMessageSelect && onMessageSelect(message.id);
            }, 50);
        }
    };

    const [touchStart, setTouchStart] = useState(null);
    const [touchTimeout, setTouchTimeout] = useState(null);

    const handleTouchStart = (e) => {
        if (isSelectionMode) return;
        
        const touch = e.touches[0];
        setTouchStart({ x: touch.clientX, y: touch.clientY });
        
        const timeout = setTimeout(() => {
            handleMessageLongPress(e);
        }, 500);
        
        setTouchTimeout(timeout);
    };

    const handleTouchEnd = (e) => {
        if (touchTimeout) {
            clearTimeout(touchTimeout);
            setTouchTimeout(null);
        }
        setTouchStart(null);
    };

    const handleTouchMove = (e) => {
        if (!touchStart || !touchTimeout) return;
        
        const touch = e.touches[0];
        const deltaX = Math.abs(touch.clientX - touchStart.x);
        const deltaY = Math.abs(touch.clientY - touchStart.y);
        
        if (deltaX > 10 || deltaY > 10) {
            clearTimeout(touchTimeout);
            setTouchTimeout(null);
        }
    };

    const handleCheckboxChange = (e) => {
        e.stopPropagation();
        onMessageSelect && onMessageSelect(message.id);
    };

    const handleSelect = () => {
        if (!isSelectionMode && onEnterSelectionMode) {
            onEnterSelectionMode();
            setTimeout(() => {
                onMessageSelect && onMessageSelect(message.id);
            }, 50);
        }
    };

    const handleDelete = () => {
        if (!message.id || !currentUser?.id) {
            console.error('Cannot delete message: missing message ID or user ID');
            return;
        }
        setShowDeleteConfirm(true);
    };
    
    const confirmDelete = async () => {
        setIsDeleting(true);
        try {
            const response = await apiClient.delete(`/messages/${message.id}?userId=${currentUser.id}`);
            
            if (response.ok) {
                if (onMessageDeleted) {
                    onMessageDeleted(message.id);
                }
                setTimeout(() => {
                    setIsDeleting(false);
                    setShowDeleteConfirm(false);
                }, 800);
            } else {
                const errorText = await response.text();
                console.error('Failed to delete message:', response.status, errorText);
                setIsDeleting(false);
                
                if (response.status === 403) {
                    alert('You are not authorized to delete this message.');
                } else {
                    alert('Error deleting message. Please try again.');
                }
            }
        } catch (error) {
            console.error('Error deleting message:', error);
            setIsDeleting(false);
            alert('Error deleting message. Please try again.');
        }
    };

    const getCleanFileName = (fileName) => {
        if (!fileName) return 'Unknown File';
        
        let cleanName = fileName;
        
        if (cleanName.includes('/')) {
            cleanName = cleanName.split('/').pop();
        }
        
        cleanName = cleanName.replace(/^\d+_/, '');
        
        if (message.fileType?.startsWith('audio/')) {
            cleanName = cleanName.replace(/^audio_\d+\./, '').replace(/^recording_\d+\./, '') || 'Audio Message';
        }
        
        return cleanName || fileName;
    };

    const renderFileContent = () => {
        if (!message.fileUrl || !message.fileType) return null;

        const fileType = message.fileType;

        if (fileType === 'image/gif') {
            return (
                <div className="message-file">
                    <div className="gif-attachment">
                        {imageLoading ? (
                            <div style={{ padding: '20px', textAlign: 'center' }}>Loading...</div>
                        ) : imageUrl ? (
                            <img 
                                src={imageUrl} 
                                alt="GIF" 
                                className="gif-preview"
                                style={{ maxWidth: '300px', maxHeight: '300px', borderRadius: '8px' }}
                                onLoad={onImageLoad}
                            />
                        ) : null}
                    </div>
                </div>
            );
        }

        if (fileType.startsWith('image/')) {
            return (
                <div className="message-file">
                    <div className="image-attachment">
                        {imageLoading ? (
                            <div style={{ padding: '20px', textAlign: 'center' }}>Loading...</div>
                        ) : imageUrl ? (
                            <img 
                                src={imageUrl} 
                                alt="Image" 
                                className="image-preview"
                                style={{ maxWidth: '300px', maxHeight: '300px', borderRadius: '8px' }}
                                onLoad={onImageLoad}
                            />
                        ) : null}
                    </div>
                </div>
            );
        }

        if (fileType.startsWith('video/')) {
            return (
                <div className="message-file">
                    <div className="video-attachment">
                        {imageLoading ? (
                            <div style={{ padding: '20px', textAlign: 'center' }}>Loading...</div>
                        ) : imageUrl ? (
                            <div className="message-video-container">
                                <video 
                                    src={imageUrl} 
                                    className="message-video-player"
                                    style={{ maxWidth: '300px', maxHeight: '300px', borderRadius: '8px' }}
                                    controls
                                    preload="metadata"
                                    onClick={() => setShowVideoModal(true)}
                                    onPlay={(e) => e.target.parentElement.classList.add('playing')}
                                    onPause={(e) => e.target.parentElement.classList.remove('playing')}
                                    onEnded={(e) => e.target.parentElement.classList.remove('playing')}
                                />
                            </div>
                        ) : null}
                    </div>
                </div>
            );
        }

        if (fileType.startsWith('audio/')) {
            return (
                <div className="message-file">
                    <div className="audio-attachment">
                        <div style={{ flex: 1 }}>
                            <AudioPlayer 
                                audioUrl={message.fileUrl} 
                                fileName={getCleanFileName(message.fileName)}
                                className="audio-preview"
                            />
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div className="message-file">
                <div className="document-attachment" onClick={() => window.open(message.fileUrl, '_blank')}>
                    <div className="document-icon-container">
                        <img src="/file-solid.svg" alt="Document" className="file-type-icon" />
                    </div>
                    <div className="file-details">
                        <div className="file-info">
                            <div className="file-name">{getCleanFileName(message.fileName)}</div>
                            {message.fileSize && (
                                <div className="file-size">{formatFileSize(message.fileSize)}</div>
                            )}
                        </div>
                        <div className="file-actions">
                            <a 
                                href={message.fileUrl} 
                                download={getCleanFileName(message.fileName)} 
                                className="download-link"
                                onClick={(e) => e.stopPropagation()}
                            >
                                Download
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    const getMessageStatusIcon = () => {
        if (!isOwnMessage) return null;

        if (message.isOptimistic) {
            return <div className="status-icon-container">
                <img src="/clock-regular.svg" alt="Sending" className="status-icon sending rotating" title="Sending..." />
            </div>;
        }

        const status = message.messageStatus?.toUpperCase();
        
        switch (status) {
            case 'SENT':
                return <div className="status-icon-container">
                    <img src="/sent.png" alt="Sent" className="status-icon sent" title="Sent" />
                </div>;
                
            case 'DELIVERED':
                return <div className="status-icon-container">
                    <img src="/delivered.png" alt="Delivered" className="status-icon delivered" title="Delivered" />
                </div>;
                
            case 'READ':
                return <div className="status-icon-container">
                    <img src="/read.png" alt="Read" className="status-icon read" 
                        title={message.readAt ? `Read at ${formatTime(message.readAt)}` : "Read"} />
                </div>;
                
            case 'NOT_SENT':
            case 'ERROR':
                return <div className="status-icon-container">
                    <img src="/error.png" alt="Error" className="status-icon error" 
                        title="Failed to send. Tap to retry." 
                        style={{ cursor: 'pointer' }} />
                </div>;
                
            default:
                return null;
        }
    };

    return (
        <>
            <div 
                className={`message ${isOwnMessage ? 'sent' : 'received'} ${isGroupChat ? 'group-chat' : ''} ${isHighlighted ? 'highlighted' : ''} ${isSelectionMode ? 'selection-mode' : ''} ${isSelected ? 'selected' : ''}`} 
                ref={ref}
                data-message-id={message.id}
                data-sender-id={message.senderId}
                onClick={handleMessageClick}
                onContextMenu={handleMessageLongPress}
                onTouchStart={handleTouchStart}
                onTouchEnd={handleTouchEnd}
                onTouchMove={handleTouchMove}
            >
                {isSelectionMode && (
                    <div className={`selection-checkbox ${isOwnMessage ? 'sent-selection' : 'received-selection'}`}>
                        <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={handleCheckboxChange}
                            onClick={(e) => e.stopPropagation()}
                        />
                        <div className={`checkbox-custom ${isSelected ? 'checked' : ''}`}>
                     
                        </div>
                    </div>
                )}
                
                {isGroupChat && !isOwnMessage && senderInfo && (
                    <div className="group-message-header">
                        <Avatar
                            src={senderInfo.avatar}
                            name={senderInfo.name}
                            size={32}
                            className="group-sender-avatar"
                        />
                        <span 
                            className="group-sender-name"
                            style={{ color: senderInfo.color }}
                        >
                            {senderInfo.name}
                        </span>
                    </div>
                )}
                
                {isGroupChat && isOwnMessage && (
                    <div className="group-message-header own-message">
                        <span className="group-sender-name own">You</span>
                    </div>
                )}
                
                <div className="message-content">
                    <MessageMenu
                        isOwnMessage={isOwnMessage}
                        onReply={handleReply}
                        onCopy={handleCopy}
                        onForward={handleForward}
                        onEdit={handleEdit}
                        onDelete={handleDelete}
                        onSelect={handleSelect}
                        isAnyMenuOpen={isAnyMenuOpen}
                        setIsAnyMenuOpen={setIsAnyMenuOpen}
                        canEdit={isOwnMessage && !!(message.content && message.content.trim()) && !message.fileUrl}
                        isSelectionMode={isSelectionMode}
                    />
                    
                    {message.repliedToMessage && !message.forwarded && (
                        <div 
                            className="replied-message" 
                            onClick={() => onScrollToMessage && onScrollToMessage(message.repliedToMessage.id)}
                            style={{ cursor: onScrollToMessage ? 'pointer' : 'default' }}
                        >
                            <div className="replied-message-indicator"></div>
                            <div className="replied-message-content">
                                <div className="replied-message-author">{message.repliedToMessage.senderName}</div>
                                <div className="replied-message-text">
                                    {message.repliedToMessage.content || 'Media message'}
                                </div>
                            </div>
                        </div>
                    )}
                    {renderFileContent()}
                    {message.content && 
                     message.content.trim() !== '' &&
                     message.fileUrl && 
                     message.content.length > 3 &&
                     !message.content.includes('.') && 
                     !message.content.toLowerCase().includes('voice message') && 
                     !message.content.toLowerCase().includes('audio message') && 
                     !message.content.toLowerCase().includes('recording') && 
                     !message.content.toLowerCase().includes('voice memo') &&
                     !message.content.toLowerCase().includes('audio file') &&
                     !(message.fileType === 'image/gif' && message.content.match(/^🎬/)) &&
                     (
                        <div className="message-text">{message.content}</div>
                     )}
                     
                     {message.content && 
                      message.content.trim() !== '' &&
                      !message.fileUrl && (
                        <div className="message-text">{message.content}</div>
                     )}
                     
                     {message.edited && (
                        <div className="message-edited-indicator">
                            <span>edited</span>
                        </div>
                     )}
                     
                     {message.forwarded && (
                        <div className="message-forwarded-indicator">
                            <span>forwarded</span>
                        </div>
                     )}
                </div>
                
                <div className="message-footer-external">
                    <span className="message-timestamp-external">{formatTime(message.timestamp)}</span>
                    {getMessageStatusIcon()}
                </div>
            </div>

            {message.fileType?.startsWith('video/') && imageUrl && (
                <MessageVideoModal 
                    videoUrl={imageUrl}
                    isOpen={showVideoModal}
                    onClose={() => setShowVideoModal(false)}
                />
            )}
            <ConfirmationModal
                isOpen={showDeleteConfirm}
                onClose={() => !isDeleting && setShowDeleteConfirm(false)}
                onConfirm={confirmDelete}
                title="Delete Message"
                message="Are you sure you want to delete this message? This action cannot be undone."
                confirmText="Delete"
                cancelText="Cancel"
                type="danger"
                isLoading={isDeleting}
            />
        </>
    );
});

export default Message;