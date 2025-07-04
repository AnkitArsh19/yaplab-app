import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import data from '@emoji-mart/data';
import Picker from '@emoji-mart/react';
import { useWavesurfer } from '@wavesurfer/react';
import VideoPlayer from './VideoPlayer';
import '../../styles/AttachmentPreview.css';

function AttachmentPreview({ attachments = [], onSend, onClose, onAddMore }) {
    const [currentIndex, setCurrentIndex] = useState(0);
    const [caption, setCaption] = useState('');
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState(null);
    const emojiPickerRef = useRef(null);
    const [objectUrls, setObjectUrls] = useState({});
    const currentAttachment = attachments[currentIndex];

    useEffect(() => {
        const newUrls = {};
        attachments.forEach((attachment, index) => {
            if (!objectUrls[index]) {
                newUrls[index] = URL.createObjectURL(attachment);
            }
        });
        
        if (Object.keys(newUrls).length > 0) {
            setObjectUrls(prev => ({ ...prev, ...newUrls }));
        }
        
        return () => {
            Object.values(newUrls).forEach(url => {
                URL.revokeObjectURL(url);
            });
        };
    }, [attachments.length]);
    
    useEffect(() => {
        return () => {
            Object.values(objectUrls).forEach(url => {
                URL.revokeObjectURL(url);
            });
        };
    }, []);
    
    const waveformRef = useRef(null);
    const { wavesurfer, isPlaying } = useWavesurfer({
        container: waveformRef,
        height: 32,
        waveColor: 'rgb(141, 67, 61)',
        progressColor: 'rgb(205, 166, 132)',
        cursorColor: '#8C433D',
        barWidth: 3,
        barRadius: 2,
        barGap: 1,
        barHeight: 1,
        normalize: true,
        url: currentAttachment?.type?.startsWith('audio/') && objectUrls[currentIndex]
             ? objectUrls[currentIndex] : null
    });
    
    const handleEmojiSelect = (emoji) => {
        setCaption(prev => prev + emoji.native);
        setShowEmojiPicker(false);
    };
    
    const handleSend = async () => {
        if (isUploading) return;
        
        setIsUploading(true);
        setUploadError(null);
        setShowEmojiPicker(false);
        
        try {
            await onSend(attachments, caption.trim());
            
        } catch (error) {
            let errorMessage = 'Failed to upload files';
            if (error.message.includes('PAYLOAD_TOO_LARGE') || error.message.includes('413')) {
                errorMessage = 'File too large. Please use files smaller than 50MB.';
            } else if (error.message.includes('fetch')) {
                errorMessage = 'Network error. Please check your connection and try again.';
            } else if (error.message.includes('size exceeds')) {
                errorMessage = 'File size exceeds limit. Please use smaller files.';
            } else if (error.message) {
                errorMessage = error.message;
            }
            
            setUploadError(errorMessage);
            setIsUploading(false);
        }
    };
    
    const handlePlayPause = () => {
        if (wavesurfer) {
            wavesurfer.playPause();
        }
    };

    const renderPreview = () => {
        if (!currentAttachment || !objectUrls[currentIndex]) return null;

        const { type, name, size } = currentAttachment;
        const fileUrl = objectUrls[currentIndex];

        if (type.startsWith('image/')) {
            return (
                <div className="preview-content image-preview">
                    <img 
                        src={fileUrl} 
                        alt={name}
                        className="preview-image"
                    />
                </div>
            );
        }
        
        if (type.startsWith('video/')) {
            return (
                <div className="preview-content video-preview">
                    <VideoPlayer 
                        videoUrl={fileUrl}
                        showCloseButton={false}
                        showFullscreenButton={true}
                        autoPlay={false}
                        className="attachment-preview-video"
                    />
                </div>
            );
        }

        if (type.startsWith('audio/')) {
            return (
                <div className="preview-content audio-preview">
                    <div className="audio-container">
                        <div className="audio-icon">
                            <img src="/audio-icon.svg" alt="Audio" />
                        </div>
                        <div className="audio-info">
                            <div className="audio-name">{name}</div>
                            <div className="audio-size">{formatFileSize(size)}</div>
                        </div>
                    </div>
                    <div className="audio-controls">
                        <button className="play-pause-btn" onClick={handlePlayPause}>
                            <img 
                                src={isPlaying ? "/pause-icon.svg" : "/play-icon.svg"} 
                                alt={isPlaying ? "Pause" : "Play"}
                            />
                        </button>
                        <div className="waveform-container" ref={waveformRef} />
                    </div>
                </div>
            );
        }

        return (
            <div className="preview-content document-preview">
                <div className="document-container">
                    <div className="document-icon">
                        <img src="/file-solid.svg" alt="Document" />
                    </div>
                    <div className="document-info">
                        <div className="document-name">{name}</div>
                        <div className="document-size">{formatFileSize(size)}</div>
                        <div className="document-type">{type}</div>
                    </div>
                </div>
            </div>
        );
    };

    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const renderAttachmentThumbnail = (attachment, index) => {
        const { type, name } = attachment;
        const fileUrl = objectUrls[index];
        
        if (!fileUrl) return null;
        
        if (type.startsWith('image/')) {
            return (
                <img 
                    src={fileUrl} 
                    alt={name}
                    className="thumbnail-image"
                />
            );
        }
        
        if (type.startsWith('video/')) {
            return (
                <div className="thumbnail-video">
                    <video 
                        src={fileUrl}
                        className="thumbnail-video-element"
                        muted
                        preload="metadata"
                    />
                    <div className="thumbnail-video-overlay">
                        <img src="/play-icon-light.svg" alt="Video" />
                    </div>
                </div>
            );
        }
        
        if (type.startsWith('audio/')) {
            return (
                <div className="thumbnail-audio">
                    <img src="/audio-icon.svg" alt="Audio" />
                </div>
            );
        }
        
        return (
            <div className="thumbnail-document">
                <img src="/file-solid.svg" alt="Document" />
            </div>
        );
    };

    if (!attachments.length) return null;

    return (
        <div className="attachment-preview-overlay">
            <div className="attachment-preview-container">
                <div className="preview-header">
                    <button 
                        className={`close-btn ${isUploading ? 'disabled' : ''}`} 
                        onClick={isUploading ? undefined : onClose}
                        disabled={isUploading}
                    >
                        <img src="/cross-icon.png" alt="Close" />
                    </button>
                    <div className="attachment-counter">
                        {isUploading ? 'Uploading...' : `${currentIndex + 1} of ${attachments.length}`}
                    </div>
                </div>

                <div className="preview-main">
                    {renderPreview()}
                </div>

                <div className="attachments-bar">
                    <div className="thumbnails-container">
                        {attachments.map((attachment, index) => (
                            <div 
                                key={index}
                                className={`thumbnail ${index === currentIndex ? 'active' : ''} ${isUploading ? 'disabled' : ''}`}
                                onClick={isUploading ? undefined : () => setCurrentIndex(index)}
                            >
                                {renderAttachmentThumbnail(attachment, index)}
                            </div>
                        ))}
                    </div>

                    <button 
                        className={`add-more-btn ${isUploading ? 'disabled' : ''}`} 
                        onClick={isUploading ? undefined : onAddMore}
                        disabled={isUploading}
                    >
                        <img src="/plus-solid.svg" alt="Add more" />
                    </button>
                </div>

                <div className="preview-footer">
                    <div className="caption-container">
                        <div className="caption-input-container">
                            <button 
                                className={`emoji-btn ${isUploading ? 'disabled' : ''}`}
                                onClick={isUploading ? undefined : () => setShowEmojiPicker(!showEmojiPicker)}
                                disabled={isUploading}
                            >
                                <img src="/emoji-icon.svg" alt="Emoji" />
                            </button>
                            
                            <input
                                type="text"
                                placeholder="Caption (optional)"
                                value={caption}
                                onChange={(e) => setCaption(e.target.value)}
                                className="caption-input"
                                disabled={isUploading}
                            />
                        </div>

                        {showEmojiPicker && (
                            <div className="emoji-picker-container" ref={emojiPickerRef}>
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
                            </div>
                        )}
                    </div>

                    {uploadError && (
                        <div className="error-message">
                            <p>{uploadError}</p>
                            <button onClick={() => setUploadError(null)} className="dismiss-error">
                                ✕
                            </button>
                        </div>
                    )}

                    <motion.button 
                        className={`send-btn ${isUploading ? 'uploading' : ''}`}
                        onClick={handleSend}
                        disabled={isUploading}
                        initial={{ scale: 1, backgroundColor: "#8C433D" }}
                        whileHover={!isUploading ? { scale: 1.05, backgroundColor: "#A0574D" } : {}}
                        whileTap={!isUploading ? { scale: 0.95 } : {}}
                        transition={{ type: 'spring', stiffness: 400, damping: 20 }}
                    >
                        {isUploading ? (
                            <div className="loading-spinner"></div>
                        ) : (
                            <img src="/send-icon.png" alt="Send" />
                        )}
                    </motion.button>
                </div>
            </div>
        </div>
    );
}

export default AttachmentPreview;
