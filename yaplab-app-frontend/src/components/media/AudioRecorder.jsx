import React, { useState, useRef, useEffect, useImperativeHandle, forwardRef } from 'react';
import WaveSurfer from 'wavesurfer.js';
import RecordPlugin from 'wavesurfer.js/dist/plugins/record.esm.js';
import '../../styles/AudioRecorder.css';

const AudioRecorder = forwardRef(({ onAudioRecorded, onCancel }, ref) => {
    const waveformRef = useRef(null);
    const wavesurferRef = useRef(null);
    const recordPluginRef = useRef(null);
    const pendingSend = useRef(false);

    const [recordingStatus, setRecordingStatus] = useState('recording');
    const [isPlaying, setIsPlaying] = useState(false);
    const [recordingTime, setRecordingTime] = useState(0);
    const [playbackTime, setPlaybackTime] = useState(0);
    const [audioBlob, setAudioBlob] = useState(null);

    useEffect(() => {
        if (waveformRef.current) {
            const ws = WaveSurfer.create({
                container: waveformRef.current,
                waveColor: '#8E8E8E',
                progressColor: '#E91E63',
                barWidth: 2,
                barGap: 1,
                barRadius: 2,
                height: 32,
                cursorWidth: 0,
                interact: false,
                hideScrollbar: true
            });
            wavesurferRef.current = ws;

            const record = ws.registerPlugin(RecordPlugin.create({ 
                scrollingWaveform: true,
                audioBitsPerSecond: 128000,
                renderRecordedAudio: false
            }));
            recordPluginRef.current = record;

            record.on('record-start', () => {
                setRecordingTime(0);
            });

            record.on('record-progress', (time) => {
                setRecordingTime(time / 1000);
            });

            record.on('record-end', (blob) => {
                setRecordingStatus('stopped');
                setAudioBlob(blob);
                
                if (pendingSend.current) {
                    pendingSend.current = false;
                    try {
                        onAudioRecorded({ 
                            blob, 
                            duration: recordingTime, 
                            type: blob.type || 'audio/webm' 
                        });
                    } catch (error) {
                    }
                    return;
                }
                
                const url = URL.createObjectURL(blob);
                loadAudio(url);
            });

            ws.on('play', () => setIsPlaying(true));
            ws.on('pause', () => setIsPlaying(false));
            ws.on('timeupdate', (time) => setPlaybackTime(time));
            ws.on('finish', () => {
                setIsPlaying(false);
                ws.seekTo(0);
                setPlaybackTime(0);
            });

            startRecording();
        }

        return () => {
            if (wavesurferRef.current) {
                try {
                    wavesurferRef.current.isDestroyed = true;
                    wavesurferRef.current.destroy();
                } catch (error) {
                }
            }
            pendingSend.current = false;
        };
    }, []);

    const loadAudio = async (url) => {
        try {
            if (wavesurferRef.current && !wavesurferRef.current.isDestroyed) {
                await wavesurferRef.current.load(url);
                if (wavesurferRef.current && !wavesurferRef.current.isDestroyed) {
                    wavesurferRef.current.setOptions({ interact: true });
                }
            }
        } catch (error) {
            URL.revokeObjectURL(url);
        }
    };

    useImperativeHandle(ref, () => ({
        handleSend: async () => {
            const record = recordPluginRef.current;
            if (!record) {
                console.warn('Record plugin not available');
                return;
            }

            try {
                if (record.isPaused()) {
                    if (record.getRecordedBlob && typeof record.getRecordedBlob === 'function') {
                        const blob = await record.getRecordedBlob();
                        onAudioRecorded({
                            blob,
                            duration: recordingTime,
                            type: blob.type || 'audio/webm'
                        });
                        record.stopRecording();
                        return;
                    } else {
                        pendingSend.current = true;
                        record.stopRecording();
                        return;
                    }
                }

                if (record.isRecording()) {
                    pendingSend.current = true;
                    await record.stopRecording();
                    return;
                }

                if (audioBlob) {
                    onAudioRecorded({
                        blob: audioBlob,
                        duration: recordingTime,
                        type: audioBlob.type || 'audio/webm'
                    });
                    return;
                }
            } catch (error) {
                pendingSend.current = false;
            }
        }
    }));

    const startRecording = async () => {
        try {
            await recordPluginRef.current.startRecording({ deviceId: 'default' });
        } catch (err) {
            onCancel();
        }
    };

    const handleStopRecording = () => {
        if (recordingStatus === 'recording' || recordingStatus === 'paused') {
            recordPluginRef.current.stopRecording();
        }
    };

    const handleToggleRecordingPause = () => {
        if (recordingStatus === 'recording') {
            recordPluginRef.current.pauseRecording();
            setRecordingStatus('paused');
        } else if (recordingStatus === 'paused') {
            recordPluginRef.current.resumeRecording();
            setRecordingStatus('recording');
        }
    };

    const handlePlayPause = () => {
        if (wavesurferRef.current && audioBlob) {
            wavesurferRef.current.playPause();
        }
    };

    const handleDelete = () => {
        onCancel();
    };

    const formatTime = (seconds) => {
        const roundedSeconds = Math.floor(seconds);
        const mins = Math.floor(roundedSeconds / 60);
        const secs = roundedSeconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    return (
        <div className="audio-recorder-container">
            <div className="playback-control-wrapper">
                {recordingStatus === 'stopped' ? (
                    <button className="record-btn play-btn" onClick={handlePlayPause} disabled={!audioBlob}>
                        <img src={isPlaying ? "/pause-icon.svg" : "/play-icon.svg"} alt="Play/Pause" />
                    </button>
                ) : (
                    <button className="record-btn" onClick={handleToggleRecordingPause}>
                        <img src={recordingStatus === 'paused' ? '/play-icon.svg' : '/pause-icon.svg'} alt="Pause/Resume Recording" />
                    </button>
                )}
            </div>

            {recordingStatus !== 'stopped' && (
                <button className="record-btn stop-btn" onClick={handleStopRecording}>
                    <img src="/stop-icon.svg" alt="Stop Recording" />
                </button>
            )}

            <div className="waveform-wrapper">
                <div ref={waveformRef} className="waveform-container" />
            </div>

            <div className="recording-time">{formatTime(recordingStatus === 'stopped' ? playbackTime : recordingTime)}</div>

            <button className="record-btn delete-btn" onClick={handleDelete}>
                <img src="/delete-icon.svg" alt="Cancel" />
            </button>
        </div>
    );
});

export default AudioRecorder;
