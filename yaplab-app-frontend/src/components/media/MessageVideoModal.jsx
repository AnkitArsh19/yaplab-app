import React from 'react';
import { motion } from 'framer-motion';
import VideoPlayer from './VideoPlayer';
import '../../styles/VideoPlayer.css';

function VideoModal({ videoUrl, onClose, isOpen }) {
    if (!isOpen) return null;

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    return (
        <motion.div 
            className="video-modal-overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={handleOverlayClick}
        >
            <motion.div 
                className="video-modal-content"
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.8, opacity: 0 }}
                transition={{ type: 'spring', damping: 20, stiffness: 300 }}
            >
                <VideoPlayer 
                    videoUrl={videoUrl}
                    onClose={onClose}
                    showCloseButton={true}
                    showFullscreenButton={true}
                    autoPlay={true}
                    className="modal-video-player"
                />
            </motion.div>
        </motion.div>
    );
}

export default VideoModal;
