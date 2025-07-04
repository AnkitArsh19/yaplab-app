import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useWavesurfer } from '@wavesurfer/react';
import apiClient from '../../utils/apiClient';
import audioManager from '../../utils/audioManager';
import '../../styles/AudioPlayer.css';

function AudioPlayer({ audioUrl, fileName, fileSize, onLoad }) {
    const containerRef = useRef(null);
    const [hasError, setHasError] = useState(false);
    const [authenticatedAudioUrl, setAuthenticatedAudioUrl] = useState(null);
    const audioPlayerInterfaceRef = useRef(null);
    const [duration, setDuration] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [wavesurferReady, setWavesurferReady] = useState(false);
    
    const { wavesurfer, isPlaying, currentTime } = useWavesurfer({
        container: containerRef,
        height: 32,
        waveColor: '#F5DEB3',
        progressColor: '#DEB887',
        cursorColor: '#CD853F',
        barWidth: 2,
        barRadius: 1,
        barGap: 2,
        barHeight: 2,
        responsive: true,
    });

    useEffect(() => {
        if (wavesurfer && wavesurferReady && !audioPlayerInterfaceRef.current) {
            const playerId = `audio-${Date.now()}-${Math.random()}`;
            
            audioPlayerInterfaceRef.current = {
                id: playerId,
                pause: () => {
                    if (wavesurfer && !wavesurfer.isDestroyed) {
                        wavesurfer.pause();
                    }
                },
                isPlaying: () => {
                    return isPlaying;
                },
                play: () => {
                    if (wavesurfer && !wavesurfer.isDestroyed) {
                        wavesurfer.play();
                    }
                }
            };
            
            audioManager.register(audioPlayerInterfaceRef.current);
        }
        
        return () => {
            if (audioPlayerInterfaceRef.current) {
                audioManager.unregister(audioPlayerInterfaceRef.current);
                audioPlayerInterfaceRef.current = null;
            }
        };
    }, [wavesurfer, wavesurferReady]);

    useEffect(() => {
        if (wavesurfer) {
            const handleReady = () => {
                setDuration(wavesurfer.getDuration());
                setIsLoading(false);
                setWavesurferReady(true);
                if (onLoad) onLoad();
            };

            const handleError = (error) => {
                console.error('Wavesurfer error:', error);
                setHasError(true);
                setIsLoading(false);
            };

            wavesurfer.on('ready', handleReady);
            wavesurfer.on('error', handleError);

            return () => {
                wavesurfer.un('ready', handleReady);
                wavesurfer.un('error', handleError);
            };
        }
    }, [wavesurfer, onLoad]);

    useEffect(() => {
        const loadAuthenticatedAudio = async () => {
            if (!audioUrl || !wavesurfer) return;
            
            setIsLoading(true);
            setHasError(false);
            setWavesurferReady(false);
            
            if (authenticatedAudioUrl) {
                URL.revokeObjectURL(authenticatedAudioUrl);
                setAuthenticatedAudioUrl(null);
            }
            
            try {
                const response = await apiClient.request(audioUrl, {
                    method: 'GET',
                    headers: {},
                });
                
                if (response.ok) {
                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);
                    setAuthenticatedAudioUrl(url);
                    
                    try {
                        await wavesurfer.load(url);
                    } catch (loadError) {
                        setHasError(true);
                        setIsLoading(false);
                        URL.revokeObjectURL(url);
                    }
                } else {
                    setHasError(true);
                    setIsLoading(false);
                }
            } catch (error) {
                setHasError(true);
                setIsLoading(false);
            }
        };

        loadAuthenticatedAudio();
    }, [audioUrl, wavesurfer]);

    useEffect(() => {
        return () => {
            if (authenticatedAudioUrl) {
                URL.revokeObjectURL(authenticatedAudioUrl);
            }
        };
    }, [authenticatedAudioUrl]);

    useEffect(() => {
        if (wavesurfer && audioPlayerInterfaceRef.current && wavesurferReady) {
            if (isPlaying) {
                audioManager.setCurrentAudio(audioPlayerInterfaceRef.current);
            } else {
                if (audioManager.getCurrentAudio() === audioPlayerInterfaceRef.current) {
                    audioManager.setCurrentAudio(null);
                }
            }
        }
    }, [isPlaying, wavesurfer, wavesurferReady]);

    const onPlayPause = useCallback(() => {
        if (hasError || !wavesurfer || !wavesurferReady) return;
        
        if (isPlaying) {
            wavesurfer.pause();
        } else {
            audioManager.pauseAll();
            wavesurfer.play();
        }
    }, [hasError, wavesurfer, isPlaying, wavesurferReady]);

    const formatTime = (seconds) => {
        if (!seconds || isNaN(seconds) || !isFinite(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const formatTimeDisplay = () => {
        const current = formatTime(currentTime);
        
        if (isLoading) {
            return `${current} / --:--`;
        }
        
        if (duration > 0) {
            return `${current} / ${formatTime(duration)}`;
        }
        
        return `${current} / --:--`;
    };

    if (hasError) {
        return (
            <div className="audio-player error">
                <div className="audio-icon">
                    <img src="/audio-icon.svg" alt="Audio" />
                </div>
                <div className="audio-info">
                    <div className="error-text">Failed to load audio</div>
                </div>
            </div>
        );
    }

    return (
        <div className="audio-player">
            <button 
                className={`play-pause-btn ${isLoading ? 'loading' : ''}`}
                onClick={onPlayPause}
                disabled={isLoading || !wavesurferReady}
                aria-label={isPlaying ? 'Pause' : 'Play'}
            >
                {isLoading ? (
                    <div className="loading-spinner"></div>
                ) : isPlaying ? (
                    <img src="/pause-icon-light.svg" alt="Pause" width="16" height="16" />
                ) : (
                    <img src="/play-icon-light.svg" alt="Play" width="16" height="16" />
                )}
            </button>

            <div className="audio-content">
                <div className="audio-info">
                    <div className="time-display">
                        {isLoading ? 'Loading...' : formatTimeDisplay()}
                    </div>
                </div>
                <div className="waveform-container">
                    <div ref={containerRef} />
                </div>
            </div>
        </div>
    );
}

export default AudioPlayer;
