// Global audio manager to ensure only one audio plays at a time
class AudioManager {
    constructor() {
        this.currentAudio = null;
        this.listeners = new Set();
    }

    register(audioPlayer) {
        if (!audioPlayer || !audioPlayer.id) {
            return;
        }
        
        const existingPlayer = Array.from(this.listeners).find(p => p.id === audioPlayer.id);
        if (existingPlayer) {
            return;
        }
        
        this.listeners.add(audioPlayer);
    }

    unregister(audioPlayer) {
        if (!audioPlayer) return;
        
        const wasDeleted = this.listeners.delete(audioPlayer);
        if (this.currentAudio === audioPlayer) {
            this.currentAudio = null;
        }
    }

    setCurrentAudio(audioPlayer) {
        if (this.currentAudio && this.currentAudio !== audioPlayer) {
            try {
                if (typeof this.currentAudio.pause === 'function') {
                    this.currentAudio.pause();
                }
            } catch (error) {
                console.warn('AudioManager: Error pausing current audio:', error);
            }
        }
        this.currentAudio = audioPlayer;
    }

    getCurrentAudio() {
        return this.currentAudio;
    }

    pauseAll() {
        this.listeners.forEach(player => {
            try {
                if (player && typeof player.isPlaying === 'function' && player.isPlaying()) {
                    if (typeof player.pause === 'function') {
                        player.pause();
                    }
                }
            } catch (error) {
                console.warn('AudioManager: Error pausing player:', error);
            }
        });
        this.currentAudio = null;
    }

    isCurrentAudio(audioPlayer) {
        return this.currentAudio === audioPlayer;
    }

    getStatus() {
        return {
            totalRegistered: this.listeners.size,
            currentAudioId: this.currentAudio?.id || null,
            registeredIds: Array.from(this.listeners).map(p => p.id)
        };
    }
}

const audioManager = new AudioManager();
export default audioManager;
