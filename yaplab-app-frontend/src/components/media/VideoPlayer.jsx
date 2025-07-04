import React, { useState, useRef, useEffect } from 'react';
import '../../styles/VideoPlayer.css';

function VideoPlayer({ 
    videoUrl, 
    onClose, 
    showCloseButton = true, 
    showFullscreenButton = true,
    autoPlay = false,
    className = ""
}) {
    const videoRef = useRef(null);
    const [isVideoPlaying, setIsVideoPlaying] = useState(false);
    const [currentVideoTime, setCurrentVideoTime] = useState(0);
    const [videoDuration, setVideoDuration] = useState(0);
    const [isVideoMuted, setIsVideoMuted] = useState(true);
    const [isFullscreen, setIsFullscreen] = useState(false);

    useEffect(() => {
        if (autoPlay && videoRef.current) {
            handleVideoPlayPause();
        }
    }, [autoPlay]);

    // Video controls
    const handleVideoLoad = () => {
        if (videoRef.current) {
            setVideoDuration(videoRef.current.duration);
            setCurrentVideoTime(0);
            setIsVideoPlaying(!videoRef.current.paused);
        }
    };

    const handleVideoClick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        handleVideoPlayPause();
    };

    const handleVideoPlayPause = async () => {
        if (videoRef.current) {
            try {
                if (isVideoPlaying) {
                    await videoRef.current.pause();
                    setIsVideoPlaying(false);
                } else {
                    await videoRef.current.play();
                    setIsVideoPlaying(true);
                }
            } catch (error) {
            }
        }
    };

    const handleVideoTimeUpdate = () => {
        if (videoRef.current) {
            setCurrentVideoTime(videoRef.current.currentTime);
        }
    };

    const handleVideoEnded = () => {
        setIsVideoPlaying(false);
        setCurrentVideoTime(0);
        if (videoRef.current) {
            videoRef.current.currentTime = 0;
        }
    };

    const handleVideoMute = () => {
        if (videoRef.current) {
            videoRef.current.muted = !videoRef.current.muted;
            setIsVideoMuted(videoRef.current.muted);
        }
    };

    const handleProgressClick = (e) => {
        if (videoRef.current) {
            const rect = e.currentTarget.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const newTime = (clickX / rect.width) * videoDuration;
            videoRef.current.currentTime = newTime;
            setCurrentVideoTime(newTime);
        }
    };

    const handleFullscreen = () => {
        if (videoRef.current) {
            if (!isFullscreen) {
                if (videoRef.current.requestFullscreen) {
                    videoRef.current.requestFullscreen();
                } else if (videoRef.current.webkitRequestFullscreen) {
                    videoRef.current.webkitRequestFullscreen();
                } else if (videoRef.current.mozRequestFullScreen) {
                    videoRef.current.mozRequestFullScreen();
                }
                setIsFullscreen(true);
            } else {
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else if (document.webkitExitFullscreen) {
                    document.webkitExitFullscreen();
                } else if (document.mozCancelFullScreen) {
                    document.mozCancelFullScreen();
                }
                setIsFullscreen(false);
            }
        }
    };

    // Listen for fullscreen changes
    useEffect(() => {
        const handleFullscreenChange = () => {
            setIsFullscreen(!!document.fullscreenElement);
        };

        document.addEventListener('fullscreenchange', handleFullscreenChange);
        document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
        document.addEventListener('mozfullscreenchange', handleFullscreenChange);

        return () => {
            document.removeEventListener('fullscreenchange', handleFullscreenChange);
            document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
            document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
        };
    }, []);

    const formatVideoTime = (seconds) => {
        if (!seconds || isNaN(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    return (
        <div className={`video-player-container ${className}`}>
            {showCloseButton && onClose && (
                <button className="video-close-btn" onClick={onClose}>
                    <img src="/cross-icon.png" alt="Close" />
                </button>
            )}
            
            <div className="video-wrapper">
                <video 
                    ref={videoRef}
                    className="custom-video-player"
                    onClick={handleVideoClick}
                    onLoadedMetadata={handleVideoLoad}
                    onTimeUpdate={handleVideoTimeUpdate}
                    onEnded={handleVideoEnded}
                    onPlay={() => setIsVideoPlaying(true)}
                    onPause={() => setIsVideoPlaying(false)}
                    onError={() => {}}
                    muted={isVideoMuted}
                    playsInline
                    preload="metadata"
                    controls={false}
                    style={{ objectFit: 'contain' }}
                >
                    <source src={videoUrl} type="video/mp4" />
                    Your browser does not support the video tag.
                </video>
                
                {/* Custom video overlay controls */}
                <div className={`video-overlay ${!isVideoPlaying ? 'show' : ''}`}>
                    <button 
                        className="video-play-btn" 
                        onClick={handleVideoPlayPause}
                    >
                        <img src="/play-icon-light.svg" alt="Play" />
                    </button>
                </div>
                
                {/* Video progress bar - always visible */}
                <div className="video-progress-container">
                    <div className="video-time">
                        {formatVideoTime(currentVideoTime)} / {formatVideoTime(videoDuration)}
                    </div>
                    <div className="video-progress-bar" onClick={handleProgressClick}>
                        <div 
                            className="video-progress-fill"
                            style={{ width: `${videoDuration ? (currentVideoTime / videoDuration) * 100 : 0}%` }}
                        />
                    </div>
                    <div className="video-controls-right">
                        <button 
                            className="video-play-pause-btn" 
                            onClick={handleVideoPlayPause}
                        >
                            <img 
                                src={isVideoPlaying ? "/pause-icon-light.svg" : "/play-icon-light.svg"} 
                                alt={isVideoPlaying ? "Pause" : "Play"} 
                            />
                        </button>
                        <button 
                            className="video-sound-btn" 
                            onClick={handleVideoMute}
                        >
                            <img 
                                src={isVideoMuted ? "/volume-mute-icon.svg" : "/volume-icon.png"} 
                                alt={isVideoMuted ? "Unmute" : "Mute"} 
                            />
                        </button>
                        {showFullscreenButton && (
                            <button 
                                className="video-fullscreen-btn" 
                                onClick={handleFullscreen}
                            >
                                <img src="/expand-solid.svg" alt="Fullscreen" />
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default VideoPlayer;
