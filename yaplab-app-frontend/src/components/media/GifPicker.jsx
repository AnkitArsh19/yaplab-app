import React, { useState, useEffect, useRef, useCallback } from 'react';
import '../../styles/GifPicker.css';
import apiClient from '../../utils/apiClient';

function GifPicker({ onGifSelect, currentUser }) {
    const [gifs, setGifs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [hasMore, setHasMore] = useState(true);
    const [nextPos, setNextPos] = useState(0);
    const [searchTimeout, setSearchTimeout] = useState(null);
    const gridRef = useRef(null);

    useEffect(() => {
        fetchTrendingGifs();
    }, []);

    const fetchTrendingGifs = async (currentPos = 0, appendResults = false) => {
        try {
            setLoading(true);
            setError(null);
            const response = await apiClient.get(`/api/gifs/trending?limit=20&pos=${currentPos}`);
            
            if (!response.ok) {
                throw new Error(`Failed to fetch trending GIFs: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            if (appendResults) {
                setGifs(prev => [...prev, ...(data.results || [])]);
            } else {
                setGifs(data.results || []);
            }
            setNextPos(data.next ? parseInt(data.next) : 0);
            setHasMore(!!data.next);
        } catch (err) {
            setError(err.message);
            console.error('Error fetching trending GIFs:', err);
        } finally {
            setLoading(false);
        }
    };

    const searchGifs = async (query, pos = 0, append = false) => {
        try {
            setLoading(true);
            setError(null);
            
            const response = await apiClient.get(`/api/gifs/search?q=${encodeURIComponent(query)}&limit=20&pos=${pos}`);
            
            if (!response.ok) {
                throw new Error(`Failed to search GIFs: ${response.status} ${response.statusText}`);
            }
            
            const data = await response.json();
            
            if (append) {
                setGifs(prev => [...prev, ...(data.results || [])]);
            } else {
                setGifs(data.results || []);
            }
            
            setNextPos(data.next ? parseInt(data.next) : 0);
            setHasMore(!!data.next);
        } catch (err) {
            setError(err.message);
            console.error('Error searching GIFs:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSearchChange = (e) => {
        const query = e.target.value;
        setSearchQuery(query);
        
        if (searchTimeout) {
            clearTimeout(searchTimeout);
        }
        
        const timeout = setTimeout(() => {
            if (query.trim()) {
                searchGifs(query.trim());
            } else {
                fetchTrendingGifs(); 
            }
        }, 300);
        
        setSearchTimeout(timeout);
    };

    const loadMoreGifs = useCallback(() => {
        if (loading || !hasMore) return;
        
        if (searchQuery.trim()) {
            searchGifs(searchQuery.trim(), nextPos, true);
        } else {
            fetchTrendingGifs(nextPos, true);
        }
    }, [loading, hasMore, searchQuery, nextPos]);

    const handleGifClick = async (gif) => {
        try {
            const gifUrl = gif.media_formats?.gif?.url || gif.media_formats?.tinygif?.url;
            
            if (!gifUrl) {
                alert('Error: No GIF URL found for this GIF. Please try another one.');
                return;
            }
            
            let userId = currentUser?.id || currentUser?.userId || localStorage.getItem('userId');
            
            if (!userId) {
                alert('Error: You must be logged in to send GIFs. Please log in and try again.');
                return;
            }

            const formData = new FormData();
            formData.append('gifUrl', gifUrl);
            formData.append('title', gif.title || 'GIF');
            formData.append('userId', userId.toString());
            
            const response = await apiClient.request('/api/gifs/download', {
                method: 'POST',
                body: formData,
                headers: {}
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Response error:', errorText);
                console.error('Response headers:', response.headers);
                
                if (response.status === 401 || response.status === 403) {
                    alert('Authentication error: Please log out and log back in, then try again.');
                    return;
                } else if (response.status === 400) {
                    alert(`Bad request: ${errorText}. Please try a different GIF.`);
                    return;
                }
                
                throw new Error(`Failed to download GIF: ${response.status} ${response.statusText} - ${errorText}`);
            }

            const fileData = await response.json();

            onGifSelect({
                id: gif.id,
                title: gif.title || 'GIF',
                fileId: fileData.id,
                fileName: fileData.fileName,
                fileUrl: fileData.fileUrl,
                fileSize: fileData.fileSize,
                fileType: fileData.fileType,
                width: gif.media_formats?.gif?.dims?.[0] || 200,
                height: gif.media_formats?.gif?.dims?.[1] || 200
            });
        } catch (error) {
            console.error('Error handling GIF selection:', error);
            alert(`GIF Error: ${error.message}. Please try again.`);
            return;
        }
    };

    const handleScroll = useCallback((e) => {
        const { scrollTop, scrollHeight, clientHeight } = e.target;
        if (scrollHeight - scrollTop <= clientHeight * 1.5) {
            loadMoreGifs();
        }
    }, [loadMoreGifs]);

    return (
        <div className="gif-picker">
            <div className="gif-search-container">
                <input
                    type="text"
                    className="gif-search-input"
                    placeholder="Search GIFs..."
                    value={searchQuery}
                    onChange={handleSearchChange}
                />
            </div>
            
            {error && (
                <div className="gif-error">
                    <div className="error-icon">⚠️</div>
                    <div className="error-message">{error}</div>
                    <button 
                        className="retry-button"
                        onClick={() => {
                            setError(null);
                            if (searchQuery.trim()) {
                                searchGifs(searchQuery.trim());
                            } else {
                                fetchTrendingGifs();
                            }
                        }}
                    >
                        Retry
                    </button>
                </div>
            )}
            
            <div 
                className="gif-grid" 
                ref={gridRef}
                onScroll={handleScroll}
                style={{ display: error ? 'none' : 'grid' }}
            >
                {gifs.map((gif) => (
                    <div
                        key={gif.id}
                        className="gif-item"
                        onClick={() => handleGifClick(gif)}
                        title={gif.title || 'GIF'}
                    >
                        <img
                            src={gif.media_formats?.tinygif?.url || gif.media_formats?.gif?.url}
                            alt={gif.title || 'GIF'}
                            loading="lazy"
                            onError={(e) => {
                                e.target.style.display = 'none';
                            }}
                        />
                    </div>
                ))}
                
                {loading && (
                    <div className="gif-loading">
                        <div className="loading-spinner"></div>
                        <span>Loading GIFs...</span>
                    </div>
                )}
                
                {!loading && gifs.length === 0 && !error && (
                    <div className="gif-empty">
                        {searchQuery ? 'No GIFs found' : 'No trending GIFs available'}
                    </div>
                )}
            </div>
        </div>
    );
}

export default GifPicker;
