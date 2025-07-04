import React, { useState, useEffect, useRef } from 'react';
import '../../styles/MessageInput.css';
import websocketService from '../../utils/websocketService.js';
import apiClient from '../../utils/apiClient.js';
import data from '@emoji-mart/data';
import Picker from '@emoji-mart/react';
import { motion } from 'framer-motion';
import EmojiGifPopover from '../media/EmojiGifPopover';
import GifPicker from '../media/GifPicker';
import AudioRecorder from '../media/AudioRecorder';
import AttachmentMenu from '../media/AttachmentMenu';
import AttachmentPreview from '../media/AttachmentPreview';

function MessageInput({ onSendMessage, disabled = false, loading = false, wsConnectionState = 'disconnected', autoFocus = false, resetFocusSignal, chatAvailable = true, onInputChange, roomId, currentUser, replyingToMessage, onCancelReply, editingMessage, onCancelEdit }) {
    const CHARACTER_LIMIT = 2000;
    const WARNING_THRESHOLD = 0.8;
    const DANGER_THRESHOLD = 0.9;
    const CRITICAL_THRESHOLD = 0.95;

    const [message, setMessage] = useState('');
    const [isRecordingMode, setIsRecordingMode] = useState(false);
    const prevIsRecordingMode = useRef(isRecordingMode);
    const inputRef = useRef(null);
    const audioRecorderRef = useRef(null);
    const typingTimeout = useRef(null);
    const [showPopover, setShowPopover] = useState(false);
    const [activeTab, setActiveTab] = useState('emoji');
    const [keepPickerOpen, setKeepPickerOpen] = useState(false);
    const popoverRef = useRef(null);
    
    const [selectedAttachments, setSelectedAttachments] = useState([]);
    const [showAttachmentPreview, setShowAttachmentPreview] = useState(false);

    useEffect(() => {
        if (inputRef.current) {
            inputRef.current.style.height = 'auto';
            const lineHeight = 22;
            const minHeight = lineHeight;
            const maxLines = 3;
            const maxHeight = lineHeight * maxLines;
            const newHeight = Math.max(inputRef.current.scrollHeight, minHeight);
            inputRef.current.style.height = Math.min(newHeight, maxHeight) + 'px';
        }
    }, [message]);

    useEffect(() => {
        if (autoFocus && inputRef.current && chatAvailable) {
            inputRef.current.focus();
        }
    }, [autoFocus, resetFocusSignal, chatAvailable]);

    useEffect(() => {
        function handleClickOutside(e) {
            if (popoverRef.current && !popoverRef.current.contains(e.target)) {
                setShowPopover(false);
            }
        }
        if (showPopover) {
            document.addEventListener('mousedown', handleClickOutside);
        } else {
            document.removeEventListener('mousedown', handleClickOutside);
        }
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [showPopover]);

    useEffect(() => {
        if (prevIsRecordingMode.current && !isRecordingMode && inputRef.current) {
            inputRef.current.focus();
        }
        prevIsRecordingMode.current = isRecordingMode;
    }, [isRecordingMode]);

    useEffect(() => {
        if (editingMessage) {
            setMessage(editingMessage.content || '');
        }
    }, [editingMessage]);

    useEffect(() => {
        if (!editingMessage && !replyingToMessage) {
            setMessage('');
        }
    }, [editingMessage, replyingToMessage]);
    
    const handleChange = (e) => {
        const newValue = e.target.value;
        
        if (newValue.length > CHARACTER_LIMIT) {
            return;
        }
        
        setMessage(newValue);
        if (onInputChange) onInputChange(e);
        if (roomId && wsConnectionState === 'connected') {
            websocketService.startTyping(roomId);
            if (typingTimeout.current) clearTimeout(typingTimeout.current);
            typingTimeout.current = setTimeout(() => {
                websocketService.stopTyping(roomId);
            }, 2000);
        }
    };

    const handleKeyDown = (e) => {
        if (e.ctrlKey && e.shiftKey && e.key === 'E') {
            e.preventDefault();
            setKeepPickerOpen(prev => !prev);
            return;
        }
        
        if (e.key === 'Enter' && !e.shiftKey && !disabled && !loading && chatAvailable && message.trim()) {
            e.preventDefault();
            handleSubmit(e);
        }
    };

    const handleBlur = () => {
        if (roomId && wsConnectionState === 'connected') {
            websocketService.stopTyping(roomId);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!message.trim() || disabled || loading || !chatAvailable || message.length > CHARACTER_LIMIT) {
            return;
        }

        const messageToSend = message.trim();
        try {
            await onSendMessage(messageToSend);
            setMessage('');
            if (inputRef.current) inputRef.current.focus();
            if (roomId && wsConnectionState === 'connected') {
                websocketService.stopTyping(roomId);
            }
        } catch (error) {
            console.error('MessageInput: onSendMessage failed:', error);
            setMessage(messageToSend);
            if (inputRef.current) inputRef.current.focus();
        }
    };

    const handleEmojiSelect = (emoji, event) => {
        setMessage(prev => prev + emoji.native);
        
        if (!keepPickerOpen) {
            setShowPopover(false);
        }
        
        if (inputRef.current) inputRef.current.focus();
    };

    const handleGifSelect = async (gif) => {
        try {
            if (gif.fileId) {
                const gifMessage = {
                    content: '🎬 GIF',
                    fileId: gif.fileId,
                    fileName: gif.fileName,
                    fileUrl: gif.fileUrl,
                    fileSize: gif.fileSize,
                    fileType: gif.fileType
                };
                
                await onSendMessage(gifMessage);
            } else {
                setMessage(prev => prev + `[GIF: ${gif.title}](${gif.url}) `);
            }
        } catch (error) {
            console.error('Failed to send GIF:', error);
        }
        
        setShowPopover(false);
        if (inputRef.current) inputRef.current.focus();
    };

    const handleAudioButtonClick = () => {
        setIsRecordingMode(true);
        setShowPopover(false);
    };

    const handleFileSelect = async (files, fileType) => {
        const fileArray = Array.isArray(files) ? files : [files];
        
        setSelectedAttachments(fileArray);
        setShowAttachmentPreview(true);
        setShowPopover(false);
    };

    const handleSendAttachments = async (attachments, caption) => {
        try {
            const authToken = localStorage.getItem('authToken');
            if (!authToken) {
                throw new Error('No authentication token found. Please log in again.');
            }
            
            if (!currentUser || !currentUser.id) {
                throw new Error('User information not available. Please log in again.');
            }

            for (const file of attachments) {
                const formData = new FormData();
                formData.append('file', file);
                formData.append('userId', currentUser.id);
                
                const response = await fetch(`${apiClient.baseURL}/files/upload`, {
                    method: 'POST',
                    body: formData,
                    headers: {
                        'Authorization': `Bearer ${authToken}`
                    }
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('Upload failed with status:', response.status, 'Response:', errorText);
                    throw new Error(`File upload failed: ${response.status} - ${errorText}`);
                }

                const uploadResult = await response.json();

                const fileMessage = {
                    content: caption || `📎 ${file.name}`,
                    fileId: uploadResult.id,
                    fileName: uploadResult.fileName,
                    fileUrl: uploadResult.fileUrl,    
                    fileSize: uploadResult.fileSize,
                    fileType: uploadResult.fileType
                };

                await onSendMessage(fileMessage);
            }

            setShowAttachmentPreview(false);
            setSelectedAttachments([]);

        } catch (error) {
            console.error('Failed to upload file:', error);
            throw error;
        }
    };

    const handleCloseAttachmentPreview = () => {
        setShowAttachmentPreview(false);
        setSelectedAttachments([]);
    };

    const handleAddMoreAttachments = () => {
        const currentType = selectedAttachments.length > 0 ? selectedAttachments[0].type : '';
        let fileType = 'document';
        
        if (currentType.startsWith('image/')) {
            fileType = 'image';
        } else if (currentType.startsWith('video/')) {
            fileType = 'video';
        } else if (currentType.startsWith('audio/')) {
            fileType = 'audio';
        }
        
        const input = document.createElement('input');
        input.type = 'file';
        input.style.display = 'none';
        input.multiple = true;
        
        switch(fileType) {
            case 'image':
                input.accept = 'image/*';
                break;
            case 'video':
                input.accept = 'video/*';
                break;
            case 'audio':
                input.accept = 'audio/*';
                break;
            case 'document':
                input.accept = '.pdf,.doc,.docx,.txt,.rtf,.odt,.xls,.xlsx,.ppt,.pptx';
                break;
            default:
                input.accept = '*/*';
        }

        input.onchange = (e) => {
            const newFiles = Array.from(e.target.files);
            if (newFiles.length > 0) {
                const oversizedFiles = newFiles.filter(file => file.size > 50 * 1024 * 1024);
                if (oversizedFiles.length > 0) {
                    alert('File size must be less than 50MB');
                    return;
                }
                
                const allFiles = [...selectedAttachments, ...newFiles];
                const totalSize = allFiles.reduce((sum, file) => sum + file.size, 0);
                if (totalSize > 50 * 1024 * 1024) {
                    alert('Total file size must be less than 50MB');
                    return;
                }
                
                setSelectedAttachments(allFiles);
            }
            document.body.removeChild(input);
        };
        
        document.body.appendChild(input);
        input.click();
    };

    const handleAudioRecorded = async (audioData) => {
        try {
            const authToken = localStorage.getItem('authToken');
            if (!authToken) {
                throw new Error('No authentication token found. Please log in again.');
            }
            
            if (!currentUser || !currentUser.id) {
                throw new Error('User information not available. Please log in again.');
            }

            const audioFile = new File([audioData.blob], `audio_${Date.now()}.webm`, {
                type: audioData.type || 'audio/webm'
            });

            if (!audioData.blob || audioData.blob.size === 0) {
                throw new Error('Invalid audio data: empty or missing blob');
            }

            if (audioData.blob.size > 50 * 1024 * 1024) {
                throw new Error('Audio file too large (max 50MB)');
            }

            const formData = new FormData();
            formData.append('file', audioFile);
            formData.append('userId', currentUser.id);
            
            const response = await fetch(`${apiClient.baseURL}/files/upload`, {
                method: 'POST',
                body: formData,
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Upload failed with status:', response.status, 'Response:', errorText);
                throw new Error(`File upload failed: ${response.status} - ${errorText}`);
            }

            const uploadResult = await response.json();

            const audioMessage = {
                content: '🎤 Voice message',
                fileId: uploadResult.id,
                fileName: uploadResult.fileName,
                fileUrl: uploadResult.fileUrl,
                fileSize: uploadResult.fileSize,
                fileType: uploadResult.fileType
            };

            await onSendMessage(audioMessage);
            setIsRecordingMode(false);
        } catch (error) {
            console.error('Failed to send audio message:', error);
            alert(`Failed to send audio message: ${error.message}`);
            setIsRecordingMode(false);
        }
    };

    const handleAudioCancel = () => {
        setIsRecordingMode(false);
    };

    const handleSendClick = (e) => {
        e.preventDefault();
        if (isRecordingMode) {
            if (audioRecorderRef.current) {
                audioRecorderRef.current.handleSend();
            }
        } else {
            handleSubmit(e);
        }
    };

    const shouldShowCounter = message.length >= CHARACTER_LIMIT * WARNING_THRESHOLD;
    const getCounterColor = () => {
        const ratio = message.length / CHARACTER_LIMIT;
        if (ratio >= CRITICAL_THRESHOLD) return '#dc3545';
        if (ratio >= DANGER_THRESHOLD) return '#ffc107';
        return '#6c757d';
    };

    return (
        <div className="message-input-container" style={{ position: 'relative' }}>
            
            {replyingToMessage && (
                <div className="reply-preview">
                    <div className="reply-preview-content">
                        <div className="reply-preview-header">
                            <span className="reply-preview-label">Replying to {replyingToMessage.senderName}</span>
                            <button 
                                type="button" 
                                className="reply-cancel-button"
                                onClick={onCancelReply}
                                title="Cancel reply"
                            >
                                ×
                            </button>
                        </div>
                        <div className="reply-preview-text">
                            {replyingToMessage.content || 'Media message'}
                        </div>
                    </div>
                </div>
            )}
            
            {editingMessage && (
                <div className="edit-preview">
                    <div className="edit-preview-content">
                        <div className="edit-preview-header">
                            <span className="edit-preview-label">Editing message</span>
                            <button 
                                type="button" 
                                className="edit-cancel-button"
                                onClick={onCancelEdit}
                                title="Cancel edit"
                            >
                                ×
                            </button>
                        </div>
                        <div className="edit-preview-text">
                            {editingMessage.content}
                        </div>
                    </div>
                </div>
            )}
            
            <div className="message-input-wrapper">
                {isRecordingMode ? (
                    <AudioRecorder 
                        ref={audioRecorderRef}
                        onAudioRecorded={handleAudioRecorded}
                        onCancel={handleAudioCancel}
                    />
                ) : (
                    <>
                        <div className="input-with-icons">
                            <div className="input-icons-left">
                                <motion.button 
                                    type="button" 
                                    className={`icon-button emoji-button ${keepPickerOpen ? 'keep-open-mode' : ''}`}
                                    disabled={disabled || !chatAvailable}
                                    onClick={() => setShowPopover(v => !v)}
                                    onContextMenu={(e) => {
                                        e.preventDefault();
                                        setKeepPickerOpen(prev => !prev);
                                    }}
                                    whileHover={{ scale: 1.1 }}
                                    whileTap={{ scale: 0.9 }}
                                    title={keepPickerOpen ? "Multi-select mode ON (right-click or Ctrl+Shift+E to toggle)" : "Right-click or Ctrl+Shift+E for multi-select mode"}
                                >
                                    <img src="/emoji-icon.svg" alt="Emoji" className="input-icon" />
                                </motion.button>
                                <AttachmentMenu onFileSelect={handleFileSelect} />
                            </div>

                            {showPopover && (
                                <div ref={popoverRef}>
                                    <EmojiGifPopover activeTab={activeTab} setActiveTab={setActiveTab}>
                                        {activeTab === 'emoji' && (
                                            <Picker
                                                data={data}
                                                onEmojiSelect={handleEmojiSelect}
                                                theme="light"
                                                style={{ border: 'none', boxShadow: 'none', background: 'transparent' }}
                                                color="#8C433D"
                                                showPreview={false}
                                                showSkinTones={false}
                                                native={true}
                                            />
                                        )}
                                        {activeTab === 'gif' && (
                                            <GifPicker onGifSelect={handleGifSelect} currentUser={currentUser} />
                                        )}
                                    </EmojiGifPopover>
                                </div>
                            )}

                            <textarea
                                value={message}
                                onChange={handleChange}
                                onBlur={handleBlur}
                                onKeyDown={handleKeyDown}
                                placeholder={loading ? "Sending..." : chatAvailable ? "Type a message..." : "Select a chat to send messages"}
                                disabled={disabled || loading || !chatAvailable}
                                className="message-input"
                                ref={inputRef}
                                autoFocus={autoFocus}
                                rows={1}
                                style={{ resize: 'none', overflow: 'hidden' }}
                            />
                            <div className="input-icons-right">
                                <motion.button 
                                    type="button" 
                                    className="icon-button audio-button"
                                    disabled={disabled || !chatAvailable}
                                    onClick={handleAudioButtonClick}
                                    whileHover={{ scale: 1.1 }}
                                    whileTap={{ scale: 0.9 }}
                                >
                                    <img src="/audio-icon.svg" alt="Voice" className="input-icon" />
                                </motion.button>
                            </div>
                        </div>

                        {shouldShowCounter && (
                            <div 
                                className="character-counter"
                                style={{ 
                                    color: getCounterColor(),
                                    fontSize: '12px',
                                    textAlign: 'right',
                                    marginTop: '4px',
                                    marginRight: '8px',
                                    fontFamily: 'Regular, sans-serif'
                                }}
                            >
                                {message.length}/{CHARACTER_LIMIT}
                            </div>
                        )}
                    </>
                )}

                <motion.button 
                    type="button" 
                    disabled={disabled || loading || (!isRecordingMode && !message.trim()) || !chatAvailable || message.length > CHARACTER_LIMIT}
                    className="send-button-separate"
                    onClick={handleSendClick}
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.95 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 20 }}
                >
                    <img src="/send-icon.png" alt="Send" className="send-icon" />
                </motion.button>
            </div>

            {showAttachmentPreview && (
                <AttachmentPreview
                    attachments={selectedAttachments}
                    onSend={handleSendAttachments}
                    onClose={handleCloseAttachmentPreview}
                    onAddMore={handleAddMoreAttachments}
                />
            )}
        </div>
    );
}

export default MessageInput;