import React, { useState, useRef, useEffect } from 'react';
import '../../styles/VideoModal.css';

function VideoModal({ videoUrl, fileType, isOpen, onClose }) {
    const videoRef = useRef(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [isMuted, setIsMuted] = useState(false);
    const [isFullscreen, setIsFullscreen] = useState(false);

    useEffect(() => {
        if (isOpen && videoRef.current) {
            videoRef.current.currentTime = 0;
            setCurrentTime(0);
            setIsPlaying(false);
        }
    }, [isOpen]);

    useEffect(() => {
        const handleEscape = (e) => {
            if (e.key === 'Escape') {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
            document.body.style.overflow = '';
        };
    }, [isOpen, onClose]);

    const handlePlayPause = () => {
        if (videoRef.current) {
            if (isPlaying) {
                videoRef.current.pause();
            } else {
                videoRef.current.play();
            }
            setIsPlaying(!isPlaying);
        }
    };

    const handleTimeUpdate = () => {
        if (videoRef.current) {
            setCurrentTime(videoRef.current.currentTime);
        }
    };

    const handleLoadedMetadata = () => {
        if (videoRef.current) {
            setDuration(videoRef.current.duration);
        }
    };

    const handleProgressClick = (e) => {
        if (videoRef.current) {
            const rect = e.currentTarget.getBoundingClientRect();
            const pos = (e.clientX - rect.left) / rect.width;
            videoRef.current.currentTime = pos * duration;
        }
    };

    const handleMuteToggle = () => {
        if (videoRef.current) {
            videoRef.current.muted = !isMuted;
            setIsMuted(!isMuted);
        }
    };

    const handleFullscreen = () => {
        if (videoRef.current) {
            if (!isFullscreen) {
                if (videoRef.current.requestFullscreen) {
                    videoRef.current.requestFullscreen();
                } else if (videoRef.current.webkitRequestFullscreen) {
                    videoRef.current.webkitRequestFullscreen();
                } else if (videoRef.current.msRequestFullscreen) {
                    videoRef.current.msRequestFullscreen();
                }
            } else {
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else if (document.webkitExitFullscreen) {
                    document.webkitExitFullscreen();
                } else if (document.msExitFullscreen) {
                    document.msExitFullscreen();
                }
            }
        }
    };

    const formatTime = (time) => {
        const minutes = Math.floor(time / 60);
        const seconds = Math.floor(time % 60);
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    };

    if (!isOpen) return null;

    return (
        <div className="video-modal-overlay" onClick={onClose}>
            <div className="video-modal-container" onClick={(e) => e.stopPropagation()}>
                <button className="video-modal-close" onClick={onClose}>
                    ✕
                </button>
                
                <div className="video-modal-content">
                    <div className="custom-video-container">
                        <video
                            ref={videoRef}
                            className="custom-video-player"
                            onClick={handlePlayPause}
                            onTimeUpdate={handleTimeUpdate}
                            onLoadedMetadata={handleLoadedMetadata}
                            onPlay={() => setIsPlaying(true)}
                            onPause={() => setIsPlaying(false)}
                            onEnded={() => setIsPlaying(false)}
                            muted={isMuted}
                            playsInline
                        >
                            <source src={videoUrl} type={fileType} />
                            Your browser does not support the video tag.
                        </video>

                        {/* Custom play button overlay */}
                        {!isPlaying && (
                            <div className="custom-play-overlay" onClick={handlePlayPause}>
                                <div className="custom-play-button">
                                    <img src="/play-icon-light.svg" alt="Play" />
                                </div>
                            </div>
                        )}

                        {/* Custom controls */}
                        <div className="custom-video-controls">
                            <button className="control-btn play-pause-btn" onClick={handlePlayPause}>
                                <img 
                                    src={isPlaying ? "/pause-icon-light.svg" : "/play-icon-light.svg"} 
                                    alt={isPlaying ? "Pause" : "Play"}
                                />
                            </button>

                            <div className="time-display">
                                {formatTime(currentTime)} / {formatTime(duration)}
                            </div>

                            <div className="progress-container" onClick={handleProgressClick}>
                                <div className="progress-bar">
                                    <div 
                                        className="progress-fill"
                                        style={{ width: `${duration ? (currentTime / duration) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <button className="control-btn mute-btn" onClick={handleMuteToggle}>
                                <img 
                                    src={isMuted ? "/audio-icon.svg" : "/headphones-solid.svg"} 
                                    alt={isMuted ? "Unmute" : "Mute"}
                                />
                            </button>

                            <button className="control-btn fullscreen-btn" onClick={handleFullscreen}>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="white">
                                    <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default VideoModal;
